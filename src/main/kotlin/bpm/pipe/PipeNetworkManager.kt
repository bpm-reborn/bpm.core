package bpm.pipe

import bpm.mc.block.BasePipeBlock
import bpm.mc.block.EnderControllerBlock
import bpm.mc.block.EnderControllerTileEntity
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents.CUSTOM_NAME
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import bpm.common.logging.KotlinLogging
import bpm.common.network.Listener
import bpm.server.ServerRuntime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object PipeNetworkManager : Listener {

    private val networks = mutableListOf<PipeNetwork>()
    private val pipeTypeCache = ConcurrentHashMap<Class<out BasePipeBlock>, MutableSet<BlockPos>>()
    private val logger = KotlinLogging.logger {}
    private val mappedControllers = ConcurrentHashMap<UUID, MutableSet<Pair<EnderControllerTileEntity, BlockPos>>>()

    private val pendingUpdates = mutableSetOf<PipeNetwork.LevelPipe>()

    private val updateInProgress = AtomicBoolean(false)
    fun onPipeAdded(pipe: BasePipeBlock, level: Level, pos: BlockPos) {
        addToTypeCache(pipe, pos)
        queueNetworkUpdate(level, pos)
        if(pipe is EnderControllerBlock) {
            val controllerTileEntity = level.getBlockEntity(pos) as? EnderControllerTileEntity
            if (controllerTileEntity != null) {
                onControllerPlaced(controllerTileEntity)
            } else {
                logger.warn { "Couldn't add controller at $pos, no tile entity found" }
            }
        }
        logger.info { "Adding pipe at $pos of type ${pipe::class.simpleName}" }
    }


    fun onPipeRemoved(pipe: BasePipeBlock, level: Level, pos: BlockPos) {
        removeFromTypeCache(pipe, pos)
        if (pipe is EnderControllerBlock) {
            val controllerTileEntity = level.getBlockEntity(pos) as? EnderControllerTileEntity
            if (controllerTileEntity != null) {
                onControllerRemoved(controllerTileEntity)
            } else {
                logger.warn { "Couldn't remove controller at $pos, no tile entity found" }
            }
        }
        queueNetworkUpdate(level, pos)
        logger.info { "Removing pipe at $pos of type ${pipe::class.simpleName}" }
    }

    fun queueNetworkUpdate(level: Level, pos: BlockPos) {
        val block = level.getBlockState(pos).block
        if (block !is BasePipeBlock) {
            logger.warn { "Tried to queue network update for non-pipe block at $pos" }
            return
        }
        pendingUpdates.add(PipeNetwork.LevelPipe(level, block, pos))
    }




    override fun onTick(delta: Float, tick: Int) {
        if (updateInProgress.compareAndSet(false, true)) {
            try {
                processPendingUpdates()
            } finally {
                updateInProgress.set(false)
            }
        }
    }

    private fun processPendingUpdates() {
        val updatedPositions = mutableSetOf<BlockPos>()
        val updates = pendingUpdates.toList()
        pendingUpdates.clear()

        for (levelPipe in updates) {
            if (levelPipe.pos in updatedPositions) continue
            updateNetworkAt(levelPipe.level, levelPipe.pos)
            updatedPositions.add(levelPipe.pos)
        }

        cleanupNetworks()
    }

    private fun updateNetworkAt(level: Level, pos: BlockPos) {
        val blockState = level.getBlockState(pos)
        val block = blockState.block

        if (block !is BasePipeBlock) {
            removeFromAllNetworks(level, pos)
            return
        }

        val connectedNetworks = findConnectedNetworks(level, pos)
        when {
            connectedNetworks.isEmpty() -> createNetwork(block, level, pos)
            connectedNetworks.size == 1 -> connectedNetworks.first().addPipe(block, level, pos)
            else -> mergeNetworks(connectedNetworks, block, level, pos)
        }

        if (block is EnderControllerBlock) {
            val entity = level.getBlockEntity(pos)
            if (entity != null) {
                onControllerPlaced(entity as EnderControllerTileEntity)
                logger.info { "Controller placed at $pos" }
            } else {
                logger.warn { "Couldn't add controller at $pos, no tile entity found" }
                //We only know the block position, not the workspace uuid. we need to find the controller and update the mapping

                val controller = locateUuid(level, pos)

                if (controller != null) {
                    val tile = getController(controller)
                    if (tile != null) {
                        mappedControllers[controller]?.add(tile to pos)
                    }
                }
            }
        }
    }

    private fun locateUuid(level: Level, pos: BlockPos): UUID? {
        for((uuid, controller) in mappedControllers)
            if(controller.any { it.second == pos }) return uuid
        return null
    }

    fun getNetworkForPos(level: Level, pos: BlockPos): PipeNetwork? {
        return networks.find { network -> network.contains(level, pos) }
    }


    private fun removeFromAllNetworks(level: Level, pos: BlockPos) {
        networks.forEach { it.removePipe(level, pos) }
    }

    private fun cleanupNetworks() {
        networks.removeAll { it.isEmpty() }
        networks.forEach { it.cleanupGhostPipes() }
    }

    private fun createNetwork(pipe: BasePipeBlock, level: Level, pos: BlockPos): PipeNetwork {
        val network = PipeNetwork()
        network.addPipe(pipe, level, pos)
        networks.add(network)
        logger.info { "Created new network for pipe at $pos" }
        return network
    }

    private fun mergeNetworks(networksToMerge: List<PipeNetwork>, pipe: BasePipeBlock, level: Level, pos: BlockPos) {
        val mergedNetwork = PipeNetwork()
        networksToMerge.forEach { network ->
            network.pipes.forEach { (pipePos, pipeBlock) ->
                mergedNetwork.addPipe(pipeBlock.pipe, level, pipePos)
            }
            networks.remove(network)
        }
        mergedNetwork.addPipe(pipe, level, pos)
        networks.add(mergedNetwork)
        logger.info { "Merged ${networksToMerge.size} networks" }
        //TODO: When networks are merged, if there's multiple controllers, drop all but one, and import it's workspace int our workspace
        //Maybe could do this with a imgui confirmation window or something ?


        //For now, just remove all controllers but the first one
        val controllers = networksToMerge.flatMap { it.pipes.values }.filter { it.pipe is EnderControllerBlock }
        if (controllers.size > 1) {
            val controller = controllers.first() //Safe to keep
            processKeptController(controller)
            controllers.drop(1).forEach { value ->
                dropController(level, value.pos)
            }
        }
    }

    private fun processKeptController(controller: PipeNetwork.LevelPipe) {
        val tileEntity = controller.level.getBlockEntity(controller.pos) as? EnderControllerTileEntity ?: return
        //TODO: Combine the workspaces
//        val uuid = controller.getUUID()
//        ServerRuntime.recompileWorkspace(uuid)
        onControllerPlaced(tileEntity)
    }

    private fun dropController(level: Level, pos: BlockPos) {
        val blockEntity = level.getBlockEntity(pos) as? EnderControllerTileEntity
        val state = level.getBlockState(pos)
        if (blockEntity != null && !level.isClientSide) {
            val stack = ItemStack(state.block)
            val serverLevel = level as net.minecraft.server.level.ServerLevel
            val registryAccess = serverLevel.registryAccess()
            stack.set(
                CUSTOM_NAME,
                Component.literal("Ender Controller").withStyle(ChatFormatting.DARK_PURPLE)
                    .withStyle(ChatFormatting.BOLD)
            )
            blockEntity.saveToItem(stack, registryAccess)
            ItemEntity(level, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, stack).apply {
                setDefaultPickUpDelay()
                level.addFreshEntity(this)
            }
        }
    }


    private fun findConnectedNetworks(level: Level, pos: BlockPos): List<PipeNetwork> {
        return networks.filter { network ->
            Direction.entries.any { direction ->
                network.contains(level, pos.relative(direction))
            }
        }
    }

    private fun onControllerPlaced(entity: EnderControllerTileEntity) {
        val uuid = entity.getUUID()
        mappedControllers.getOrPut(uuid) { mutableSetOf() }.add(entity to entity.blockPos)
        ServerRuntime.recompileWorkspace(uuid)
        logger.info { "Controller placed: $uuid" }
    }

    private fun onControllerRemoved(entity: EnderControllerTileEntity) {
        val uuid = entity.getUUID()
        mappedControllers[uuid]?.clear()
        removeFromAllNetworks(entity.level!!, entity.blockPos)
        ServerRuntime.closeWorkspace(uuid)
        logger.info { "Controller removed: $uuid" }
    }


    private fun addToTypeCache(pipe: BasePipeBlock, pos: BlockPos) {
        pipeTypeCache.getOrPut(pipe::class.java) { ConcurrentHashMap.newKeySet() }.add(pos)
    }

    private fun removeFromTypeCache(pipe: BasePipeBlock, pos: BlockPos) {
        pipeTypeCache[pipe::class.java]?.remove(pos)
    }

    fun hasControllerInNetwork(level: Level, posIn: BlockPos): Boolean {
        val connectedNetworks = findConnectedNetworks(level, posIn)
        return connectedNetworks.any { network ->
            network.pipes.any { (pos, pipe) -> pipe.pipe is EnderControllerBlock && pos != posIn }
        }
    }

    // The uuid sometimes isn't correctly initially set because of how chunk loading and tiles, so we just check once
    // if the uuid is the same as any other controller's uuid, we update the mapping
    private fun patchControllerUuids(uuid: UUID) = mappedControllers.forEach { (key, value) ->
        value.forEach { (controller, _) ->
            if (controller.getUUID() == uuid) {
                mappedControllers.remove(key)
                mappedControllers[uuid] = value
            }
        }
    }

    fun getController(uuid: UUID): EnderControllerTileEntity? {
        val controller =  mappedControllers[uuid]?.firstOrNull()
        if (controller == null) {
            patchControllerUuids(uuid)
            return mappedControllers[uuid]?.firstOrNull()?.first
        }
        return controller.first
    }

    fun getControllers(): List<EnderControllerTileEntity> {
        return mappedControllers.values.flatten().map { it.first }
    }

    fun getControllerPosition(uuid: UUID): BlockPos? {
        val controller =  mappedControllers[uuid]?.firstOrNull()
        if (controller == null) {
            patchControllerUuids(uuid)
            return mappedControllers[uuid]?.firstOrNull()?.second
        }
        return controller.second
    }

    fun getControllerPositions(uuid: UUID): List<BlockPos> {
        return getControllers().map { it.blockPos }
    }

    fun getControllerPositions(): List<BlockPos> {
        return getControllers().map { it.blockPos }
    }


}