package bpm.client.render.panel
//
import bpm.client.render.panel.Panel
import bpm.client.runtime.windows.CanvasGraphics
import imgui.ImGui
import imgui.flag.*
import imgui.type.ImBoolean
import imgui.internal.ImGui as ImGuiInternal
import java.util.concurrent.ConcurrentHashMap
import org.joml.Vector2f
//
//class DockspaceManager(private val graphics: CanvasGraphics) {
//
//    private val panels = ConcurrentHashMap<String, Panel>()
//
//    fun init() {
//        val dockspaceId = ImGui.getID("MyDockspace")
//        // Set up the initial docking layout when the application starts
//        ImGuiInternal.dockBuilderRemoveNode(dockspaceId)
//        ImGuiInternal.dockBuilderAddNode(dockspaceId, imgui.internal.flag.ImGuiDockNodeFlags.DockSpace)
//
//        // Set the dockspace size to match the main viewport
//        val viewport = ImGui.getMainViewport()
//        ImGuiInternal.dockBuilderSetNodeSize(dockspaceId, viewport.sizeX, viewport.sizeY)
//
//        // Create the default docking splits
//        val leftId = ImGuiInternal.dockBuilderSplitNode(dockspaceId, ImGuiDir.Left, 0.2f, null, null)
//        val leftBottomId = ImGuiInternal.dockBuilderSplitNode(leftId, ImGuiDir.Down, 0.5f, null, null)
//        val mainAreaId = ImGuiInternal.dockBuilderGetCentralNode(dockspaceId).id
//        val consoleId = ImGuiInternal.dockBuilderSplitNode(mainAreaId, ImGuiDir.Down, 0.3f, null, null)
//
//        // Set up default docking locations
//        ImGuiInternal.dockBuilderDockWindow("Variables", leftId)
//        ImGuiInternal.dockBuilderDockWindow("Proxies", leftBottomId)
//        ImGuiInternal.dockBuilderDockWindow("Console", consoleId)
//
//        ImGuiInternal.dockBuilderFinish(dockspaceId)
//    }
//
//    fun beginDockspace() {
//        val viewport = ImGui.getMainViewport()
//        ImGui.setNextWindowPos(viewport.posX, viewport.posY)
//        ImGui.setNextWindowSize(viewport.sizeX, viewport.sizeY)
//        ImGui.setNextWindowViewport(viewport.id)
//
//        val windowFlags = ImGuiWindowFlags.MenuBar or
//                ImGuiWindowFlags.NoDocking or
//                ImGuiWindowFlags.NoTitleBar or
//                ImGuiWindowFlags.NoCollapse or
//                ImGuiWindowFlags.NoResize or
//                ImGuiWindowFlags.NoMove or
//                ImGuiWindowFlags.NoBringToFrontOnFocus or
//                ImGuiWindowFlags.NoNavFocus
//
//        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0f)
//        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0f)
//        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f)
//
//        ImGui.begin("DockSpace", windowFlags)
//        ImGui.popStyleVar(3)
//
//        val dockspaceId = ImGui.getID("MyDockspace")
//        ImGui.dockSpace(dockspaceId, 0f, 0f, ImGuiDockNodeFlags.None)
//    }
//
//    fun endDockspace() {
//        ImGui.end()
//    }
//
//    fun renderPanels() {
//        panels.values.forEach { panel ->
//            ImGui.begin(panel.title, ImGuiWindowFlags.None)
//            panel.render(ImGui.getWindowDrawList(), Vector2f(ImGui.getCursorScreenPos().x, ImGui.getCursorScreenPos().y), 1f)
//            ImGui.end()
//        }
//    }
//
//    fun addPanel(panel: Panel) {
//        panels[panel.title] = panel
//        panel.setupPanel(graphics)
//    }
//}

class DockspaceManager(private val graphics: CanvasGraphics) {
    private var initialized = false
    private val dockspaceID = ImGui.getID("MyDockspace")
    private val firstTime = ImBoolean(true)

    private fun setupDockspace() {
        if (!firstTime.get()) return

        ImGuiInternal.dockBuilderRemoveNode(dockspaceID)
        ImGuiInternal.dockBuilderAddNode(dockspaceID, imgui.internal.flag.ImGuiDockNodeFlags.DockSpace)

        val mainDockID = dockspaceID

        // Create splits
        val leftID = ImGuiInternal.dockBuilderSplitNode(mainDockID, ImGuiDir.Left, 0.20f, null, null)
        val leftDownID = ImGuiInternal.dockBuilderSplitNode(leftID, ImGuiDir.Down, 0.50f, null, null)
        val remainingID = ImGuiInternal.dockBuilderGetCentralNode(mainDockID).id
        val bottomID = ImGuiInternal.dockBuilderSplitNode(remainingID, ImGuiDir.Down, 0.30f, null, null)

        // Dock windows
        ImGuiInternal.dockBuilderDockWindow("Test Left Top", leftID)
        ImGuiInternal.dockBuilderDockWindow("Test Left Bottom", leftDownID)
        ImGuiInternal.dockBuilderDockWindow("Test Console", bottomID)
        ImGuiInternal.dockBuilderDockWindow("Test Canvas", remainingID)

        ImGuiInternal.dockBuilderFinish(mainDockID)
        firstTime.set(false)
    }

    fun beginDockspace() {
        val viewport = ImGui.getMainViewport()

        ImGui.setNextWindowPos(viewport.posX, viewport.posY)
        ImGui.setNextWindowSize(viewport.sizeX, viewport.sizeY)
        ImGui.setNextWindowViewport(viewport.id)

        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f)

        val window_flags = ImGuiWindowFlags.NoDocking or
                ImGuiWindowFlags.NoTitleBar or
                ImGuiWindowFlags.NoCollapse or
                ImGuiWindowFlags.NoResize or
                ImGuiWindowFlags.NoMove or
                ImGuiWindowFlags.NoBringToFrontOnFocus or
                ImGuiWindowFlags.NoNavFocus

        if (ImGui.begin("DockSpaceDemo", window_flags)) {
            ImGui.popStyleVar(3)

            ImGui.dockSpace(dockspaceID, 0f, 0f)
            setupDockspace()

            // Test windows
            if (ImGui.begin("Test Left Top", ImGuiWindowFlags.NoCollapse)) {
                ImGui.text("Left Top Content")
            }
            ImGui.end()

            if (ImGui.begin("Test Left Bottom", ImGuiWindowFlags.NoCollapse)) {
                ImGui.text("Left Bottom Content")
            }
            ImGui.end()

            if (ImGui.begin("Test Console", ImGuiWindowFlags.NoCollapse)) {
                ImGui.text("Console Content")
            }
            ImGui.end()

            if (ImGui.begin("Test Canvas", ImGuiWindowFlags.NoCollapse)) {
                ImGui.text("Canvas Content")
            }
            ImGui.end()
        }
        ImGui.end()
    }

    fun endDockspace() {
        // Nothing needed here anymore
    }

    // We can add these back once the basic docking is working
    fun renderPanels() {}
    fun addPanel(panel: Panel) {}
}