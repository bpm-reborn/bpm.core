package bpm.client.render.panels

interface IDockable {

    var dockId: Int
    var isDocked: Boolean
    var dockPosition: DockPosition

    fun onDock(dockId: Int, position: DockPosition)
    fun onUndock()

}