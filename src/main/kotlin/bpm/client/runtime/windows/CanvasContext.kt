package bpm.client.runtime.windows

import bpm.client.font.Fonts
import bpm.client.runtime.ClientRuntime
import bpm.client.runtime.Keyboard
import bpm.client.runtime.Platform
import bpm.client.utils.use
import bpm.common.logging.KotlinLogging
import bpm.common.network.Client
import bpm.common.network.Endpoint
import bpm.common.network.Listener
import bpm.common.network.NetUtils
import bpm.common.packets.Packet
import bpm.common.packets.internal.Time
import bpm.common.property.Property
import bpm.common.property.PropertyMap
import bpm.common.property.configured
import bpm.common.upstream.Schemas
import bpm.common.type.NodeLibrary
import bpm.common.type.NodeType
import bpm.common.utils.contains
import bpm.common.workspace.Workspace
import bpm.common.workspace.graph.Edge
import bpm.common.workspace.graph.Function
import bpm.common.workspace.graph.Link
import bpm.common.workspace.graph.Node
import bpm.common.workspace.graph.User
import bpm.common.workspace.packets.*
import imgui.ImGui
import imgui.flag.ImGuiMouseButton
import imgui.flag.ImGuiMouseCursor
import org.joml.Vector2f
import org.joml.Vector4f
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CanvasContext : Listener {

    private val runtime: ClientRuntime get() = Endpoint.installed()
    private val workspace: Workspace get() = runtime.workspace ?: throw IllegalStateException("Workspace is null")

    private val nodeMovePacket = NodeMoved()
    private var lastSent: Time = Time.now

    // The maximum send rate in milliseconds, 60 PPS (Packets Per Second)
    private val maxSendRate = 1000 / 60

    // The maximum move threshold in pixels
    private val maxMoveThreshold = 5f
    private val connectedUsers = mutableMapOf<UUID, User>()

    private var isSelecting: Boolean = false
    private var selectionStart: Vector2f? = null
    private var selectionEnd: Vector2f? = null
    private var selectedEdge: Pair<Node, Edge>? = null
    private val selectedNodeIds = mutableSetOf<UUID>()
    private val selectedLinkIds = mutableSetOf<UUID>()

    private val nodesInSelectionBox = mutableSetOf<UUID>()
    private val linksInSelectionBox = mutableSetOf<UUID>()
    val selectedNodes get() = selectedNodeIds.mapNotNull(workspace::getNode)
    val selectedLinks get() = selectedLinkIds.mapNotNull(workspace::getLink)
    private var lastSelectionBounds: Pair<Vector2f, Vector2f>? = null
    private var hasValidSelectionBounds = false


    private val headerFamily get() = Fonts.getFamily("Inter")["Bold"]
    private val headerFont get() = headerFamily[workspace.settings.fontHeaderSize]
    private val bodyFamily get() = Fonts.getFamily("Inter")["Regular"]
    private val bodyFont get() = bodyFamily[workspace.settings.fontSize]

    private var resizingFunction: Function? = null
    private var resizeStartPos: Vector2f? = null
    private var initialFunctionSize: Vector2f? = null

    internal var isLinking = false

    private val dragOffset: Vector2f = Vector2f()
    internal var isDraggingNode = false
    internal var wasDraggingNode = false
    private var isDraggingGroup = false
    private val groupDragOffset = mutableMapOf<UUID, Vector2f>()
    private var draggedSourceEdge: Pair<Node, Edge>? = null
    var draggedEdge: Pair<Any, Edge>? = null
    var dragStartPos: Vector2f? = null
    private var draggedFunction: Function? = null
    private var functionDragOffset: Vector2f = Vector2f()
    private var hoveredFunctionHeader: UUID? = null
    private val nodeOffsets = mutableMapOf<UUID, Vector2f>()
    var hoveredTitleBar: UUID? = null
    var hoveredPin: Pair<UUID, Edge>? = null

    internal val variablesMenu by lazy { VariablesMenu(this) }
    val notificationManager = NotificationManager()

    private val gridSize = 20f // Grid size for snapping
    val graphics by lazy { CanvasGraphics(runtime.canvasWindow!!, this) }

    /**
     * Represents the node library used in the application.
     *
     * @property nodeLibrary The node library instance.
     */
    private val nodeLibrary: NodeLibrary = Client.installed<Schemas>().library

    /**
     * A private constant representing the zoom level of the canvas.
     *
     * This value is used to determine the zoom level of the canvas.
     *
     * @return The zoom level of the canvas.
     */
    var zoom: Float
        get() = workspace.settings.zoom
        set(value) {
            workspace.settings.zoom = value
        }

    private fun isOverFunctionResizeHandle(function: Function, mousePos: Vector2f): Boolean {
        val bounds = getFunctionBounds(function)
        val handleSize = 10f * zoom

        // Check if mouse is in bottom-right corner
        return mousePos.x >= bounds.z - handleSize && mousePos.x <= bounds.z &&
                mousePos.y >= bounds.w - handleSize && mousePos.y <= bounds.w
    }

    fun handleNode(node: Node, nodeBounds: Vector4f, headerBounds: Vector4f) {
        if (isLinking) return

        handleStartDrag(node, headerBounds)
        if (node.dragged) {
            isDraggingNode = true
            handleNodeDrag(node, nodeBounds, headerBounds)
        }
    }

    fun updateHoverState(mousePos: Vector2f) {
        hoveredTitleBar = null
        hoveredPin = null
        updateFunctionHoverState(mousePos)

        for (node in workspace.graph.nodes) {
            val nodeBounds = getNodeBounds(node)
            val headerBounds = getHeaderBounds(node)
            if (headerBounds.contains(mousePos.x, mousePos.y)) {
                hoveredTitleBar = node.uid
                return
            }

        }
    }

    fun isPointOverEdge(point: Vector2f, edgeBounds: Vector4f): Boolean {
        return point.x >= edgeBounds.x && point.x <= edgeBounds.z && point.y >= edgeBounds.y && point.y <= edgeBounds.w
    }

    fun isPointOverEdge(point: Vector2f, edgePos: Vector2f, isExec: Boolean = false): Boolean {
        if (isExec) {
            val triangleSize = 8f * zoom
            val triangleHitbox = 12f * zoom // Slightly larger than the visual size for easier interaction
            return point.x >= edgePos.x - triangleHitbox && point.x <= edgePos.x + triangleHitbox && point.y >= edgePos.y - triangleHitbox && point.y <= edgePos.y + triangleHitbox
        } else {
            val edgeRadius = 4f * zoom
            val dx = point.x - edgePos.x
            val dy = point.y - edgePos.y
            return dx * dx + dy * dy <= edgeRadius * edgeRadius
        }
    }


    fun getHoverCursor(): Int {
        val mousePos = Vector2f(ImGui.getMousePos().x, ImGui.getMousePos().y)

        // Check if mouse is over any function's resize handle
        workspace.graph.functions.forEach { function ->
            if (isOverFunctionResizeHandle(function, mousePos)) {
                return ImGuiMouseCursor.Hand
            }
        }

        return when {
            hoveredFunctionHeader != null -> ImGuiMouseCursor.Hand
            hoveredTitleBar != null && !isDraggingNode -> ImGuiMouseCursor.Hand
            isDraggingNode -> ImGuiMouseCursor.None
            hoveredPin != null -> ImGuiMouseCursor.ResizeAll
            else -> ImGuiMouseCursor.Arrow
        }
    }

    fun isLinkSelected(link: Link): Boolean {
        return selectedLinkIds.contains(link.uid)
    }

    fun isNodeSelected(node: Node): Boolean {
        return selectedNodeIds.contains(node.uid)
    }

    private fun handleNodeDrag(node: Node, nodeBounds: Vector4f, headerBounds: Vector4f) {
        val mousePos = ImGui.getMousePos()
        if (node.dragged) {
            val mx = mousePos.x / runtime.workspace!!.settings.zoom
            val my = mousePos.y / runtime.workspace!!.settings.zoom
            val isShiftDown =
                Platform.isKeyDown(ClientRuntime.Key.LEFT_SHIFT) || Platform.isKeyDown(ClientRuntime.Key.RIGHT_SHIFT)

            if (isDraggingGroup) {
                // Move all selected nodes
                for (selectedNodeId in selectedNodeIds) {
                    val selectedNode = runtime.workspace!!.getNode(selectedNodeId)
                    if (selectedNode != null) {
                        val offset = groupDragOffset[selectedNodeId] ?: Vector2f()
                        val newX = if (isShiftDown) snapToGrid(mx - offset.x) else mx - offset.x
                        val newY = if (isShiftDown) snapToGrid(my - offset.y) else my - offset.y

                        val constrainedPos = constrainNodeToFunction(selectedNode, newX, newY)
                        selectedNode.x = constrainedPos.x
                        selectedNode.y = constrainedPos.y
                        sendMovePacket(selectedNode)
                    }
                }
            } else {
                // Move only the dragged node
                val newX = if (isShiftDown) snapToGrid(mx - dragOffset.x) else mx - dragOffset.x
                val newY = if (isShiftDown) snapToGrid(my - dragOffset.y) else my - dragOffset.y

                val constrainedPos = constrainNodeToFunction(node, newX, newY)
                node.x = constrainedPos.x
                node.y = constrainedPos.y
                sendMovePacket(node)
            }
        }

        if (!ImGui.isMouseDown(ImGuiMouseButton.Left)) {
            node.dragged = false
            runtime.selectedNode = null
            isDraggingNode = false
            isDraggingGroup = false
            groupDragOffset.clear()
            wasDraggingNode = true
        }
    }


    fun getEdgeBounds(owner: Node, edge: Edge): Vector4f {
        val nodeBounds = getNodeBounds(owner)
        val nodePos = workspace.convertPosition(owner.x, owner.y)
        val nodeSize = workspace.convertSize(owner.width, owner.height)
        var xPos = 0f
        var yOffset: Float
        var yPos = 0f
        var xOffset = 0f
        var textXPos = 0f
        var textYPos = 0f
        val offset = 16f
        bodyFont.use {
            val textSize = ImGui.calcTextSize(edge.name)
            val textHeight = textSize.y

            val isInput = edge.direction == "input"
            val edgeCount = workspace.graph.getEdges(owner).count { it.direction == edge.direction }
            val edgeIndex = workspace.graph.getEdges(owner).filter { it.direction == edge.direction }.indexOf(edge)

            xPos = if (isInput) nodeBounds.x else nodeBounds.z
            xOffset = if (isInput) -offset * zoom else offset * zoom
            yOffset = ((nodeSize.y / (edgeCount) * (edgeIndex))) + offset * zoom
            yPos = nodePos.y + yOffset

            textXPos = if (isInput) xPos + 15f * zoom else xPos - textSize.x - 15f * zoom
            textYPos = yPos - textHeight / 2
        }

        return Vector4f(xPos + xOffset, yPos, textXPos, textYPos)
    }

    fun getNodeBounds(node: Node, headerPadding: Float = 30f): Vector4f {
        val nodePos = convertToScreenCoordinates(Vector2f(node.x, node.y))
        val nodeSize = convertToScreenSize(Vector2f(node.width, node.height))
        val padding = headerPadding * zoom

        // Ensure the node is large enough to accommodate all edges
        val inputCount = workspace.graph.getEdges(node).count { it.direction == "input" }
        val outputCount = workspace.graph.getEdges(node).count { it.direction == "output" }
        val edgeCount = maxOf(inputCount, outputCount)
        val minHeight = (edgeCount) * 20f * zoom

        nodeSize.y = minHeight
//
        return Vector4f(
            nodePos.x - padding / 2f,
            nodePos.y - padding - 2 * zoom,
            nodePos.x + nodeSize.x,
            (nodePos.y + nodeSize.y) - 5 * zoom,
        )
    }

    fun getFunctionBounds(function: Function, headerPadding: Float = 30f): Vector4f {
        val functionPos = convertToScreenCoordinates(Vector2f(function.x, function.y))
        val functionSize = convertToScreenSize(Vector2f(function.width, function.height))
        val padding = headerPadding * zoom
        return Vector4f(
            functionPos.x - padding / 2f,
            functionPos.y - padding - 2 * zoom,
            functionPos.x + functionSize.x,
            (functionPos.y + functionSize.y) - 5 * zoom,
        )
    }

    fun getFunctionHeaderBounds(function: Function): Vector4f {
        return headerFont.use {
            val functionPos = workspace.convertPosition(function.x, function.y)
            val functionSize = workspace.convertSize(function.width, function.height)
            val titleSize = ImGui.calcTextSize(function.name)
            val titleHeight = titleSize.y
            val titleX = functionPos.x - 8 * zoom
            val titleY = ((functionPos.y - titleHeight / 2) - 8 * zoom) - 15f * zoom

            Vector4f(titleX, titleY, functionPos.x + functionSize.x, titleY + titleHeight)
        }
    }

    fun updateFunctionHoverState(mousePos: Vector2f) {
        hoveredFunctionHeader = null

        for (function in workspace.graph.functions) {
            val headerBounds = getFunctionHeaderBounds(function)
            if (headerBounds.contains(mousePos.x, mousePos.y)) {
                hoveredFunctionHeader = function.uid
                return
            }
        }
    }

    private fun calculateNodeOffsets(function: Function) {
        nodeOffsets.clear()
        function.nodes.forEach { nodeRef ->
            val node = workspace.getNode(nodeRef.get()) ?: return@forEach
            nodeOffsets[node.uid] = Vector2f(node.x - function.x, node.y - function.y)
        }
    }

    fun handleFunctionDrag() {
        val mousePos = ImGui.getMousePos()
        val mx = mousePos.x / workspace.settings.zoom
        val my = mousePos.y / workspace.settings.zoom

        // Start dragging
        if (hoveredFunctionHeader != null && ImGui.isMouseClicked(ImGuiMouseButton.Left) && draggedFunction == null) {
            val function = workspace.graph.getFunction(hoveredFunctionHeader!!) ?: return
            draggedFunction = function
            functionDragOffset.set(mx - function.x, my - function.y)
            calculateNodeOffsets(function)
        }

        // Continue dragging
        if (draggedFunction != null && ImGui.isMouseDown(ImGuiMouseButton.Left)) {
            val isShiftDown = Platform.isKeyDown(ClientRuntime.Key.LEFT_SHIFT) ||
                    Platform.isKeyDown(ClientRuntime.Key.RIGHT_SHIFT)

            val newX = if (isShiftDown) snapToGrid(mx - functionDragOffset.x) else mx - functionDragOffset.x
            val newY = if (isShiftDown) snapToGrid(my - functionDragOffset.y) else my - functionDragOffset.y

            val deltaX = newX - draggedFunction!!.x
            val deltaY = newY - draggedFunction!!.y

            // Update function position
            draggedFunction!!.x = newX
            draggedFunction!!.y = newY

            // Update all child nodes maintaining their relative positions
            draggedFunction!!.nodes.forEach { nodeRef ->
                val node = workspace.getNode(nodeRef.get()) ?: return@forEach
                val offset = nodeOffsets[node.uid] ?: return@forEach

                val newNodeX = newX + offset.x
                val newNodeY = newY + offset.y

                node.x = newNodeX
                node.y = newNodeY

                // Send node move packet for each child node
                sendMovePacket(node)
            }

            // Send function move packet
            val packet = FunctionMoved(
                draggedFunction!!.uid,
                newX,
                newY
            )
            client.send(packet)
        }

        // End dragging
        if (!ImGui.isMouseDown(ImGuiMouseButton.Left)) {
            draggedFunction = null
            nodeOffsets.clear()
        }
    }

    fun getHeaderBounds(node: Node): Vector4f {
        return headerFont.use {
            val nodePos = workspace.convertPosition(node.x, node.y)
            val nodeSize = workspace.convertSize(node.width, node.height)
            val titleSize = ImGui.calcTextSize(node.name)
            val titleHeight = titleSize.y
            val titleX = nodePos.x - 8 * zoom
            val titleY = ((nodePos.y - titleHeight / 2) - 8 * zoom) - 15f * zoom

            Vector4f(titleX, titleY, nodePos.x + nodeSize.x, titleY + titleHeight)
        }
    }

    private fun isLinkInSelection(link: Link, topLeft: Vector2f, bottomRight: Vector2f): Boolean {
        val sourceEdge = workspace.graph.getEdge(link.from) ?: return false
        val targetEdge = workspace.graph.getEdge(link.to) ?: return false

        // Get source and target owners (can be either Node or Function)
        val sourceOwner = workspace.getNode(sourceEdge.owner)
            ?: workspace.graph.getFunction(sourceEdge.owner)
            ?: return false

        val targetOwner = workspace.getNode(targetEdge.owner)
            ?: workspace.graph.getFunction(targetEdge.owner)
            ?: return false

        // Skip if either end is in a minimized function
        if (sourceOwner is Node) {
            val function = workspace.graph.getFunction(sourceOwner.function)
            if (function?.minimized == true) return false
        }

        if (targetOwner is Node) {
            val function = workspace.graph.getFunction(targetOwner.function)
            if (function?.minimized == true) return false
        }

        // Get appropriate bounds based on owner type
        val sourceBounds = when (sourceOwner) {
            is Node -> getNodeBounds(sourceOwner)
            is Function -> getFunctionBounds(sourceOwner)
            else -> return false
        }

        val targetBounds = when (targetOwner) {
            is Node -> getNodeBounds(targetOwner)
            is Function -> getFunctionBounds(targetOwner)
            else -> return false
        }

        // Get edge positions
        val startPos = when (sourceOwner) {
            is Node -> getEdgePosition(sourceOwner, sourceEdge, sourceBounds)
            is Function -> getEdgePosition(sourceOwner, sourceEdge, sourceBounds)
            else -> return false
        }

        val endPos = when (targetOwner) {
            is Node -> getEdgePosition(targetOwner, targetEdge, targetBounds)
            is Function -> getEdgePosition(targetOwner, targetEdge, targetBounds)
            else -> return false
        }

        val midX = (startPos.x + endPos.x) / 2
        val controlPoint1 = Vector2f(midX, startPos.y)
        val controlPoint2 = Vector2f(midX, endPos.y)

        // Check multiple points along the curve
        val steps = 20
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val point = getBezierPoint(startPos, controlPoint1, controlPoint2, endPos, t)
            if (point.x in topLeft.x..bottomRight.x && point.y in topLeft.y..bottomRight.y) {
                return true
            }
        }
        return false
    }

    private fun isNodeInSelection(node: Node, topLeft: Vector2f, bottomRight: Vector2f): Boolean {
        val bounds = getNodeBounds(node)
        return bounds.x < bottomRight.x && bounds.z > topLeft.x && bounds.y < bottomRight.y && bounds.w > topLeft.y
    }

    fun isNodeInSelectionBox(node: Node): Boolean {
        return nodesInSelectionBox.contains(node.uid)
    }

    fun isLinkInSelectionBox(link: Link): Boolean {
        return linksInSelectionBox.contains(link.uid)
    }

    fun handleSelection(isPropertyWindowHovered: Boolean) {
        if (isLinking || isPropertyWindowHovered || draggedFunction != null) return

        val mousePos = Vector2f(ImGui.getMousePos().x, ImGui.getMousePos().y)
        val isLeftClickPressed = ImGui.isMouseClicked(ImGuiMouseButton.Left)
        val isLeftClickReleased = ImGui.isMouseReleased(ImGuiMouseButton.Left)
        val isLeftClickDragging = ImGui.isMouseDragging(ImGuiMouseButton.Left)
        val isCtrlPressed = Platform.isKeyDown(ClientRuntime.Key.LEFT_CONTROL)

        // Check for function resize start
        if (isLeftClickPressed && resizingFunction == null) {
            workspace.graph.functions.forEach { function ->
                if (isOverFunctionResizeHandle(function, mousePos)) {
                    resizingFunction = function
                    resizeStartPos = mousePos
                    initialFunctionSize = Vector2f(function.width, function.height)
                    return
                }
            }
        }

        // Handle function resizing
        if (resizingFunction != null) {
            if (isLeftClickDragging) {
                val startPos = resizeStartPos!!
                val initialSize = initialFunctionSize!!
                val delta = Vector2f(
                    mousePos.x - startPos.x,
                    mousePos.y - startPos.y
                )

                // Convert delta to world space
                val worldDelta = Vector2f(
                    delta.x / zoom,
                    delta.y / zoom
                )

                // Calculate minimum sizes
                val minWidth = 100f  // Minimum width
                val minHeight = 100f // Minimum height

                // Apply new size with minimum constraints
                resizingFunction!!.width = maxOf(initialSize.x + worldDelta.x, minWidth)
                resizingFunction!!.height = maxOf(initialSize.y + worldDelta.y, minHeight)
            }

            if (isLeftClickReleased) {
                //Send resize packet
                val packet = FunctionResized(
                    resizingFunction!!.uid,
                    resizingFunction!!.width,
                    resizingFunction!!.height
                )
                client.send(packet)
                resizingFunction = null
                resizeStartPos = null
                initialFunctionSize = null
                return
            }
        }

        if (isLeftClickPressed && !isDraggingNode) {
            val clickedOnNode = workspace.graph.nodes.any { node ->
                val bounds = getNodeBounds(node)
                mousePos.x in bounds.x..bounds.z && mousePos.y in bounds.y..bounds.w
            }

            if (!clickedOnNode) {
                val clickedLink = findLinkUnderMouse(Vector2f(mousePos.x, mousePos.y))
                if (clickedLink != null) {
                    handleLinkSelection(clickedLink, isCtrlPressed)
                } else {
                    if (!isCtrlPressed) {
                        clearSelection()
                    }
                    selectionStart = Vector2f(mousePos.x, mousePos.y)
                    selectionEnd = selectionStart
                }
            }
        }

        if (isLeftClickDragging && selectionStart != null) {
            isSelecting = true
            selectionEnd = Vector2f(mousePos.x, mousePos.y)
            updateNodesInSelectionBox()
            updateLinksInSelectionBox()
        }

        if (isLeftClickReleased) {
            if (isSelecting) {
                updateFinalSelection()
                isSelecting = false
            } else if (!wasDraggingNode) {
                // Handle selection of individually clicked nodes
                if (Keyboard.isKeyUp(ClientRuntime.Key.LEFT_CONTROL)) {
                    clearSelection()
                }

                val nodesUnderMouse = workspace.graph.nodes.filter { node ->
                    val bounds = getNodeBounds(node)
                    mousePos.x in bounds.x..bounds.z && mousePos.y in bounds.y..bounds.w
                }

                nodesUnderMouse.forEach { node ->
                    selectedNodeIds.add(node.uid)
                    //node.selected = true
                }
            }

            selectionStart = null
            selectionEnd = null
        }


    }

    private fun updateNodesInSelectionBox() {
        val start = selectionStart ?: return
        val end = selectionEnd ?: return

        val topLeft = Vector2f(minOf(start.x, end.x), minOf(start.y, end.y))
        val bottomRight = Vector2f(maxOf(start.x, end.x), maxOf(start.y, end.y))

        nodesInSelectionBox.clear()

        for (node in workspace.graph.nodes) {
            if (isNodeInSelection(node, topLeft, bottomRight)) {
                nodesInSelectionBox.add(node.uid)
            }
        }
    }

    private fun updateLinksInSelectionBox() {
        val start = selectionStart ?: return
        val end = selectionEnd ?: return

        val topLeft = Vector2f(minOf(start.x, end.x), minOf(start.y, end.y))
        val bottomRight = Vector2f(maxOf(start.x, end.x), maxOf(start.y, end.y))

        linksInSelectionBox.clear()
        workspace.graph.links.forEach { link ->
            if (isLinkInSelection(link, topLeft, bottomRight)) {
                linksInSelectionBox.add(link.uid)
            }
        }
    }

    fun updateFinalSelection() {
        if (selectionStart == null || selectionEnd == null) return

        val topLeft = Vector2f(
            minOf(selectionStart!!.x, selectionEnd!!.x),
            minOf(selectionStart!!.y, selectionEnd!!.y)
        )
        val bottomRight = Vector2f(
            maxOf(selectionStart!!.x, selectionEnd!!.x),
            maxOf(selectionStart!!.y, selectionEnd!!.y)
        )

        // Store the last selection bounds
        lastSelectionBounds = Pair(topLeft, bottomRight)
        hasValidSelectionBounds = true

        if (!Platform.isKeyDown(ClientRuntime.Key.LEFT_CONTROL)) {
            clearSelection()
        }

        workspace.graph.nodes.forEach { node ->
            if (nodesInSelectionBox.contains(node.uid)) {
                selectedNodeIds.add(node.uid)
            }
        }
        nodesInSelectionBox.clear()

        workspace.graph.links.forEach { link ->
            if (linksInSelectionBox.contains(link.uid)) {
                selectedLinkIds.add(link.uid)
            }
        }
        linksInSelectionBox.clear()
    }

    private fun clearSelection() {
        selectedNodeIds.clear()
        selectedLinkIds.clear()
//        hasValidSelectionBounds = false
//        lastSelectionBounds = null
    }

    private fun findLinkUnderMouse(mousePos: Vector2f): Link? {
        // First check links connected to nodes within functions
        for (function in workspace.graph.functions) {
            if (function.minimized) continue

            // Get all nodes within this function
            val nodesInFunction = function.nodes.mapNotNull { workspace.getNode(it.get()) }

            // Check links connected to these nodes
            for (node in nodesInFunction) {
                val links = workspace.graph.links.filter { link ->
                    val sourceEdge = workspace.getEdge(link.from)
                    val targetEdge = workspace.getEdge(link.to)

                    (sourceEdge?.owner == node.uid || targetEdge?.owner == node.uid)
                }

                for (link in links) {
                    if (isMouseOverLink(link, mousePos)) {
                        return link
                    }
                }
            }

            // Check links connected to the function itself
            val functionLinks = workspace.graph.links.filter { link ->
                val sourceEdge = workspace.getEdge(link.from)
                val targetEdge = workspace.getEdge(link.to)

                (sourceEdge?.owner == function.uid || targetEdge?.owner == function.uid)
            }

            for (link in functionLinks) {
                if (isMouseOverLink(link, mousePos)) {
                    return link
                }
            }
        }

        // Then check remaining links (outside functions)
        return workspace.graph.links
            .filter { link ->
                val sourceEdge = workspace.getEdge(link.from)
                val targetEdge = workspace.getEdge(link.to)
                val sourceNode = sourceEdge?.owner?.let { workspace.getNode(it) }
                val targetNode = targetEdge?.owner?.let { workspace.getNode(it) }

                // Only consider links where at least one end is not in a function
                sourceNode?.function == NetUtils.DefaultUUID || targetNode?.function == NetUtils.DefaultUUID
            }
            .find { link -> isMouseOverLink(link, mousePos) }
    }

    private fun getBezierPoint(p0: Vector2f, p1: Vector2f, p2: Vector2f, p3: Vector2f, t: Float): Vector2f {
        val u = 1 - t
        val tt = t * t
        val uu = u * u
        val uuu = uu * u
        val ttt = tt * t

        return Vector2f(
            uuu * p0.x + 3 * uu * t * p1.x + 3 * u * tt * p2.x + ttt * p3.x,
            uuu * p0.y + 3 * uu * t * p1.y + 3 * u * tt * p2.y + ttt * p3.y
        )
    }

    // Add a method to get the last selection bounds
    fun getLastSelectionBounds(): Pair<Vector2f, Vector2f>? {
        return if (hasValidSelectionBounds) lastSelectionBounds else null
    }

    // Method to check if there's a valid selection with bounds
    fun hasValidSelection(): Boolean {
        return hasValidSelectionBounds && lastSelectionBounds != null
    }

    // Add a method to get all nodes within the last selection bounds
    fun getNodesInLastSelection(): List<Node> {
        val bounds = lastSelectionBounds ?: return emptyList()
        return workspace.graph.nodes.filter { node ->
            isNodeInSelection(node, bounds.first, bounds.second)
        }
    }

    // Add a method to get all links within the last selection bounds
    fun getLinksInLastSelection(): List<Link> {
        val bounds = lastSelectionBounds ?: return emptyList()
        return workspace.graph.links.filter { link ->
            isLinkInSelection(link, bounds.first, bounds.second)
        }
    }

    fun isMouseOverLink(link: Link, mousePos: Vector2f): Boolean {
        val sourceEdge = workspace.getEdge(link.from) ?: return false
        val targetEdge = workspace.getEdge(link.to) ?: return false

        // Get source and target owners (can be either Node or Function)
        val sourceOwner = workspace.getNode(sourceEdge.owner)
            ?: workspace.graph.getFunction(sourceEdge.owner)
            ?: return false

        val targetOwner = workspace.getNode(targetEdge.owner)
            ?: workspace.graph.getFunction(targetEdge.owner)
            ?: return false

        // Skip if either end is in a minimized function
        if (sourceOwner is Node) {
            val function = workspace.graph.getFunction(sourceOwner.function)
            if (function?.minimized == true) return false
        }

        if (targetOwner is Node) {
            val function = workspace.graph.getFunction(targetOwner.function)
            if (function?.minimized == true) return false
        }

        // Get appropriate bounds based on owner type
        val sourceBounds = when (sourceOwner) {
            is Node -> getNodeBounds(sourceOwner)
            is Function -> getFunctionBounds(sourceOwner)
            else -> return false
        }

        val targetBounds = when (targetOwner) {
            is Node -> getNodeBounds(targetOwner)
            is Function -> getFunctionBounds(targetOwner)
            else -> return false
        }

        // Get edge positions
        val startPos = when (sourceOwner) {
            is Node -> getEdgePosition(sourceOwner, sourceEdge, sourceBounds)
            is Function -> getEdgePosition(sourceOwner, sourceEdge, sourceBounds)
            else -> return false
        }

        val endPos = when (targetOwner) {
            is Node -> getEdgePosition(targetOwner, targetEdge, targetBounds)
            is Function -> getEdgePosition(targetOwner, targetEdge, targetBounds)
            else -> return false
        }

        val midX = (startPos.x + endPos.x) / 2
        val controlPoint1 = Vector2f(midX, startPos.y)
        val controlPoint2 = Vector2f(midX, endPos.y)

        val mouseVector = Vector2f(mousePos.x, mousePos.y)

        // Check multiple points along the curve
        val steps = 20
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val point = getBezierPoint(startPos, controlPoint1, controlPoint2, endPos, t)
            if (mouseVector.distance(point) < 5f * zoom) {
                return true
            }
        }
        return false
    }

    fun isMouseOverNode(node: Node, mousePos: Vector2f): Boolean {
        val bounds = getNodeBounds(node)
        return mousePos.x in bounds.x..bounds.z && mousePos.y in bounds.y..bounds.w
    }

    fun selectLink(link: Link) {
        selectedLinkIds.add(link.uid)
    }

    fun unselectLink(link: Link) {
        selectedLinkIds.remove(link.uid)
    }

    fun clearLinkSelection() {
        selectedLinkIds.clear()
    }

    fun getSelectedLinks(): Set<UUID> {
        return selectedLinkIds.toSet()
    }


    private fun handleLinkSelection(link: Link, isCtrlPressed: Boolean) {
        if (isCtrlPressed) {
            if (selectedLinkIds.contains(link.uid)) {
                selectedLinkIds.remove(link.uid)
            } else {
                selectedLinkIds.add(link.uid)
            }
        } else {
            clearSelection()
            selectedLinkIds.add(link.uid)
        }
    }

    /**
     * Send a move packet for the given node if necessary.
     *
     * @param node The node to send the move packet for.
     */
    private fun sendMovePacket(node: Node) {
        //diff in x and y between the last sent (nodeMovePacket) and the current node position. If the diff is greater than the maxMoveThreshold, send the packet
        val diffX = abs(node.x - nodeMovePacket.x)
        val diffY = abs(node.y - nodeMovePacket.y)
        if (Time.now - lastSent > maxSendRate || (diffX > maxMoveThreshold || diffY > maxMoveThreshold)) {
            nodeMovePacket.uid = node.uid
            nodeMovePacket.from = client.uuid
            nodeMovePacket.x = node.x
            nodeMovePacket.y = node.y
            runtime.client.send(nodeMovePacket)
            lastSent = Time.now


            // After sending the move packet, check if this node is part of a function
            // and update the function bounds if necessary
//            workspace.graph.functions.forEach { function ->
//                if (function.nodes.any { it.uid == node.uid }) {
//                    workspace.graph.updateFunctionBoundsIfNeeded(function, node)
//                }
//            }

        }
    }

    fun handleStartDrag(node: Node, headerBounds: Vector4f) {
        if ((node.dragged || selectionStart != null || isDraggingNode)) return
        val mousePos = ImGui.getMousePos()
        val mx = mousePos.x / runtime.workspace!!.settings.zoom
        val my = mousePos.y / runtime.workspace!!.settings.zoom
        //Expand the header bounds to include the entire node
//        val headerHovered = headerBounds.contains(mousePos.x, mousePos.y)
        if (hoveredTitleBar == node.uid && ImGui.isMouseDown(ImGuiMouseButton.Left) && !node.dragged) {
            dragOffset.set(mx - node.x, my - node.y)
            node.dragged = true
            runtime.selectedNode = node
            isDraggingNode = true

            if (selectedNodeIds.contains(node.uid)) {
                isDraggingGroup = true
                // Calculate offsets for all selected nodes
                for (selectedNodeId in selectedNodeIds) {
                    val selectedNode = runtime.workspace!!.getNode(selectedNodeId)
                    if (selectedNode != null) {
                        groupDragOffset[selectedNodeId] = Vector2f(mx - selectedNode.x, my - selectedNode.y)
                    }
                }

            } else {
                // If the dragged node is not in the selection, clear the selection and select only this node
                selectedNodeIds.clear()
                selectedNodeIds.add(node.uid)
            }
        }
    }

    private fun snapToGrid(value: Float): Float {
        return (value / gridSize).toInt() * gridSize
    }

    fun getSelection(): Pair<Vector2f, Vector2f>? {
        if (!isSelecting) return null

        return selectionStart?.let { start ->
            selectionEnd?.let { end ->
                Pair(start, end)
            }
        }
    }

    fun createVariableNode(type: bpm.common.workspace.packets.NodeType, position: Vector2f, name: String) {
        val worldPos = convertToWorldCoordinates(position)
        client.send(VariableNodeCreateRequest(type, worldPos, name))
    }

    fun convertToScreenCoordinates(worldPos: Vector2f): Vector2f {
        return Vector2f(
            (worldPos.x * workspace.settings.zoom) + workspace.settings.position.x,
            (worldPos.y * workspace.settings.zoom) + workspace.settings.position.y
        )
    }

    private fun convertToScreenSize(worldSize: Vector2f): Vector2f {
        return Vector2f(
            worldSize.x * workspace.settings.zoom, worldSize.y * workspace.settings.zoom
        )
    }

    fun convertToWorldCoordinates(screenPos: Vector2f): Vector2f {
        return Vector2f(
            (screenPos.x - workspace.settings.position.x) / workspace.settings.zoom,
            (screenPos.y - workspace.settings.position.y) / workspace.settings.zoom
        )
    }

    //COllect the selected nodes from the selection start and end if selectionNodesIds is empty
    private fun collectSelectedNodes() {
        if (selectedNodeIds.isEmpty() && selectionStart != null && selectionEnd != null) {
            val topLeft = Vector2f(
                minOf(selectionStart!!.x, selectionEnd!!.x),
                minOf(selectionStart!!.y, selectionEnd!!.y)
            )
            val bottomRight = Vector2f(
                maxOf(selectionStart!!.x, selectionEnd!!.x),
                maxOf(selectionStart!!.y, selectionEnd!!.y)
            )

            for (node in workspace.graph.nodes) {
                if (isNodeInSelection(node, topLeft, bottomRight)) {
                    selectedNodeIds.add(node.uid)
                }
            }
            for (link in workspace.graph.links) {
                if (isLinkInSelection(link, topLeft, bottomRight)) {
                    selectedLinkIds.add(link.uid)
                }
            }
        }
    }

    private fun constrainNodeToFunction(node: Node, newX: Float, newY: Float): Vector2f {
        // Find if this node belongs to any function
        val parentFunction = workspace.graph.functions.find { it.nodes.any { n -> n.get() == node.uid } }
            ?: return Vector2f(newX, newY)

        // Get both bounds in screen space
        val oldNodeBounds = getNodeBounds(node)
        val functionBounds = getFunctionBounds(parentFunction)

        // Convert the new position to screen space for comparison
        val newScreenPos = convertToScreenCoordinates(Vector2f(newX, newY))

        // Calculate node dimensions in screen space
        val nodeWidth = oldNodeBounds.z - oldNodeBounds.x
        val nodeHeight = oldNodeBounds.w - oldNodeBounds.y

        // Calculate constraints in screen space
        val minScreenX = functionBounds.x + 40 * zoom
        val minScreenY = functionBounds.y + 70 * zoom
        val maxScreenX = functionBounds.z - nodeWidth - 10 * zoom
        val maxScreenY = functionBounds.w - nodeHeight + 15 * zoom
        val minX = min(minScreenX, maxScreenX)
        val minY = min(minScreenY, maxScreenY)
        val maxX = max(minScreenX, maxScreenX)
        val maxY = max(minScreenY, maxScreenY)
        // Constrain in screen space
        val constrainedScreenX = newScreenPos.x.coerceIn(minX, maxX)
        val constrainedScreenY = newScreenPos.y.coerceIn(minY, maxY)

        // Convert back to world coordinates
        return convertToWorldCoordinates(Vector2f(constrainedScreenX, constrainedScreenY))
    }


    fun deleteSelected() {
        if (selectedNodeIds.isEmpty() || selectedLinkIds.isEmpty() && selectionStart != null && selectionEnd != null) {
            collectSelectedNodes()
        }
        for (nodeId in selectedNodeIds) {
            // Notify the server about node deletion
            client.send(NodeDeleteRequest(nodeId))
        }

        for (linkId in selectedLinkIds) {
            // Notify the server about link deletion
            client.send(LinkDeleteRequest(linkId))
        }

//        selectedNodeIds.clear()
//        selectedLinkIds.clear()

        // Reset any state that might prevent adding new nodes
//        isDraggingNode = false
//        isSelecting = false
//        selectionStart = null
//        selectionEnd = null
    }


    /**
     * Called when a packet is received.
     *
     * @param packet the packet that was received
     */
    override fun onPacket(packet: Packet, from: UUID) {
        if (packet is NodeMoved) {
            val node = runtime.workspace!!.getNode(packet.uid)
            if (node == null) {
                logger.warn { "Failed to move node: ${packet.uid}" }
                return
            }
            node.x = packet.x
            node.y = packet.y
            logger.info { "Moved node: ${packet.uid}" }

//            workspace.graph.functions.forEach { function ->
//                if (function.nodes.any { it.uid == node.uid }) {
//                    workspace.graph.updateFunctionBoundsIfNeeded(function, node)
//                }
//            }

        } else if (packet is UserConnectedToWorkspace) {
            packet.users.forEach { user ->
                connectedUsers[user.uid] = user
            }
            logger.info { "Received user connected to workspace: ${packet.users}" }
        } else if (packet is NodeCreated) {
            val node = packet.node
            processNewNode(node)

            if (node.function != NetUtils.DefaultUUID) {
                val function = workspace.graph.getFunction(node.function)
                if (function != null) {
                    workspace.graph.getFunction(node.function)?.nodes?.add(Property.UUID(node.uid))
                }
            }

            //If the node is within a function,
            //update the function bounds to include the new node


            // Check if this node creation was initiated by dragging an edge
            if (pendingNodeCreation != null && pendingNodeCreation?.nodeType == "${node.type}/${node.name}") {
                draggedSourceEdge?.let { (sourceNode, sourceEdge) ->
                    // Find the first compatible edge in the new node
                    val compatibleEdge = workspace.graph.getEdges(node).firstOrNull { edge ->
                        canConnect(sourceEdge, edge)
                    }

                    if (compatibleEdge != null) {
                        // Create the link
                        if (sourceEdge.direction == "output") {
                            createLink(sourceNode, sourceEdge, node, compatibleEdge)
                        } else {
                            createLink(node, compatibleEdge, sourceNode, sourceEdge)
                        }
                    }
                }
                pendingNodeCreation = null
                draggedSourceEdge = null
            }
        } else if (packet is NodeDeleted) {
            packet.uuids.forEach(workspace::removeNode)
        } else if (packet is LinkCreated) {
            val link = packet.link
            workspace.addLink(link)
        } else if (packet is LinkDeleted) {
            packet.uuids.forEach(workspace::removeLink)
        } else if (packet is EdgePropertyUpdate) {
            val edge = workspace.graph.getEdge(packet.edgeUid) ?: return
            edge.properties["value"] = packet.property
        } else if (packet is NotifyMessage) {
            notificationManager.addNotification(packet)
        } else if (packet is VariableCreated) {
            workspace.addVariable(packet.name, packet.property["value"])
            logger.info { "Received variable created: ${packet.name}" }
        } else if (packet is VariableDeleted) {
            workspace.removeVariable(packet.name)
            logger.info { "Received variable deleted: ${packet.name}" }
        } else if (packet is VariableUpdated) {
            workspace.updateVariable(packet.variableName, packet.property["value"])
            logger.info { "Received variable updated: ${packet.variableName}" }
        } else if (packet is FunctionCreated) {
            workspace.graph.addFunction(packet.function)
        } else if (packet is FunctionResized) {
            val function = workspace.graph.getFunction(packet.uid) ?: return
            function.width = packet.width
            function.height = packet.height
        } else if (packet is FunctionMoved) {
            val function = workspace.graph.getFunction(packet.uid) ?: return
            val deltaX = packet.x - function.x
            val deltaY = packet.y - function.y

            // Update function position
            function.x = packet.x
            function.y = packet.y

            // Update all child nodes
            function.nodes.forEach { nodeRef ->
                val node = workspace.getNode(nodeRef.get()) ?: return@forEach
                node.x += deltaX
                node.y += deltaY
            }
        } else if (packet is FunctionMinimized) {
            val function = workspace.graph.getFunction(packet.uid) ?: return
            function.minimized = packet.minimized
        } else if (packet is FunctionNamed) {
            val function = workspace.graph.getFunction(packet.uid) ?: return
            function.name = packet.newName
        } else if (packet is FunctionColored) {
            val function = workspace.graph.getFunction(packet.uid) ?: return
            function.color = packet.newColor
        } else if (packet is FunctionDeleted) {
            workspace.graph.removeFunction(packet.uid)
        } else if (packet is FunctionEdgeCreated) {
            val function = workspace.graph.getFunction(packet.uid) ?: return
            workspace.addEdge(function, packet.edge)
        } else if (packet is NodeEdgeCreated) {
            val node = workspace.getNode(packet.uid) ?: return
            workspace.addEdge(node, packet.edge)
        }
    }

    fun start(node: Node, edge: Edge) {
        selectedEdge = Pair(node, edge)
        val edgeBounds = getEdgeBounds(node, edge)
        dragStartPos = Vector2f(edgeBounds.x, edgeBounds.y)
        isLinking = true
    }

    //Centers the scrolled area around the majority of the nodes
    fun center() {
        //Moves the camera to origin
        val settings = workspace.settings
        val center = findCenter()
        settings.center = center
        workspace.settings.zoom = 1f
    }

    private fun findCenter(): Vector2f {
        if (workspace.graph.nodes.isEmpty())
            return Vector2f()

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        workspace.graph.nodes.forEach {
            if (it.x < minX) minX = it.x
            if (it.y < minY) minY = it.y
            if ((it.x + it.width) > maxX) maxX = (it.x + it.width)
            if ((it.y + it.height) > maxY) maxY = (it.y + it.height)
        }

        return Vector2f(minX + (maxX - minX) / 2, minY + (maxY - minY) / 2)
    }

    fun handleEdgeClick(node: Node, edge: Edge) {
        if (!isLinking) {
            start(node, edge)
        } else {
            val (sourceNode, sourceEdge) = selectedEdge ?: return
            if (canConnect(sourceEdge, edge)) {
                createLink(sourceNode, sourceEdge, node, edge)
            }
            selectedEdge = null
            dragStartPos = null
            isLinking = false
        }
    }


    private fun processNewNode(node: Node) {
        workspace.addNode(node)
        val edges = node.properties["edges"] as? PropertyMap ?: return
        for ((edgeName, edgeProperty) in edges) {
            if (edgeProperty !is PropertyMap) continue
            val edge = Edge(edgeProperty)
            edge.properties["name"] = Property.String(edgeName)
            workspace.addEdge(node, edge)
        }
    }


    private fun findEdgeUnderMouse(): Pair<Any, Edge>? {
        val mousePos = ImGui.getMousePos()

        // Check function edges first
        for (function in workspace.graph.functions) {
            if (function.minimized) continue

            val functionBounds = getFunctionBounds(function)
            val edges = workspace.graph.getEdges(function.uid)
            val inputEdges = edges.filter { it.direction == "input" }
            val outputEdges = edges.filter { it.direction == "output" }

            // Check function input edges
            inputEdges.forEach { edge ->
                val edgePos = getEdgePosition(function, edge, functionBounds)
                if (isPointOverEdge(Vector2f(mousePos.x, mousePos.y), edgePos)) {
                    return Pair(function, edge)
                }
            }

            // Check function output edges
            outputEdges.forEach { edge ->
                val edgePos = getEdgePosition(function, edge, functionBounds)
                if (isPointOverEdge(Vector2f(mousePos.x, mousePos.y), edgePos)) {
                    return Pair(function, edge)
                }
            }
        }

        // Then check node edges
        for (node in workspace.graph.nodes) {
            val nodeBounds = getNodeBounds(node)
            val edges = workspace.graph.getEdges(node)
            val inputEdges = edges.filter { it.direction == "input" }
            val outputEdges = edges.filter { it.direction == "output" }

            // Check node input edges
            inputEdges.forEach { edge ->
                val edgePos = getEdgePosition(node, edge, nodeBounds)
                if (isPointOverEdge(Vector2f(mousePos.x, mousePos.y), edgePos)) {
                    return Pair(node, edge)
                }
            }

            // Check node output edges
            outputEdges.forEach { edge ->
                val edgePos = getEdgePosition(node, edge, nodeBounds)
                if (isPointOverEdge(Vector2f(mousePos.x, mousePos.y), edgePos)) {
                    return Pair(node, edge)
                }
            }
        }

        return null
    }

    //A poor mans type system xD
    private fun canConnect(sourceEdge: Edge, targetEdge: Edge): Boolean {
        // Prevent connecting an edge to itself
        if (sourceEdge.uid == targetEdge.uid) return false

        // Prevent connecting edges of the same direction (input to input or output to output)
        if (sourceEdge.direction == targetEdge.direction) return false

        // If either is "any" type and both are not exec, it's valid
        if (sourceEdge.type == "any" && targetEdge.type != "exec" || targetEdge.type == "any" && sourceEdge.type != "exec") {
            return true
        }

        // If the source is from a function's output, it should only connect to node inputs
        val sourceIsFunction = workspace.graph.functions.any { it.uid == sourceEdge.owner }
        if (sourceIsFunction && sourceEdge.direction == "output" && targetEdge.direction != "input") {
            return false
        }

        // If the target is a function's input, it should only connect to node outputs
        val targetIsFunction = workspace.graph.functions.any { it.uid == targetEdge.owner }
        if (targetIsFunction && targetEdge.direction == "input" && sourceEdge.direction != "output") {
            return false
        }

        //If source and edge aren't exec, and the target already has a link, it's invalid
        if (sourceEdge.type != "exec" && targetEdge.type != "exec" && workspace.graph.links
                .any { it.to == targetEdge.uid }
        ) {
            return false
        }

        //Early return if the types are the same
        if (sourceEdge.type == targetEdge.type) return true

        //Splits types by "or" keyword with surrounding space. Checks against all other split types
        val sourceTypes = sourceEdge.type.split(" or ").map { it.trim() }
        val targetTypes = targetEdge.type.split(" or ").map { it.trim() }
        //Returns true if any of the source types are in the target types
        return sourceTypes.any { it in targetTypes }
    }


    private fun createLink(sourceNode: Node, sourceEdge: Edge, targetNode: Node, targetEdge: Edge) {
        val link = configured<Link> {
            "owner" to sourceNode.uid
            "from" to sourceEdge.uid
            "to" to targetEdge.uid
        }

        client.send(LinkCreateRequest(link))
    }

    private fun isMouseOverEdge(edgeBounds: Vector4f): Boolean {
        val mousePos = ImGui.getMousePos()
        val edgeCenter = Vector2f(edgeBounds.x, edgeBounds.y)
        val hitboxRadius = 5f * zoom // Adjust this value to change the hitbox size

        // Calculate the distance between the mouse and the edge center
        val dx = mousePos.x - edgeCenter.x
        val dy = mousePos.y - edgeCenter.y
        val distanceSquared = dx * dx + dy * dy

        // Check if the mouse is within the circular hitbox
        return distanceSquared <= hitboxRadius * hitboxRadius
    }

    fun handleEdgeDragging() {
        val draggedEdge = draggedEdge
        val dragStartPos = dragStartPos

        if (draggedEdge != null && dragStartPos != null) {
            val mousePos = ImGui.getMousePos()

            if (ImGui.isMouseReleased(ImGuiMouseButton.Left)) {
                val targetEdgePair = findEdgeUnderMouse()
                if (targetEdgePair != null) {
                    val (targetOwner, targetEdgeObj) = targetEdgePair

                    when (draggedEdge.first) {
                        is Node -> {
                            when (targetOwner) {
                                is Node -> createLink(
                                    draggedEdge.first as Node,
                                    draggedEdge.second,
                                    targetOwner,
                                    targetEdgeObj
                                )

                                is Function -> createFunctionLink(
                                    draggedEdge.first as Node,
                                    draggedEdge.second,
                                    targetOwner,
                                    targetEdgeObj
                                )
                            }
                        }

                        is Function -> {
                            when (targetOwner) {
                                is Node -> createFunctionLink(
                                    targetOwner,
                                    targetEdgeObj,
                                    draggedEdge.first as Function,
                                    draggedEdge.second
                                )

                                is Function -> {
                                    // Optionally handle function-to-function connections if needed
                                    logger.warn { "Function to function connections are not supported" }
                                }
                            }
                        }
                    }
                } else {
                    // Open action menu with compatible nodes when dropped over empty space
                    draggedSourceEdge = when (val source = draggedEdge.first) {
                        is Node -> Pair(source, draggedEdge.second)
                        else -> null
                    }
                    draggedSourceEdge?.let {
                        openActionMenuWithCompatibleNodes(draggedEdge.second, Vector2f(mousePos.x, mousePos.y))
                    }
                }
                this.draggedEdge = null
                this.dragStartPos = null
                isLinking = false
            }
        }
    }

    private fun createFunctionLink(node: Node, nodeEdge: Edge, function: Function, functionEdge: Edge) {
        val link = configured<Link> {
            if (nodeEdge.direction == "output" && functionEdge.direction == "input") {
                "owner" to node.uid
                "from" to nodeEdge.uid
                "to" to functionEdge.uid
            } else {
                "owner" to node.uid
                "from" to functionEdge.uid
                "to" to nodeEdge.uid
            }
        }

        client.send(LinkCreateRequest(link))
    }

    fun createNodeAndLink(position: Vector2f, nodeType: String) {
        val worldPos = convertToWorldCoordinates(position)
        val createRequest = NodeCreateRequest(nodeType, worldPos)

        //If the node position is within a function body, we need to add it to the function
        val function = workspace.graph.functions.find { function ->
            val functionBounds = getFunctionBounds(function)
            val functionPos = convertToScreenCoordinates(Vector2f(function.x, function.y))
            val functionSize = convertToScreenSize(Vector2f(function.width, function.height))
            val functionScreenBounds = Vector4f(
                functionPos.x,
                functionPos.y,
                functionPos.x + functionSize.x,
                functionPos.y + functionSize.y
            )
            functionScreenBounds.contains(position.x, position.y)
        }


        if (function != null && !function.minimized) {
            createRequest.function = function.uid
        }

        client.send(createRequest)

        // Store the create request to link it later when we receive the node creation confirmation
        pendingNodeCreation = createRequest
    }

    private var pendingNodeCreation: NodeCreateRequest? = null

    private fun openActionMenuWithCompatibleNodes(sourceEdge: Edge, position: Vector2f) {
        val compatibleNodes = getCompatibleNodes(sourceEdge)
        CustomActionMenu.openWithFilteredNodes(position, compatibleNodes)
    }


    private fun getCompatibleNodes(sourceEdge: Edge): List<String> {
        return nodeLibrary.collect().filter { nodeType ->
            hasCompatibleEdge(nodeType, sourceEdge) && !nodeType.isAbstract()
        }.map { it.meta.nodeTypeName }
    }

    private fun hasCompatibleEdge(nodeType: NodeType, sourceEdge: Edge): Boolean {
        val edges = nodeType.properties["edges"] as? Property.Object ?: return false
        return edges.get().any { (_, edgeProperty) ->
            if (edgeProperty !is Property.Object) return@any false
            val edge = Edge(edgeProperty)
            canConnect(sourceEdge, edge)
        }
    }

    fun startEdgeDrag(owner: Any, edge: Edge) {
        draggedEdge = Pair(owner, edge)
        val edgeBounds = when (owner) {
            is Node -> getEdgeBounds(owner, edge)
            is Function -> {
                val functionBounds = getFunctionBounds(owner)
                Vector4f().apply {
                    val pos = getEdgePosition(owner, edge, functionBounds)
                    x = pos.x
                    y = pos.y
                    z = pos.x
                    w = pos.y
                }
            }

            else -> return
        }
        dragStartPos = Vector2f(edgeBounds.x, edgeBounds.y)
        isLinking = true
    }

    fun getEdgePosition(node: Node, edge: Edge, nodeBounds: Vector4f): Vector2f {
        val edgeSpacing = 20f * zoom
        val edgeStartY = nodeBounds.y + 30f * zoom  // Start below the header

        val edges = workspace.graph.getEdges(node)
        val edgesOfSameDirection = edges.filter { it.direction == edge.direction }
        val index = edgesOfSameDirection.indexOf(edge)

        val yPos = edgeStartY + index * edgeSpacing
        val xPos = if (edge.direction == "input") nodeBounds.x - 10f * zoom else nodeBounds.z + 10f * zoom

        return Vector2f(xPos, yPos)
    }

    fun getEdgePosition(func: Function, edge: Edge, nodeBounds: Vector4f): Vector2f {
        val edgeSpacing = 20f * zoom
        val edgeStartY = nodeBounds.y + 50f * zoom  // Start below the header

        val edges = workspace.graph.getEdges(func.uid)
        val edgesOfSameDirection = edges.filter { it.direction == edge.direction }
        val index = edgesOfSameDirection.indexOf(edge)

        val yPos = edgeStartY + index * edgeSpacing
        val xPos = if (edge.direction == "input") nodeBounds.x + 8f * zoom else nodeBounds.z - 8f * zoom
        return Vector2f(xPos, yPos)
    }

    fun isEdgeSelected(edge: Edge): Boolean {
        return selectedEdge?.second?.uid == edge.uid
    }

    companion object {

        private val logger = KotlinLogging.logger {}
    }

}

