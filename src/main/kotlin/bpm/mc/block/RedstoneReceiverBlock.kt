package bpm.mc.block

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.neoforged.neoforge.common.extensions.IBlockExtension


class RedstoneReceiverBlock(properties: Properties) : Block(properties), IBlockExtension {
    companion object {
        val FACING = BlockStateProperties.FACING
        val ON = BooleanProperty.create("on")
        val ALL_STATES = listOf(FACING, ON)
    }

    init {
        registerDefaultState(stateDefinition.any().apply {
            setValue(FACING, Direction.NORTH)
            setValue(ON, false)
        })
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(*ALL_STATES.toTypedArray())
    }

    // Shapes for all directions
    private val SHAPE_DOWN: VoxelShape by lazy {
        var shape = Shapes.empty()
        shape = Shapes.join(shape, Shapes.box(0.71875, 0.25, 0.25, 0.75, 0.59375, 0.28125), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.25, 0.25, 0.25, 0.28125, 0.59375, 0.28125), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.28125, 0.59375, 0.25, 0.71875, 0.625, 0.28125), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.28125, 0.59375, 0.71875, 0.71875, 0.625, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.25, 0.59375, 0.25, 0.28125, 0.625, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.71875, 0.59375, 0.25, 0.75, 0.625, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.71875, 0.59375, 0.25, 0.75, 0.625, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.25, 0.25, 0.71875, 0.28125, 0.59375, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.71875, 0.25, 0.71875, 0.75, 0.59375, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.28125, 0.25, 0.28125, 0.71875, 0.625, 0.71875), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.390625, 0.25, 0.5, 0.609375, 0.5625, 0.5), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.5, 0.25, 0.390625, 0.5, 0.5625, 0.609375), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.0625, 0.0625, 0.9375, 0.25, 0.9375), BooleanOp.OR)
        shape =
            Shapes.join(shape, Shapes.box(0.121875, -0.065625, 0.121875, 0.878125, 0.065625, 0.878125), BooleanOp.OR)
        shape
    }

    private val SHAPE_UP: VoxelShape by lazy {
        var shape = Shapes.empty()
        shape = Shapes.join(shape, Shapes.box(0.71875, 0.40625, 0.25, 0.75, 0.75, 0.28125), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.25, 0.40625, 0.25, 0.28125, 0.75, 0.28125), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.28125, 0.375, 0.25, 0.71875, 0.40625, 0.28125), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.28125, 0.375, 0.71875, 0.71875, 0.40625, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.25, 0.375, 0.25, 0.28125, 0.40625, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.71875, 0.375, 0.25, 0.75, 0.40625, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.25, 0.40625, 0.71875, 0.28125, 0.75, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.71875, 0.40625, 0.71875, 0.75, 0.75, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.28125, 0.375, 0.28125, 0.71875, 0.75, 0.71875), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.390625, 0.4375, 0.5, 0.609375, 0.75, 0.5), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.5, 0.4375, 0.390625, 0.5, 0.75, 0.609375), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.75, 0.0625, 0.9375, 0.9375, 0.9375), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.121875, 0.9375, 0.121875, 0.878125, 1.065625, 0.878125), BooleanOp.OR)
        shape
    }

    private val SHAPE_SOUTH: VoxelShape by lazy {
        var shape = Shapes.empty()
        shape = Shapes.join(shape, Shapes.box(0.71875, 0.25, 0.40625, 0.75, 0.28125, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.25, 0.25, 0.40625, 0.28125, 0.28125, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.28125, 0.25, 0.375, 0.71875, 0.28125, 0.40625), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.25, 0.71875, 0.375, 0.28125, 0.75, 0.40625), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.25, 0.28125, 0.375, 0.28125, 0.71875, 0.40625), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.71875, 0.28125, 0.375, 0.75, 0.71875, 0.40625), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.71875, 0.71875, 0.375, 0.75, 0.75, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.28125, 0.71875, 0.375, 0.71875, 0.75, 0.40625), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.28125, 0.28125, 0.375, 0.71875, 0.71875, 0.71875), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.390625, 0.5, 0.4375, 0.609375, 0.5, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.5, 0.390625, 0.4375, 0.5, 0.609375, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.0625, 0.75, 0.9375, 0.9375, 0.9375), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.121875, 0.121875, 0.9375, 0.878125, 0.878125, 1.065625), BooleanOp.OR)
        shape
    }

    private val SHAPE_NORTH: VoxelShape by lazy {
        var shape = Shapes.empty()
        shape = Shapes.join(shape, Shapes.box(0.25, 0.25, 0.25, 0.28125, 0.28125, 0.59375), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.71875, 0.25, 0.25, 0.75, 0.28125, 0.59375), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.28125, 0.25, 0.59375, 0.71875, 0.28125, 0.625), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.71875, 0.71875, 0.25, 0.75, 0.75, 0.625), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.71875, 0.28125, 0.59375, 0.75, 0.71875, 0.625), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.25, 0.28125, 0.59375, 0.28125, 0.71875, 0.625), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.25, 0.71875, 0.25, 0.28125, 0.75, 0.625), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.28125, 0.71875, 0.59375, 0.71875, 0.75, 0.625), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.28125, 0.28125, 0.28125, 0.71875, 0.71875, 0.625), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.390625, 0.5, 0.25, 0.609375, 0.5, 0.5625), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.5, 0.390625, 0.25, 0.5, 0.609375, 0.5625), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.0625, 0.0625, 0.9375, 0.9375, 0.25), BooleanOp.OR)
        shape =
            Shapes.join(shape, Shapes.box(0.121875, 0.121875, -0.065625, 0.878125, 0.878125, 0.065625), BooleanOp.OR)
        shape
    }

    private val SHAPE_WEST: VoxelShape by lazy {
        var shape = Shapes.empty()
        shape = Shapes.join(shape, Shapes.box(0.25, 0.25, 0.25, 0.59375, 0.28125, 0.28125), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.25, 0.25, 0.71875, 0.59375, 0.28125, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.59375, 0.25, 0.28125, 0.625, 0.28125, 0.71875), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.25, 0.71875, 0.71875, 0.625, 0.75, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.59375, 0.28125, 0.71875, 0.625, 0.71875, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.59375, 0.28125, 0.25, 0.625, 0.71875, 0.28125), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.25, 0.71875, 0.25, 0.625, 0.75, 0.28125), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.59375, 0.71875, 0.28125, 0.625, 0.75, 0.71875), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.28125, 0.28125, 0.28125, 0.625, 0.71875, 0.71875), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.25, 0.5, 0.390625, 0.5625, 0.5, 0.609375), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.25, 0.390625, 0.5, 0.5625, 0.609375, 0.5), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.0625, 0.0625, 0.25, 0.9375, 0.9375), BooleanOp.OR)
        shape =
            Shapes.join(shape, Shapes.box(-0.065625, 0.121875, 0.121875, 0.065625, 0.878125, 0.878125), BooleanOp.OR)
        shape
    }

    private val SHAPE_EAST: VoxelShape by lazy {
        var shape = Shapes.empty()
        shape = Shapes.join(shape, Shapes.box(0.40625, 0.25, 0.71875, 0.75, 0.28125, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.40625, 0.25, 0.25, 0.75, 0.28125, 0.28125), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.375, 0.25, 0.28125, 0.40625, 0.28125, 0.71875), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.375, 0.71875, 0.25, 0.75, 0.75, 0.28125), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.375, 0.28125, 0.25, 0.40625, 0.71875, 0.28125), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.375, 0.28125, 0.71875, 0.40625, 0.71875, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.375, 0.71875, 0.71875, 0.75, 0.75, 0.75), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.375, 0.71875, 0.28125, 0.40625, 0.75, 0.71875), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.375, 0.28125, 0.28125, 0.71875, 0.71875, 0.71875), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.4375, 0.5, 0.390625, 0.75, 0.5, 0.609375), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.4375, 0.390625, 0.5, 0.75, 0.609375, 0.5), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.75, 0.0625, 0.0625, 0.9375, 0.9375, 0.9375), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.9375, 0.121875, 0.121875, 1.065625, 0.878125, 0.878125), BooleanOp.OR)
        shape
    }

    // Get shape based on facing direction
    override fun getShape(
        state: BlockState,
        p_60556_: BlockGetter,
        pos: BlockPos,
        p_60558_: CollisionContext
    ): VoxelShape {
        return when (state.getValue(FACING)) {
            Direction.DOWN -> SHAPE_DOWN
            Direction.UP -> SHAPE_UP
            Direction.NORTH -> SHAPE_NORTH
            Direction.SOUTH -> SHAPE_SOUTH
            Direction.EAST -> SHAPE_EAST
            Direction.WEST -> SHAPE_WEST
            else -> SHAPE_NORTH
        }
    }

    override fun getLightEmission(state: BlockState, level: BlockGetter, pos: BlockPos): Int {
        return if (state.getValue(ON)) 15 else 0
    }

    override fun animateTick(state: BlockState, level: Level, pos: BlockPos, random: RandomSource) {
        if (!state.getValue(ON)) return
        if (random.nextInt(100) > 50) return

        // Get the center position based on facing direction
        val facing = state.getValue(FACING)
        val x = pos.x.toDouble() + 0.5
        val y = pos.y.toDouble() + 0.5
        val z = pos.z.toDouble() + 0.5

        // Normalize between -0.25 and 0.25
        val offset = random.nextDouble() * 0.5 - 0.37
        val offsetX = when (facing) {
            Direction.EAST -> -offset
            Direction.WEST -> offset
            else -> 0.0
        }
        val offsetY = when (facing) {
            Direction.UP -> -offset
            Direction.DOWN -> offset
            else -> 0.0
        }
        val offsetZ = when (facing) {
            Direction.SOUTH -> -offset
            Direction.NORTH -> offset
            else -> 0.0
        }

        level.addParticle(
            DustParticleOptions.REDSTONE,
            x + offsetX,
            y + offsetY,
            z + offsetZ,
            0.0,
            0.0,
            0.0
        )
    }

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        context: BlockHitResult
    ): InteractionResult {
        if (level.isClientSide) return InteractionResult.PASS
        if (!player.isShiftKeyDown) return super.useWithoutItem(state, level, pos, player, context)

        // Update state to on or off
        val newState = state.cycle(ON)
        // Update the block state in the world
        level.setBlock(pos, newState, 3)
        return InteractionResult.SUCCESS
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        // Get the face the player clicked on (opposite to the direction the block will face)
        val direction = context.clickedFace.opposite
        val item = context.itemInHand
        //TODO: check if item has been linked to a controller

        return defaultBlockState()
            .setValue(FACING, direction)
            .setValue(ON, false)
    }
}