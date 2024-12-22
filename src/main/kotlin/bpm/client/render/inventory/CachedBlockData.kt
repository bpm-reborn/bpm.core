package bpm.client.render.inventory

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block

data class CachedBlockData(
    val block: Block,
    val blockId: ResourceLocation,
    val modId: String,
    val itemStack: ItemStack,
    val name: String,
    val tags: Set<String>
)