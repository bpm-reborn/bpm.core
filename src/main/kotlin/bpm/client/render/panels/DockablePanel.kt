package bpm.client.render.panels

import org.joml.Vector2f

abstract class DockablePanel(
    title: String,
    initialPosition: Vector2f = Vector2f(0f, 0f),
    initialSize: Vector2f = Vector2f(300f, 200f)
) : BasePanel(title, initialPosition, initialSize), IDockable {
    //Each dockable panel will be contain a dockspace it's self
    override var dockId: Int = dockIdCounter++
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

    companion object {

        const val DOCK_ID_NONE = -1
        //Start at 1 as 0 is reserved for the root
        private var dockIdCounter = 1
    }
}