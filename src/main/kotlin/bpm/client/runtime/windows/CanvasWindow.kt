package bpm.client.runtime.windows

import bpm.client.font.Fonts
import bpm.client.render.IRender
import bpm.client.runtime.ClientRuntime
import bpm.client.runtime.Keyboard
import bpm.client.utils.toVec2f
import bpm.common.network.Endpoint
import bpm.common.utils.FontAwesome
import bpm.common.workspace.Workspace
import bpm.common.workspace.WorkspaceSettings
import bpm.common.workspace.graph.Link
import bpm.common.workspace.graph.Node
import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiButtonFlags
import imgui.flag.ImGuiMouseButton
import imgui.flag.ImGuiPopupFlags
import imgui.flag.ImGuiWindowFlags
import org.joml.Vector2f
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
    val graphics = CanvasGraphics(this, context)

    private val buttons: MutableSet<CanvasButton> = HashSet()

    private val headerFamily get() = Fonts.getFamily("Inter")["Bold"]
    private val headerFont get() = headerFamily[workspace.settings.fontHeaderSize]
    private val bodyFamily get() = Fonts.getFamily("Inter")["Regular"]
    private val bodyFont get() = bodyFamily[workspace.settings.fontSize]
    private val fontAwesomeFamily = Fonts.getFamily("Fa")["Regular"]
    private val fontAwesome get() = fontAwesomeFamily[workspace.settings.fontHeaderSize]

    private val selectionContextOverlay = SelectionContextOverlay(workspace)


    /**
     * Returns the bounds of the context settings as a 4D vector.
     *
     * The bounds represent the range or extent of the context settings.
     *
     * @return The bounds of the context settings as a 4D vector.
     */
    private val bounds: Vector4f get() = workspace.settings.bounds

    /**
     * Represents the position of the context menu.
     *
     * This vector represents the position of the context menu.
     *
     * @return The position of the context menu.
     */
    private var contextMenuPosition = Vector2f()

    /**
     * A private constant representing the offset vector.
     *
     * This vector represents the position of the canvas relative to the center of the workspace.
     *
     * @return The position vector of the canvas.
     */
    private val position: Vector2f get() = workspace.settings.position

    var currentTime = 0f
    private val customActionMenu = context.customActionMenu

    private var hoveredNodeId: UUID? = null
    private var hoveredLinkId: UUID? = null
    val hoveredNode: Node? get() = hoveredNodeId?.let { workspace.getNode(it) }
    val hoveredLink: Link? get() = hoveredLinkId?.let { workspace.getLink(it) }

    private var isPropertyWindowHovered = false
    private var lastCanvasSize: Vector2f = Vector2f()

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

    /**
     * Manage all the rendering related to the main canvas here.
     */
    override fun render() {
        val mainViewport = ImGui.getMainViewport()
        ImGui.setNextWindowPos(mainViewport.posX, mainViewport.posY)
        ImGui.setNextWindowSize(mainViewport.sizeX, mainViewport.sizeY)

        ImGui.begin(
            "Canvas",
            ImGuiWindowFlags.NoTitleBar or ImGuiWindowFlags.NoResize or ImGuiWindowFlags.NoMove or ImGuiWindowFlags.NoScrollbar or ImGuiWindowFlags.NoScrollWithMouse or ImGuiWindowFlags.NoBringToFrontOnFocus or ImGuiWindowFlags.NoNavFocus
        )
        val drawList = ImGui.getWindowDrawList()

        val isActionMenuHovered = customActionMenu.isVisible() && customActionMenu.isHovered()
        val isVariablesMenuHovered = false

        setupCanvas()

        isPropertyWindowHovered = isActionMenuHovered || isVariablesMenuHovered

        if (!selectionContextOverlay.isHovered() && !isPropertyWindowHovered) handleCanvas()

        val mousePos = ImGui.getMousePos()
        val displaySize = ImGui.getIO().displaySize

        updateAnimationTime()
        context.handleEdgeDragging()
        context.handleSelection(selectionContextOverlay.isHovered() || isVariablesMenuHovered)
        handleHover()
        handleContextMenu()

        // Update cursor based on hover state
        context.updateHoverState(Vector2f(mousePos.x, mousePos.y))
        ImGui.setMouseCursor(context.getHoverCursor())
        buttons.forEach {
            it.handleClick()
        }
        context.variablesMenu.update()

        handleKeys()
        /**
         * Renders the background and the grids for the canvas
         */
        graphics.renderBackground(drawList, bounds) {
            graphics.drawGrid(drawList, position, bounds, context.zoom)
            graphics.renderLinks(drawList, workspace.graph.links)
            graphics.renderNodes(drawList, workspace.graph.nodes)
            graphics.renderEdgeDrag(drawList, context.draggedEdge, context.dragStartPos)
            graphics.renderSelectionBox(drawList)
            buttons.forEach {
                it.render(drawList)
            }
            graphics.renderMousePosText(drawList, bounds, mousePos.toVec2f)
            customActionMenu.render(drawList)
//            context.variablesMenu.render(drawList)
            graphics.renderPanels(drawList)
            context.notificationManager.renderNotifications(drawList, displaySize)
        }

        // Render the custom action menu
        context.wasDraggingNode = false
        ImGui.end()
    }

    private val initialOpen get() = System.currentTimeMillis() - openTime < 100
    private var openTime = System.currentTimeMillis()

    fun close() {
        savedSettings[workspace.uid] = workspace.settings
        this.position
        this.customActionMenu.close()
    }

    fun open() {
        openTime = System.currentTimeMillis()
        workspace.settings = savedSettings[workspace.uid] ?: WorkspaceSettings()
    }

    companion object {

        private val savedSettings = mutableMapOf<UUID, WorkspaceSettings>()

    }

    private fun handleContextMenu() {
        if (ImGui.isMouseClicked(ImGuiMouseButton.Right) && !initialOpen) {
            val mousePos = ImGui.getMousePos()
            val selectedNodes = context.selectedNodes.ifEmpty { findNodesUnderMouse(mousePos) }.toSet()
            val selectedLinks = context.selectedLinks.ifEmpty { findLinksUnderMouse(mousePos) }.toSet()

            customActionMenu.open(mousePos, selectedNodes.isNotEmpty() || selectedLinks.isNotEmpty())
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
        if (isPropertyWindowHovered) {
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
            customActionMenu.close()
        }
    }

    /**
     * Used to update the scrolled offset of the canvas
     */
    private fun handleCanvas() {
        if (isPropertyWindowHovered) {
            return
        }
        val isActive = ImGui.isItemActive() // Held
        val io = ImGui.getIO()
        // Pan (we use a zero mouse threshold when there's no context menu)
        // You may decide to make that threshold dynamic based on whether the mouse is hovering something etc.
        val mouseThresholdForPan = -1.0f
        if (isActive && ImGui.isMouseDragging(ImGuiMouseButton.Middle, mouseThresholdForPan)) {
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

