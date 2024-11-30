package bpm.client.render.panels

import org.joml.Vector2f

abstract class DockablePanel(
    id: String,
    title: String,
    initialPosition: Vector2f = Vector2f(0f, 0f),
    initialSize: Vector2f = Vector2f(300f, 200f)
) : BasePanel(id, title, initialPosition, initialSize), IDockable {

    override var dockId: Int = -1
    override var isDocked: Boolean = false
    override var dockPosition: DockPosition = DockPosition.FLOATING

    override fun onDock(dockId: Int, position: DockPosition) {
        this.dockId = dockId
        this.isDocked = true
        this.dockPosition = position

        // Additional docking logic
    }

    override fun onUndock() {
        this.dockId = -1
        this.isDocked = false
        this.dockPosition = DockPosition.FLOATING

        // Additional undocking logic
    }
}