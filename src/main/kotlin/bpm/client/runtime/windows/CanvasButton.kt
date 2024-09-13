package bpm.client.runtime.windows

import imgui.ImColor
import imgui.ImDrawList
import imgui.ImFont
import imgui.ImGui

class CanvasButton(
    private val font: ImFont,
    private val x: Float,
    private val y: Float,
    private val icon: String,
    private val fontSize: Float = 20f,
    private val width: Float = 30f,
    private val height: Float = 30f,
    private val padding: Float = 5f,
    private val color: Int = ImColor.rgba(50, 50, 50, 255),
    private val hoverColor: Int = ImColor.rgba(60, 60, 60, 255),
    val onClick: () -> Unit = {}
) {

    fun render(drawList: ImDrawList) {
        val isHovered = ImGui.isMouseHoveringRect(x, y, x + width, y + height)
        val buttonColor = if (isHovered) hoverColor else color
        drawList.addRectFilled(
            x - padding / 2, y - padding / 2, x + width + padding / 2, y + height + padding / 2, buttonColor, 5f
        )
        drawList.addText(
            font, fontSize, x + width / 2 - 4f, y, ImColor.rgba(255, 255, 255, 255), icon
        )
    }

    fun handleClick() {
        val isHovered = ImGui.isMouseHoveringRect(x, y, x + width, y + height)
        if (isHovered && ImGui.isMouseClicked(0)) {
            onClick()
        }
    }

}