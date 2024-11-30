package bpm.client.render.panels

import imgui.ImDrawList
import org.joml.Vector2f

interface IPanel {

    val id: String
    val title: String
    var isVisible: Boolean
    var position: Vector2f
    var size: Vector2f

    fun render(drawList: ImDrawList)
    fun onResize(newSize: Vector2f)
    fun onMove(newPosition: Vector2f)
}