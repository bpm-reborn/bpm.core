package bpm.client.render.inventory

import net.minecraft.world.item.ItemStack

/**
 * Represents a slot in the inventory that can hold an item
 */
data class InventorySlot(
    val x: Float,
    val y: Float,
    val item: ItemStack? = null,
    val isEnabled: Boolean = true,
    val onClick: (() -> Unit)? = null
)
