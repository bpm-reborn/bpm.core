package bpm.client.render.panels

import bpm.client.dockspace.Panel
import imgui.ImColor
import imgui.ImDrawList

object Panels {

    object Links : Panel(title = "Links") {

        override fun renderContent(drawList: ImDrawList) {
            drawList.addText(position.x + 100f, position.y + 100f, ImColor.rgba(195, 134, 162, 255), "Hello, world!")
        }
    }

    object Variables : Panel(title = "Variables") {

        override fun renderContent(drawList: ImDrawList) {
            drawList.addText(position.x + 100f, position.y + 100f, ImColor.rgba(195, 134, 162, 255), "Hello, world!")
        }
    }

}