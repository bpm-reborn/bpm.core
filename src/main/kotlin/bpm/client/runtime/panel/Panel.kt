package bpm.client.runtime.panel

import imgui.ImDrawList
import imgui.ImGui
import org.joml.Vector2f
import bpm.client.font.Fonts
import bpm.client.utils.use


abstract class Panel(private val title: String, private val icon: String) {

    protected val iconFont = Fonts.getFamily("Fa")["Regular"][32]
    protected val textFont = Fonts.getFamily("Inter")["Regular"][24]
    private val displaySize get() = ImGui.getIO().displaySize


    //Gets the width a 33% of the display size or min of 150px
    val panelWidth get() = (displaySize.x * 0.33f).coerceAtLeast(220f).coerceAtMost(500f)

    //Gets the height a 50% of the display size or min of 150px
    val panelHeight get() = (displaySize.y * 0.5f).coerceAtLeast(150f)

    fun render(drawList: ImDrawList, position: Vector2f) {
        val size = Vector2f(panelWidth, panelHeight)
        renderBackground(drawList, position, size)
        renderTitle(drawList, position, size)
        renderContent(drawList, position, size)
    }

    private fun renderBackground(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        drawList.addRectFilled(
            position.x,
            position.y,
            position.x + size.x,
            position.y + size.y,
            ImGui.colorConvertFloat4ToU32(0.2f, 0.2f, 0.2f, 1f)
        )
    }

    private fun renderTitle(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
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
    }

    private fun renderContent(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        val contentStart = Vector2f(position.x + 10f, position.y + 40f)
        ImGui.pushClipRect(
            contentStart.x,
            contentStart.y,
            contentStart.x + size.x - 20f,
            contentStart.y + size.y - 50f,
            true
        )
        //Draw border
        drawList.addRect(
            contentStart.x,
            contentStart.y,
            contentStart.x + size.x - 20f,
            contentStart.y + size.y - 50f,
            ImGui.colorConvertFloat4ToU32(0.3f, 0.3f, 0.3f, 1f)
        )
        renderBody(drawList, contentStart, size)
        ImGui.popClipRect()
    }

    protected abstract fun renderBody(drawList: ImDrawList, position: Vector2f, size: Vector2f)
}
