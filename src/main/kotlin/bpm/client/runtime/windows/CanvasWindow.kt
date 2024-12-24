package bpm.client.runtime.windows

import bpm.client.font.Fonts
import bpm.client.render.IRender
import bpm.client.render.panel.ConsolePanel
import bpm.client.render.panel.DockspaceManager
import bpm.client.render.panel.ProxiesPanel
import bpm.client.render.panel.VariablesPanel
import bpm.client.render.renderspace.Renderspace
import bpm.client.render.renderspace.Split
import bpm.client.runtime.ClientRuntime
import bpm.client.runtime.Keyboard
import bpm.client.utils.toVec2f
import bpm.common.network.Endpoint
import bpm.common.utils.FontAwesome
import bpm.common.workspace.Workspace
import bpm.common.workspace.WorkspaceSettings
import bpm.common.workspace.graph.Link
import bpm.common.workspace.graph.Node
import imgui.ImColor
import imgui.ImDrawList
import imgui.ImGui
import imgui.ImVec2
import imgui.flag.*
import net.minecraft.client.gui.GuiGraphics
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector4f
import java.util.*

class CanvasWindow(private val runtime: ClientRuntime) : IRender {

    val workspace: Workspace get() = runtime.workspace ?: error("Workspace not set")

    /**
     * Represents the canvas context used in the application.
     *
     * @property context The canvas context instance.
     */
    val context = Endpoint.installed<CanvasContext>()

    private val buttons: MutableSet<CanvasButton> = HashSet()
    val graphics get() = context.graphics
    private val fontAwesomeFamily = Fonts.getFamily("Fa")["Regular"]
    private val fontAwesome get() = fontAwesomeFamily[workspace.settings.fontHeaderSize]
    private val renderSpace = Renderspace("Dockspace").apply {
        val root = addWindow("Canvas", this@CanvasWindow).apply {
            noTitleBar = true
        }
        root.addWindow(
            "Variables",
            VariablesPanel,
            splitDirection = Split.Direction.LEFT,
            splitSize = 0.33f
        ).apply {
            noTitleBar = true
        }
        root.addWindow(
            "Proxies",
            ProxiesPanel,
            splitDirection = Split.Direction.RIGHT,
            splitSize = 0.5f
        ).apply {
            noTitleBar = true
        }
        root.addWindow(
            "Console",
            ConsolePanel,
            splitDirection = Split.Direction.DOWN,
            splitSize = 0.5f
        ).apply {
            noTitleBar = true
        }
    }


    private val selectionContextOverlay = SelectionContextOverlay(workspace)

    /**
     * Returns the bounds of the context settings as a 4D vector.
     *
     * The bounds represent the range or extent of the context settings.
     *
     * @return The bounds of the context settings as a 4D vector.
     */
    val bounds: Vector4f get() = workspace.settings.bounds

    /**
     * A private constant representing the offset vector.
     *
     * This vector represents the position of the canvas relative to the center of the workspace.
     *
     * @return The position vector of the canvas.
     */
    private val position: Vector2f get() = workspace.settings.position

    var currentTime = 0f
        private set
    private var hoveredNodeId: UUID? = null
    private var hoveredLinkId: UUID? = null
    val hoveredNode: Node? get() = hoveredNodeId?.let { workspace.getNode(it) }
    val hoveredLink: Link? get() = hoveredLinkId?.let { workspace.getLink(it) }
    var shouldCancelActions = false
        private set

    init {
        buttons += CanvasButton(
            fontAwesome,
            bounds.z - 50f,
            bounds.y + 20f,
            FontAwesome.Play,
            fontSize = 25f,
            width = 30f,
            height = 30f
        ) {
            runtime.compile(workspace)
        }

        buttons += CanvasButton(
            fontAwesome,
            bounds.z - 50f,
            bounds.y + 60f,
            FontAwesome.Rotate,
            fontSize = 25f,
            width = 30f,
            height = 30f
        ) {
            runtime.reloadNodeLibrary()
        }

        buttons += CanvasButton(
            fontAwesome,
            bounds.z - 50f,
            bounds.y + 100f,
            FontAwesome.Crosshairs,
            fontSize = 25f,
            width = 30f,
            height = 30f
        ) {
            context.center()
        }
    }

    private val lastSize = Vector2i()

    fun process(gfx: GuiGraphics) {
        ImGui.setNextWindowPos(0f, 0f)
        ImGui.setNextWindowSize(ImGui.getIO().displaySize.x, 50f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0f)
        ImGui.begin(
            "Navbar",
            ImGuiWindowFlags.NoTitleBar or ImGuiWindowFlags.NoResize or ImGuiWindowFlags.NoMove or ImGuiWindowFlags.NoCollapse or ImGuiWindowFlags.NoScrollbar or ImGuiWindowFlags.NoScrollWithMouse or ImGuiWindowFlags.NoBringToFrontOnFocus
                    or ImGuiWindowFlags.NoNavFocus or ImGuiWindowFlags.NoBackground
        )
        drawNavbar(ImGui.getWindowDrawList())
        ImGui.end()
        ImGui.popStyleVar(2)
        renderSpace.process(gfx, graphics)
    }

    private fun drawNavbar(drawList: ImDrawList) {
        val startPos = ImGui.getCursorScreenPos()
        val size = ImVec2(ImGui.getIO().displaySize.x, 50f)
        drawList.addRectFilled(
            startPos.x,
            startPos.y,
            startPos.x + size.x, startPos.y + size.y,
            ImColor.rgba(50, 50, 50, 255)
        )


        //Draw the play, stop, refresh, and focus/unfocus buttons
        val buttonSize = 30f
        val buttonPadding = 10f
        val buttonStart = ImVec2(size.x - buttonSize - buttonPadding, startPos.y + buttonPadding)
        val buttonStop = ImVec2(size.x - buttonSize * 2 - buttonPadding * 2, startPos.y + buttonPadding)
        val buttonRefresh = ImVec2(size.x - buttonSize * 3 - buttonPadding * 3, startPos.y + buttonPadding)
        val buttonFocus = ImVec2(size.x - buttonSize * 4 - buttonPadding * 4, startPos.y + buttonPadding)

        drawList.addRectFilled(
            buttonStart.x,
            buttonStart.y,
            buttonStart.x + buttonSize,
            buttonStart.y + buttonSize,
            ImColor.rgba(50, 50, 50, 255)
        )
        drawList.addRectFilled(
            buttonStop.x,
            buttonStop.y,
            buttonStop.x + buttonSize,
            buttonStop.y + buttonSize,
            ImColor.rgba(50, 50, 50, 255)
        )

        drawList.addRectFilled(
            buttonRefresh.x,
            buttonRefresh.y,
            buttonRefresh.x + buttonSize,
            buttonRefresh.y + buttonSize,
            ImColor.rgba(50, 50, 50, 255)
        )

        drawList.addRectFilled(
            buttonFocus.x,
            buttonFocus.y,
            buttonFocus.x + buttonSize,
            buttonFocus.y + buttonSize,
            ImColor.rgba(50, 50, 50, 255)
        )

        val fontAwesome = fontAwesomeFamily[25]
        drawList.addText(
            fontAwesome,
            25f,
            buttonStart.x + 5f,
            buttonStart.y,
            ImColor.rgba(255, 255, 255, 255),
            FontAwesome.Play
        )
        drawList.addText(
            fontAwesome,
            25f,
            buttonStop.x + 5f,
            buttonStop.y,
            ImColor.rgba(255, 255, 255, 255),
            FontAwesome.Stop
        )
        drawList.addText(
            fontAwesome,
            25f,
            buttonRefresh.x + 5f,
            buttonRefresh.y,
            ImColor.rgba(255, 255, 255, 255),
            FontAwesome.Rotate
        )
        drawList.addText(
            fontAwesome,
            25f,
            buttonFocus.x + 5f,
            buttonFocus.y,
            ImColor.rgba(255, 255, 255, 255),
            FontAwesome.Crosshairs
        )


        //Draw the title
        val title = workspace.workspaceName
        val titleSize = ImGui.calcTextSize(title)
        val titleStart = ImVec2(startPos.x + 10f, startPos.y + 10f)
        drawList.addText(
            fontAwesome,
            25f,
            titleStart.x,
            titleStart.y,
            ImColor.rgba(255, 255, 255, 255),
            title
        )
    }


    /**
     * Manage all the rendering related to the main canvas here.
     */

    override fun render(gfx: CanvasGraphics, guiGfx: GuiGraphics) {
        val isActionMenuHovered = CustomActionMenu.isVisible() && CustomActionMenu.isHovered()
        setupCanvas()

        shouldCancelActions = isActionMenuHovered || selectionContextOverlay.isHovered()

        if (!selectionContextOverlay.isHovered() && !shouldCancelActions) {
            handleCanvas()
        }

        val mousePos = ImGui.getMousePos()
        val displaySize = ImGui.getIO().displaySize

        handleWindowResize(displaySize)
        updateAnimationTime()
        context.handleEdgeDragging()
        context.handleSelection(shouldCancelActions)
        handleHover()
        handleContextMenu()

        context.updateHoverState(Vector2f(mousePos.x, mousePos.y))
        ImGui.setMouseCursor(context.getHoverCursor())

        buttons.forEach { it.handleClick() }
        context.variablesMenu.update()
        handleKeys()

        // Render canvas content
        val foreground = ImGui.getForegroundDrawList()
        val drawList = ImGui.getWindowDrawList()

        graphics.renderBackground(drawList, bounds) {
            graphics.drawGrid(drawList, position, bounds, context.zoom)
            graphics.renderFunctions(drawList, workspace.graph.functions)
            graphics.renderLinks(drawList, workspace.graph.links)
            graphics.renderNodes(drawList, workspace.graph.nodes)
            graphics.renderEdgeDrag(drawList, context.draggedEdge, context.dragStartPos)
            graphics.renderSelectionBox(drawList)

            buttons.forEach { it.render(foreground) }
            CustomActionMenu.render(foreground)
            graphics.renderMousePosText(drawList, bounds, mousePos.toVec2f)
            graphics.renderPanels(drawList)
            context.notificationManager.renderNotifications(drawList, displaySize)
        }

        context.wasDraggingNode = false
//        }
//        ImGui.end()
//        ImGui.popStyleVar()

        // Render the panels
//        dockspaceManager.renderPanels()
//        dockspaceManager.endDockspace()
    }


    private fun handleWindowResize(displaySize: ImVec2) {
        if (lastSize.x != displaySize.x.toInt() || lastSize.y != displaySize.y.toInt()) {

            val newSize = Vector2f(displaySize.x, displaySize.y)
            val oldSize = Vector2f(lastSize.x.toFloat(), lastSize.y.toFloat())
            renderSpace.onResize(oldSize, newSize)
            lastSize.set(displaySize.x.toInt(), displaySize.y.toInt())
        }
    }


    fun renderPost(gfx: GuiGraphics) {
        graphics.renderOverlay(gfx, bounds)
    }

    private val initialOpen get() = System.currentTimeMillis() - openTime < 100
    private var openTime = System.currentTimeMillis()

    fun close() {
        savedSettings[workspace.uid] = workspace.settings
        CustomActionMenu.close()
    }

    fun open() {
        openTime = System.currentTimeMillis()
        workspace.settings = savedSettings[workspace.uid] ?: WorkspaceSettings()
    }

    companion object {

        private val savedSettings = mutableMapOf<UUID, WorkspaceSettings>()

    }

    private fun handleContextMenu() {
        if (ImGui.isMouseClicked(ImGuiMouseButton.Right) && !initialOpen && !ImGui.getIO().keyShift) {
            val mousePos = ImGui.getMousePos()
            val selectedNodes = context.selectedNodes.ifEmpty { findNodesUnderMouse(mousePos) }.toSet()
            val selectedLinks = context.selectedLinks.ifEmpty { findLinksUnderMouse(mousePos) }.toSet()

            CustomActionMenu.open(
                mousePos,
                selectedNodes.isNotEmpty() || selectedLinks.isNotEmpty(),
                true,
                selectedNodes,
                selectedLinks
            )
            context.variablesMenu.closePopup()
        }
    }

    private fun findLinksUnderMouse(mousePos: ImVec2): Set<Link> {
        return workspace.graph.links.filter { link ->
            context.isMouseOverLink(link, mousePos.toVec2f)
        }.toSet()
    }

    private fun findNodesUnderMouse(mousePos: ImVec2): Set<Node> {
        return workspace.graph.nodes.filter { node ->
            context.isMouseOverNode(node, mousePos.toVec2f)
        }.toSet()
    }

    private fun handleHover() {
        if (shouldCancelActions) {
            hoveredNodeId = null
            hoveredLinkId = null
            return
        }

        hoveredNodeId = null
        hoveredLinkId = null

        val mousePos = ImGui.getMousePos()

        // Check for node hover
        for (node in workspace.graph.nodes) {
            val bounds = context.getNodeBounds(node)
            if (mousePos.x in bounds.x..bounds.z && mousePos.y in bounds.y..bounds.w) {
                hoveredNodeId = node.uid
                break
            }
        }

        // Check for link hover if no node is hovered
        if (hoveredNodeId == null) {
            for (link in workspace.graph.links) {
                if (context.isMouseOverLink(link, mousePos.toVec2f)) {
                    hoveredLinkId = link.uid
                    break
                }
            }
        }
    }


    private fun updateAnimationTime() {
        currentTime += ImGui.getIO().deltaTime
    }

    /**
     * Initializes the canvas and sets up the scrolling and the canvas size
     */
    private fun setupCanvas() {
        val canvasStart = ImGui.getCursorScreenPos() // ImDrawList API uses screen coordinates!
        val canvasSize = ImGui.getContentRegionAvail() // Resize canvas to what's available
        if (canvasSize.x < 50.0f) canvasSize.x = 50.0f
        if (canvasSize.y < 50.0f) canvasSize.y = 50.0f
        val canvasStop = ImVec2(canvasStart.x + canvasSize.x, canvasStart.y + canvasSize.y)
        bounds.set(canvasStart.x, canvasStart.y, canvasStop.x, canvasStop.y)
        // This will catch our interactions
        ImGui.invisibleButton(
            "canvas",
            canvasSize.x,
            canvasSize.y,
            ImGuiButtonFlags.MouseButtonLeft or ImGuiButtonFlags.MouseButtonRight or ImGuiButtonFlags.MouseButtonMiddle
        )
    }

    private fun handleKeys() {
        if (Keyboard.isKeyPressed(ClientRuntime.Key.DELETE)) {
            context.deleteSelected()
            CustomActionMenu.close()
        }
    }

    /**
     * Used to update the scrolled offset of the canvas
     */
    private fun handleCanvas() {
        if (shouldCancelActions || !ImGui.isItemHovered()) {
            return
        }
        val isActive = ImGui.isItemActive() // Held
        val io = ImGui.getIO()
        // Pan (we use a zero mouse threshold when there's no context menu)
        // You may decide to make that threshold dynamic based on whether the mouse is hovering something etc.
        val mouseThresholdForPan = -1.0f

        // Check for middle mouse button panning
        val isMiddleMousePanning = isActive && ImGui.isMouseDragging(ImGuiMouseButton.Middle, mouseThresholdForPan)

        // Check for Shift + Right click panning
        val isShiftRightClickPanning = isActive && ImGui.isMouseDragging(
            ImGuiMouseButton.Right,
            mouseThresholdForPan
        ) &&
                ImGui.getIO().keyShift

        // Apply panning if either method is active
        if (isMiddleMousePanning || isShiftRightClickPanning) {
            position.x += io.getMouseDelta().x
            position.y += io.getMouseDelta().y
        }

        // Context menu (under default mouse threshold)
        val dragDelta = ImGui.getMouseDragDelta(ImGuiMouseButton.Middle)
        if (dragDelta.x == 0.0f && dragDelta.y == 0.0f) {
            ImGui.openPopupOnItemClick("context", ImGuiPopupFlags.MouseButtonMiddle)
        }
        val zoom = context.zoom
        //Handles the canvasCtx.zooming
        val mouseWheel = io.mouseWheel
        if (mouseWheel != 0.0f && !context.isLinking) {
            val zoomDelta = mouseWheel * 0.10f
            context.zoom += zoomDelta
            context.zoom = context.zoom.coerceIn(0.5f, 2f)
        }

        // Adjust scrolled to keep the center point consistent
        val center = workspace.viewportCenter
        position.x -= (center.x - position.x) * (context.zoom - zoom) / zoom
        position.y -= (center.y - position.y) * (context.zoom - zoom) / zoom
    }

}

