// Modified RedstoneReceiverItem.kt
package bpm.mc.item

import bpm.mc.registries.ModBlocks
import bpm.mc.selection.SelectionManager
import net.minecraft.world.item.*
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.core.BlockPos

class RedstoneReceiverItem : BlockItem(ModBlocks.REDSTONE_RECEIVER, Item.Properties().rarity(Rarity.EPIC)) {
    // Track the last created selection UUID for updating or removing
    private var currentSelectionUUID: java.util.UUID? = null

    override fun isFoil(stack: ItemStack): Boolean {
        return true
    }

    override fun isEnchantable(stack: ItemStack): Boolean {
        return false
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val pos = context.clickedPos
        val player = context.player

        if (level.isClientSide && player != null) {
            // If there's already a selection, remove it
            currentSelectionUUID?.let {
                SelectionManager.client.deleteSelection(it)
                currentSelectionUUID = null
            }

            // Create a new block selection at the clicked position
            currentSelectionUUID = SelectionManager.client.createBlockSelection(pos)

            return InteractionResult.SUCCESS
        }

        // Only handle this on client side, don't place the block yet
        return if (level.isClientSide) InteractionResult.SUCCESS else InteractionResult.PASS
    }


}