package bpm.client.runtime.panel

import bpm.client.font.Fonts
import imgui.ImColor
import imgui.ImDrawList
import org.joml.Vector2f
data object ProxiesPanel : Panel("Proxies", "\uf1ec") {

    private val font = Fonts.getFamily("Inter")["Regular"][32]

    override fun renderBody(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        drawList.addText(
            font,
            32f,
            position.x + 10f,
            position.y + 10f,
            ImColor.rgba(34, 28, 24, 255),
            "Proxies"
        )
    }
}