package bpm.client.render.inventory

import bpm.client.runtime.windows.CanvasGraphics
import bpm.common.utils.FontAwesome
import imgui.ImDrawList
import imgui.ImGui
import imgui.ImVec2
import imgui.type.ImString
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack
import org.joml.Vector2f

object BlockSelectionScreen : MinecraftInventoryScreen(
    title = "Block Selection",
    width = 9 * 18f + 22f,  // 9 columns + padding
    height = ((6 * 18f) + 14f + 16f + (3 * 18f + 7f + 18f))  // 5 rows + padding + search + player inventory
) {

    private val COLUMNS = 9
    private val ROWS = 5
    private val ITEMS_PER_PAGE = COLUMNS * ROWS

    private var currentPage = 0
    private val searchQuery = ImString(256)
    private var selectionCallback: ((ItemStack) -> Unit)? = null


    override fun onShow() {
        currentPage = 0
        searchQuery.set("")
    }

    fun show(callback: (ItemStack) -> Unit) {
        this.selectionCallback = callback
        super.show()
    }


    override fun onHide() {
        // Clean up if needed
    }

    override fun renderHeader(drawList: ImDrawList, startPos: ImVec2) {
        val filteredBlocks = BlockCache.search(searchQuery.get())
        val totalPages = ((filteredBlocks?.size ?: 0) + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE

        // Navigation and search controls
        val navButtonWidth = 10f * guiScale
        val navButtonHeight = 8f * guiScale
        val searchWidth = ((width * guiScale - navButtonWidth * 4) - 4 * guiScale)
        val startPos = ImGui.getCursorScreenPos()
        if (MinecraftUI.input(
                "search",
                searchQuery,
                searchWidth,
                navButtonHeight,
                "Search..."
            )
        ) {
            currentPage = 0
        }

        ImGui.setCursorScreenPos(ImGui.getCursorScreenPosX(), startPos.y)
        // Navigation buttons
        ImGui.setCursorPosX(ImGui.getWindowWidth() - navButtonWidth * 2 - inventoryPadding - 4 * guiScale)
        if (MinecraftUI.iconButton(FontAwesome.CaretLeft, navButtonWidth, navButtonHeight, currentPage > 0)) {
            currentPage--
        }
        ImGui.sameLine()
        ImGui.setCursorPosX(ImGui.getWindowWidth() - navButtonWidth - (inventoryPadding * 2) + 6 * guiScale)
        if (MinecraftUI.iconButton(
                FontAwesome.CaretRight,
                navButtonWidth,
                navButtonHeight,
                currentPage < totalPages - 1
            )
        ) {
            currentPage++
        }
        ImGui.dummy(0f, inventoryPadding)
    }

    override fun renderContent(drawList: ImDrawList, gui: CanvasGraphics) {
        val filteredBlocks = BlockCache.search(searchQuery.get())
        val startIdx = maxOf(currentPage * ITEMS_PER_PAGE, 0)
        val endIdx = minOf(startIdx + ITEMS_PER_PAGE, filteredBlocks?.size ?: 0)

        val gridStartX = ImGui.getCursorScreenPos().x
        val gridStartY = ImGui.getCursorScreenPos().y

        //Makes sure the from index is not greater than the size of the list
        if (startIdx >= (filteredBlocks?.size ?: 0)) {
            currentPage--
            return
        }

        filteredBlocks?.subList(startIdx, endIdx)?.forEachIndexed { index, blockData ->
            val row = index / COLUMNS
            val col = index % COLUMNS
            val slot = InventorySlot(
                x = gridStartX + col * slotSpacing,
                y = gridStartY + row * slotSpacing,
                item = blockData.itemStack,
                onClick = {
                    selectionCallback?.invoke(blockData.itemStack)
                }
            )

            renderSlot(drawList, slot)

            recordedDrawCalls.add { gfx ->
                gui.renderBlockItem(
                    blockData.itemStack,
                    slot.x + slotPadding,
                    slot.y - slotPadding * 2,
                    slotSize - slotPadding * 2
                )
            }
        }
    }

    override fun renderFooter(drawList: ImDrawList, gui: CanvasGraphics) {
        val player = Minecraft.getInstance().player ?: return

        // Fix: Use scaled values consistently for positioning
//        val inventoryStartY = (height * guiScale + ( slotSize)) - inventoryPadding
//        ImGui.setCursorPosY(inventoryStartY - 10 * guiScale)
        ImGui.setCursorPosY(ImGui.getCursorPosY() + (slotSize * 5) + 6f * guiScale)
        MinecraftUI.sectionTitle("Inventory")
        ImGui.setCursorScreenPos(ImGui.getCursorScreenPos().x, ImGui.getCursorScreenPos().y - 8f * guiScale)

        val startX = ImGui.getCursorScreenPos().x
        val startY = ImGui.getCursorScreenPos().y


        // Main inventory (3 rows)
        for (row in 0..2) {
            for (col in 0..8) {
                val index = (row * 9 + col + 9)
                val stack = player.inventory.items[index]
                val slot = InventorySlot(
                    x = startX + col * slotSpacing,
                    y = startY + row * slotSpacing,
                    item = stack
                )

                renderSlot(drawList, slot)

                if (!stack.isEmpty) {
                    recordedDrawCalls.add { gfx ->
                        gui.renderBlockItem(
                            stack,
                            slot.x + slotPadding,
                            slot.y - slotPadding * 2,
                            slotSize - slotPadding * 2
                        )
                    }
                }
            }
        }

        // Fix: Calculate hotbar position using scaled values
        val hotbarY = startY + (3 * baseSlotSize + baseInventoryPadding) * guiScale
        for (i in 0..8) {
            val stack = player.inventory.items[i]
            val slot = InventorySlot(
                x = startX + i * slotSpacing,
                y = hotbarY,
                item = stack
            )

            renderSlot(drawList, slot)

            if (!stack.isEmpty) {
                recordedDrawCalls.add { gfx ->
                    gui.renderBlockItem(
                        stack,
                        slot.x + slotPadding,
                        slot.y - slotPadding * 2,
                        slotSize - slotPadding * 2
                    )
                }
            }
        }
    }
}