package bpm.client.render.panels

import imgui.ImDrawList
import imgui.ImGui
import imgui.flag.ImGuiWindowFlags
import org.joml.Vector2f

abstract class BasePanel(
    override val id: String,
    override val title: String,
    initialPosition: Vector2f = Vector2f(0f, 0f),
    initialSize: Vector2f = Vector2f(300f, 200f)
) : IPanel {

    override var isVisible: Boolean = true
    override var position: Vector2f = initialPosition
    override var size: Vector2f = initialSize

    protected var parent: IPanel? = null
    protected val children = mutableListOf<IPanel>()

    protected open fun beforeRender(drawList: ImDrawList) {}
    protected open fun afterRender(drawList: ImDrawList) {}

    protected abstract fun renderContent(drawList: ImDrawList)

    override fun render(drawList: ImDrawList) {
        if (!isVisible) return

        beforeRender(drawList)

        // Set up ImGui window
        ImGui.setNextWindowPos(position.x, position.y)
        ImGui.setNextWindowSize(size.x, size.y)

        val windowFlags = ImGuiWindowFlags.NoCollapse or
                ImGuiWindowFlags.NoBringToFrontOnFocus

        if (ImGui.begin("$id##$title", windowFlags)) {
            renderContent(drawList)

            // Render children
            children.forEach { child ->
                child.render(drawList)
            }
        }
        ImGui.end()

        afterRender(drawList)
    }

    override fun onResize(newSize: Vector2f) {
        size = newSize
        children.forEach { child ->
            // Implement child resizing logic based on layout
        }
    }

    override fun onMove(newPosition: Vector2f) {
        val delta = Vector2f(newPosition).sub(position)
        position = newPosition

        // Move children relative to parent movement
        children.forEach { child ->
            child.onMove(Vector2f(child.position).add(delta))
        }
    }

    fun addChild(panel: IPanel) {
        children.add(panel)
        if (panel is BasePanel) {
            panel.parent = this
        }
    }

    fun removeChild(panel: IPanel) {
        children.remove(panel)
        if (panel is BasePanel) {
            panel.parent = null
        }
    }
}