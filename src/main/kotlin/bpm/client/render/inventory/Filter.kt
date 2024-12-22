package bpm.client.render.inventory

import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

// Updated Filter data classes to include settings
sealed class Filter {
    abstract val name: String
    abstract val matchNbt: Boolean
    abstract val isBlacklist: Boolean

    data class ItemFilter(
        val target: ItemStack,
        override val matchNbt: Boolean,
        override val isBlacklist: Boolean,
        override val name: String = target.displayName.string
    ) : Filter()

    data class TagFilter(
        val tagId: String,
        override val matchNbt: Boolean,
        override val isBlacklist: Boolean,
        override val name: String = tagId
    ) : Filter()
}