package bpm.client.runtime.windows

import imgui.ImColor
import imgui.ImDrawList
import imgui.ImGui
import imgui.flag.*
import bpm.common.logging.KotlinLogging
import bpm.client.font.Fonts
import bpm.client.runtime.Platform
import bpm.client.runtime.ClientRuntime
import bpm.client.utils.use
import bpm.common.network.Client
import bpm.common.network.Endpoint
import bpm.common.network.Listener
import bpm.common.packets.Packet
import bpm.common.packets.internal.Time
import bpm.common.property.Property
import bpm.common.property.PropertyMap
import bpm.common.property.configured
import bpm.common.schemas.Schemas
import bpm.common.type.NodeLibrary
import bpm.common.type.NodeType
import bpm.common.utils.contains
import bpm.common.workspace.Workspace
import bpm.common.workspace.graph.Edge
import bpm.common.workspace.graph.Link
import bpm.common.workspace.graph.Node
import bpm.common.workspace.graph.User
import bpm.common.workspace.packets.*
import org.joml.Vector2f
import org.joml.Vector4f
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.math.max

class CanvasContext : Listener {

    private val runtime: ClientRuntime get() = Endpoint.installed()
    private val dragOffset: Vector2f = Vector2f()
    private val nodeMovePacket = NodeMoved()
    private var lastSent: Time = Time.now
    // The maximum send rate in milliseconds, 60 PPS (Packets Per Second)
    private val maxSendRate = 1000 / 60
    // The maximum move threshold in pixels
    private val maxMoveThreshold = 5f
    private val connectedUsers = mutableMapOf<UUID, User>()
    internal var isDraggingNode = false

    private var lastNotificationEndTime = 0f
    private var selectionStart: Vector2f? = null
    private var selectionEnd: Vector2f? = null
    private val selectedNodes = mutableSetOf<UUID>()
    private val selectedLinks = mutableSetOf<UUID>()
    private val notificationQueue = ConcurrentLinkedQueue<NotifyMessage>()
    private var lastNotification: NotifyMessage? = null
    private var repeatedCount = 0
    private val workspace: Workspace get() = runtime.workspace ?: throw IllegalStateException("Workspace is null")

    private val headerFamily get() = Fonts.getFamily("Inter")["Bold"]
    private val headerFont get() = headerFamily[workspace.settings.fontHeaderSize]
    private val bodyFamily get() = Fonts.getFamily("Inter")["Regular"]
    private val bodyFont get() = bodyFamily[workspace.settings.fontSize]
    private val fontawesomeFamily get() = Fonts.getFamily("Fa")["Regular"]
    private val fontawesome get() = fontawesomeFamily[workspace.settings.fontSize]

    private var selectedEdge: Pair<Node, Edge>? = null
    private var dragStartPos: Vector2f? = null

    internal var isLinking = false

    private val gridSize = 20f // Grid size for snapping
    private var isDraggingGroup = false
    private val groupDragOffset = mutableMapOf<UUID, Vector2f>()
    private val nodesInSelectionBox = mutableSetOf<UUID>()
    var hoveredTitleBar: UUID? = null
    var hoveredPin: Pair<UUID, Edge>? = null
    private var draggedEdge: Pair<Node, Edge>? = null

    internal val customActionMenu: CustomActionMenu by lazy { CustomActionMenu(workspace, this) }
    private var draggedSourceEdge: Pair<Node, Edge>? = null
    internal val variablesMenu by lazy { VariablesMenu(this) }
    private val notificationManager = NotificationManager()
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


    override fun onTick(delta: Float, tick: Int) {
        //send mouse position if on window
        val mousePos = ImGui.getMousePos()
        //val mx = mousePos.x / runtime.workspace!!.settings.zoom

    }

    fun updateNodesInSelectionBox() {
        val start = selectionStart ?: return
        val end = selectionEnd ?: return

        val topLeft = Vector2f(minOf(start.x, end.x), minOf(start.y, end.y))
        val bottomRight = Vector2f(maxOf(start.x, end.x), maxOf(start.y, end.y))

        nodesInSelectionBox.clear()

        for (node in workspace.graph.nodes) {
            val bounds = computeNodeBounds(node)
            if (bounds.x < bottomRight.x && bounds.z > topLeft.x && bounds.y < bottomRight.y && bounds.w > topLeft.y) {
                nodesInSelectionBox.add(node.uid)
            }
        }
    }

    fun updateHoverState(mousePos: Vector2f) {
        hoveredTitleBar = null
        hoveredPin = null

        for (node in workspace.graph.nodes) {
            val headerBounds = computeHeaderBounds(node)
            if (headerBounds.contains(mousePos.x, mousePos.y)) {
                hoveredTitleBar = node.uid
                return
            }

            for (edge in workspace.graph.getEdges(node)) {
                val edgeBounds = computeEdgeBounds(node, edge)
                if (isPointOverEdge(mousePos, edgeBounds)) {
                    hoveredPin = Pair(node.uid, edge)
                    return
                }
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

    fun notifications() {

        val drawList = ImGui.getWindowDrawList()
        val displaySize = ImGui.getIO().displaySize
        notificationManager.renderNotifications(drawList, displaySize)

//        val drawList = ImGui.getWindowDrawList()
//        val currentTime = ImGui.getTime().toFloat()
//        val displaySize = ImGui.getIO().displaySize
//        var yOffset = displaySize.y - 20f  // Start from bottom
//
//        val visibleNotifications = mutableListOf<NotifyMessage>()
//        val iterator = notificationQueue.iterator()
//        var lastUniqueNotification: NotifyMessage? = null
//        var repeatedCount = 0
//
//        while (iterator.hasNext() && visibleNotifications.size < 4) {
//            val message = iterator.next()
//
//            // If this is a new unique message, reset the count
//            if (lastUniqueNotification == null || message.message != lastUniqueNotification.message) {
//                lastUniqueNotification = message.copy()
//                repeatedCount = 1
//            } else {
//                repeatedCount++
//            }
//
//            // Update the header with the repeat count
//            message.header = if (repeatedCount > 1) "${message.header} ($repeatedCount)" else message.header
//
//            // Calculate alpha based on the time the notification became visible
//            val visibleTime = currentTime - max(message.time, lastNotificationEndTime)
//            val alpha = calculateAlpha(visibleTime, message.lifetime)
//
//            if (alpha <= 0) {
//                if (visibleTime > message.lifetime) {
//                    iterator.remove()
//                    lastNotificationEndTime = currentTime
//                }
//                continue
//            }
//
//            visibleNotifications.add(message)
//        }
//
//        for (message in visibleNotifications.asReversed()) {  // Render from bottom to top
//            val visibleTime = currentTime - max(message.time, lastNotificationEndTime)
//            val alpha = calculateAlpha(visibleTime, message.lifetime)
//            val bgColor = parseColor(message.color, (alpha * 255).toInt())
//            val textColor = ImColor.rgba(255, 255, 255, (alpha * 255).toInt())
//            val headerColor = ImColor.rgba(255, 255, 255, (alpha * 255).toInt())
//
//            val padding = 16f
//            val iconSize = 24f
//            val headerHeight = 32f
//            val margin = 15f
//
//            headerFont.use {
//                val headerSize = ImGui.calcTextSize(message.header)
//
//                bodyFont.use {
//                    val textSize = ImGui.calcTextSize(message.message)
//
//                    val totalWidth = maxOf(headerSize.x, textSize.x) + padding * 3 + iconSize
//                    val totalHeight = headerHeight + textSize.y + padding * 3
//
//                    val xPos = displaySize.x - totalWidth - margin - (margin / 2.5f)
//                    yOffset -= totalHeight  // Move up for each notification
//                    val yPos = yOffset - margin / 2.5f
//
//                    // Main background with subtle gradient
//                    drawList.addRectFilledMultiColor(
//                        xPos, yPos,
//                        xPos + totalWidth, yPos + totalHeight,
//                        ImColor.rgba(60, 60, 60, (alpha * 255).toInt()).toLong(),
//                        ImColor.rgba(50, 50, 50, (alpha * 255).toInt()).toLong(),
//                        ImColor.rgba(40, 40, 40, (alpha * 255).toInt()).toLong(),
//                        ImColor.rgba(30, 30, 30, (alpha * 255).toInt()).toLong()
//                    )
//
//                    //Draws a header bar with the bg color
//                    drawList.addRectFilled(
//                        xPos, yPos,
//                        xPos + totalWidth, yPos + headerHeight,
//                        bgColor,
//                        2f
//                    )
//
//                    // Colored accent bar based on notification type
//                    val accentColor = when (message.type) {
//                        NotifyMessage.NotificationType.INFO -> ImColor.rgba(70, 130, 180, (alpha * 255).toInt())
//                        NotifyMessage.NotificationType.SUCCESS -> ImColor.rgba(60, 179, 113, (alpha * 255).toInt())
//                        NotifyMessage.NotificationType.WARNING -> ImColor.rgba(255, 165, 0, (alpha * 255).toInt())
//                        NotifyMessage.NotificationType.ERROR -> ImColor.rgba(220, 20, 60, (alpha * 255).toInt())
//                    }
//                    drawList.addRectFilled(
//                        xPos, yPos,
//                        xPos + 4f, yPos + totalHeight,
//                        accentColor,
//                        2f
//                    )
//
//                    // Subtle border
//                    drawList.addRect(
//                        xPos, yPos,
//                        xPos + totalWidth, yPos + totalHeight,
//                        ImColor.rgba(200, 200, 200, (alpha * 77).toInt()),  // 30% of 255 is about 77
//                        4f
//                    )
//
//                    // Icon
//                    val icon = message.icon.toChar().toString()
//                    fontawesome.use {
//                        drawList.addText(
//                            fontawesome,
//                            iconSize,
//                            xPos + padding,
//                            yPos + (headerHeight - iconSize) / 2,
//                            headerColor,
//                            icon
//                        )
//                    }
//
//                    // Header text
//                    headerFont.use {
//                        drawList.addText(
//                            headerFont,
//                            workspace.settings.fontHeaderSize.toFloat(),
//                            xPos + padding * 2 + iconSize,
//                            yPos + (headerHeight - headerSize.y) / 2,
//                            headerColor,
//                            message.header
//                        )
//                    }
//
//                    // Message text
//                    bodyFont.use {
//                        drawList.addText(
//                            bodyFont,
//                            workspace.settings.fontSize.toFloat(),
//                            xPos + padding,
//                            yPos + headerHeight + padding,
//                            textColor,
//                            message.message
//                        )
//                    }
//
//                    yOffset -= 10f  // Add some space between notifications
//                }
//            }
//        }
    }

    private fun calculateAlpha(visibleTime: Float, lifetime: Float): Float {
        val fadeInTime = 0.3f
        val fadeOutTime = 0.5f

        return when {
            visibleTime < 0f -> 0f
            visibleTime < fadeInTime -> visibleTime / fadeInTime
            visibleTime > lifetime - fadeOutTime -> 1f - (visibleTime - (lifetime - fadeOutTime)) / fadeOutTime
            visibleTime > lifetime -> 0f
            else -> 1f
        }.coerceIn(0f, 1f)
    }

    private fun parseColor(colorString: String, alpha: Int): Int {
        return try {
            val rgb = colorString.removePrefix("#").toInt(16)
            ImColor.rgba((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF, alpha)
        } catch (e: Exception) {
            ImColor.rgba(255, 255, 255, alpha)  // Default to white if parsing fails
        }
    }

    private fun getColors(type: NotifyMessage.NotificationType, alpha: Int): Pair<Int, Int> {
        val colorVec = when (type) {
            NotifyMessage.NotificationType.INFO -> listOf(70, 130, 180, 255, 255, 255)
            NotifyMessage.NotificationType.SUCCESS -> listOf(60, 179, 113, 255, 255, 255)
            NotifyMessage.NotificationType.WARNING -> listOf(255, 165, 0, 0, 0, 0)
            NotifyMessage.NotificationType.ERROR -> listOf(220, 20, 60, 255, 255, 255)
        }

        val bgColor = ImColor.rgba(colorVec[0], colorVec[1], colorVec[2], alpha)
        val textColor = ImColor.rgba(colorVec[3], colorVec[4], colorVec[5], alpha)

        return Pair(bgColor, textColor)
    }


    fun isPointHovered(bounds: Vector4f): Boolean {
        val mousePos = ImGui.getMousePos()
        return bounds.contains(mousePos.x, mousePos.y)
    }


    fun getHoverCursor(): Int {
        return when {
            hoveredTitleBar != null -> ImGuiMouseCursor.Hand
            hoveredPin != null -> ImGuiMouseCursor.ResizeAll
            else -> ImGuiMouseCursor.Arrow
        }
    }

    fun isNodeInSelectionBox(node: Node): Boolean {
        return nodesInSelectionBox.contains(node.uid)
    }

    fun isLinkSelected(link: Link): Boolean {
        return selectedLinks.contains(link.uid)
    }

    fun isNodeSelected(node: Node): Boolean {
        return selectedNodes.contains(node.uid)
    }


    private fun handleNodeDrag(node: Node, nodeBounds: Vector4f, headerBounds: Vector4f) {
        val mousePos = ImGui.getMousePos()
        if (node.dragged) {
            val mx = mousePos.x / runtime.workspace!!.settings.zoom
            val my = mousePos.y / runtime.workspace!!.settings.zoom
            val isShiftDown = Platform.isKeyDown(ClientRuntime.Key.LEFT_SHIFT) || Platform.isKeyDown(ClientRuntime.Key.RIGHT_SHIFT)

            if (isDraggingGroup) {
                // Move all selected nodes
                for (selectedNodeId in selectedNodes) {
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
            // We're not unselecting nodes here anymore
            // unselectAllNodes()
        }
    }

    fun unselectAllNodes() {
        for (nodeId in selectedNodes) {
            val node = runtime.workspace!!.getNode(nodeId)
            node?.selected = false
        }
        selectedNodes.clear()
    }


    private fun updateSelection() {
        val start = selectionStart ?: return
        val end = selectionEnd ?: return

        val topLeft = Vector2f(minOf(start.x, end.x), minOf(start.y, end.y))
        val bottomRight = Vector2f(maxOf(start.x, end.x), maxOf(start.y, end.y))

        selectedNodes.clear()
        selectedLinks.clear()

        for (node in workspace.graph.nodes) {
            val bounds = computeNodeBounds(node)
            if (bounds.x < bottomRight.x && bounds.z > topLeft.x && bounds.y < bottomRight.y && bounds.w > topLeft.y) {
                selectedNodes.add(node.uid)
                node.selected = true  // Set the selected property of the node
            }
        }


        for (link in workspace.graph.getLinks()) {
            val sourceEdge = workspace.graph.getEdge(link.from) ?: continue
            val sourceNode = workspace.getNode(sourceEdge.owner) ?: continue
            val targetEdge = workspace.graph.getEdge(link.to) ?: continue
            val targetNode = workspace.getNode(targetEdge.owner) ?: continue

            val sourceBounds = computeEdgeBounds(sourceNode, sourceEdge)
            val targetBounds = computeEdgeBounds(targetNode, targetEdge)

            if ((sourceBounds.x in topLeft.x..bottomRight.x && sourceBounds.y in topLeft.y..bottomRight.y) || (targetBounds.x in topLeft.x..bottomRight.x && targetBounds.y in topLeft.y..bottomRight.y)) {
                selectedLinks.add(link.uid)
            }
        }
    }


    fun computeEdgeBounds(owner: Node, edge: Edge): Vector4f {
        val nodeBounds = computeNodeBounds(owner)
        val nodePos = workspace.convertPosition(owner.x, owner.y)
        val nodeSize = workspace.convertSize(owner.width, owner.height)
        var xPos = 0f
        var yOffset = 0f
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


    fun computeNodeBounds(node: Node, headerPadding: Float = 20f): Vector4f {
        val nodePos = convertToScreenCoordinates(Vector2f(node.x, node.y))
        val nodeSize = convertToScreenSize(Vector2f(node.width, node.height))
        val padding = headerPadding * zoom

        // Ensure the node is large enough to accommodate all edges
        val inputCount = workspace.graph.getEdges(node).count { it.direction == "input" }
        val outputCount = workspace.graph.getEdges(node).count { it.direction == "output" }
        val edgeCount = maxOf(inputCount, outputCount)
        val minHeight = (edgeCount + 1) * 20f * zoom

        nodeSize.y = maxOf(nodeSize.y, minHeight)
//
        return Vector4f(
            nodePos.x - padding / 2f,
            nodePos.y - padding / 2f,
            nodePos.x + nodeSize.x,
            nodePos.y + nodeSize.y,
        )
    }


    fun computeHeaderBounds(node: Node): Vector4f {
        return headerFont.use {
            val nodePos = workspace.convertPosition(node.x, node.y)
            val nodeSize = workspace.convertSize(node.width, node.height)
            val titleSize = ImGui.calcTextSize(node.name)
            val titleWidth = titleSize.x + 4f * zoom
            val titleHeight = titleSize.y
            val titleX = nodePos.x - 8 * zoom
            val titleY = (nodePos.y - titleHeight / 2) - 8 * zoom

            Vector4f(titleX, titleY, nodePos.x + nodeSize.x, titleY + titleHeight)
        }
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
                val bounds = computeNodeBounds(node)
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
            selectionEnd = Vector2f(mousePos.x, mousePos.y)
            updateNodesInSelectionBox()
            updateLinksInSelectionBox()
        }

        if (isLeftClickReleased) {
            if (selectionStart != null) {
                updateFinalSelection()
                selectionStart = null
                selectionEnd = null
            }
        }
    }


    private fun clearSelection() {
        selectedNodes.clear()
        selectedLinks.clear()
        workspace.graph.nodes.forEach { it.selected = false }
    }

    private fun updateLinksInSelectionBox() {
        val start = selectionStart ?: return
        val end = selectionEnd ?: return

        val topLeft = Vector2f(minOf(start.x, end.x), minOf(start.y, end.y))
        val bottomRight = Vector2f(maxOf(start.x, end.x), maxOf(start.y, end.y))

        workspace.graph.getLinks().forEach { link ->
            if (isLinkInSelectionBox(link, topLeft, bottomRight)) {
                selectedLinks.add(link.uid)
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
            val bounds = computeNodeBounds(node)
            if (bounds.x < bottomRight.x && bounds.z > topLeft.x && bounds.y < bottomRight.y && bounds.w > topLeft.y) {
                selectedNodes.add(node.uid)
                node.selected = true
            }
        }

        workspace.graph.getLinks().forEach { link ->
            if (isLinkInSelectionBox(link, topLeft, bottomRight)) {
                selectedLinks.add(link.uid)
            }
        }
    }

    private fun findLinkUnderMouse(mousePos: Vector2f): Link? {
        return workspace.graph.getLinks().find { link ->
            isMouseOverLink(link, mousePos)
        }
    }

    private fun isLinkInSelectionBox(link: Link, topLeft: Vector2f, bottomRight: Vector2f): Boolean {
        val sourceEdge = workspace.graph.getEdge(link.from) ?: return false
        val sourceNode = workspace.getNode(sourceEdge.owner) ?: return false
        val targetEdge = workspace.graph.getEdge(link.to) ?: return false
        val targetNode = workspace.getNode(targetEdge.owner) ?: return false

        val sourceBounds = computeEdgeBounds(sourceNode, sourceEdge)
        val targetBounds = computeEdgeBounds(targetNode, targetEdge)

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

        val sourceBounds = computeEdgeBounds(sourceNode, sourceEdge)
        val targetBounds = computeEdgeBounds(targetNode, targetEdge)

        val startPos = Vector2f(sourceBounds.x, sourceBounds.y)
        val endPos = Vector2f(targetBounds.x, targetBounds.y)

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


    fun selectLink(link: Link) {
        selectedLinks.add(link.uid)
    }

    fun unselectLink(link: Link) {
        selectedLinks.remove(link.uid)
    }

    fun clearLinkSelection() {
        selectedLinks.clear()
    }

    fun getSelectedLinks(): Set<UUID> {
        return selectedLinks.toSet()
    }


    private fun handleLinkSelection(link: Link, isCtrlPressed: Boolean) {
        if (isCtrlPressed) {
            if (selectedLinks.contains(link.uid)) {
                selectedLinks.remove(link.uid)
            } else {
                selectedLinks.add(link.uid)
            }
        } else {
            clearSelection()
            selectedLinks.add(link.uid)
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
            nodeMovePacket.x = node.x
            nodeMovePacket.y = node.y
            runtime.client.send(nodeMovePacket)
            lastSent = Time.now
            logger.info { "Sent move packet for node ${node.uid}" }
        }
    }

    fun handleStartDrag(node: Node, headerBounds: Vector4f) {
        if (node.dragged || (runtime.selectedNode != null && runtime.selectedNode!!.uid != node.uid) || selectionStart != null) return
        val mousePos = ImGui.getMousePos()
        val mx = mousePos.x / runtime.workspace!!.settings.zoom
        val my = mousePos.y / runtime.workspace!!.settings.zoom
        val headerHovered = headerBounds.contains(mousePos.x, mousePos.y)
        if (headerHovered && ImGui.isMouseDown(ImGuiMouseButton.Left) && !node.dragged) {
            dragOffset.set(mx - node.x, my - node.y)
            node.dragged = true
            runtime.selectedNode = node
            isDraggingNode = true

            if (selectedNodes.contains(node.uid)) {
                isDraggingGroup = true
                // Calculate offsets for all selected nodes
                for (selectedNodeId in selectedNodes) {
                    val selectedNode = runtime.workspace!!.getNode(selectedNodeId)
                    if (selectedNode != null) {
                        groupDragOffset[selectedNodeId] = Vector2f(mx - selectedNode.x, my - selectedNode.y)
                    }
                }
            } else {
                // If the dragged node is not in the selection, clear the selection and select only this node
                selectedNodes.clear()
                selectedNodes.add(node.uid)
            }
        }
    }

    private fun snapToGrid(value: Float): Float {
        return (value / gridSize).toInt() * gridSize
    }

    fun getSelection(): Pair<Vector2f, Vector2f>? {
        return selectionStart?.let { start ->
            selectionEnd?.let { end ->
                Pair(start, end)
            }
        }
    }

    fun handleContextMenu(contextMenuPosition: Vector2f) {
        if (ImGui.beginPopup("CanvasContextMenu", ImGuiPopupFlags.MouseButtonRight)) {
            if (ImGui.beginMenu("Create Node")) {
                for (nodeTypeMeta in nodeLibrary) {
                    if (ImGui.menuItem(nodeTypeMeta.nodeTypeName)) {
                        createNode(contextMenuPosition, nodeTypeMeta.nodeTypeName)
                    }
                }
                ImGui.endMenu()
            }

            if (selectedNodes.isNotEmpty() || selectedLinks.isNotEmpty()) {
                if (ImGui.menuItem("Delete Selected")) {
                    deleteSelected()
                }
            }

            ImGui.endPopup()
        }
    }


    fun createNode(position: Vector2f, nodeType: String) {
        // Convert screen position to world position
        val worldPos = convertToWorldCoordinates(position)
        client.send(NodeCreateRequest(nodeType, worldPos))
    }

    fun createVariableNode(type: bpm.common.workspace.packets.NodeType, position: Vector2f, name: String) {
        val worldPos = convertToWorldCoordinates(position)
        client.send(VariableNodeCreateRequest(type, worldPos, name))
    }

    fun convertToScreenCoordinates(worldPos: Vector2f): Vector2f {
        return Vector2f(
            (worldPos.x * workspace.settings.zoom) + workspace.settings.scrolled.x,
            (worldPos.y * workspace.settings.zoom) + workspace.settings.scrolled.y
        )
    }

    fun convertToScreenSize(worldSize: Vector2f): Vector2f {
        return Vector2f(
            worldSize.x * workspace.settings.zoom, worldSize.y * workspace.settings.zoom
        )
    }

    fun convertToWorldCoordinates(screenPos: Vector2f): Vector2f {
        return Vector2f(
            (screenPos.x - workspace.settings.scrolled.x) / workspace.settings.zoom,
            (screenPos.y - workspace.settings.scrolled.y) / workspace.settings.zoom
        )
    }


    private fun deleteSelected() {
        for (nodeId in selectedNodes) {
//            workspace.removeNode(nodeId)

            //colects all the links that are connected to the node
            val links = workspace.graph.getLinks(nodeId)
            for (link in links) {
                client.send(LinkDeleteRequest(link.uid))
            }


            client.send(NodeDeleteRequest(nodeId))

            // Notify the server about node deletion
//            runtime.client.send(NodeDeleted(nodeId))
        }
        for (linkId in selectedLinks) {
//            workspace.removeLink(linkId)
            client.send(LinkDeleteRequest(linkId))
            // Notify the server about link deletion
//            runtime.client.send(LinkDeleted(linkId))
        }
        selectedNodes.clear()
        selectedLinks.clear()

        // Reset any state that might prevent adding new nodes
        isDraggingNode = false
        selectionStart = null
        selectionEnd = null
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
            workspace.removeNode(packet.uuid)
        } else if (packet is LinkCreated) {
            val link = packet.link
            workspace.addLink(link)
        } else if (packet is LinkDeleted) {
            workspace.removeLink(packet.uuid)
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

    fun tooltip(
        icon: String,
        text: String,
        iconFontSize: Int = 28,
        textFontSize: Int = 24,
        iconColor: Int = ImColor.rgba(33, 150, 243, 255)
    ) {
        val drawList = ImGui.getForegroundDrawList()
        val iconFont = fontawesomeFamily[iconFontSize]
        val textFont = headerFamily[textFontSize]

        // Draws a custom tool tip with an icon and text.
        val iconSize = iconFont.calcTextSizeA(iconFontSize.toFloat(), Float.MAX_VALUE, 0f, icon)
        val textSize = textFont.calcTextSizeA(textFontSize.toFloat(), Float.MAX_VALUE, 0f, text)
        val padding = 10f
        val tooltipWidth = iconSize.x + textSize.x + padding * 3
        val tooltipHeight = max(iconSize.y, textSize.y) + padding * 2

        val tooltipPos = Vector2f(ImGui.getMousePos().x + 16, ImGui.getMousePos().y - tooltipHeight / 2)
        val tooltipBounds = Vector4f(
            tooltipPos.x, tooltipPos.y, tooltipPos.x + tooltipWidth, tooltipPos.y + tooltipHeight
        )

        drawList.addRectFilled(
            tooltipBounds.x,
            tooltipBounds.y,
            tooltipBounds.z,
            tooltipBounds.w,
            ImColor.rgba(30, 30, 30, 255),
            40f,
        )

        drawList.addRect(
            tooltipBounds.x,
            tooltipBounds.y,
            tooltipBounds.z,
            tooltipBounds.w,
            ImColor.rgba(200, 200, 200, 255),
            40f,
        )
        val leftMargin = 3.33f
        //Draw circle around the icon
        drawList.addCircleFilled(
            tooltipBounds.x + padding + iconSize.x / 2 + leftMargin,
            tooltipBounds.y + padding + iconSize.y / 2,
            iconSize.x / 2 + padding / 1.75f + 1,
            iconColor
        )

        drawList.addText(
            iconFont,
            iconFontSize.toFloat() + 8,
            tooltipBounds.x + padding - 2f + leftMargin,
            tooltipBounds.y + padding - 8f,
            ImColor.rgba(255, 255, 255, 255),
            icon.toString()
        )

        drawList.addText(
            textFont,
            textFontSize.toFloat(),
            tooltipBounds.x + iconSize.x + padding * 2 + leftMargin,
            tooltipBounds.y + padding + 2,
            ImColor.rgba(255, 255, 255, 255),
            text
        )

        if (tooltipBounds.x < 0) {
            tooltipBounds.x = 0f
            tooltipBounds.z = tooltipWidth
        }

        if (tooltipBounds.z > ImGui.getMainViewport().size.x) {
            tooltipBounds.z = ImGui.getMainViewport().size.x
            tooltipBounds.x = tooltipBounds.z - tooltipWidth
        }

        if (tooltipBounds.y < 0) {
            tooltipBounds.y = 0f
            tooltipBounds.w = tooltipHeight
        }

        if (tooltipBounds.w > ImGui.getMainViewport().size.y) {
            tooltipBounds.w = ImGui.getMainViewport().size.y
            tooltipBounds.y = tooltipBounds.w - tooltipHeight
        }


    }


    fun start(node: Node, edge: Edge) {
        selectedEdge = Pair(node, edge)
        val edgeBounds = computeEdgeBounds(node, edge)
        dragStartPos = Vector2f(edgeBounds.x, edgeBounds.y)
        isLinking = true
    }

    //Centers the scrolled area around the majority of the nodes
    fun center() {
        //Moves the camera to origin
        workspace.settings.scrolled.x = 0f
        workspace.settings.scrolled.y = 0f
        workspace.settings.zoom = 1f
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
            val nodeBounds = computeNodeBounds(node)
            val edges = workspace.graph.getEdges(node)
            val inputEdges = edges.filter { it.direction == "input" }
            val outputEdges = edges.filter { it.direction == "output" }

            val edgeSpacing = 20f * zoom
            val edgeStartY = nodeBounds.y + 30f * zoom  // Start below the header

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
        if (sourceEdge.type != "exec" && targetEdge.type != "exec" && workspace.graph.getLinks()
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

    fun handleEdgeDragging(drawList: ImDrawList) {
        val draggedEdge = draggedEdge
        val dragStartPos = dragStartPos

        if (draggedEdge != null && dragStartPos != null) {
            val (sourceNode, sourceEdge) = draggedEdge
            val nodeBounds = computeNodeBounds(sourceNode)
            val sourcePos = getEdgePosition(sourceNode, sourceEdge, nodeBounds)
            val mousePos = ImGui.getMousePos()

            // Draw the dragging line
            drawList.addBezierCubic(
                sourcePos.x,
                sourcePos.y,
                sourcePos.x + 50f * zoom,
                sourcePos.y,
                mousePos.x - 50f * zoom,
                mousePos.y,
                mousePos.x,
                mousePos.y,
                ImColor.rgba(255, 255, 255, 200),
                2f * zoom,
                20
            )

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
        customActionMenu.openWithFilteredNodes(position, compatibleNodes)
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
        val edgeBounds = computeEdgeBounds(node, edge)
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

