package bpm.client.render.panel

import imgui.ImDrawList
import imgui.ImGui
import org.joml.Vector2f
import bpm.client.font.Fonts
import bpm.client.runtime.windows.CanvasContext
import bpm.client.runtime.windows.CanvasGraphics
import bpm.client.utils.toVec2f
import bpm.client.utils.use
import bpm.common.network.Endpoint
import bpm.common.utils.FontAwesome
import imgui.ImColor
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiMouseCursor
import imgui.type.ImString
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import org.joml.Vector2i
import org.joml.Vector3f

abstract class Panel(val title: String, val icon: String) {

    open val isDragging: Boolean = false
    protected lateinit var manager: PanelManager
    protected lateinit var graphics: CanvasGraphics

    val iconFam = Fonts.getFamily("Fa")["Regular"]
    val boldFam = Fonts.getFamily("Inter")["Bold"]
    val bodyFam = Fonts.getFamily("Inter")["Regular"]
    private val displaySize get() = ImGui.getIO().displaySize
    protected val context = Endpoint.installed<CanvasContext>()
    //    val panelWidth get() = (displaySize.x * 0.33f).coerceAtLeast(350f).coerceAtMost(500f)
//    val panelHeight get() = (displaySize.y * 0.5f).coerceAtLeast(150f)
    var position = Vector2f()
    var lastMaximizedPosition = Vector2f()
    private val windowSize = Vector2i(Minecraft.getInstance().window.width, Minecraft.getInstance().window.height)
    private var hasInitialized = false
    internal var isResizing = false
    private var resizeStartPos = Vector2f()
    private var resizeStartSize = Vector2f()

    // Minimum and maximum size constraints
    private val minWidth = 250f
    private val minHeight = 150f
    private val maxWidthPercent = 0.5f
    private val maxHeightPercent = 0.8f
    // Mutable panel size properties
    var panelWidth = 0f
        internal set
    var panelHeight = 0f
        internal set
    protected val buttonColor = ImColor.rgba(58, 58, 60, 255)
    protected val buttonHoverColor = ImColor.rgba(68, 68, 70, 255)

    private fun updatePanelSize() {
        val displayWidth = ImGui.getIO().displaySize.x
        val displayHeight = ImGui.getIO().displaySize.y

//        panelWidth = panelWidth.coerceIn(minWidth, displayWidth * maxWidthPercent)
//        panelHeight = panelHeight.coerceIn(minHeight, displayHeight * maxHeightPercent)


//        manager.snapPanel(this)
    }

    internal fun setupPanel(graphics: CanvasGraphics, manager: PanelManager) {
        this.manager = manager
        this.graphics = graphics
    }


    fun render(drawList: ImDrawList, position: Vector2f, scale: Float) {

        this.position = position
        val newWindowSize = Vector2i(Minecraft.getInstance().window.width, Minecraft.getInstance().window.height)
        if (newWindowSize != windowSize) {
            updatePanelSize()
            onResize()
            windowSize.set(newWindowSize)
        }

        val size = Vector2f(panelWidth * scale, panelHeight * scale)

        renderBackground(drawList, position, size)
        renderTitle(drawList, position, size)
        renderContent(drawList, position, size)
        renderFooter(drawList, position, size)
        renderResizeHandle(drawList, position, size)
        handleResize(position, size, scale)
    }

    internal open fun onResize() = Unit

    private fun renderResizeHandle(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        val handleSize = 15
        val handlePos = Vector2f(position.x + size.x - handleSize, position.y + size.y - handleSize)

        // Draw the resize handle (caret)
//        drawList.addTriangleFilled(
//            handlePos.x, handlePos.y, handlePos.x + handleSize, handlePos.y, handlePos.x, handlePos.y + handleSize,
//            ImGui.colorConvertFloat4ToU32(0.3f, 0.3f, 0.3f, 1f)
//        )
        drawList.addTriangleFilled(
            handlePos.x,
            handlePos.y + handleSize,
            handlePos.x + handleSize,
            handlePos.y + handleSize,
            handlePos.x + handleSize,
            handlePos.y,
            ImGui.colorConvertFloat4ToU32(0.3f, 0.3f, 0.3f, 1f)
        )
        //The opposite in white
//        drawList.addTriangleFilled(
//            handlePos.x, handlePos.y, handlePos.x + handleSize, handlePos.y, handlePos.x, handlePos.y + handleSize,
//            ImGui.colorConvertFloat4ToU32(1f, 1f, 1f, 1f)
//        )
    }

    private fun handleResize(position: Vector2f, size: Vector2f, scale: Float) {
        val handleSize = 15f
        val handlePos = Vector2f(position.x + size.x - handleSize, position.y + size.y - handleSize)
        val mousePos = ImGui.getMousePos()

        if (ImGui.isMouseHoveringRect(handlePos.x, handlePos.y, handlePos.x + handleSize, handlePos.y + handleSize)) {
            ImGui.setMouseCursor(ImGuiMouseCursor.ResizeNWSE)
        }

        if (ImGui.isMouseClicked(0) && ImGui.isMouseHoveringRect(
                handlePos.x,
                handlePos.y,
                handlePos.x + handleSize,
                handlePos.y + handleSize
            )
        ) {
            isResizing = true
            resizeStartPos.set(mousePos.x, mousePos.y)
            resizeStartSize.set(size)
        }

        if (isResizing) {
            if (ImGui.isMouseDown(0)) {
                val deltaX = mousePos.x - resizeStartPos.x
                val deltaY = mousePos.y - resizeStartPos.y

                val newWidth = (resizeStartSize.x + deltaX) / scale
                val newHeight = (resizeStartSize.y + deltaY) / scale

                val displayWidth = ImGui.getIO().displaySize.x
                val displayHeight = ImGui.getIO().displaySize.y

                panelWidth = newWidth.coerceIn(minWidth, displayWidth * maxWidthPercent)
                panelHeight = newHeight.coerceIn(minHeight, displayHeight * maxHeightPercent)

                onResize()
            } else {
                isResizing = false
            }
        }
    }

    fun updatePosition(newPosition: Vector2f) {
        position = newPosition
        lastMaximizedPosition = newPosition
    }

    protected open fun renderTitle(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        val titleHeight = 30f
        drawList.addRectFilled(
            position.x,
            position.y,
            position.x + size.x,
            position.y + titleHeight,
            ImGui.colorConvertFloat4ToU32(0.3f, 0.3f, 0.3f, 1f)
        )

        iconFam[32].use {
            drawList.addText(
                it, 32f, position.x + 15f, position.y, ImGui.colorConvertFloat4ToU32(1f, 1f, 1f, 1f), icon
            )
        }

        boldFam[22].use {
            drawList.addText(
                it, 22f, position.x + 40f, position.y + 7f, ImGui.colorConvertFloat4ToU32(1f, 1f, 1f, 1f), title
            )
        }

        renderMinimizeButton(drawList, Vector2f(position.x + size.x - 30f, position.y + 5f))
    }

    protected open fun renderBackground(
        drawList: ImDrawList, position: Vector2f, size: Vector2f
    ) {
        drawList.addRectFilled(
            position.x,
            position.y,
            position.x + size.x,
            position.y + size.y,
            ImGui.colorConvertFloat4ToU32(0.2f, 0.2f, 0.2f, 1f)
        )
    }

    protected open fun renderFooter(
        drawList: ImDrawList, position: Vector2f, size: Vector2f
    ) {
        val searchBarHeight = 50f
        val searchBarWidth = size.x - 20f
        val searchBarPosition = Vector2f(position.x + 10f, position.y + size.y - searchBarHeight - 10f)
        val searchBarSize = Vector2f(searchBarWidth, searchBarHeight)
        ImGui.setCursorScreenPos(searchBarPosition.x, searchBarPosition.y)
        val pos = ImGui.getCursorScreenPos()
        ImGui.pushClipRect(
            searchBarPosition.x,
            searchBarPosition.y,
            searchBarPosition.x + searchBarSize.x,
            searchBarPosition.y + searchBarSize.y,
            true
        )
        ImGui.setNextWindowPos(searchBarPosition.x, searchBarPosition.y)
//        ImGui.getForegroundDrawList()
//            .addRect(pos.x, pos.y, pos.x + searchBarSize.x, pos.y + searchBarSize.y, ImColor.rgba(255, 0, 60, 255), 5f)
        if (ImGui.beginChild("##footer$title", 0f, 0f)) {
            val startPos = ImGui.getCursorScreenPos()
            drawList.addRectFilled(
                startPos.x,
                startPos.y,
                startPos.x + searchBarSize.x,
                startPos.y + searchBarSize.y,
                ImColor.rgba(33, 32, 35, 255),
                7f
            )
            renderFooterContent(drawList, startPos.toVec2f, searchBarSize)
        }
        ImGui.endChild()
        ImGui.popClipRect()
    }

    private val searchBuffer = ImString(128)
    private fun renderSearchInput(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        // Search icon
        iconFam[32].use {
            drawList.addText(
                it, 32f, position.x + 5f, position.y - 2f, ImColor.rgba(150, 150, 150, 255), FontAwesome.MagnifyingGlass
            )
        }

        // Search input field
        bodyFam.header.use {
            ImGui.setCursorScreenPos(position.x + 25f, position.y)
            ImGui.pushItemWidth(size.x - 30f)
            ImGui.pushStyleColor(ImGuiCol.FrameBg, ImColor.rgba(60, 60, 60, 0).toInt())
            ImGui.pushStyleColor(ImGuiCol.Text, ImColor.rgba(200, 200, 200, 255).toInt())
            ImGui.pushStyleColor(ImGuiCol.Border, ImColor.rgba(60, 60, 60, 255).toInt())
            ImGui.inputText("##search", searchBuffer)
            ImGui.popStyleColor(3)
            ImGui.popItemWidth()
        }

    }

    protected open fun renderFooterContent(
        drawList: ImDrawList, position: Vector2f, size: Vector2f
    ) = Unit

    private fun renderMinimizeButton(drawList: ImDrawList, position: Vector2f) {
        val buttonSize = 20f
        val isHovered = isMouseOver(position, buttonSize, buttonSize)
        val buttonColor = if (isHovered) ImGui.colorConvertFloat4ToU32(0.4f, 0.4f, 0.4f, 1f)
        else ImGui.colorConvertFloat4ToU32(0.3f, 0.3f, 0.3f, 1f)

        drawList.addRectFilled(
            position.x, position.y, position.x + buttonSize, position.y + buttonSize, buttonColor, 5f
        )

        iconFam[18].use {
            drawList.addText(
                it,
                18f,
                position.x + 2f,
                position.y + 2f,
                ImGui.colorConvertFloat4ToU32(1f, 1f, 1f, 1f),
                "\uf078" // Down arrow
            )
        }

        if (isHovered && ImGui.isMouseClicked(0)) {
            manager.minimizePanel(this)
        }
    }


    private fun renderContent(
        drawList: ImDrawList, position: Vector2f, size: Vector2f
    ) {
        val contentStart = Vector2f(position.x + 10f, position.y + 40f)
        val contentSize = Vector2f(size.x - 20f, size.y - 110f)
        drawList.addRectFilled(
            contentStart.x,
            contentStart.y,
            contentStart.x + contentSize.x,
            contentStart.y + contentSize.y,
            ImGui.colorConvertFloat4ToU32(0.1f, 0.1f, 0.1f, 1f),
            10f
        )
        ImGui.pushClipRect(
            contentStart.x, contentStart.y, contentStart.x + contentSize.x, contentStart.y + contentSize.y, true
        )


        ImGui.setNextWindowPos(contentStart.x, contentStart.y)
        if (ImGui.beginChild("##content_$title", contentSize.x, contentSize.y, false)) {
            renderBody(drawList, contentStart, contentSize)
        }
        ImGui.endChild()
        renderAfter(graphics, drawList, Vector2f(position), size)
        ImGui.popClipRect()
    }

    protected abstract fun renderBody(
        drawList: ImDrawList, position: Vector2f, size: Vector2f
    )

    protected open fun renderAfter(graphics: CanvasGraphics, drawList: ImDrawList, position: Vector2f, size: Vector2f) =
        Unit

    protected fun isMouseOver(pos: Vector2f, width: Float, height: Float): Boolean {
        val mousePos = ImGui.getMousePos()
        return mousePos.x >= pos.x && mousePos.x <= pos.x + width && mousePos.y >= pos.y && mousePos.y <= pos.y + height
    }

    open fun renderPost(gfx: GuiGraphics, scaledPos: Vector3f, scaledSize: Vector3f) = Unit


}