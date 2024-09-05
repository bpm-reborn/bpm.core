package bpm.mc.block

import bpm.common.logging.KotlinLogging
import bpm.mc.visual.NodeEditorGui
import bpm.pipe.PipeNetworkManager
import bpm.server.ServerRuntime
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents.CUSTOM_NAME
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import java.util.*


class EnderControllerBlock(properties: Properties) : BasePipeBlock(properties), EntityBlock {

    private val shape = Shapes.box(3/16.0, 3/16.0, 3/16.0, 13/16.0, 13/16.0, 13/16.0)
    private val logger = KotlinLogging.logger { }

    override fun useItemOn(
        p_316304_: ItemStack,
        p_316362_: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        p_316595_: InteractionHand,
        p_316140_: BlockHitResult
    ): ItemInteractionResult {
        if (!level.isClientSide) {
            val tileEntity = level.getBlockEntity(pos) as? EnderControllerTileEntity
            if (tileEntity == null) {
                logger.error { "Ender Controller Tile Entity is null" }
                return ItemInteractionResult.sidedSuccess(level.isClientSide)
            }
            val uuid = tileEntity.getUUID()
            val playerUUID = player.uuid
            ServerRuntime.openWorkspace(uuid, playerUUID)
            logger.debug { "Opening workspace for Ender Controller with uid $uuid" }
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide)
    }


    override fun setPlacedBy(
        level: Level,
        p_49848_: BlockPos,
        p_49849_: BlockState,
        p_49850_: LivingEntity?,
        p_49851_: ItemStack
    ) {
        super.setPlacedBy(level, p_49848_, p_49849_, p_49850_, p_49851_)
        if (!level.isClientSide) {
            (level.getBlockEntity(p_49848_) as? EnderControllerTileEntity)?.setUUID(UUID.randomUUID())
            val tile = level.getBlockEntity(p_49848_)
            if (tile is EnderControllerTileEntity) {
//                val workspace = tile.workspace
//                if (workspace == null) {
//                    Initialize the uuid
//                    tile.setUUID(UUID.randomUUID())
//                    tile.setChanged()
//                    logger.debug { "Created new workspace for Ender Controller with uid ${tile.getUUID()}" }
//                } else{
                ServerRuntime.recompileWorkspace(tile.getUUID())
                val uuid = tile.getUUID()
                val playerUUID = (p_49850_ as? Player)?.uuid
                if (playerUUID != null) {
                    ServerRuntime.openWorkspace(uuid, playerUUID)
                }
//                }
            }
        }
    }

    override fun getCollisionShape(
        p_60572_: BlockState, p_60573_: BlockGetter, p_60574_: BlockPos, p_60575_: CollisionContext
    ): VoxelShape {
        return shape
    }

    override fun getShape(
        p_60555_: BlockState, p_60556_: BlockGetter, p_60557_: BlockPos, p_60558_: CollisionContext
    ): VoxelShape {
        return shape
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val level = context.level
        val pos = context.clickedPos
        if (PipeNetworkManager.hasControllerInNetwork(level, pos)) {
            return null
        }
        return super.getStateForPlacement(context)
    }

    override fun getUpdatedState(level: Level, pos: BlockPos, currentState: BlockState): BlockState {
        var state = super.getUpdatedState(level, pos, currentState)
        // Temp fix to get rid of the ugly part at the top of the controller when there is no connection.
        if (state.hasProperty(UP) && !canConnectTo(level, pos, Direction.UP)) {
            state = state.setValue(UP, false)
        }
        return state
    }

    override fun neighborChanged(
        state: BlockState, level: Level, pos: BlockPos, block: Block, fromPos: BlockPos, isMoving: Boolean
    ) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving)
        if (!level.isClientSide) {
            PipeNetworkManager.queueNetworkUpdate(level, pos)
        }
    }

    override fun canBeReplaced(state: BlockState, context: BlockPlaceContext): Boolean {
        return false
    }


    private fun dropController(level: Level, pos: BlockPos) {
        val blockEntity = level.getBlockEntity(pos) as? EnderControllerTileEntity
        if (blockEntity != null && !level.isClientSide) {
            val stack = ItemStack(this)
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


    fun makeShape(): VoxelShape {
        var shape = Shapes.empty()
        shape = Shapes.join(shape, Shapes.box(0.421875, 0.4375, 0.0, 0.4375, 0.5625, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.4375, 0.5625, 0.0, 0.5625, 0.578125, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.4375, 0.421875, 0.0, 0.5625, 0.4375, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.4375, 0.0, 0.578125, 0.5625, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.421875, 0.5703125, 0.0, 0.4296875, 0.578125, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.5703125, 0.5703125, 0.0, 0.578125, 0.578125, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.421875, 0.421875, 0.0, 0.4296875, 0.4296875, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.5703125, 0.421875, 0.0, 0.578125, 0.4296875, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.4296875, 0.0, 0.5703125, 0.4375, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.5625, 0.0, 0.5703125, 0.5703125, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.4296875, 0.5625, 0.0, 0.4375, 0.5703125, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.4296875, 0.4296875, 0.0, 0.4375, 0.4375, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.4296875, 0.0, 0.5703125, 0.4375, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.4296875, 0.4296875, 0.0, 0.4375, 0.4375, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.4375, 0.421875, 0.0, 0.5625, 0.4375, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.4375, 0.5625, 0.0, 0.5625, 0.578125, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.421875, 0.4375, 0.0, 0.4375, 0.5625, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.4375, 0.0, 0.578125, 0.5625, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.421875, 0.421875, 0.0, 0.4296875, 0.4296875, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.421875, 0.5703125, 0.0, 0.4296875, 0.578125, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.5703125, 0.421875, 0.0, 0.578125, 0.4296875, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.5703125, 0.5703125, 0.0, 0.578125, 0.578125, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.5625, 0.0, 0.5703125, 0.5703125, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.4296875, 0.5625, 0.0, 0.4375, 0.5703125, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.4296875, 0.4296875, 0.0, 0.4375, 0.4375, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.4296875, 0.0, 0.5703125, 0.4375, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.4296875, 0.0, 0.5703125, 0.4375, 1.0), BooleanOp.OR)
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.5625, 0.0, 0.5703125, 0.5703125, 1.0), BooleanOp.OR)
        shape = Shapes.join(
            shape,
            Shapes.box(0.4215625, 0.4215625, 0.984375, 0.5787500000000001, 0.5784375000000002, 1.0000625),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape, Shapes.box(
                0.42156250000000006,
                0.42156250000000006,
                -0.00006250000000007638,
                0.5784375000000002,
                0.5787500000000001,
                0.015625
            ), BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(0.4375, 0.4371875, 0.99375, 0.45312500000000006, 0.5621875000000001, 1.003125),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(0.5465625000000001, 0.4371875, 0.99375, 0.5621875000000001, 0.5621875000000001, 1.003125),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(0.45281250000000006, 0.4375, 0.99375, 0.5465625000000001, 0.453125, 1.003125),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(0.45281250000000006, 0.546875, 0.99375, 0.5465625000000001, 0.5625, 1.003125),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(0.484375, 0.49687500000000007, 1.001625, 0.515625, 0.5343749999999999, 1.0022499999999999),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(0.4375, 0.4371875, 0.996875, 0.45312500000000006, 0.5621875000000001, 1.00625),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(0.5465625000000001, 0.4371875, 0.996875, 0.5621875000000001, 0.5621875000000001, 1.00625),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(0.45281250000000006, 0.4375, 0.996875, 0.5465625000000001, 0.453125, 1.00625),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(0.45281250000000006, 0.546875, 0.996875, 0.5465625000000001, 0.5625, 1.00625),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape, Shapes.box(0.46875, 0.471875, 0.996875, 0.5, 0.5093749999999999, 1.0046875), BooleanOp.OR
        )
        shape = Shapes.join(
            shape, Shapes.box(0.5, 0.471875, 0.99625, 0.53125, 0.5093749999999999, 1.0040624999999999), BooleanOp.OR
        )
        shape = Shapes.join(
            shape, Shapes.box(0.46875, 0.503125, 0.9975, 0.5, 0.5406249999999999, 1.0053125), BooleanOp.OR
        )
        shape = Shapes.join(
            shape, Shapes.box(0.5, 0.503125, 0.998125, 0.53125, 0.5406249999999999, 1.0059375), BooleanOp.OR
        )
        shape = Shapes.join(
            shape, Shapes.box(0.46875, 0.48125, 1.0015625, 0.5, 0.5187499999999999, 1.0021874999999998), BooleanOp.OR
        )
        shape = Shapes.join(
            shape, Shapes.box(0.5, 0.48125, 1.0015625, 0.53125, 0.5187499999999999, 1.0021874999999998), BooleanOp.OR
        )
        shape = Shapes.join(
            shape, Shapes.box(
                0.4212499999999999, 0.4215625, -0.00006250000000007638, 0.5784374999999999, 0.5784375000000002, 0.015625
            ), BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(0.546875, 0.4371875, -0.006250000000000089, 0.5625, 0.5621875000000001, 0.0031250000000000444),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape, Shapes.box(
                0.43781249999999994,
                0.4371875,
                -0.006250000000000089,
                0.45343749999999994,
                0.5621875000000001,
                0.0031250000000000444
            ), BooleanOp.OR
        )
        shape = Shapes.join(
            shape, Shapes.box(
                0.45343749999999994, 0.4375, -0.006250000000000089, 0.5471874999999999, 0.453125, 0.0031250000000000444
            ), BooleanOp.OR
        )
        shape = Shapes.join(
            shape, Shapes.box(
                0.45343749999999994, 0.546875, -0.006250000000000089, 0.5471874999999999, 0.5625, 0.0031250000000000444
            ), BooleanOp.OR
        )
        shape = Shapes.join(
            shape, Shapes.box(
                0.484375, 0.49687500000000007, 0.029000000000000137, 0.515625, 0.5343749999999999, 0.029625000000000012
            ), BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(0.46875, 0.48125, 0.029062500000000213, 0.5, 0.5187499999999999, 0.02968750000000009),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(0.5, 0.48125, 0.029062500000000213, 0.53125, 0.5187499999999999, 0.02968750000000009),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(0.46875, 0.471875, -0.004687499999999956, 0.5, 0.5093749999999999, 0.0031250000000000444),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(0.46875, 0.503125, -0.005312500000000053, 0.5, 0.5406249999999999, 0.0024999999999999467),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(0.5, 0.471875, -0.004062499999999858, 0.53125, 0.5093749999999999, 0.003750000000000142),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(0.5, 0.503125, -0.005937499999999929, 0.53125, 0.5406249999999999, 0.001875000000000071),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape,
            Shapes.box(0.546875, 0.4371875, -0.0031250000000000444, 0.5625, 0.5621875000000001, 0.006249999999999978),
            BooleanOp.OR
        )
        shape = Shapes.join(
            shape, Shapes.box(
                0.43781249999999994,
                0.4371875,
                -0.0031250000000000444,
                0.45343749999999994,
                0.5621875000000001,
                0.006249999999999978
            ), BooleanOp.OR
        )
        shape = Shapes.join(
            shape, Shapes.box(
                0.45343749999999994, 0.4375, -0.0031250000000000444, 0.5471874999999999, 0.453125, 0.006249999999999978
            ), BooleanOp.OR
        )
        shape = Shapes.join(
            shape, Shapes.box(
                0.45343749999999994, 0.546875, -0.0031250000000000444, 0.5471874999999999, 0.5625, 0.006249999999999978
            ), BooleanOp.OR
        )
        shape = Shapes.join(
            shape, Shapes.box(
                0.42156250000000006,
                0.42156250000000006,
                -0.00006250000000007638,
                0.5784375000000002,
                0.5787500000000001,
                0.015625
            ), BooleanOp.OR
        )

        return shape
    }

    override fun newBlockEntity(p_153215_: BlockPos, p_153216_: BlockState): BlockEntity? {
        val controller = EnderControllerTileEntity(p_153215_, p_153216_)
        val uuid = controller.getUUID()
        return controller
    }


}
