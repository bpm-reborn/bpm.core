package bpm.client.runtime.renders

import imgui.ImGui
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiWindowFlags
import imgui.internal.flag.ImGuiDockNodeFlags
import imgui.type.ImInt
import bpm.common.utils.orEquals
import imgui.internal.ImGui as ImGuiInternal

class Renderspace(dockName: String) : Dockable(dockName, null) {
    override fun render() {

        val flags = ImGuiWindowFlags.NoNavFocus.orEquals(
            ImGuiWindowFlags.NoTitleBar,
            ImGuiWindowFlags.NoCollapse,
            ImGuiWindowFlags.NoResize,
            ImGuiWindowFlags.NoMove,
            ImGuiWindowFlags.NoBringToFrontOnFocus
        )
        val viewport = ImGui.getMainViewport()
        ImGui.setNextWindowPos(0f, 0f)
        ImGui.setNextWindowSize(ImGui.getIO().displaySize.x, ImGui.getIO().displaySize.y)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f)
        ImGui.begin("Window##$dockspaceId", flags)
        ImGui.setNextWindowViewport(viewport.id)
        ImGui.popStyleVar()

        var dockspaceID = ImGui.getID(dockspaceId)
        val node = ImGuiInternal.dockBuilderGetNode(dockspaceID)
        if (node == null || node.ptr == 0L || node.id == 0 || dirty)
            rebuildDock()

        dockspaceID = ImGui.getID(dockspaceId)
        ImGui.dockSpace(dockspaceID, 0f, 0f, ImGuiDockNodeFlags.None)
        ImGui.end()
        super.render()
    }

    // Recreates the dockspace, and all docked windows. This is called when the dockspace is first created, or when a window is added/removed.
    override fun rebuildDock() {
        val viewport = ImGui.getWindowViewport()
        val dockspaceID = ImGui.getID(dockspaceId)
        ImGuiInternal.dockBuilderRemoveNode(dockspaceID)
        ImGuiInternal.dockBuilderAddNode(dockspaceID, ImGuiDockNodeFlags.DockSpace)
        ImGuiInternal.dockBuilderSetNodeSize(dockspaceID, viewport.sizeX, viewport.sizeY)
        dockMainId = ImInt(dockspaceID)
        super.rebuildDock()
        ImGuiInternal.dockBuilderFinish(dockspaceID)

    }

    override fun toString(): String {
        return dockspaceId
    }

}