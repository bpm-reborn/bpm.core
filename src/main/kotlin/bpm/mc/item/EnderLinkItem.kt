package bpm.mc.item

import bpm.mc.block.EnderControllerTileEntity
import bpm.mc.projectile.EnderLinkProjectile
import bpm.mc.registries.ModComponents
import bpm.pipe.PipeNetwork
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

class QuantumEntanglementItem : Item(Properties().stacksTo(1)) {

    override fun use(
        level: Level,
        player: Player,
        hand: InteractionHand
    ): InteractionResultHolder<ItemStack> {
        val itemStack = player.getItemInHand(hand)

        if (!level.isClientSide) {

            if (itemStack.has(ModComponents.CONTROLLER_UUID)) {
                //Spawn linking projectile if item is linked to a controller
                return spawnProjectile(level, player, itemStack)
            }
            //If the player isn't shifting, fail
            if (!player.isShiftKeyDown) return InteractionResultHolder.fail(itemStack)

            //See if we're looking at a controller
            val controller = locateLookedAtController(level, player)
                ?: return InteractionResultHolder.fail(itemStack)

            //Link to controller if player is looking at one
            return linkToController(level, player, itemStack, controller)
        }

        return InteractionResultHolder.fail(itemStack)
    }

    private fun locateLookedAtController(
        level: Level,
        player: Player
    ): EnderControllerTileEntity? {
        val rayTraceResult = player.pick(16.0, 0.0f, false)
        //TODO: this might need to be an entity bc it's a tile entity?
        if (rayTraceResult.type == HitResult.Type.BLOCK) {
            val blockPos = (rayTraceResult as BlockHitResult).blockPos
            val blockEntity = level.getBlockEntity(blockPos)
            if (blockEntity is EnderControllerTileEntity) return blockEntity
        }
        return null
    }


    private fun spawnProjectile(
        level: Level,
        player: Player,
        itemStack: ItemStack
    ): InteractionResultHolder<ItemStack> {
        val controllerUUID = itemStack.get(ModComponents.CONTROLLER_UUID)
            ?: return InteractionResultHolder.fail(itemStack)
        val controller = PipeNetwork.getController(controllerUUID)
            ?: return InteractionResultHolder.fail(itemStack)
        val controllerPos = controller.blockPos

        // Spawn the projectile
        val projectile = EnderLinkProjectile(level, player, controllerPos, controllerUUID)
        projectile.shootFromRotation(player, player.xRot, player.yRot, 2.5f, 0.5f, 1.0f)
        //Make the projectile slow
//        projectile.setDeltaMovement(projectile.deltaMovement.scale(0.5))
        level.addFreshEntity(projectile)

        // Play throw sound
        level.playSound(
            null,
            player.x,
            player.y,
            player.z,
            SoundEvents.AXOLOTL_HURT,
            SoundSource.NEUTRAL,
            1.0f,
            0.4f / (level.random.nextFloat() * 0.4f + 0.8f)
        )



        //Remove the controller UUID from the item
        itemStack.remove(ModComponents.CONTROLLER_UUID)
        return InteractionResultHolder.success(itemStack)
    }

    private fun linkToController(
        level: Level,
        player: Player,
        itemStack: ItemStack,
        controller: EnderControllerTileEntity
    ): InteractionResultHolder<ItemStack> {
        val uuid = controller.getUUID()
        // Store the controller's UUID in the item
        itemStack.set(ModComponents.CONTROLLER_UUID, uuid)

        // Play linking sound
        level.playSound(
            null,
            player.x,
            player.y,
            player.z,
            SoundEvents.ALLAY_ITEM_GIVEN,
            SoundSource.NEUTRAL,
            0.5f,
            0.4f / (level.random.nextFloat() * 0.4f + 0.8f)
        )

        return InteractionResultHolder.success(itemStack)
    }


}
