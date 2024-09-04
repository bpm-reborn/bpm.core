package bpm.client.runtime.renders

import imgui.flag.ImGuiDir

data class Split(val direction: Direction, val size: Float) {
    enum class Direction(val dir: Int, val defaultSize: Float) {
        LEFT(ImGuiDir.Left, 0.2f),
        RIGHT(ImGuiDir.Right, 0.15f),
        UP(ImGuiDir.Up, 0.2f),
        DOWN(ImGuiDir.Down, 0.25f),
        CENTER(ImGuiDir.None, 0f)
    }
}