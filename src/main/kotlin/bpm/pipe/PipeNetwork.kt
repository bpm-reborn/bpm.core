package bpm.pipe


import bpm.common.bootstrap.BpmIO
import bpm.common.logging.KotlinLogging
import bpm.common.network.Client
import bpm.common.network.Listener
import bpm.common.network.Server
import bpm.common.packets.Packet
import bpm.common.serial.Serial
import bpm.mc.block.BasePipeBlock
import bpm.mc.block.EnderControllerBlock
import bpm.mc.block.EnderControllerTileEntity
import bpm.mc.block.EnderProxyBlock
import bpm.network.level
import bpm.pipe.proxy.*
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.capabilities.Capabilities
import thedarkcolour.kotlinforforge.neoforge.kotlin.enumMapOf
import java.nio.file.Path
import java.util.UUID

object PipeNetwork {

    private val logger = KotlinLogging.logger {}
    private var state = PipeNetManagerState()
    // Path to the directory where the pipe network state is saved, initialized in setupPipeDirectory
    private val pipeDir: Path = setupPipeDirectory()


    fun load(level: ServerLevel) {
        ProxyManagerServer.clear()
        state = BpmIO.loadPipeNetState(level) ?: return
        val worldPipes = state.networks.flatMap { it.value.pipes.values }.filter {
            it.level == level.dimension()
        } //The pipes for this world
        worldPipes.forEach { (levelKey, worldPos, relPos, type) ->
            val block = level.getBlockState(worldPos).block
            if (block is BasePipeBlock && block !is EnderControllerBlock) {
                onPipeAdded(block, level, worldPos)
            }
        }
    }

    fun save(serverLevel: ServerLevel) = BpmIO.savePipeNetState(serverLevel, state)

    fun hasControllerInNetwork(level: Level, posIn: BlockPos): Boolean {
        val connectedNetworks = state.blockPosToNetwork[posIn]?.let { state.networks[it] } ?: return false
        return connectedNetworks.pipes.any { (_, trackedPipe) ->
            trackedPipe.type == EnderControllerBlock::class.java
        }
    }

    fun onPipeAdded(pipe: BasePipeBlock, level: Level, pos: BlockPos) {
        val connectedNetworks = findConnectedNetworks(level, pos)
        when {
            connectedNetworks.isEmpty() -> createNetwork(pipe, level, pos)
            connectedNetworks.size == 1 -> connectedNetworks.first().addPipe(pipe.javaClass, level, pos)
            else -> mergeNetworks(connectedNetworks, pipe, level, pos)
        }


        state.blockPosToNetwork[pos] = state.networks.entries.find { it.value.pipes.containsKey(pos) }?.key ?: return

        if (pipe is EnderControllerBlock) {
            val entity = level.getBlockEntity(pos) as? EnderControllerTileEntity
            if (entity != null) {
                onControllerPlaced(entity)
            } else {
                logger.warn { "Couldn't add controller at $pos, no tile entity found" }
            }
        } else if (pipe is EnderProxyBlock) {
            if (!ProxyManagerServer.contains(pos)) {
                val proxiableBlocks = findProxiableBlocksInRadius(level, pos, 5).map {
                    it to ProxiedState(
                        relativePos = it.subtract(pos), proxiedFaces = enumMapOf(
                            Direction.NORTH to ProxiedType.NONE,
                            Direction.EAST to ProxiedType.NONE,
                            Direction.SOUTH to ProxiedType.NONE,
                            Direction.WEST to ProxiedType.NONE,
                            Direction.UP to ProxiedType.NONE,
                            Direction.DOWN to ProxiedType.NONE
                        )
                    )
                }.toMap()

                ProxyManagerServer[pos] = ProxyState(
                    origin = pos, proxiedBlocks = proxiableBlocks.toMutableMap()
                )
                logger.debug("Added proxy at $pos")
            }
        }

        logger.info { "Added pipe at $pos of type ${pipe::class.simpleName}" }
    }


    private fun canProxyBlockConnectTo(level: Level, pos: BlockPos, direction: Direction): Boolean {
        val blockEntity = level.getBlockEntity(pos)
        return blockEntity != null && (hasItemHandlerCapability(level, pos, direction) || hasFluidHandlerCapability(
            level, pos, direction
        ))
    }

    private fun hasItemHandlerCapability(level: Level, pos: BlockPos, side: Direction): Boolean {
        val maybeHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side)
        return maybeHandler != null
    }

    private fun hasFluidHandlerCapability(level: Level, pos: BlockPos, side: Direction): Boolean {
        val maybeHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side)
        return maybeHandler != null
    }

    private fun findProxiableBlocksInRadius(level: Level, pos: BlockPos, radius: Int): List<BlockPos> {
        val buffer = mutableListOf<BlockPos>()
        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
                    val currentPos = pos.offset(x, y, z)
                    Direction.entries.forEach { direction ->
                        if (canProxyBlockConnectTo(level, currentPos, direction)) {
                            buffer.add(currentPos)
                        }
                    }
                }
            }
        }
        logger.debug("Found ${buffer.size} proxiable blocks in radius $radius")
        return buffer
    }


    fun onPipeRemoved(pipe: BasePipeBlock, level: Level, pos: BlockPos) {
        state.blockPosToNetwork.remove(pos)
        val networkId = state.blockPosToNetwork[pos] ?: return
        val network = state.networks[networkId] ?: return

        network.removePipe(level, pos)
        if (network.isEmpty()) {
            state.networks.remove(networkId)
        } else {
            if (pipe is EnderControllerBlock) {
                val entity = level.getBlockEntity(pos) as? EnderControllerTileEntity
                if (entity != null) {
                    Server.send(PacketProxyRemoveWorkspace(entity.getUUID()))
                    onControllerRemoved(entity)
                }
            } else if (pipe is EnderProxyBlock) {
                ProxyManagerServer.remove(pos)
                Server.send(PacketRemoveProxy(pos))
            }
            val splitNetworks = network.split(level, pos)
            if (splitNetworks.size > 1) {
                val newNetworks = state.networks
                val newBlockPosToNetwork = state.blockPosToNetwork
                newNetworks.remove(networkId)
                splitNetworks.forEach { newNetwork ->
                    val newNetworkId = UUID.randomUUID()
                    newNetworks[newNetworkId] = newNetwork
                    newNetwork.pipes.keys.forEach { newBlockPosToNetwork[it] = newNetworkId }
                }
            }
        }

        logger.info { "Removed pipe at $pos of type ${pipe::class.simpleName}" }
    }

    private fun createNetwork(pipe: BasePipeBlock, level: Level, pos: BlockPos): PipeNet {
        val network = PipeNet()
        val networkId = UUID.randomUUID()
        network.addPipe(pipe::class.java, level, pos)
        state.networks[networkId] = network
        state.blockPosToNetwork[pos] = networkId
        logger.info { "Created new network for pipe at $pos" }
        return network
    }

    private fun mergeNetworks(networksToMerge: List<PipeNet>, pipe: BasePipeBlock, level: Level, pos: BlockPos) {
        val mergedNetwork = PipeNet()
        val newNetworkId = UUID.randomUUID()

        val newNetworks = state.networks
        val newBlockPosToNetwork = state.blockPosToNetwork

        networksToMerge.forEach { network ->
            network.pipes.forEach { (pipePos, trackedPipe) ->
                mergedNetwork.addPipe(trackedPipe.type, level, pipePos)
                newBlockPosToNetwork[pipePos] = newNetworkId
            }
            newNetworks.values.remove(network)
        }

        mergedNetwork.addPipe(pipe.javaClass, level, pos)
        newBlockPosToNetwork[pos] = newNetworkId
        newNetworks[newNetworkId] = mergedNetwork



        logger.info { "Merged ${networksToMerge.size} networks" }

        // Handle multiple controllers in merged network
        val controllers = mergedNetwork.pipes.values.filter { it.type == EnderControllerBlock::class.java }
        if (controllers.size > 1) {
            controllers.drop(1).forEach { controller ->
                dropController(level, controller.worldPos)
            }
        }
    }

    private fun findConnectedNetworks(level: Level, pos: BlockPos): List<PipeNet> {
        return Direction.entries.mapNotNull { direction -> state.blockPosToNetwork[pos.relative(direction)] }.distinct()
            .mapNotNull { state.networks[it] }
    }

    fun onControllerPlaced(entity: EnderControllerTileEntity) {
        val controllerUuid = entity.getUUID()
        val networkId = state.blockPosToNetwork[entity.blockPos] ?: return
        state.controllerToNetwork[controllerUuid] = networkId
        logger.info { "Controller placed: $controllerUuid" }
    }

    private fun onControllerRemoved(entity: EnderControllerTileEntity) {
        val controllerUuid = entity.getUUID()
        state.controllerToNetwork.remove(controllerUuid)
        logger.info { "Controller removed: $controllerUuid" }
    }

    private fun dropController(level: Level, pos: BlockPos) {
        // Implementation for dropping controller item
        // This would be similar to your original implementation
    }

    fun getNetworkForPos(level: Level, pos: BlockPos): PipeNet? {
        val networkId = state.blockPosToNetwork[pos] ?: return null
        return state.networks[networkId]
    }

    fun getController(uuid: UUID): EnderControllerTileEntity? {
        val networkId = state.controllerToNetwork[uuid] ?: return null
        val network = state.networks[networkId] ?: return null
        val controllerPos = network.pipes.entries.find { it.value.type == EnderControllerBlock::class.java }?.key
            ?: return null
        val level = (network.pipes[controllerPos]?.level
            ?: Level.OVERWORLD).let { ResourceKey.create(Registries.DIMENSION, it.location()) }
        val serverLevel = level.level
        return serverLevel.getBlockEntity(controllerPos) as? EnderControllerTileEntity
    }

    fun getWorldForController(uuid: UUID): ServerLevel? {
        val networkId = state.controllerToNetwork[uuid] ?: return null
        val network = state.networks[networkId] ?: return null
        val controllerPos = network.pipes.entries.find { it.value.type == EnderControllerBlock::class.java }?.key
            ?: return null
        val level = (network.pipes[controllerPos]?.level
            ?: Level.OVERWORLD).let { ResourceKey.create(Registries.DIMENSION, it.location()) }
        return level.level
    }

    fun getControllers(): List<EnderControllerTileEntity> {
        return state.controllerToNetwork.keys.mapNotNull { getController(it) }
    }

    fun getControllerPosition(uuid: UUID): BlockPos? {
        val networkId = state.controllerToNetwork[uuid] ?: return null
        val network = state.networks[networkId] ?: return null
        val controllerPos = network.pipes.entries.find { it.value.type == EnderControllerBlock::class.java }?.key
            ?: return null
        return controllerPos
    }

    fun getNetwork(uuid: UUID): PipeNet? {
        val networkId = state.controllerToNetwork[uuid] ?: return null
        return state.networks[networkId]
    }


    fun getControllerPositions(): List<BlockPos> {
        return getControllers().mapNotNull { it.blockPos }
    }


    private fun setupPipeDirectory(): Path {
        val gameDir = FMLPaths.GAMEDIR.get()

        val path = gameDir.resolve("meng").resolve("pipe_networks")
        if (!path.toFile().exists()) {
            path.toFile().mkdirs()
        }
        return path
    }

    @OnlyIn(Dist.CLIENT)
    object ProxyManagerClient : Listener {

        private val workspaceToProxies = mutableMapOf<UUID, List<ProxyState>>() //The proxies for the workspace
        private val proxies = mutableMapOf<BlockPos, ProxyState>() //The proxies for the current workspace

        fun getProxies(): List<ProxyState> = workspaceToProxies.values.flatten()
        fun getProxies(uuid: UUID): List<ProxyState> = workspaceToProxies[uuid] ?: emptyList()
        fun getProxy(pos: BlockPos): ProxyState? = proxies[pos]
        fun getProxiesForWorkspace(uuid: UUID): List<ProxyState> = workspaceToProxies[uuid] ?: emptyList()
        fun clear() {
            workspaceToProxies.clear()
            proxies.clear()
        }

        fun remove(pos: BlockPos) = proxies.remove(pos)

        fun removeProxiesForWorkspace(uuid: UUID) {
            workspaceToProxies.remove(uuid)
        }


        fun requestProxies(uuid: UUID) = Client.send(PacketProxiedStatesRequest(uuid))

        override fun onPacket(packet: Packet, from: UUID) {
            if (packet is PacketProxiedStatesResponse) {
                workspaceToProxies[packet.workspace] = packet.proxiedStates
                packet.proxiedStates.forEach { state ->
                    state.proxiedBlocks.keys.forEach { pos ->
                        proxies[pos] = state
                    }
                }
            }

            if (packet is PacketProxyRemoveWorkspace) {
                removeProxiesForWorkspace(packet.workspace)
            }

            if (packet is PacketRemoveProxy) {
                remove(packet.proxyOrigin)
            }
        }
    }

    object ProxyManagerServer : Listener {

        private val proxies get() = state.proxies
        fun remove(pos: BlockPos) = state.proxies.remove(pos)
        fun clear() = proxies.clear()
        fun isEmpty(): Boolean = proxies.isEmpty()
        fun forEach(action: (BlockPos, ProxyState) -> Unit) = proxies.forEach(action)
        operator fun set(pos: BlockPos, state: ProxyState) = let { proxies[pos] = state }
        operator fun get(pos: BlockPos): ProxyState? = proxies[pos]
        operator fun contains(pos: BlockPos): Boolean = proxies.containsKey(pos)

        val keys: Set<BlockPos> get() = proxies.keys.toSet() //Creates a new set to prevent modification
        val values: List<ProxyState> get() = proxies.values.toList() //Creates a new list to prevent modification
        val size: Int get() = proxies.size

        /**
         * Gets the proxies for the given controller on the server
         */
        fun getProxies(uuid: UUID): List<ProxyState> {
            val networkId = state.controllerToNetwork[uuid] ?: return let {
                logger.warn { "No network found for controller $uuid" }
                emptyList()
            }
            val network = state.networks[networkId] ?: return let {
                logger.warn { "No network found for controller $uuid" }
                emptyList()
            }

            val states = network.pipes.values.filter { it.type == EnderProxyBlock::class.java }
                .mapNotNull { state.proxies[it.worldPos] }
            return states
        }

        override fun onPacket(packet: Packet, from: UUID) {
            if (packet is PacketProxyUpdate) {
                val proxy = this[packet.proxyOrigin] ?: return
                val proxyOrigin = packet.proxyOrigin
                val absoluteProxiedState = packet.proxiedState
                absoluteProxiedState.absolutePos = proxyOrigin.offset(absoluteProxiedState.relativePos)
                //Filter out any pro
                proxy[absoluteProxiedState.absolutePos] = absoluteProxiedState
            } else if (packet is PacketProxyRequest) {
                val proxy = this[packet.proxyOrigin] ?: return
                //Filter out any that have all none for the proxied type
                val blocks = proxy.proxiedBlocks.filter { it.key != packet.proxyOrigin }
                proxy.proxiedBlocks.clear()
                proxy.proxiedBlocks.putAll(blocks)
                Server.send(PacketProxyResponse(packet.proxyOrigin, proxy), from)
            } else if (packet is PacketProxiedStatesRequest) {
                val uuid = packet.workspaceUid
                //Find all the proxies states assosiated with the workspace
                val proxies = getProxies(uuid)
                //Send the proxies states to the client
                Server.send(PacketProxiedStatesResponse(uuid, proxies.toMutableList()), from)
            }
        }
    }

}