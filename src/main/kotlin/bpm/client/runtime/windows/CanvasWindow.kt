package bpm.client.runtime.windows

import imgui.*
import imgui.flag.*
import imgui.type.ImString
import bpm.client.font.Fonts
import bpm.client.render.IRender
import bpm.client.runtime.ClientRuntime
import bpm.client.utils.toVec2f
import bpm.common.utils.FontAwesome
import bpm.client.utils.use
import bpm.common.network.Client
import bpm.common.network.Endpoint
import bpm.common.network.listener
import bpm.common.property.Property
import bpm.common.property.cast
import bpm.common.property.castOr
import bpm.common.schemas.Schemas
import bpm.common.utils.fmodf
import bpm.common.workspace.Workspace
import bpm.common.workspace.WorkspaceSettings
import bpm.common.workspace.graph.Edge
import bpm.common.workspace.graph.Link
import bpm.common.workspace.graph.Node
import bpm.common.workspace.packets.EdgePropertyUpdate
import org.joml.Vector2f
import org.joml.Vector4f
import org.joml.Vector4i
import java.util.*
import kotlin.math.pow

class CanvasWindow(private val runtime: ClientRuntime) : IRender {

    val workspace: Workspace get() = runtime.workspace ?: error("Workspace not set")


    private val headerFamily get() = Fonts.getFamily("Inter")["Bold"]
    private val headerFont get() = headerFamily[workspace.settings.fontHeaderSize]
    private val bodyFamily get() = Fonts.getFamily("Inter")["Regular"]
    private val bodyFont get() = bodyFamily[workspace.settings.fontSize]
    private val fontAwesomeFamily = Fonts.getFamily("Fa")["Regular"]
    private val fontAwesome get() = fontAwesomeFamily[workspace.settings.fontHeaderSize]

    private val selectionContextOverlay = SelectionContextOverlay(workspace)
    private val selectedNodes = mutableSetOf<UUID>()
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
     * A private constant representing the scrolled vector.
     *
     * This vector represents the amount of scrolling applied to the context.
     *
     * @return The scrolled vector.
     */
    private val scrolled: Vector2f get() = workspace.settings.scrolled

    /**
     * Represents the canvas context used in the application.
     *
     * @property canvasCtx The canvas context instance.
     */
    private val canvasCtx = Endpoint.installed<CanvasContext>()
    private var currentTime = 0f
    private val customActionMenu = canvasCtx.customActionMenu

    private var hoveredNode: UUID? = null
    private var hoveredLink: UUID? = null
    private var isPropertyWindowHovered = false
    private val arrowCount = 5
    private val execSegmentCount = 10
    private val execSegmentLength = 10f // Length of each segment in the exec link
    private val execGapRatio = 0.5f // Ratio of gap to segment length
    private var lastCanvasSize: Vector2f = Vector2f()
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

        // Update selectedNodes set
        selectedNodes.clear()
        selectedNodes.addAll(workspace.graph.nodes.filter { it.selected }.map { it.uid })

        val isActionMenuHovered = customActionMenu.isVisible() && customActionMenu.isHovered()
        val isVariablesMenuHovered = false

        setupCanvas()

        isPropertyWindowHovered = isActionMenuHovered || isVariablesMenuHovered

        if (!selectionContextOverlay.isHovered() && !isPropertyWindowHovered) handleCanvas()

        val drawList = ImGui.getWindowDrawList()
        renderBackground {
            drawGrid()
            renderLinks()
            renderNodes()
            canvasCtx.handleEdgeDragging(drawList)
            renderSelectionBox()
        }

        updateAnimationTime()
        canvasCtx.handleSelection(selectionContextOverlay.isHovered() || isVariablesMenuHovered)
        handleHover()
        handleContextMenu()
        // Update cursor based on hover state
        val mousePos = ImGui.getMousePos()
        canvasCtx.updateHoverState(Vector2f(mousePos.x, mousePos.y))
        ImGui.setMouseCursor(canvasCtx.getHoverCursor())

        renderButton(bounds.z - 50f, bounds.y + 20f, FontAwesome.Play, fontSize = 25f, width = 30f, height = 30f) {
            runtime.compile(workspace)
        }

        renderButton(bounds.z - 50f, bounds.y + 60f, FontAwesome.Reply, fontSize = 25f, width = 30f, height = 30f) {
            runtime.reloadNodeLibrary()
        }

        renderButton(
            bounds.z - 50f,
            bounds.y + 100f,
            FontAwesome.AlignCenter,
            fontSize = 25f,
            width = 30f,
            height = 30f
        ) {
            canvasCtx.center()
        }

        // Render the selection context overlay
//        selectionContextOverlay.render(selectedNodes)

        // Render the variables menu
        canvasCtx.variablesMenu.render(drawList)
        canvasCtx.variablesMenu.update()

        // Render the custom action menu
        customActionMenu.render(drawList)
        canvasCtx.notifications()
        ImGui.end()
    }
    //True for the first 1.5 seconds the canvas is open

    private val initialOpen get() = System.currentTimeMillis() - openTime < 100
    private var openTime = System.currentTimeMillis()

    fun close() {
        savedSettings[workspace.uid] = workspace.settings
        this.scrolled
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
            val selectedNodes = findSelectedNodesUnderMouse(mousePos)
            val selectedLinks = findSelectedLinksUnderMouse(mousePos)

            if (selectedNodes.isNotEmpty() || selectedLinks.isNotEmpty()) {
                customActionMenu.open(mousePos, selectedNodes, selectedLinks)
            } else {
                customActionMenu.open(mousePos)
            }
            canvasCtx.variablesMenu.closePopup()
        }
    }


    private fun findSelectedLinksUnderMouse(mousePos: ImVec2): Set<Link> {
        return workspace.graph.getLinks().filter { link ->
            canvasCtx.isLinkSelected(link) || canvasCtx.isMouseOverLink(link, mousePos.toVec2f)
        }.toSet()
    }

    private fun findSelectedNodesUnderMouse(mousePos: ImVec2): Set<Node> {
        val nodesUnderMouse = workspace.graph.nodes.filter { node ->
            val bounds = canvasCtx.computeNodeBounds(node)
            mousePos.x in bounds.x..bounds.z && mousePos.y in bounds.y..bounds.w
        }.toSet()

        return if (nodesUnderMouse.isNotEmpty()) {
            nodesUnderMouse
        } else {
            workspace.graph.nodes.filter { it.selected }.toSet()
        }
    }


    private inline fun renderButton(
        x: Float,
        y: Float,
        icon: String,
        fontSize: Float = 20f,
        width: Float = 30f,
        height: Float = 30f,
        padding: Float = 5f,
        color: Int = ImColor.rgba(50, 50, 50, 255),
        hoverColor: Int = ImColor.rgba(60, 60, 60, 255),
        crossinline onClick: () -> Unit = {}
    ) {
        val drawList = ImGui.getWindowDrawList()
        val isHovered = ImGui.isMouseHoveringRect(x, y, x + width, y + height)
        val buttonColor = if (isHovered) hoverColor else color
        drawList.addRectFilled(
            x - padding / 2, y - padding / 2, x + width + padding / 2, y + height + padding / 2, buttonColor, 5f
        )
        drawList.addText(
            fontAwesome, fontSize, x + width / 2 - 4f, y, ImColor.rgba(255, 255, 255, 255), icon
        )
        if (isHovered && ImGui.isMouseClicked(0)) {
            onClick()
        }
    }

    private fun renderNodes() {
        val drawList = ImGui.getWindowDrawList()
        for (node in workspace.graph.nodes) {
            val nodeBounds = canvasCtx.computeNodeBounds(node)
            val headerBounds = canvasCtx.computeHeaderBounds(node)
            val rawColor: Vector4i = node.color
            val nodeColor = ImColor.rgba(rawColor.x, rawColor.y, rawColor.z, rawColor.w)
            renderNodeBody(drawList, node, nodeBounds, nodeColor)
            renderNodeHeader(drawList, node, headerBounds, nodeBounds)
            renderEdges(drawList, node, nodeBounds)

            canvasCtx.handleNode(node, nodeBounds, headerBounds)
        }

        renderToolTips()

        if (canvasCtx.isDraggingNode) {
        }
    }


    private fun renderToolTips() {

        if (canvasCtx.isDraggingNode) return
        // Show tooltip for hovered node
        if (canvasCtx.hoveredTitleBar != null) {
            val node = workspace.getNode(canvasCtx.hoveredTitleBar!!) ?: return
            val nodeType = listener<Schemas>(Endpoint.Side.CLIENT).library.get("${node.type}/${node.name}") ?: return
            val description: Property<String> = nodeType["description"].cast()
            val color = node.color
            canvasCtx.tooltip(
                node.icon.toChar().toString(),
                description.get()
            )
        }

        if (canvasCtx.hoveredPin != null) {
            val edge = canvasCtx.hoveredPin!!.second
            val description = edge.description
            val color = getEdgeColor(edge.type)
            canvasCtx.tooltip(FontAwesome.Info, description, color)
        }
    }

    private fun renderNodeBody(drawList: ImDrawList, node: Node, bounds: Vector4f, color: Int) {
        // Adjust bounds to account for edge offset
        val edgeOffset = 10f * canvasCtx.zoom
        val adjustedBounds = Vector4f(
            bounds.x, bounds.y, bounds.z, bounds.w
        )

        // Main body
        drawList.addRectFilled(
            adjustedBounds.x, adjustedBounds.y, adjustedBounds.z, adjustedBounds.w, color, 10f * canvasCtx.zoom
        )

        // Hover effect
        if (hoveredNode == node.uid || canvasCtx.isNodeInSelectionBox(node)) {
            drawList.addRect(
                adjustedBounds.x - 1f * canvasCtx.zoom,
                adjustedBounds.y - 1f * canvasCtx.zoom,
                adjustedBounds.z + 1f * canvasCtx.zoom,
                adjustedBounds.w + 1f * canvasCtx.zoom,
                ImColor.rgba(69, 163, 230, 185),
                10f * canvasCtx.zoom,
                ImDrawFlags.None,
                2f * canvasCtx.zoom
            )
        }

        // Selection outline
        if (canvasCtx.isNodeSelected(node)) {
            selectedNodes.add(node.uid)
            drawList.addRect(
                adjustedBounds.x - 1.5f * canvasCtx.zoom,
                adjustedBounds.y - 1.5f * canvasCtx.zoom,
                adjustedBounds.z + 1.5f * canvasCtx.zoom,
                adjustedBounds.w + 1.5f * canvasCtx.zoom,
                ImColor.rgba(255, 255, 255, 255),
                10f * canvasCtx.zoom,
                ImDrawFlags.None,
                2f * canvasCtx.zoom
            )
        } else {
            selectedNodes.remove(node.uid)
        }
    }


    private fun renderShadowedText(
        drawList: ImDrawList,
        font: ImFont,
        size: Float,
        x: Float,
        y: Float,
        color: Int,
        text: String,
        scale: Float = 1.05f,
        offsetX: Float = 1f,
        offsetY: Float = 1f

    ) {
        // Draw shadow
        drawList.addText(font, size * scale, x + offsetX, y + offsetY, ImColor.rgba(50, 50, 50, 200), text)
        // Draw text
        drawList.addText(font, size, x, y, color, text)
    }


    private fun renderNodeHeader(drawList: ImDrawList, node: Node, titleBounds: Vector4f, nodeBounds: Vector4f) {
        val paddingX = 12f * canvasCtx.zoom
        val paddingY = 5f * canvasCtx.zoom

        val icon = node.icon.toChar().toString()
        val iconSize = 24f * canvasCtx.zoom
        val maxX = nodeBounds.z // Use node bounds for max width

        // Header background
        drawList.addRectFilled(
            nodeBounds.x - paddingX / 2f,
            titleBounds.y - paddingY / 2f,
            maxX + paddingX / 2f,
            titleBounds.w + paddingY / 2f,
            ImColor.rgba(70, 70, 70, 255),
            10f * canvasCtx.zoom
        )

        // Header border
        drawList.addRect(
            nodeBounds.x - paddingX / 2f,
            titleBounds.y - paddingY / 2f,
            maxX + paddingX / 2f,
            titleBounds.w + paddingY / 2f,
            ImColor.rgba(0, 0, 0, 255),
            10f * canvasCtx.zoom,
            ImDrawFlags.None,
            2f * canvasCtx.zoom
        )

        // Render icon
        renderShadowedText(
            drawList,
            fontAwesome,
            iconSize,
            nodeBounds.x + paddingX / 2f - 4 * canvasCtx.zoom,
            nodeBounds.y - 15f * canvasCtx.zoom,
            ImColor.rgba(255, 255, 255, 255),
            icon,
            scale = 1f,
            offsetY = 2.5f * canvasCtx.zoom,
            offsetX = 2.5f * canvasCtx.zoom
        )

        // Icon separator line
        drawList.addLine(
            nodeBounds.x + iconSize - 5 * canvasCtx.zoom,
            titleBounds.y - paddingY / 2f,
            nodeBounds.x + iconSize - 5 * canvasCtx.zoom,
            titleBounds.w + paddingY / 2f,
            ImColor.rgba(0, 0, 0, 255),
            2f * canvasCtx.zoom
        )

        // Calculate available width for text
        val availableWidth = maxX - (nodeBounds.x + iconSize + 2f * canvasCtx.zoom)
        val fullText = node.name
        var displayText = fullText

        // Check if header is hovered
        val mousePos = ImGui.getMousePos()
        if (mousePos.x >= nodeBounds.x && mousePos.x <= maxX && mousePos.y >= titleBounds.y && mousePos.y <= titleBounds.w) {
            // Show full text on hover if it was truncated
            if (displayText != fullText) {
                ImGui.beginTooltip()
                ImGui.text(fullText)
                ImGui.endTooltip()
            }

            // Highlight effect on hover
            drawList.addRectFilled(
                nodeBounds.x - paddingX / 2f,
                titleBounds.y - paddingY / 2f,
                maxX + paddingX / 2f,
                titleBounds.w + paddingY / 2f,
                ImColor.rgba(100, 100, 100, 150),
                10f * canvasCtx.zoom
            )
        }

        // Render node name
        headerFont.use {
            val textSize = ImGui.calcTextSize(fullText)

            if (textSize.x > availableWidth) {
                // Truncate text if it's too long
                displayText = truncateText(fullText, availableWidth)
            }

            val textX = nodeBounds.x + iconSize + 2f * canvasCtx.zoom
            val textY = titleBounds.y - 1 * canvasCtx.zoom

            // Render truncated or full text
            renderShadowedText(
                drawList,
                headerFont,
                workspace.settings.fontHeaderSize.toFloat() * 1f,
                textX,
                textY,
                ImColor.rgba(255, 255, 255, 255),
                displayText,
                scale = 1f,
                offsetX = 3f * canvasCtx.zoom,
                offsetY = 3f * canvasCtx.zoom
            )
        }


    }

    private fun truncateText(text: String, availableWidth: Float): String {
        val ellipsis = "..."
        var truncated = text
        while (ImGui.calcTextSize(truncated + ellipsis).x > availableWidth && truncated.length > 1) {
            truncated = truncated.dropLast(1)
        }
        return truncated + ellipsis
    }


    /*private fun renderEdges(drawList: ImDrawList, node: Node) {
        val edges = workspace.graph.getEdges(node)
        if (edges.isEmpty()) return

        val nodeBounds = canvasCtx.computeNodeBounds(node)

        for (edge in edges) {
            val bounds = canvasCtx.canvasCtx.computeEdgeBounds(node, edge)
            val xPos = bounds.x
            val yPos = bounds.y

            // Determine edge color based on type
            val color = when (edge.type) {
                "exec" -> ImColor.rgba(255, 255, 255, 255)
                "number" -> ImColor.rgba(100, 255, 100, 255)
                "string" -> ImColor.rgba(255, 100, 100, 255)
                "boolean" -> ImColor.rgba(255, 255, 100, 255)
                else -> ImColor.rgba(200, 200, 200, 255)
            }

            // Render edge shape based on type
            when (edge.type) {
                "exec" -> {
                    // Square shape for exec type
                    val size = 8f * canvasCtx.zoom
                    drawList.addRectFilled(
                        xPos - size/2, yPos - size/2,
                        xPos + size/2, yPos + size/2,
                        color
                    )
                }
                else -> {
                    // Circle shape for other types
                    drawList.addCircleFilled(xPos, yPos, 4f * canvasCtx.zoom, color)
                }
            }

            // Render edge label
            headerFont.use {
                val labelSizes = ImGui.calcTextSize(edge.name)
                val sizeX = labelSizes.x * 0.8f
                val sizeY = labelSizes.y * 0.8f
                val labelX = if (edge.direction == "input") xPos + 14f * canvasCtx.zoom else xPos - sizeX - 14f * canvasCtx.zoom
                val labelY = yPos - sizeY / 2

                // Add background for better readability
                drawList.addRectFilled(
                    labelX - 2f * canvasCtx.zoom,
                    labelY - 2f * canvasCtx.zoom,
                    labelX + sizeX + 2f * canvasCtx.zoom,
                    labelY + sizeY + 2f * canvasCtx.zoom,
                    ImColor.rgba(40, 40, 40, 200)
                )

                drawList.addText(
                    headerFont,
                    workspace.settings.fontHeaderSize.toFloat() * 0.8f,
                    labelX,
                    labelY,
                    ImColor.rgba(220, 220, 220, 255),
                    edge.name
                )
            }

            // Handle hover and selection
            val mousePos = ImGui.getMousePos()
            if (isPointOverEdge(Vector2f(mousePos.x, mousePos.y), bounds)) {
                // Highlight on hover
                drawList.addCircle(xPos, yPos, 6f * canvasCtx.zoom, ImColor.rgba(255, 255, 255, 200), 12, 2f * canvasCtx.zoom)

                // Show tooltip with edge description
                ImGui.beginTooltip()
                ImGui.text("${edge.name}: ${edge.description}")
                ImGui.endTooltip()

                // Handle edge click
                if (ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
                    canvasCtx.handleEdgeClick(node, edge)
                    ImGui.setMouseCursor(ImGuiMouseCursor.ResizeAll)  // Change cursor to indicate dragging
                }
            }

            // Render connected indicator
            if (workspace.graph.getLinks().any { it.from == edge.uid || it.to == edge.uid }) {
                drawList.addCircleFilled(xPos, yPos, 3f * canvasCtx.zoom, ImColor.rgba(255, 255, 255, 255))
            }
        }
    }*/

//    private fun renderEdges(drawList: ImDrawList, node: Node, nodeBounds: Vector4f) {
//        val edges = workspace.graph.getEdges(node)
//        val inputEdges = edges.filter { it.direction == "input" }
//        val outputEdges = edges.filter { it.direction == "output" }
//
//        // Render input edges
//        inputEdges.forEach { edge ->
//            renderEdge(drawList, node, edge, nodeBounds)
//        }
//
//        // Render output edges
//        outputEdges.forEach { edge ->
//            renderEdge(drawList, node, edge, nodeBounds)
//        }
//    }


    private fun renderEdges(drawList: ImDrawList, node: Node, nodeBounds: Vector4f) {
        val edges = workspace.graph.getEdges(node)
        val inputEdges = edges.filter { it.direction == "input" }
        val outputEdges = edges.filter { it.direction == "output" }

        val edgeSpacing = 20f * canvasCtx.zoom
        val edgeStartY = nodeBounds.y + 30f * canvasCtx.zoom  // Start below the header

        // Render input edges
        inputEdges.forEachIndexed { index, edge ->
            val yPos = edgeStartY + index * edgeSpacing
            renderEdge(drawList, node, edge, nodeBounds, Vector2f(nodeBounds.x - 10f * canvasCtx.zoom, yPos), true)
        }

        // Render output edges
        outputEdges.forEachIndexed { index, edge ->
            val yPos = edgeStartY + index * edgeSpacing
            renderEdge(drawList, node, edge, nodeBounds, Vector2f(nodeBounds.z + 10f * canvasCtx.zoom, yPos), false)
        }
    }

    private fun getEdgeColor(type: String): Int {
        // Compute hash of the input string
        val hash = type.hashCode()

        // Use the hash to generate RGB values
        val r = (hash and 0xFF0000) shr 16
        val g = (hash and 0x00FF00) shr 8
        val b = hash and 0x0000FF

        // Ensure the color is not too dark by setting a minimum brightness
        val minBrightness = 100
        val br = maxOf(r, minBrightness)
        val bg = maxOf(g, minBrightness)
        val bb = maxOf(b, minBrightness)

        // Return the color as an RGBA value
        return ImColor.rgba(br, bg, bb, 255)
    }

    private fun renderEdge(
        drawList: ImDrawList, node: Node, edge: Edge, nodeBounds: Vector4f, pos: Vector2f, isInput: Boolean
    ) {
        val color = getEdgeColor(edge.type)
        val edgeRadius = 4f * canvasCtx.zoom
        val isConnected = workspace.graph.getLinks().any { it.from == edge.uid || it.to == edge.uid }

        if (edge.type == "exec" && edge.name == "exec_in" || edge.name == "exec_out" || edge.name == "exec") {


            //render the triangle
            val triangleSize = 8f * canvasCtx.zoom
            val triangleX = if (edge.direction == "input") pos.x + 18f * canvasCtx.zoom else pos.x - triangleSize - 18f * canvasCtx.zoom
            val triangleY = pos.y - triangleSize / 2
            val triangleBounds = Vector4f(triangleX, triangleY, triangleX + triangleSize, triangleY + triangleSize)
            val triangleColor = ImColor.rgba(255, 255, 255, 255)
            drawList.addTriangleFilled(
                triangleBounds.x,
                triangleBounds.y,
                triangleBounds.z,
                triangleBounds.y + triangleSize / 2,
                triangleBounds.x,
                triangleBounds.w,
                triangleColor
            )
        }


        // Draw circle for other types
        if (isConnected) {
            drawList.addCircleFilled(pos.x, pos.y, edgeRadius, color)
        } else {
            drawList.addCircle(pos.x, pos.y, edgeRadius, color, 12, 1.5f * canvasCtx.zoom)
        }

        // Render edge label and input field for non-exec types
        if (edge.type == "exec" && (edge.name == "exec_in" || edge.name == "exec_out" || edge.name == "exec")) {
            // Do nothing
        } else {
            bodyFont.use {
                val labelWidth = ImGui.calcTextSize(edge.name).x
                val inputWidth = 60f * canvasCtx.zoom // Adjust this value as needed
                val spacing = 5f * canvasCtx.zoom

                val totalWidth = labelWidth + inputWidth + spacing
                val startX = if (isInput) pos.x + edgeRadius * 2 + spacing else pos.x - totalWidth - edgeRadius * 2 - spacing

                val labelX = if (isInput) startX else startX + inputWidth + spacing
                val inputX = if (isInput) startX + labelWidth + spacing else startX

                val textY = pos.y - ImGui.getTextLineHeight() / 2
                val labelPadding = 1f * canvasCtx.zoom
//                     Render text container
                if (edge.direction == "input")
                    drawList.addRectFilled(
                        startX - labelPadding,
                        textY - labelPadding,
                        startX + labelWidth + labelPadding,
                        textY + ImGui.getTextLineHeight() + labelPadding,
                        ImColor.rgba(40, 40, 40, 200),
                        2f * canvasCtx.zoom
                    )
                else
                    drawList.addRectFilled(
                        startX + inputWidth + spacing - labelPadding,
                        textY - labelPadding,
                        startX + totalWidth + labelPadding,
                        textY + ImGui.getTextLineHeight() + labelPadding,
                        ImColor.rgba(40, 40, 40, 200),
                        2f * canvasCtx.zoom
                    )

                // Render edge label
                drawList.addText(
                    bodyFont,
                    workspace.settings.fontSize.toFloat(),
                    labelX,
                    textY,
                    ImColor.rgba(220, 220, 220, 255),
                    edge.name.replace("_", " ")
                )


                // Render input field only if the edge is not connected
                if (!isConnected) {
                    renderEdgeInput(drawList, edge, inputX, textY, inputWidth * 0.66f)
                }
            }
        }

        // Handle edge interaction
//        val mousePos = ImGui.getMousePos()
//        if (canvasCtx.isPointOverEdge(Vector2f(mousePos.x, mousePos.y), pos)) {
//            drawList.addCircle(
//                pos.x,
//                pos.y,
//                edgeRadius + 2f * canvasCtx.zoom,
//                ImColor.rgba(255, 255, 255, 200),
//                12,
//                1.5f * canvasCtx.zoom
//            )
//
//            if (ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
//                canvasCtx.handleEdgeClick(node, edge)
//            }
//        }

        val mousePos = ImGui.getMousePos()
        if (canvasCtx.isPointOverEdge(Vector2f(mousePos.x, mousePos.y), pos)) {
            drawList.addCircle(
                pos.x,
                pos.y,
                edgeRadius + 2f * canvasCtx.zoom,
                ImColor.rgba(255, 255, 255, 200),
                12,
                1.5f * canvasCtx.zoom
            )

            if (ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
                canvasCtx.startEdgeDrag(node, edge)
            }
        }
    }

    private fun renderEdgeInput(drawList: ImDrawList, edge: Edge, x: Float, y: Float, width: Float) {
        val value = edge.value
        if (value.isEmpty) return
        val type = value["type"]?.cast<Property.String>()?.get() ?: "float"
        val currentValue = value["default"] ?: Property.Float(0f)

        // Set the ImGui cursor position
        ImGui.setCursorScreenPos(x, y)
        // Push ID to avoid conflicts
        ImGui.pushID(edge.uid.toString())
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0f, 0f)
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 0f, 2f * canvasCtx.zoom)
        ImGui.pushStyleVar(ImGuiStyleVar.CellPadding, 0f, 0f)
        bodyFamily[(12 * canvasCtx.zoom).toInt()].use {

            when (type) {
                "string", "text" -> {
                    val stringValue = currentValue.castOr { Property.String("") }
                    val buffer = ImString(256)
                    buffer.set(stringValue.get())

                    ImGui.pushItemWidth(width)
                    if (ImGui.inputText("##value", buffer)) {
                        val newValue = buffer.get()
                        edge.properties["value"] = edge.value.apply {
                            this["default"] = Property.String(newValue)
                            Client { it.send(EdgePropertyUpdate(edge.uid, this)) }
                        }
                    }
                    ImGui.popItemWidth()
                }

                "float" -> {
                    val min = value["min"]?.castOr { Property.Float(Float.MIN_VALUE) }?.get() ?: Float.MIN_VALUE
                    val max = value["max"]?.castOr { Property.Float(Float.MAX_VALUE) }?.get() ?: Float.MAX_VALUE
                    val floatValue = currentValue.castOr { Property.Float(0f) }
                    val buffer = floatArrayOf(floatValue.get())

                    ImGui.pushItemWidth(width)
                    if (ImGui.dragFloat("##value", buffer, 0.1f, min, max)) {
                        edge.properties["value"] = edge.value.apply {
                            this["default"] = Property.Float(buffer[0])
                            Client {
                                it.send(EdgePropertyUpdate(edge.uid, this))
                            }
                        }
                    }
                    ImGui.popItemWidth()
                }

                "int" -> {
                    val min = value["min"]?.castOr { Property.Int(Int.MIN_VALUE) }?.get() ?: Int.MIN_VALUE
                    val max = value["max"]?.castOr { Property.Int(Int.MAX_VALUE) }?.get() ?: Int.MAX_VALUE
                    val intValue = currentValue.castOr { Property.Int(0) }
                    val buffer = intArrayOf(intValue.get())

                    ImGui.pushItemWidth(width)
                    if (ImGui.dragInt("##value", buffer, 0.1f, min.toFloat(), max.toFloat())) {
                        edge.properties["value"] = edge.value.apply {
                            this["default"] = Property.Int(buffer[0])
                            Client { it.send(EdgePropertyUpdate(edge.uid, this)) }
                        }
                    }
                    ImGui.popItemWidth()
                }

                "boolean" -> {
                    val boolValue = currentValue.castOr { Property.Boolean(false) }
                    renderCheckboxProperty(
                        drawList,
                        boolValue,
                        x,
                        y + 2 * canvasCtx.zoom,
                        width,
                        ImGui.getTextLineHeight(),
                        "##value"
                    ) {
                        edge.properties["value"] = edge.value.apply {
                            this["default"] = boolValue
                            Client { it.send(EdgePropertyUpdate(edge.uid, this)) }
                        }
                    }
                }
            }
        }

        ImGui.popID()
        ImGui.popStyleVar(3)
        // Make the input focusable and clickable
        if (ImGui.isItemHovered() && ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
            ImGui.setKeyboardFocusHere(-1)
        }
    }

    private fun renderCheckboxProperty(
        drawList: ImDrawList,
        property: Property.Boolean,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String,
        onClick: () -> Unit = {}
    ) {
        //Renders a custom checkbox making use of custom fonts
        val checkSize = 12f * canvasCtx.zoom
        val checkPadding = 2f * canvasCtx.zoom
        val checkX = x + checkPadding
        val checkY = y + (height - checkSize) / 2
        val checkBounds = Vector4f(checkX, checkY, checkX + checkSize, checkY + checkSize)

        //Renders the checkbox
        drawList.addRectFilled(
            checkBounds.x,
            checkBounds.y,
            checkBounds.z,
            checkBounds.w,
            ImColor.rgba(40, 40, 40, 200)
        )
        if (property.get()) {
            drawList.addRectFilled(
                checkBounds.x + 2,
                checkBounds.y + 2,
                checkBounds.z - 2,
                checkBounds.w - 2,
                ImColor.rgba(255, 255, 255, 255)
            )
        }


        //Handles the interaction with the checkbox
        val mousePos = ImGui.getMousePos()
        if (isPointOverRect(Vector2f(mousePos.x, mousePos.y), checkBounds)) {
            ImGui.setMouseCursor(ImGuiMouseCursor.Arrow)
            drawList.addRectFilled(
                checkBounds.x,
                checkBounds.y,
                checkBounds.z,
                checkBounds.w,
                ImColor.rgba(100, 100, 100, 200)
            )
            if (ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
                property.set(!property.get())
                onClick()
            }
        }

    }

    private fun isPointOverRect(point: Vector2f, rect: Vector4f): Boolean {
        return point.x >= rect.x && point.x <= rect.z && point.y >= rect.y && point.y <= rect.w
    }


    private fun renderEdge(drawList: ImDrawList, node: Node, edge: Edge, nodeBounds: Vector4f) {
        val bounds = canvasCtx.computeEdgeBounds(node, edge)
        val xPos = bounds.x
        val yPos = bounds.y
        val color = getEdgeColor(edge.type)

        // Render edge shape
        if (edge.type == "exec") {
            val size = 8f * canvasCtx.zoom
            drawList.addRect(xPos - size / 2, yPos - size / 2, xPos + size / 2, yPos + size / 2, color)
        } else {
            drawList.addCircleFilled(xPos, yPos, 5f * canvasCtx.zoom, color, 12)
        }

        val edgeSize = 8f * canvasCtx.zoom
        val halfEdgeSize = edgeSize / 2


        if (edge.name == "exec_in" || edge.name == "exec_out" || edge.name == "exec") {
            //render the triangle
            val triangleSize = 8f * canvasCtx.zoom
            val triangleX = if (edge.direction == "input") xPos + 18f * canvasCtx.zoom else xPos - triangleSize - 18f * canvasCtx.zoom
            val triangleY = yPos - triangleSize / 2
            val triangleBounds = Vector4f(triangleX, triangleY, triangleX + triangleSize, triangleY + triangleSize)
            val triangleColor = ImColor.rgba(255, 255, 255, 255)
            drawList.addTriangleFilled(
                triangleBounds.x,
                triangleBounds.y,
                triangleBounds.z,
                triangleBounds.y + triangleSize / 2,
                triangleBounds.x,
                triangleBounds.w,
                triangleColor
            )
        } else {

            // Render edge label
            bodyFont.use {
                val textSize = ImGui.calcTextSize(edge.name)
                val textX = if (edge.direction == "input") xPos + 16f * canvasCtx.zoom else xPos - textSize.x - 16f * canvasCtx.zoom
                val textY = yPos - 8f * canvasCtx.zoom
                val textBounds = Vector4f(textX, textY, textX + textSize.x, textY + textSize.y)

                //draws a background for the text
                drawList.addRectFilled(
                    textBounds.x - 1 * canvasCtx.zoom,
                    textBounds.y,
                    textBounds.z + 1 * canvasCtx.zoom,
                    textBounds.w,
                    ImColor.rgba(40, 40, 40, 200),
                    2f * canvasCtx.zoom
                )

                drawList.addText(
                    bodyFont,
                    workspace.settings.fontSize.toFloat(),
                    textX,
                    textY,
                    ImColor.rgba(220, 220, 220, 255),
                    edge.name
                )

            }
        }


        if (canvasCtx.isEdgeSelected(edge)) {
            drawList.addCircle(
                xPos, yPos, 7f * canvasCtx.zoom, ImColor.rgba(255, 255, 255, 200), 12, 2f * canvasCtx.zoom
            )
        }

        // Handle edge interaction
        val mousePos = ImGui.getMousePos()
        if (canvasCtx.isPointOverEdge(Vector2f(mousePos.x, mousePos.y), bounds)) {
            drawList.addCircle(
                xPos, yPos, 7f * canvasCtx.zoom, ImColor.rgba(255, 255, 255, 200), 12, 2f * canvasCtx.zoom
            )

            if (ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
                canvasCtx.handleEdgeClick(node, edge)
            }
        }
    }


    private fun renderLinks() {
        val drawList = ImGui.getWindowDrawList()
        for (link in workspace.graph.getLinks()) {
            val sourceEdge = workspace.graph.getEdge(link.from) ?: continue
            val sourceNode = workspace.getNode(sourceEdge.owner) ?: continue
            val targetEdge = workspace.graph.getEdge(link.to) ?: continue
            val targetNode = workspace.getNode(targetEdge.owner) ?: continue

            val sourceBounds = canvasCtx.computeNodeBounds(sourceNode)
            val targetBounds = canvasCtx.computeNodeBounds(targetNode)

            val sourcePos = canvasCtx.getEdgePosition(sourceNode, sourceEdge, sourceBounds)
            val targetPos = canvasCtx.getEdgePosition(targetNode, targetEdge, targetBounds)

            val sourceColor = ImColor.rgba(sourceNode.color.x, sourceNode.color.y, sourceNode.color.z, 255)
            val targetColor = ImColor.rgba(targetNode.color.x, targetNode.color.y, targetNode.color.z, 255)

            if (sourceEdge.type == "exec" || targetEdge.type == "exec") {
                drawExecLink(drawList, sourcePos, targetPos, sourceColor, targetColor)
            } else {
                drawDataLink(drawList, sourcePos, targetPos, sourceColor, targetColor)
            }

            // Add visual indication for selected links
            if (canvasCtx.isLinkSelected(link)) {
                val midX = (sourcePos.x + targetPos.x) / 2
                val controlPoint1 = Vector2f(midX, sourcePos.y)
                val controlPoint2 = Vector2f(midX, targetPos.y)
                drawList.addBezierCubic(
                    sourcePos.x, sourcePos.y,
                    controlPoint1.x, controlPoint1.y,
                    controlPoint2.x, controlPoint2.y,
                    targetPos.x, targetPos.y,
                    ImColor.rgba(255, 255, 0, 255), // Yellow highlight for selected links
                    4f * canvasCtx.zoom, // Thicker line for selected links
                    50
                )
            }
        }
    }

    private fun drawExecLink(
        drawList: ImDrawList, startPos: Vector2f, endPos: Vector2f, startColor: Int, endColor: Int
    ) {
        val midX = (startPos.x + endPos.x) / 2
        val controlPoint1 = Vector2f(midX, startPos.y)
        val controlPoint2 = Vector2f(midX, endPos.y)

        // Draw the main bezier curve
        drawList.addBezierCubic(
            startPos.x,
            startPos.y,
            controlPoint1.x,
            controlPoint1.y,
            controlPoint2.x,
            controlPoint2.y,
            endPos.x,
            endPos.y,
            lerpColor(startColor, endColor, 0.5f),
            2f * canvasCtx.zoom,
            50
        )

        // Draw animated triangles
        for (i in 0 until arrowCount) {
            val t = ((currentTime * 0.5f + i.toFloat() / arrowCount) % 1.0f)
            val arrowPos = getBezierPoint(startPos, controlPoint1, controlPoint2, endPos, t)
            val arrowTangent = getBezierTangent(startPos, controlPoint1, controlPoint2, endPos, t)

            val arrowSize = (6f + 4f * kotlin.math.sin(t * kotlin.math.PI)).coerceIn(4.0, 10.0) * canvasCtx.zoom
            val arrowColor = lerpColor(startColor, endColor, t)

            drawArrow(drawList, arrowPos, arrowTangent, arrowSize.toFloat(), arrowColor)
        }
    }


    private fun drawDataLink(
        drawList: ImDrawList,
        startPos: Vector2f,
        endPos: Vector2f,
        startColor: Int,
        endColor: Int
    ) {
        val midX = (startPos.x + endPos.x) / 2
        val controlPoint1 = Vector2f(midX, startPos.y)
        val controlPoint2 = Vector2f(midX, endPos.y)

        val totalLength = getBezierLength(startPos, controlPoint1, controlPoint2, endPos)
        val scaledSegmentLength = execSegmentLength * canvasCtx.zoom
        val scaledGapLength = scaledSegmentLength * execGapRatio
        val fullSegmentLength = scaledSegmentLength + scaledGapLength

        val segmentCount = (totalLength / fullSegmentLength).toInt()

        var t = 0f
        var lastPoint = startPos
        var isDrawing = true

        for (i in 0..segmentCount) {
            val segmentEndT = (i + 1) * fullSegmentLength / totalLength
            while (t < segmentEndT && t <= 1f) {
                val point = getBezierPoint(startPos, controlPoint1, controlPoint2, endPos, t)
                val color = lerpColor(startColor, endColor, t)

                if (isDrawing) {
                    drawList.addLine(lastPoint.x, lastPoint.y, point.x, point.y, color, 2f * canvasCtx.zoom)
                }

                lastPoint = point
                t += 0.01f // Small step for smooth curve following

                if (lastPoint.distance(startPos) % fullSegmentLength < scaledSegmentLength) {
                    isDrawing = true
                } else {
                    isDrawing = false
                }
            }
        }
    }


    private fun drawArrow(drawList: ImDrawList, pos: Vector2f, tangent: Vector2f, size: Float, color: Int) {
        val normal = Vector2f(-tangent.y, tangent.x).normalize()
        val p1 = Vector2f(
            pos.x - tangent.x * size + normal.x * size * 0.5f, pos.y - tangent.y * size + normal.y * size * 0.5f
        )
        val p2 = Vector2f(
            pos.x - tangent.x * size - normal.x * size * 0.5f, pos.y - tangent.y * size - normal.y * size * 0.5f
        )

        drawList.addTriangleFilled(pos.x, pos.y, p1.x, p1.y, p2.x, p2.y, color)
    }


    private fun getBezierTangent(p0: Vector2f, p1: Vector2f, p2: Vector2f, p3: Vector2f, t: Float): Vector2f {
        val u = 1 - t
        val uu = u * u
        val tt = t * t

        return Vector2f(
            3 * uu * (p1.x - p0.x) + 6 * u * t * (p2.x - p1.x) + 3 * tt * (p3.x - p2.x),
            3 * uu * (p1.y - p0.y) + 6 * u * t * (p2.y - p1.y) + 3 * tt * (p3.y - p2.y)
        ).normalize()
    }

    private fun getBezierLength(p0: Vector2f, p1: Vector2f, p2: Vector2f, p3: Vector2f, steps: Int = 100): Float {
        var length = 0f
        var lastPoint = p0

        for (i in 1..steps) {
            val t = i.toFloat() / steps
            val point = getBezierPoint(p0, p1, p2, p3, t)
            length += lastPoint.distance(point)
            lastPoint = point
        }

        return length
    }

    private fun lerpColor(startColor: Int, endColor: Int, t: Float): Int {
        val startR = (startColor shr 16) and 0xFF
        val startG = (startColor shr 8) and 0xFF
        val startB = startColor and 0xFF

        val endR = (endColor shr 16) and 0xFF
        val endG = (endColor shr 8) and 0xFF
        val endB = endColor and 0xFF

        val r = (startR + (endR - startR) * t).toInt().coerceIn(0, 255)
        val g = (startG + (endG - startG) * t).toInt().coerceIn(0, 255)
        val b = (startB + (endB - startB) * t).toInt().coerceIn(0, 255)

        return ImColor.rgba(r, g, b, 255)
    }


    private fun renderSelectionBox() {
        val selection = canvasCtx.getSelection()
        val start = selection?.first ?: return
        val end = selection.second ?: return
        val drawList = ImGui.getWindowDrawList()
        drawList.addRect(
            start.x, start.y, end.x, end.y, ImColor.rgba(100, 100, 255, 100), 0f, ImDrawFlags.None, 2f
        )
        drawList.addRectFilled(
            start.x, start.y, end.x, end.y, ImColor.rgba(100, 100, 255, 30)
        )
    }


    private fun handleHover() {
        if (isPropertyWindowHovered) {
            hoveredNode = null
            hoveredLink = null
            return
        }

        hoveredNode = null
        hoveredLink = null

        val mousePos = ImGui.getMousePos()

        // Check for node hover
        for (node in workspace.graph.nodes) {
            val bounds = canvasCtx.computeNodeBounds(node)
            if (mousePos.x in bounds.x..bounds.z && mousePos.y in bounds.y..bounds.w) {
                hoveredNode = node.uid
                break
            }
        }

        // Check for link hover if no node is hovered
        if (hoveredNode == null) {
            for (link in workspace.graph.getLinks()) {
                if (canvasCtx.isMouseOverLink(link, mousePos.toVec2f)) {
                    hoveredLink = link.uid
                    break
                }
            }
        }
    }

    private fun getNodeIcon(nodeType: String): String {
        return when (nodeType) {
            "Math" -> FontAwesome.Calculator
            "Logic" -> FontAwesome.CodeBranch
            "Input" -> FontAwesome.ArrowRightToBracket
            "Output" -> FontAwesome.ArrowRightFromBracket
            "String" -> FontAwesome.Font
            "Array" -> FontAwesome.ListOl
            "Object" -> FontAwesome.Cube
            "Function" -> FontAwesome.Gear
            "Network" -> FontAwesome.Globe
            "File" -> FontAwesome.File
            "Database" -> FontAwesome.Database
            "Time" -> FontAwesome.Clock
            // Add more mappings as needed
            else -> FontAwesome.PuzzlePiece // Default icon
        }
    }


    private fun drawBezierLink(
        drawList: ImDrawList, startPos: Vector2f, endPos: Vector2f, color: Int, isExec: Boolean
    ) {
        val midX = (startPos.x + endPos.x) / 2
        val controlPoint1 = Vector2f(midX, startPos.y)
        val controlPoint2 = Vector2f(midX, endPos.y)

        if (isExec) {
            // Draw dashed line for exec links
            val segments = 20
            for (i in 0 until segments) {
                val t1 = i.toFloat() / segments
                val t2 = (i + 0.5f) / segments
                val p1 = getBezierPoint(startPos, controlPoint1, controlPoint2, endPos, t1)
                val p2 = getBezierPoint(startPos, controlPoint1, controlPoint2, endPos, t2)
                drawList.addLine(p1.x, p1.y, p2.x, p2.y, color, 2f * canvasCtx.zoom)
            }
        } else {
            drawList.addBezierCubic(
                startPos.x,
                startPos.y,
                controlPoint1.x,
                controlPoint1.y,
                controlPoint2.x,
                controlPoint2.y,
                endPos.x,
                endPos.y,
                color,
                2f * canvasCtx.zoom,
                50
            )
        }
    }


//    private fun drawAnimatedArrow(
//        drawList: ImDrawList, startPos: Vector2f, endPos: Vector2f, startNode: Node, endNode: Node
//    ) {
//        val t = (currentTime % linkAnimationTime) / linkAnimationTime
//        val easeT = easeInOutCubic(t)
//        val arrowPos = getBezierPoint(startPos, endPos, easeT)
//        val tangent = getBezierTangent(startPos, endPos, easeT)
//
//        val arrowSize = 10f * canvasCtx.zoom
//        val arrowAngle = kotlin.math.atan2(tangent.y, tangent.x)
//
//        // Lerp color from start node to end node
//        val startColor = ImColor.rgba(startNode.color.x, startNode.color.y, startNode.color.z, 255)
//        val endColor = ImColor.rgba(endNode.color.x, endNode.color.y, endNode.color.z, 255)
//        val lerpedColor = lerpColor(startColor, endColor, easeT)
//
//        drawList.addTriangleFilled(
//            (arrowPos.x - arrowSize * kotlin.math.cos(arrowAngle - kotlin.math.PI / 6)).toFloat(),
//            (arrowPos.y - arrowSize * kotlin.math.sin(arrowAngle - kotlin.math.PI / 6)).toFloat(),
//            (arrowPos.x - arrowSize * kotlin.math.cos(arrowAngle + kotlin.math.PI / 6)).toFloat(),
//            (arrowPos.y - arrowSize * kotlin.math.sin(arrowAngle + kotlin.math.PI / 6)).toFloat(),
//            arrowPos.x,
//            arrowPos.y,
//            lerpedColor
//        )
//    }


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


    private fun getBezierTangent(startPos: Vector2f, endPos: Vector2f, t: Float): Vector2f {
        val midX = (startPos.x + endPos.x) / 2
        val controlPoint1 = Vector2f(midX, startPos.y)
        val controlPoint2 = Vector2f(midX, endPos.y)

        val u = 1 - t
        val tangent = Vector2f(
            3 * u * u * (controlPoint1.x - startPos.x) + 6 * u * t * (controlPoint2.x - controlPoint1.x) + 3 * t * t * (endPos.x - controlPoint2.x),
            3 * u * u * (controlPoint1.y - startPos.y) + 6 * u * t * (controlPoint2.y - controlPoint1.y) + 3 * t * t * (endPos.y - controlPoint2.y)
        )

        return tangent.normalize()
    }


    private fun easeInOutCubic(t: Float): Float {
        return if (t < 0.5f) 4 * t * t * t else 1 - (-2 * t + 2).pow(3) / 2
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
            scrolled.x += io.getMouseDelta().x
            scrolled.y += io.getMouseDelta().y
        }

        // Context menu (under default mouse threshold)
        val dragDelta = ImGui.getMouseDragDelta(ImGuiMouseButton.Middle)
        if (dragDelta.x == 0.0f && dragDelta.y == 0.0f) {
            ImGui.openPopupOnItemClick("context", ImGuiPopupFlags.MouseButtonMiddle)
        }
        val zoom = canvasCtx.zoom
        //Handles the canvasCtx.zooming
        val mouseWheel = io.mouseWheel
        if (mouseWheel != 0.0f && !canvasCtx.isLinking) {
            val zoomDelta = mouseWheel * 0.10f
            canvasCtx.zoom += zoomDelta
            canvasCtx.zoom = canvasCtx.zoom.coerceIn(0.5f, 2f)
            println(canvasCtx.zoom)
        }

//        if (ImGui.isMouseClicked(ImGuiMouseButton.Right)) {
//            contextMenuPosition = Vector2f(io.mousePos.x, io.mousePos.y)
//            ImGui.openPopup("CanvasContextMenu")
//        }
        // Open custom action menu on right-click
        if (ImGui.isMouseClicked(ImGuiMouseButton.Right) && !initialOpen) {
            val mousePos = ImGui.getMousePos()
            customActionMenu.open(mousePos)
            canvasCtx.variablesMenu.closePopup()
        }
        // Adjust scrolled to keep the center point consistent
        val center = workspace.viewportCenter
        scrolled.x -= (center.x - scrolled.x) * (canvasCtx.zoom - zoom) / zoom
        scrolled.y -= (center.y - scrolled.y) * (canvasCtx.zoom - zoom) / zoom

        // Unselect all nodes after dragging is complete
//        if (ImGui.isMouseReleased(ImGuiMouseButton.Left) && canvasCtx.isDraggingNode) {
//            canvasCtx.unselectAllNodes()
//        }
    }

    /**
     * Renders the background and the grids for the canvas
     */
    private inline fun renderBackground(body: () -> Unit) {
        val drawList = ImGui.getWindowDrawList()
        //Handle the clipping of anything outside of our canvas bounds
        drawList.pushClipRect(bounds.x, bounds.y, bounds.z, bounds.w, false)
        body()
        drawList.popClipRect()
        //Draw our border
//        drawList.addRect(
//            bounds.x, bounds.y, bounds.z, bounds.w, ImColor.rgba(255, 255, 255, 255)
//        )
    }

    /**
     * A helper method to assist with the drawing of the grids
     */
    private fun drawGrid(
        step: Float = 20f, color: Int = ImColor.rgba(33, 33, 33, 120)
    ) {
        val zoomedStep = step * this.canvasCtx.zoom
        val drawList = ImGui.getWindowDrawList()
        var x = fmodf(scrolled.x, zoomedStep)
        while (x < bounds.z - bounds.x) {
            drawList.addLine(
                bounds.x + x, bounds.y, bounds.x + x, bounds.w, color
            )
            x += zoomedStep
        }
        var y = fmodf(scrolled.y, zoomedStep)
        while (y < bounds.w - bounds.y) {
            drawList.addLine(
                bounds.x, bounds.y + y, bounds.z, bounds.y + y, color
            )
            y += zoomedStep
        }
    }


}