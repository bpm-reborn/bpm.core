package bpm.client.render.inventory

import bpm.common.logging.KotlinLogging
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import java.util.*
import java.util.stream.Collectors


object BlockCache {

    //The loaded cache of block data
    private val blockDataCache: MutableMap<ResourceLocation, CachedBlockData> = HashMap()

    //The sorted list of blocks, sorted by mod, then by block name
    private val sortedBlocks: MutableList<CachedBlockData> = ArrayList()

    // The search cache, a map of search terms to a list of blocks that match that term
    private val searchCache: MutableMap<String, List<CachedBlockData>> = HashMap()

    private val logger = KotlinLogging.logger { }


    fun initialize() {
        collectBlocks()
    }

    private fun collectBlocks() {
        // Clear existing cache
        blockDataCache.clear()
        sortedBlocks.clear()
        searchCache.clear()

        // Collect all registered blocks
        val blockRegistry: Registry<Block> = BuiltInRegistries.BLOCK

        for (block in blockRegistry) {
            val blockId: ResourceLocation = blockRegistry.getKey(block) ?: continue

            // Skip blocks that shouldn't be displayed (like air)
            if (shouldSkipBlock(block)) continue

            val blockData = CachedBlockData(
                block,
                blockId,
                getModId(block),
                getItemStack(block),
                block.name.string.lowercase(),
                collectTags(block)
            )

            blockDataCache[blockId] = blockData
            sortedBlocks.add(blockData)
        }

        logger.info { "Collected ${sortedBlocks.size} blocks" }


        // Sort blocks by mod, then by block name
        sortedBlocks.sortWith { block1, block2 ->
            if (block1.modId == block2.modId) {
                block1.name.compareTo(block2.name)
            } else {
                block1.modId.compareTo(block2.modId)
            }
        }
    }

    private fun shouldSkipBlock(block: Block): Boolean {
        // Skip all air blocks
        if (block === Blocks.AIR || block === Blocks.CAVE_AIR || block === Blocks.VOID_AIR ||
            block.defaultBlockState().isAir
        ) return true

        // Skip blocks that don't have items (can't be placed in inventory)
        if (block.asItem() === Items.AIR) {
            return true
        }
        //Skip duplicate blocks
        if (blockDataCache.containsKey(block.asItem().builtInRegistryHolder().key().location())) {
            return true
        }


        // Skip blocks that are marked as air in any way
        val blockState = block.defaultBlockState()
        return blockState.isAir || blockState.isEmpty
    }

    private fun getModId(block: Block): String {
        val blockId = block.builtInRegistryHolder().key()
        return blockId.location().namespace
    }

    private fun getItemStack(block: Block): ItemStack {
        return ItemStack(block.asItem())
    }

    private fun collectTags(block: Block): Set<String> {
        val tags: MutableSet<String> = HashSet()
        block.builtInRegistryHolder().tags()
            .forEach { tag: TagKey<Block?> -> tags.add(tag.location().toString().lowercase(Locale.getDefault())) }
        return tags
    }

    fun search(query: String): List<CachedBlockData>? {
        if (query.isEmpty()) {
            return ArrayList(sortedBlocks)
        }

        // Check if we have a cached result
        val lowerQuery = query.lowercase(Locale.getDefault())
        if (searchCache.containsKey(lowerQuery)) {
            return searchCache[lowerQuery]
        }

        // Perform new search
        val results = sortedBlocks.stream()
            .filter { blockData: CachedBlockData -> matchesSearch(blockData, lowerQuery) }
            .collect(Collectors.toList())

        // Cache results (consider implementing a cache size limit)
        searchCache[lowerQuery] = results
        return results
    }

    private fun matchesSearch(blockData: CachedBlockData, query: String): Boolean {
        // Check name
        if (blockData.name.contains(query)) {
            return true
        }

        // Check mod ID
        if (blockData.blockId.namespace.contains(query)) {
            return true
        }

        // Check tags
        return blockData.tags.stream().anyMatch { tag: String -> tag.contains(query) }
    }

    fun clearCache() {
        searchCache.clear()
    }


}