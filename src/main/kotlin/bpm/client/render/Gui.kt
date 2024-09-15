package bpm.client.render

import bpm.client.font.Fonts
import bpm.client.utils.use
import imgui.ImColor
import imgui.ImDrawList
import imgui.ImFont
import imgui.ImGui
import imgui.flag.ImGuiMouseButton
import org.joml.Vector2f

object Gui {

    val font = Fonts.getFamily("Inter")["Regular"]

    fun toolTip(text: String) {
        //Draws a tooltip of the given text at the mouse position
        val drawList = ImGui.getForegroundDrawList()
        val bodyFont = font.header
        bodyFont.use {
            val size = ImGui.calcTextSize(text)
            val padding = 4f
            val pos = ImGui.getMousePos()
            val windowPos = ImGui.getWindowPos()
            val windowSize = ImGui.getWindowSize()
            //Draws the background of the tooltip
            val pos1 = Vector2f(pos.x, pos.y + 20)
            val pos2 = Vector2f(pos1.x + size.x + padding * 2, pos1.y + size.y + padding * 2)
            drawList.addRectFilled(pos1.x, pos1.y, pos2.x, pos2.y, ImColor.rgba(0.1f, 0.1f, 0.1f, 0.9f))
            //Draws the text of the tooltip
            drawList.addText(pos1.x + padding, pos1.y + padding, ImColor.rgba(1f, 1f, 1f, 1f), text)
        }
    }

    fun isMouseOver(position: Vector2f, size: Vector2f): Boolean {
        val mousePos = ImGui.getMousePos()
        return mousePos.x >= position.x && mousePos.x <= position.x + size.x &&
                mousePos.y >= position.y && mousePos.y <= position.y + size.y
    }


}

