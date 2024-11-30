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

    private val headerFamily get() = Fonts.getFamily("Inter")["Bold"]
    private val headerFont get() = headerFamily[workspace.settings.fontHeaderSize]
    private val bodyFamily get() = Fonts.getFamily("Inter")["Regular"]
    private val bodyFont get() = bodyFamily[workspace.settings.fontSize]
    private val fontAwesomeFamily get() = Fonts.getFamily("Fa")["Regular"]

    internal var isLinking = false

    private val dragOffset: Vector2f = Vector2f()
    internal var isDraggingNode = false
    internal var wasDraggingNode = false
    private var isDraggingGroup = false
    private val groupDragOffset = mutableMapOf<UUID, Vector2f>()
    private var draggedSourceEdge: Pair<Node, Edge>? = null
    var draggedEdge: Pair<Node, Edge>? = null
    var dragStartPos: Vector2f? = null

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

        for (node in workspace.graph.nodes) {
            val nodeBounds = getNodeBounds(node)
            val headerBounds = getHeaderBounds(node)
            if (headerBounds.contains(mousePos.x, mousePos.y)) {
                hoveredTitleBar = node.uid
                return
            }

//            for (edge in workspace.graph.getEdges(node)) {
//                val edgeBounds = getEdgePosition(node, edge, nodeBounds)
//                if (isPointOverEdge(mousePos, edgeBounds)) {
//                    hoveredPin = Pair(node.uid, edge)
//                    return
//                }
//            }
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

    fun notifications() {

    }

    fun getHoverCursor(): Int {
        return when {
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
                        selectedNode.x = if (isShiftDown) snapToGrid(mx - offset.x) else mx - offset.x
                        selectedNode.y = if (isShiftDown) snapToGrid(my - offset.y) else my - offset.y
                        sendMovePacket(selectedNode)
                    }
                }
            } else {
                // Move only the dragged node
                node.x = if (isShiftDown) snapToGrid(mx - dragOffset.x) else mx - dragOffset.x
                node.y = if (isShiftDown) snapToGrid(my - dragOffset.y) else my - dragOffset.y
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

            // We're not unselecting nodes here anymore
            // unselectAllNodes()
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
        val sourceNode = workspace.getNode(sourceEdge.owner) ?: return false
        val targetEdge = workspace.graph.getEdge(link.to) ?: return false
        val targetNode = workspace.getNode(targetEdge.owner) ?: return false

        val sourceBounds = getEdgeBounds(sourceNode, sourceEdge)
        val targetBounds = getEdgeBounds(targetNode, targetEdge)

        val startPos = Vector2f(sourceBounds.x, sourceBounds.y)
        val endPos = Vector2f(targetBounds.x, targetBounds.y)

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
        if (isLinking || isPropertyWindowHovered) return

        val mousePos = ImGui.getMousePos()
        val isLeftClickPressed = ImGui.isMouseClicked(ImGuiMouseButton.Left)
        val isLeftClickReleased = ImGui.isMouseReleased(ImGuiMouseButton.Left)
        val isLeftClickDragging = ImGui.isMouseDragging(ImGuiMouseButton.Left)
        val isCtrlPressed = Platform.isKeyDown(ClientRuntime.Key.LEFT_CONTROL)

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

    private fun updateFinalSelection() {
        if (selectionStart == null || selectionEnd == null) return

        val topLeft = Vector2f(minOf(selectionStart!!.x, selectionEnd!!.x), minOf(selectionStart!!.y, selectionEnd!!.y))
        val bottomRight = Vector2f(
            maxOf(selectionStart!!.x, selectionEnd!!.x), maxOf(selectionStart!!.y, selectionEnd!!.y)
        )

        if (!Platform.isKeyDown(ClientRuntime.Key.LEFT_CONTROL)) {
            clearSelection()
        }

        workspace.graph.nodes.forEach { node ->
            if (nodesInSelectionBox.contains(node.uid)) {
                selectedNodeIds.add(node.uid)
                //node.selected = true
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
        //workspace.graph.nodes.forEach { it.selected = false }
    }

    private fun findLinkUnderMouse(mousePos: Vector2f): Link? {
        return workspace.graph.links.find { link ->
            isMouseOverLink(link, mousePos)
        }
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


    fun isMouseOverLink(link: Link, mousePos: Vector2f): Boolean {
        val sourceEdge = workspace.graph.getEdge(link.from) ?: return false
        val sourceNode = workspace.getNode(sourceEdge.owner) ?: return false
        val targetEdge = workspace.graph.getEdge(link.to) ?: return false
        val targetNode = workspace.getNode(targetEdge.owner) ?: return false

        val sourceBounds = getNodeBounds(sourceNode)
        val targetBounds = getNodeBounds(targetNode)

//        val startPos = Vector2f(sourceBounds.x, sourceBounds.y)
//        val endPos = Vector2f(targetBounds.x, targetBounds.y)
        val startPos = getEdgePosition(sourceNode, sourceEdge, sourceBounds)
        val endPos = getEdgePosition(targetNode, targetEdge, targetBounds)
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
//            logger.info { "Sent move packet for node ${node.uid}" }
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
        } else if (packet is UserConnectedToWorkspace) {
            packet.users.forEach { user ->
                connectedUsers[user.uid] = user
            }
            logger.info { "Received user connected to workspace: ${packet.users}" }
        } else if (packet is NodeCreated) {
            val node = packet.node
            processNewNode(node)

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

    private fun findEdgeUnderMouse(): Pair<Node, Edge>? {
        val mousePos = ImGui.getMousePos()
        for (node in workspace.graph.nodes) {
            val nodeBounds = getNodeBounds(node)
            val edges = workspace.graph.getEdges(node)
            val inputEdges = edges.filter { it.direction == "input" }
            val outputEdges = edges.filter { it.direction == "output" }

            val edgeSpacing = 20f * zoom
            val edgeStartY = nodeBounds.y + 30f * zoom

            // Check input edges
            inputEdges.forEachIndexed { index, edge ->
                val yPos = edgeStartY + index * edgeSpacing
                val edgePos = Vector2f(nodeBounds.x - 10f * zoom, yPos)
                if (isPointOverEdge(Vector2f(mousePos.x, mousePos.y), edgePos)) {
                    return Pair(node, edge)
                }
            }

            // Check output edges
            outputEdges.forEachIndexed { index, edge ->
                val yPos = edgeStartY + index * edgeSpacing
                val edgePos = Vector2f(nodeBounds.z + 10f * zoom, yPos)
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

        // Prevent connecting edges from the same node
        if (sourceEdge.owner == targetEdge.owner) return false


        //If source and edge aren't exec, and the target already has a link, it's invalid
        if (sourceEdge.type != "exec" && targetEdge.type != "exec" && workspace.graph.links
                .any { it.to == targetEdge.uid }
        ) {
            return false
        }

        // If either is "any" type and both are not exec, it's valid
        if (sourceEdge.type == "any" && targetEdge.type != "exec" || targetEdge.type == "any" && sourceEdge.type != "exec") {
            return true
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
            val (sourceNode, sourceEdge) = draggedEdge

            val mousePos = ImGui.getMousePos()

            if (ImGui.isMouseReleased(ImGuiMouseButton.Left)) {
                val targetEdge = findEdgeUnderMouse()
                if (targetEdge != null) {
                    val (targetNode, targetEdgeObj) = targetEdge
                    if (canConnect(sourceEdge, targetEdgeObj)) {
                        createLink(sourceNode, sourceEdge, targetNode, targetEdgeObj)
                    }
                } else {
                    // Open action menu with compatible nodes when dropped over empty space
                    draggedSourceEdge = Pair(sourceNode, sourceEdge)
                    openActionMenuWithCompatibleNodes(sourceEdge, Vector2f(mousePos.x, mousePos.y))
                }
                this.draggedEdge = null
                this.dragStartPos = null
                isLinking = false
            }
        }
    }

    fun createNodeAndLink(position: Vector2f, nodeType: String) {
        val worldPos = convertToWorldCoordinates(position)
        val createRequest = NodeCreateRequest(nodeType, worldPos)
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

    fun startEdgeDrag(node: Node, edge: Edge) {
        draggedEdge = Pair(node, edge)
        val edgeBounds = getEdgeBounds(node, edge)
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

    fun isEdgeSelected(edge: Edge): Boolean {
        return selectedEdge?.second?.uid == edge.uid
    }

    companion object {

        private val logger = KotlinLogging.logger {}
    }

}

