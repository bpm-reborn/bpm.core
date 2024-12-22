package bpm.client.render.inventory

import bpm.client.runtime.ClientRuntime
import bpm.client.runtime.windows.CanvasGraphics
import imgui.ImColor
import imgui.ImDrawList
import imgui.ImGui
import imgui.ImVec2
import imgui.flag.*
import imgui.type.ImBoolean
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack
import org.joml.Vector2f

/**
 * Base class for Minecraft-style inventory screens
 */
abstract class MinecraftInventoryScreen(
    protected val title: String,
    protected val width: Float,
    protected val height: Float
) {

    protected val isOpen = ImBoolean(false)
    protected val slots = mutableListOf<InventorySlot>()
    protected var hoveredItem: ItemStack? = null
    protected var hoveredItemPos = Vector2f()
    protected val recordedDrawCalls = mutableListOf<(gfx: CanvasGraphics) -> Unit>()

    // Minecraft-accurate colors
    protected val backgroundColor = ImColor.rgba(198, 198, 198, 255)
    protected val slotColor = ImColor.rgba(139, 139, 139, 255)
    protected val slotBorderColor = ImColor.rgba(55, 55, 55, 255)

    // Base sizes (will be scaled)
    protected val baseSlotSize = 18f
    protected val basePadding = 1f
    protected val baseInventoryPadding = 7f

    // Scaled dimensions
    protected val guiScale get() = Minecraft.getInstance().options.guiScale().get().toFloat()
    protected val slotSize get() = baseSlotSize * guiScale
    protected val slotPadding get() = basePadding * guiScale
    protected val slotSpacing get() = (baseSlotSize + basePadding) * guiScale  // Fix: Scale the total spacing
    protected val inventoryPadding get() = baseInventoryPadding * guiScale
    fun show() {
        isOpen.set(true)
        onShow()
    }

    fun hide() {
        isOpen.set(false)
        onHide()
    }

    fun render(gui: CanvasGraphics) {
        if (!isOpen.get()) return

        hoveredItem = null
        setupWindow()

        val windowFlags = ImGuiWindowFlags.NoResize or
                ImGuiWindowFlags.NoMove or
                ImGuiWindowFlags.NoCollapse or
                ImGuiWindowFlags.NoDocking or
                ImGuiWindowFlags.NoTitleBar

        applyWindowStyle()

        if (ImGui.begin(title, isOpen, windowFlags)) {
            val drawList = ImGui.getWindowDrawList()
            val startPos = ImGui.getCursorScreenPos()

            ImGui.setCursorScreenPos(startPos.x, startPos.y + inventoryPadding)
            // Custom header content
            renderHeader(drawList, startPos)

            // Main content area
            renderContent(drawList, gui)

            // Footer content
            renderFooter(drawList, gui)
        }

        ImGui.end()
        restoreWindowStyle()
    }

    protected fun renderSlot(drawList: ImDrawList, slot: InventorySlot) {
        // Draw slot background
        drawList.addRectFilled(
            slot.x, slot.y,
            slot.x + slotSize,
            slot.y + slotSize,
            slotColor,
            0f
        )

        // Draw slot border
        drawList.addRect(
            slot.x, slot.y,
            slot.x + slotSize,
            slot.y + slotSize,
            slotBorderColor,
            0f,
            0,
            1f
        )

        // Handle hover and click
        if (ImGui.isMouseHoveringRect(slot.x, slot.y, slot.x + slotSize, slot.y + slotSize)) {
            slot.item?.let {
                hoveredItem = it
                hoveredItemPos = Vector2f(slot.x, slot.y)
            }

            if (slot.isEnabled && ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
                slot.onClick?.invoke()
            }
        }
    }

    open fun renderPost(gfx: GuiGraphics, graphics: CanvasGraphics) {
        if (!isOpen.get()) return

        val viewport = ImGui.getMainViewport()
        val transformedPos = graphics.toScreenSpaceVector(viewport.pos.x, viewport.pos.y)
        val transformedSize = graphics.toScreenSpaceVector(viewport.sizeX, viewport.sizeY)

        gfx.enableScissor(
            transformedPos.x.toInt(),
            transformedPos.y.toInt(),
            (transformedPos.x + transformedSize.x).toInt(),
            (transformedPos.y + transformedSize.y).toInt()
        )

        recordedDrawCalls.forEach { it(graphics) }
        recordedDrawCalls.clear()

        gfx.disableScissor()

        hoveredItem?.let { stack ->
            val pos = graphics.toScreenSpaceVector(ImGui.getMousePos().x, ImGui.getMousePos().y)
            gfx.renderTooltip(
                Minecraft.getInstance().font,
                stack,
                pos.x.toInt(),
                pos.y.toInt()
            )
        }
    }

    private fun setupWindow() {
        val viewport = ImGui.getMainViewport()
        val scaledWidth = width * guiScale
        val scaledHeight = (height * guiScale) + inventoryPadding / (guiScale * 2)

        //Set to match the right side of the filter screens position
        val filterScreenPos = FilterScreen.position
        val filterScreenSize = FilterScreen.size
        val filterScreenRight = filterScreenPos.x + filterScreenSize.x
        val filterScreenTop = filterScreenPos.y

        ImGui.setNextWindowPos(
            filterScreenRight,
            filterScreenTop
        )

        ImGui.setNextWindowSize(scaledWidth, scaledHeight)
    }

    private fun applyWindowStyle() {
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, inventoryPadding, 0f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 1f)
        ImGui.pushStyleColor(ImGuiCol.WindowBg, backgroundColor)
        ImGui.pushStyleColor(ImGuiCol.Border, slotBorderColor)
    }

    private fun restoreWindowStyle() {
        ImGui.popStyleVar(3)
        ImGui.popStyleColor(2)
    }

    // Abstract methods to be implemented by specific inventory screens
    protected abstract fun onShow()
    protected abstract fun onHide()
    protected abstract fun renderHeader(drawList: ImDrawList, startPos: ImVec2)
    protected abstract fun renderContent(drawList: ImDrawList, gui: CanvasGraphics)
    protected abstract fun renderFooter(drawList: ImDrawList, gui: CanvasGraphics)
}