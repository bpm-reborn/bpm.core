package bpm.mc.block

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape


class EnderProxyBlock(properties: Properties) : BasePipeBlock(properties) {

    private val shapeCache = mutableMapOf<Int, VoxelShape>()

    private val visualShapeCache = mutableMapOf<Int, VoxelShape>()
    private val collisionShapeCache = mutableMapOf<Int, VoxelShape>()

    // Visual shapes

    private fun computeShape(state: BlockState, collision: Boolean): VoxelShape {
        var shape = Shapes.empty()
        val baseShape = makeShape(if (collision) 0.65f else 1.0f)

        for (direction in Direction.values()) {
            if (state.getValue(getPropertyForDirection(direction))) {
                val rotatedShape = rotateShape(baseShape, direction.opposite)
                //If we are computing the collision shape, we need to shrink the shape slightly
                shape = Shapes.or(shape, rotatedShape)
            }
        }

        return shape
    }



    fun rotateShape(shape: VoxelShape, facing: Direction): VoxelShape {
        val buffer = mutableListOf<VoxelShape>()
        shape.forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
            val rotatedBox = when (facing) {
                Direction.NORTH -> AABB(minX, minY, minZ, maxX, maxY, maxZ)
                Direction.SOUTH -> AABB(1 - maxX, minY, 1 - maxZ, 1 - minX, maxY, 1 - minZ)
                Direction.EAST -> AABB(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)
                Direction.WEST -> AABB(minZ, minY, 1 - maxX, maxZ, maxY, 1 - minX)
                Direction.UP -> AABB(minX, 1 - maxZ, minY, maxX, 1 - minZ, maxY)
                Direction.DOWN -> AABB(minX, minZ, 1 - maxY, maxX, maxZ, 1 - minY)
            }
            buffer.add(Shapes.create(rotatedBox))
        }

        return buffer.reduce { acc, voxelShape -> Shapes.or(acc, voxelShape) }
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val cacheKey = getShapeCacheKey(state)
        return visualShapeCache.getOrPut(cacheKey) { computeShape(state, false) }
    }

    override fun getCollisionShape(
        state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext
    ): VoxelShape {
        val cacheKey = getShapeCacheKey(state)
        return collisionShapeCache.getOrPut(cacheKey) { computeShape(state, true) }
    }

    private fun getShapeCacheKey(state: BlockState): Int {
        var key = 0
        for (direction in Direction.values()) {
            if (state.getValue(getPropertyForDirection(direction))) {
                key = key or (1 shl direction.ordinal)
            }
        }
        return key
    }

    fun makeShape(scale: Float): VoxelShape {
        var shape = Shapes.empty()
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.4212500000000001 * scale, 0.4215625 * scale, 0.9784374999999975 * scale,
                0.5784375000000002 * scale, 0.5784375 * scale, 0.9941249999999969 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.43781250000000005 * scale, 0.546875 * scale, 0.9465624999999976 * scale,
                0.5628124999999999 * scale, 0.5625 * scale, 0.9871874999999976 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.43781250000000005 * scale, 0.43781249999999994 * scale, 0.9465624999999976 * scale,
                0.5628124999999999 * scale, 0.45343749999999994 * scale, 0.9871874999999976 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.546875 * scale, 0.45343749999999994 * scale, 0.9465624999999976 * scale,
                0.5625 * scale, 0.5471875000000002 * scale, 0.9871874999999976 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.4375 * scale, 0.45343749999999994 * scale, 0.9465624999999976 * scale,
                0.453125 * scale, 0.5471875000000002 * scale, 0.9871874999999976 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.484375 * scale, 0.496875 * scale, 0.9773437499999974 * scale,
                0.515625 * scale, 0.534375 * scale, 0.9779687499999975 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.5 * scale, 0.48125 * scale, 0.9773437499999974 * scale,
                0.53125 * scale, 0.51875 * scale, 0.9779687499999975 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.46875 * scale, 0.48125 * scale, 0.9773437499999974 * scale,
                0.5 * scale, 0.51875 * scale, 0.9779687499999975 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.5 * scale, 0.471875 * scale, 0.9773437499999974 * scale,
                0.53125 * scale, 0.509375 * scale, 0.9851562499999974 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.5 * scale, 0.503125 * scale, 0.9773437499999974 * scale,
                0.53125 * scale, 0.540625 * scale, 0.9851562499999974 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.46875 * scale, 0.471875 * scale, 0.9773437499999974 * scale,
                0.5 * scale, 0.509375 * scale, 0.9851562499999972 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.46875 * scale, 0.503125 * scale, 0.9773437499999974 * scale,
                0.5 * scale, 0.540625 * scale, 0.9851562499999974 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.546875 * scale, 0.4371875 * scale, 0.9460937499999976 * scale,
                0.609375 * scale, 0.5621875 * scale, 0.9867187499999975 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.39093749999999994 * scale, 0.4371875 * scale, 0.9460937499999976 * scale,
                0.45343749999999994 * scale, 0.5621875 * scale, 0.9867187499999975 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.45343749999999994 * scale, 0.390625 * scale, 0.9460937499999976 * scale,
                0.5471875000000002 * scale, 0.453125 * scale, 0.9867187499999975 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.45343749999999994 * scale, 0.546875 * scale, 0.9460937499999976 * scale,
                0.5471875000000002 * scale, 0.609375 * scale, 0.9867187499999975 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.5471874999999999 * scale, 0.40625 * scale, 0.9617187499999974 * scale,
                0.5940625000000002 * scale, 0.4375 * scale, 0.9867187499999975 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.40656249999999994 * scale, 0.40625 * scale, 0.9617187499999974 * scale,
                0.45343750000000016 * scale, 0.4375 * scale, 0.9867187499999975 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.40593749999999984 * scale, 0.5625 * scale, 0.9617187499999974 * scale,
                0.45281250000000006 * scale, 0.59375 * scale, 0.9867187499999975 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.5465624999999998 * scale, 0.5625 * scale, 0.9617187499999974 * scale,
                0.5934375000000001 * scale, 0.59375 * scale, 0.9867187499999975 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.453125 * scale, 0.453125 * scale, 0.8578124999999978 * scale,
                0.546875 * scale, 0.546875 * scale, 0.9828124999999973 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.4937839885193857 * scale, 0.46875 * scale, 0.795312499999998 * scale,
                0.5062160114806143 * scale, 0.53125 * scale, 0.9203124999999975 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.4937839885193857 * scale, 0.46875 * scale, 0.795312499999998 * scale,
                0.5062160114806143 * scale, 0.53125 * scale, 0.9203124999999975 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.4937839885193857 * scale, 0.46875 * scale, 0.795312499999998 * scale,
                0.5062160114806143 * scale, 0.53125 * scale, 0.9203124999999975 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.4937839885193857 * scale, 0.46875 * scale, 0.795312499999998 * scale,
                0.5062160114806143 * scale, 0.53125 * scale, 0.9203124999999975 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.46875 * scale, 0.4937839885193857 * scale, 0.795312499999998 * scale,
                0.53125 * scale, 0.5062160114806143 * scale, 0.9203124999999975 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.46875 * scale, 0.49378398851938576 * scale, 0.795312499999998 * scale,
                0.53125 * scale, 0.5062160114806145 * scale, 0.9203124999999975 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.46875 * scale, 0.49378398851938576 * scale, 0.795312499999998 * scale,
                0.53125 * scale, 0.5062160114806145 * scale, 0.9203124999999975 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.46875 * scale, 0.49378398851938576 * scale, 0.795312499999998 * scale,
                0.53125 * scale, 0.5062160114806145 * scale, 0.9203124999999975 * scale
            ),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(
                0.46875 * scale, 0.46875 * scale, 0.7946874999999981 * scale,
                0.53125 * scale, 0.53125 * scale, 0.795312499999998 * scale
            ),
            BooleanOp.OR
        )

        return shape
    }

    override fun canConnectTo(level: Level, pos: BlockPos, direction: Direction): Boolean {
        val neighborState = level.getBlockState(pos.relative(direction))
        return neighborState.block is BasePipeBlock
    }
}