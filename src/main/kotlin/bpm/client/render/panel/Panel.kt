package bpm.client.render.panel

import imgui.ImDrawList
import imgui.ImGui
import org.joml.Vector2f
import bpm.client.font.Fonts
import bpm.client.runtime.windows.CanvasContext
import bpm.client.runtime.windows.CanvasGraphics
import bpm.client.utils.use
import bpm.common.network.Endpoint

abstract class Panel(val title: String, val icon: String, protected val manager: PanelManager) {

    val iconFont = Fonts.getFamily("Fa")["Regular"][32]
    val textFont = Fonts.getFamily("Inter")["Bold"][24]
    private val displaySize get() = ImGui.getIO().displaySize
    protected val context = Endpoint.installed<CanvasContext>()
    val panelWidth get() = (displaySize.x * 0.33f).coerceAtLeast(350f).coerceAtMost(500f)
    val panelHeight get() = (displaySize.y * 0.5f).coerceAtLeast(150f)
    var isDragging = false
    var position = Vector2f()
    var lastMaximizedPosition = Vector2f()


    fun render(graphics: CanvasGraphics, drawList: ImDrawList, position: Vector2f, scale: Float) {
        this.position = position
        val size = Vector2f(panelWidth * scale, panelHeight * scale)

        renderBackground(graphics, drawList, position, size)
        renderTitle(graphics, drawList, position, size)
        renderContent(graphics, drawList, position, size)
    }


    fun updatePosition(newPosition: Vector2f) {
        position = newPosition
        lastMaximizedPosition = newPosition
//        manager.updatePanelPosition(this, newPosition)
    }

    protected open fun renderTitle(graphics: CanvasGraphics, drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        val titleHeight = 30f
        drawList.addRectFilled(
            position.x,
            position.y,
            position.x + size.x,
            position.y + titleHeight,
            ImGui.colorConvertFloat4ToU32(0.3f, 0.3f, 0.3f, 1f)
        )

        iconFont.use {
            drawList.addText(
                iconFont,
                20f,
                position.x + 10f,
                position.y + 5f,
                ImGui.colorConvertFloat4ToU32(1f, 1f, 1f, 1f),
                icon
            )
        }

        textFont.use {
            drawList.addText(
                textFont,
                16f,
                position.x + 40f,
                position.y + 7f,
                ImGui.colorConvertFloat4ToU32(1f, 1f, 1f, 1f),
                title
            )
        }

        renderMinimizeButton(drawList, Vector2f(position.x + size.x - 30f, position.y + 5f))
    }

    protected open fun renderBackground(
        graphics: CanvasGraphics,
        drawList: ImDrawList,
        position: Vector2f,
        size: Vector2f
    ) {
        drawList.addRectFilled(
            position.x,
            position.y,
            position.x + size.x,
            position.y + size.y,
            ImGui.colorConvertFloat4ToU32(0.2f, 0.2f, 0.2f, 1f)
        )
    }

    private fun renderMinimizeButton(drawList: ImDrawList, position: Vector2f) {
        val buttonSize = 20f
        val isHovered = isMouseOver(position, buttonSize, buttonSize)
        val buttonColor = if (isHovered) ImGui.colorConvertFloat4ToU32(0.4f, 0.4f, 0.4f, 1f)
        else ImGui.colorConvertFloat4ToU32(0.3f, 0.3f, 0.3f, 1f)

        drawList.addRectFilled(
            position.x,
            position.y,
            position.x + buttonSize,
            position.y + buttonSize,
            buttonColor,
            5f
        )

        iconFont.use {
            drawList.addText(
                iconFont,
                16f,
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


    private fun renderContent(graphics: CanvasGraphics, drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        val contentStart = Vector2f(position.x + 10f, position.y + 40f)
        val contentSize = Vector2f(size.x - 20f, size.y - 110f)
        ImGui.pushClipRect(
            contentStart.x,
            contentStart.y,
            contentStart.x + contentSize.x,
            contentStart.y + contentSize.y,
            true
        )
        ImGui.setNextWindowPos(contentStart.x, contentStart.y)
        if (ImGui.beginChild("##content_$title", contentSize.x, contentSize.y, false)) {
            renderBody(graphics, drawList, contentStart, contentSize)
        }
        ImGui.endChild()
        renderAfter(graphics, drawList, Vector2f(position), size)
        ImGui.popClipRect()
    }

    protected abstract fun renderBody(
        graphics: CanvasGraphics,
        drawList: ImDrawList,
        position: Vector2f,
        size: Vector2f
    )

    protected open fun renderAfter(graphics: CanvasGraphics, drawList: ImDrawList, position: Vector2f, size: Vector2f) =
        Unit

    private fun isMouseOver(pos: Vector2f, width: Float, height: Float): Boolean {
        val mousePos = ImGui.getMousePos()
        return mousePos.x >= pos.x && mousePos.x <= pos.x + width &&
                mousePos.y >= pos.y && mousePos.y <= pos.y + height
    }


}