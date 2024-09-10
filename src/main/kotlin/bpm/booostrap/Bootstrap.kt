package bpm.booostrap

import bpm.Bpm
import bpm.Bpm.LOGGER
import bpm.mc.visual.Overlay2D
import bpm.mc.visual.Overlay3D
import bpm.network.MinecraftNetworkAdapter
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import bpm.client.runtime.ClientRuntime
import bpm.client.runtime.windows.CanvasContext
import bpm.common.logging.KotlinLogging
import bpm.common.network.Client
import bpm.common.network.Endpoint
import bpm.common.network.Network
import bpm.common.network.Server
import bpm.common.packets.Packet
import bpm.common.schemas.Schemas
import bpm.common.serial.Serial
import bpm.common.serial.Serialize
import bpm.common.utils.*
import bpm.mc.visual.CustomBackgroundRenderer
import bpm.mc.visual.ProxyScreen
import bpm.pipe.PipeNetManager
import bpm.server.lua.LuaBuiltin
import bpm.pipe.proxy.ProxyManager
import bpm.server.ServerRuntime
import bpm.server.lua.LuaEventExecutor
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.neoforged.neoforge.client.event.*
import net.neoforged.neoforge.event.tick.ServerTickEvent
import org.apache.logging.log4j.Level
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

class Bootstrap(
    private val results: ClassResourceScanner.ScanResults
) : IBoostrap {

    private val logger = KotlinLogging.logger {}
    private val registriesList = mutableListOf<ModRegistry<*>>()
    private val packetsList = mutableListOf<KClass<out Packet>>()
    private val serializableList = mutableListOf<KClass<out Serialize<*>>>()
    private val builtIns = mutableListOf<LuaBuiltin>()
    private val ourResults = results.fromPackages("noderspace", "bpm")
    private val isRunning get() = Client.isRunning()
    /**
     * The entry point for the bootstrap.
     */
    override fun collect(): Bootstrap {
        logger.info("Boostrappin' BPM")
        collectRegistries()
        collectPackets()
        collectSerializable()
        collectLuaBuiltIns()
        logger.info("Bootstrapped ${registriesList.size} registries\n, ${packetsList.size} packets, ${serializableList.size} serializable, and ${builtIns.size} builtins")
        return this
    }

    //Registers all of the serializerables and packet handlers
    override fun register(bus: IEventBus): IBoostrap {
        registerRegistries(bus)
        registerModSpecificEvents(bus)
        logger.info("Registering serializers")
        registerSerializers()
        logger.info("Registered packets")
        registerPackets()
        return this
    }

    private fun registerModSpecificEvents(modBus: IEventBus) {
        modBus.addListener(::onRegisterPayloads)
        runForDist(clientTarget = {
            FORGE_BUS.addListener(::onClientPlayerLogin)
            FORGE_BUS.addListener(::onClientPlayerLogout)
            modBus.addListener(::onClientSetup)
            modBus.addListener(::onRegisterClientReloadListeners)
            FORGE_BUS.addListener(::renderOverlay2D)
            FORGE_BUS.addListener(::renderOverlay3D)

            modBus.addListener(::registerShaders)
            Minecraft.getInstance()
        }, serverTarget = {

            "server"
        })
        FORGE_BUS.addListener(::onServerTick)
        FORGE_BUS.addListener(::onLevelSave)
        FORGE_BUS.addListener(::onLevelLoad)
        //Server setup, should be done on client too for single player
        modBus.addListener(::onCommonSetup)
    }

    private fun onRegisterPayloads(event: RegisterPayloadHandlersEvent) {
        MinecraftNetworkAdapter.registerPayloads(event)
    }

    private fun registerRegistries(bus: IEventBus) {
        registriesList.forEach { it.register(bus) }
    }

    private fun registerSerializers() {

        val serializers = serializableList.associateWith { it.objectInstance ?: it.createInstance() }

        serializers.forEach { (kClass, instance) ->
            instance.register()
            logger.info("Registered serializer for ${kClass.simpleName}")
        }
    }

    private fun registerPackets() = packetsList.forEach(Network::register)


    private fun onRegisterClientReloadListeners(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener { pPreparationBarrier, _, _, _, pBackgroundExecutor, pGameExecutor ->
            CompletableFuture.runAsync({}, pBackgroundExecutor).thenCompose { pPreparationBarrier.wait(null) }
                .thenAcceptAsync({
                    LOGGER.log(Level.INFO, "Initializing EditorContext...")
                    if (!isRunning) ClientRuntime.start(Minecraft.getInstance().window.window)
                }, pGameExecutor)
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun renderOverlay2D(event: RenderGuiEvent.Pre) {
        if (Overlay2D.skipped) return
        ClientRuntime.newFrame()
        Overlay2D.render()
        ClientRuntime.endFrame()
    }

    @OnlyIn(Dist.CLIENT)
    private fun renderOverlay3D(event: RenderLevelStageEvent) =
        if (event.stage == RenderLevelStageEvent.Stage.AFTER_LEVEL) Overlay3D.render(
            event.levelRenderer,
            event.poseStack,
            event.projectionMatrix,
            event.modelViewMatrix,
            event.camera,
            event.frustum
        )
        else Unit

    @OnlyIn(Dist.CLIENT)
    private fun onClientPlayerLogin(event: ClientPlayerNetworkEvent.LoggingIn) {
        LOGGER.log(Level.INFO, "Client player logging in: ${event.player?.name}")
        ClientRuntime.connect()
    }

    //Cutout rendering

    @OnlyIn(Dist.CLIENT)
    private fun onClientPlayerLogout(event: ClientPlayerNetworkEvent.LoggingOut) {
        ClientRuntime.disconnect()
    }


    private fun onClientSetup(event: FMLClientSetupEvent) {
        LOGGER.log(Level.INFO, "Server client...")
        val gameDir = FMLPaths.GAMEDIR.get()

        val schemaPath = gameDir.resolve("schemas")
        if (!schemaPath.toFile().exists()) {
            schemaPath.toFile().mkdir()
        }

        Client.install(ClientRuntime)
            .install<Schemas>(schemaPath, Endpoint.Side.CLIENT)
            .install<CanvasContext>()
            .install<ProxyScreen>()
            .install<Overlay2D>()
    }

    fun getBuiltIns(): List<LuaBuiltin> {
        return builtIns
    }

    //Copies the schemas from the jar to the game directory
    private fun copySchemas() {
        val gameDir = FMLPaths.GAMEDIR.get()
        val schemaPath = gameDir.resolve("schemas")
        if (!schemaPath.toFile().exists()) {
            schemaPath.toFile().mkdir()
        }
        val resources = results.getResources("schemas")
        val mapped = resources.readResourcesToByteArrayMap().map {
            val path = it.value.resourceInfo.path
            val bytes = it.value
            val name = path.toString().substringAfter("schemas/")
            name to bytes
        }
        for ((name, resource) in mapped) {
            val bytes = resource
            val file = schemaPath.resolve(name).toFile()
            //create containing directories if they don't exist from the path to the file
            file.parentFile.mkdirs()

            //Always write when in dev mode, otherwise only write if the file doesn't exist
//            if (!file.toFile().exists() || !results.isProduction) {
            file.writeBytes(bytes.content)
//            }
        }
        //Collects the schemas from the jar

//            val file = schemaPath.resolve(name)
        //Always write when in dev mode, otherwise only write if the file doesn't exist
//            if (!file.toFile().exists() || !results.isProduction) {
//                file.toFile().writeBytes(bytes)
//            }


    }
    @OnlyIn(Dist.CLIENT)
    private fun registerShaders(event: RegisterShadersEvent) {
        try {
            event.registerShader(
                ShaderInstance(
                    event.resourceProvider,
                    ResourceLocation.tryParse("${Bpm.ID}:custom_background"),
                    DefaultVertexFormat.POSITION
                )
            ) { shaderInstance ->
                CustomBackgroundRenderer.backgroundShader = shaderInstance
            }
        } catch (e: IOException) {
            Bpm.LOGGER.error("Failed to register shaders", e)
        }
    }

    private fun onServerTick(event: ServerTickEvent.Pre) {
        LuaEventExecutor.onTick()
    }

    private fun onCommonSetup(event: FMLCommonSetupEvent) {
        logger.info("Copying schemas to game directory")
        copySchemas()
        LOGGER.log(Level.INFO, "Scanning for classes...")
        val gameDir = FMLPaths.GAMEDIR.get()
        val schemasPath = gameDir.resolve("schemas")
//        val schemasPath = Path.of("U:\\Dev\\minecraft\\modding\\mods\\bpm\\src\\main\\resources\\schemas")//gameDir.resolve("schemas")
        if (!schemasPath.toFile().exists()) {
            schemasPath.toFile().mkdir()
        }

        //We initialize this here because it's available on the client too for single player
        Server
            .install<ServerRuntime>()
            .install<Schemas>(schemasPath, Endpoint.Side.SERVER)
            .install<ProxyManager>()
            .start()
    }

    private fun onLevelSave(event: net.neoforged.neoforge.event.level.LevelEvent.Save) {
        val level = event.level
        if (level !is ServerLevel) return
        PipeNetManager.save(level)
    }

    private fun onLevelLoad(event: net.neoforged.neoforge.event.level.LevelEvent.Load) {
        val level = event.level
        if (level !is ServerLevel) return
        PipeNetManager.load(level)
    }


    //Tries to find the constructor that takes no arguments, if it fails, it will try to find the first constructor
    //that takes the booststrap instance as an argument, then it will call it with our instance.
    private fun createOrGetModRegistryInstance(clazz: KClass<*>): ModRegistry<*> {
        val result = try {
            clazz.objectInstance ?: clazz.constructors.first().call()
        } catch (e: Exception) {
            clazz.constructors.first { it.parameters.size == 1 && it.parameters[0].type.classifier == Bootstrap::class }
                .call(this)
        }
        if (result is ModRegistry<*>) {
            return result
        }
        throw IllegalStateException("Class $clazz is not a ModRegistry")
    }

    private fun collectLuaBuiltIns() {
        ourResults.classesImplementing<LuaBuiltin>().forEach {
            builtIns.add(it.objectInstance ?: it.createInstance())
        }
        logger.info("Collected builtins:\n\t(\n\t\t${builtIns.map { it.simpleClassName }.joinToString { "\n" }}\n\t) ")
    }

    private fun collectRegistries() {
        logger.info("Collecting registries")
        val modRegistries = ourResults.classesImplementing<ModRegistry<*>>()
            .map { createOrGetModRegistryInstance(it) }

        registriesList.addAll(modRegistries)

        logger.info("Collected ${modRegistries.size} registries")
    }

    private fun collectPackets() {
        logger.info("Collecting packets")
        val packets = ourResults.classesImplementing<Packet>()
        packetsList.addAll(packets)
        logger.info("Collected ${packets.size} packets")
    }

    private fun collectSerializable() {
        logger.info("Collecting serializable")
        val serializable = ourResults.classesImplementing<Serialize<*>>()
        serializableList.addAll(serializable)
        logger.info("Collected ${serializable.size} serializable")
    }

    override val registries: List<ModRegistry<*>> = registriesList
    override val packets: List<KClass<out Packet>> = packetsList
    override val serializable: List<KClass<out Serialize<*>>> = serializableList

}

