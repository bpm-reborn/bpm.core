package bpm.mc.visual

import imgui.ImColor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack
import imgui.ImGui
import imgui.ImVec2
import imgui.ImVec4
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiWindowFlags
import kotlin.math.max

class MinecraftInventoryRenderer {

    private val BASE_SLOT_SIZE = 18f
    private val BASE_SLOT_PADDING = 1f
    private val INVENTORY_COLUMNS = 9
    private val INVENTORY_ROWS = 3
    private val BASE_TITLE_HEIGHT = 12f
    private val BASE_WINDOW_PADDING_X = 7f
    private val BASE_WINDOW_PADDING_Y = 7f
    private val BASE_TITLE_TEXT_OFFSET_Y = 2f

    // Colors
    private val WINDOW_BG_COLOR = ImVec4(0.19f, 0.19f, 0.19f, 1.00f)
    private val TITLE_BG_COLOR = ImVec4(0.16f, 0.29f, 0.48f, 1.00f)
    private val TITLE_TEXT_COLOR = ImVec4(1.00f, 1.00f, 1.00f, 1.00f)
    private val SLOT_BG_COLOR = ImVec4(0.25f, 0.25f, 0.25f, 1.00f)
    private val SLOT_BORDER_COLOR = ImVec4(0.14f, 0.14f, 0.14f, 1.00f)

    fun renderInventory(items: List<ItemStack>, guiGraphics: GuiGraphics) {
        val scale = max(1, Minecraft.getInstance().options.guiScale().get())
        val scaleFactor = scale.toFloat()

        val slotSize = BASE_SLOT_SIZE * scaleFactor
        val slotPadding = BASE_SLOT_PADDING * scaleFactor
        val titleHeight = BASE_TITLE_HEIGHT * scaleFactor
        val windowPaddingX = BASE_WINDOW_PADDING_X * scaleFactor
        val windowPaddingY = BASE_WINDOW_PADDING_Y * scaleFactor
        val titleTextOffsetY = BASE_TITLE_TEXT_OFFSET_Y * scaleFactor

        val windowWidth = INVENTORY_COLUMNS * slotSize + (INVENTORY_COLUMNS - 1) * slotPadding + 2 * windowPaddingX
        val windowHeight = INVENTORY_ROWS * slotSize + (INVENTORY_ROWS - 1) * slotPadding + 2 * windowPaddingY + titleHeight

        ImGui.setNextWindowSize(windowWidth, windowHeight)
        ImGui.setNextWindowPos(ImGui.getMainViewport().posX + 10f, ImGui.getMainViewport().posY + 10f)

        ImGui.pushStyleColor(ImGuiCol.WindowBg, ImColor.rgba(WINDOW_BG_COLOR.x, WINDOW_BG_COLOR.y, WINDOW_BG_COLOR.z, WINDOW_BG_COLOR.w))
        ImGui.pushStyleColor(ImGuiCol.Border, 0f, 0f, 0f, 0f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f)

        val flags = ImGuiWindowFlags.NoMove or ImGuiWindowFlags.NoResize or
                ImGuiWindowFlags.NoCollapse or ImGuiWindowFlags.NoTitleBar

        ImGui.begin("Minecraft Inventory", flags)

        val drawList = ImGui.getWindowDrawList()
        val windowPos = ImGui.getWindowPos()

        // Draw title bar
        drawList.addRectFilled(
            windowPos.x, windowPos.y,
            windowPos.x + windowWidth, windowPos.y + titleHeight,
            ImColor.rgba(TITLE_BG_COLOR.x, TITLE_BG_COLOR.y, TITLE_BG_COLOR.z, TITLE_BG_COLOR.w)
        )

        // Draw title text
        drawList.addText(
            windowPos.x + windowPaddingX, windowPos.y + titleTextOffsetY,
            ImColor.rgba(TITLE_TEXT_COLOR.x, TITLE_TEXT_COLOR.y, TITLE_TEXT_COLOR.z, TITLE_TEXT_COLOR.w),
            "Inventory"
        )

        // Draw "close" button
        val buttonSize = 10f * scaleFactor
        val buttonPosX = windowPos.x + windowWidth - buttonSize - windowPaddingX
        val buttonPosY = windowPos.y + (titleHeight - buttonSize) / 2
        drawList.addRectFilled(
            buttonPosX, buttonPosY,
            buttonPosX + buttonSize, buttonPosY + buttonSize,
            ImGui.colorConvertFloat4ToU32(0.7f, 0.7f, 0.7f, 1.0f)
        )
        drawList.addText(
            buttonPosX + 2 * scaleFactor, buttonPosY - 1 * scaleFactor,
            ImGui.colorConvertFloat4ToU32(0.1f, 0.1f, 0.1f, 1.0f),
            "+"
        )

        // Render inventory slots
        ImGui.setCursorPos(windowPaddingX, titleHeight + windowPaddingY)
        items.take(INVENTORY_COLUMNS * INVENTORY_ROWS).forEachIndexed { index, item ->
            val row = index / INVENTORY_COLUMNS
            val col = index % INVENTORY_COLUMNS

            val slotX = ImGui.getCursorPosX() + col * (slotSize + slotPadding)
            val slotY = ImGui.getCursorPosY() + row * (slotSize + slotPadding)

            renderSlot(drawList, guiGraphics, item, slotX, slotY, slotSize, scaleFactor)

            if (col == INVENTORY_COLUMNS - 1) {
                ImGui.setCursorPosY(ImGui.getCursorPosY() + slotSize + slotPadding)
            }
        }

        ImGui.end()
        ImGui.popStyleVar(3)
        ImGui.popStyleColor(2)
    }




    private fun renderSlot(drawList: imgui.ImDrawList, guiGraphics: GuiGraphics, item: ItemStack, x: Float, y: Float, size: Float, scale: Float) {
        // Draw slot background
        drawList.addRectFilled(
            x, y, x + size, y + size,
            ImColor.rgba(SLOT_BG_COLOR.x, SLOT_BG_COLOR.y, SLOT_BG_COLOR.z, SLOT_BG_COLOR.w)
        )

        // Draw slot border
        drawList.addRect(
            x, y, x + size, y + size,
            ImColor.rgba(SLOT_BORDER_COLOR.x, SLOT_BORDER_COLOR.y, SLOT_BORDER_COLOR.z, SLOT_BORDER_COLOR.w)
        )

        // Render the Minecraft item
        if (!item.isEmpty) {
            guiGraphics.pose().pushPose()
            guiGraphics.pose().translate(x.toDouble(), y.toDouble(), 32.0)
            guiGraphics.pose().scale(scale, scale, 1f)
            guiGraphics.renderItem(item, 0, 0)
            guiGraphics.renderItemDecorations(Minecraft.getInstance().font, item, 0, 0)
            guiGraphics.pose().popPose()
        }

        // Handle interaction
        if (ImGui.isMouseHoveringRect(x, y, x + size, y + size)) {
            if (ImGui.isMouseClicked(0)) {
                println("Clicked on item: ${item.displayName.string}")
            }

            // Render tooltip
            if (!item.isEmpty) {
                ImGui.beginTooltip()
                ImGui.text(item.displayName.string)
                if (item.count > 1) {
                    ImGui.text("Count: ${item.count}")
                }
                ImGui.endTooltip()
            }
        }
    }
}