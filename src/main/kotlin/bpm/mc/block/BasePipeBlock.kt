package bpm.mc.block

import bpm.pipe.PipeNetworkManager
import bpm.server.ServerRuntime
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.neoforged.neoforge.common.extensions.IBlockExtension

open class BasePipeBlock(properties: Properties) : Block(properties), IBlockExtension {
    companion object {
        val NORTH: BooleanProperty = BooleanProperty.create("north")
        val EAST: BooleanProperty = BooleanProperty.create("east")
        val SOUTH: BooleanProperty = BooleanProperty.create("south")
        val WEST: BooleanProperty = BooleanProperty.create("west")
        val UP: BooleanProperty = BooleanProperty.create("up")
        val DOWN: BooleanProperty = BooleanProperty.create("down")
        val PROXY = BooleanProperty.create("proxy")
        val ALL_STATES = listOf(NORTH, EAST, SOUTH, WEST, UP, DOWN, PROXY)
    }

    init {
        registerDefaultState(stateDefinition.any().apply {
            ALL_STATES.forEach { setValue(it, false) }
        })
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(*ALL_STATES.toTypedArray())
    }

    override fun neighborChanged(
        state: BlockState, level: Level, pos: BlockPos, block: Block, fromPos: BlockPos, isMoving: Boolean
    ) {
        if (!level.isClientSide) {
            val newState = getUpdatedState(level, pos, state)
            if (newState != state) {
                level.setBlock(pos, newState, 3)
                notifyNeighbors(level, pos)
            }
        }
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {

        return getUpdatedState(context.level, context.clickedPos, defaultBlockState())
    }

    override fun setPlacedBy(
        p_49847_: Level,
        p_49848_: BlockPos,
        p_49849_: BlockState,
        p_49850_: LivingEntity?,
        p_49851_: ItemStack
    ) {
        super.setPlacedBy(p_49847_, p_49848_, p_49849_, p_49850_, p_49851_)
        onPipeAdded(p_49847_, p_49848_)

    }

    override fun playerDestroy(
        p_49827_: Level,
        p_49828_: Player,
        p_49829_: BlockPos,
        p_49830_: BlockState,
        p_49831_: BlockEntity?,
        p_49832_: ItemStack
    ) {
        super.playerDestroy(p_49827_, p_49828_, p_49829_, p_49830_, p_49831_, p_49832_)
        PipeNetworkManager.onPipeRemoved(this, p_49827_,p_49829_)

            if(p_49831_ is EnderControllerTileEntity) {
                //TODO: this should be calling some on removed event instead
                ServerRuntime.recompileWorkspace(p_49831_.getUUID())
            }
    }

    open fun canConnectTo(level: Level, pos: BlockPos, direction: Direction): Boolean {
        val neighborState = level.getBlockState(pos.relative(direction))
        return neighborState.block is BasePipeBlock
    }

    protected open fun getUpdatedState(level: Level, pos: BlockPos, currentState: BlockState): BlockState {
        var newState = currentState.setValue(PROXY, false) // Reset proxy state
        var connections = mutableListOf<Direction>()

        for (direction in Direction.values()) {
            val canConnect = canConnectTo(level, pos, direction)
            if (canConnect) {
                connections.add(direction)
            }
            newState = newState.setValue(getPropertyForDirection(direction), false) // Reset all connection states
        }

        when {
            connections.isEmpty() -> {
                // If there are no connections, set proxy to true and UP to true
                newState = newState.setValue(PROXY, true).setValue(UP, true)
            }
            connections.size == 1 -> {
                // If there's only one connection, set the proxy state and the corresponding direction
                newState = newState.setValue(PROXY, true).setValue(getPropertyForDirection(connections[0]), true)
            }
            else -> {
                // Otherwise, set the connection states for all connections
                for (direction in connections) {
                    newState = newState.setValue(getPropertyForDirection(direction), true)
                }
            }
        }

        return newState
    }

    protected open fun notifyNeighbors(level: Level, pos: BlockPos) {
        for (direction in Direction.values()) {
            level.updateNeighborsAt(pos.relative(direction), this)
        }
    }

    protected fun getPropertyForDirection(direction: Direction): BooleanProperty {
        return when (direction) {
            Direction.NORTH -> NORTH
            Direction.EAST -> EAST
            Direction.SOUTH -> SOUTH
            Direction.WEST -> WEST
            Direction.UP -> UP
            Direction.DOWN -> DOWN
        }
    }

    open fun onPipeAdded(level: Level, pos: BlockPos) {
        PipeNetworkManager.onPipeAdded(this, level, pos)
    }

    open fun onPipeRemoved(level: Level, pos: BlockPos) {
        PipeNetworkManager.onPipeRemoved(this, level, pos)
    }

    override fun playerWillDestroy(level: Level, pos: BlockPos, state: BlockState, player: Player): BlockState {
        val blockEntity = level.getBlockEntity(pos) as? EnderControllerTileEntity
        val block = state.block
        if (blockEntity != null && !level.isClientSide && block is BasePipeBlock) {
            PipeNetworkManager.onPipeRemoved(block, level, pos)
//            dropController(level, pos)

            // Remove the direct call to updateNetwork
        }
        return super.playerWillDestroy(level, pos, state, player)
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
//        if (!level.isClientSide) {
//
//        }
    }


    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
//        if (!level.isClientSide && state.block != newState.block) {
//            PipeNetworkManager.onPipeRemoved(this, level, pos)
//        }
        super.onRemove(state, level, pos, newState, isMoving)
    }
}