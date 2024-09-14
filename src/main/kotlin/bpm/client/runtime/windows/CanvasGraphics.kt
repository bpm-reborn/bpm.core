package bpm.client.runtime.windows

import bpm.client.font.Fonts
import bpm.client.utils.use
import bpm.common.network.Client
import bpm.common.network.Endpoint
import bpm.common.network.listener
import bpm.common.property.*
import bpm.common.schemas.Schemas
import bpm.common.utils.FontAwesome
import bpm.common.utils.fmodf
import bpm.common.workspace.graph.Edge
import bpm.common.workspace.graph.Link
import bpm.common.workspace.graph.Node
import bpm.common.workspace.packets.EdgePropertyUpdate
import imgui.ImColor
import imgui.ImDrawList
import imgui.ImFont
import imgui.ImGui
import imgui.flag.ImDrawFlags
import imgui.flag.ImGuiMouseButton
import imgui.flag.ImGuiMouseCursor
import imgui.flag.ImGuiStyleVar
import imgui.type.ImString
import org.joml.Vector2f
import org.joml.Vector4f
import org.joml.Vector4i
import kotlin.math.max
import kotlin.math.pow

class CanvasGraphics(
    private val window: CanvasWindow,
    private val context: CanvasContext
) {

    private val headerFamily get() = Fonts.getFamily("Inter")["Bold"]
    private val headerFont get() = headerFamily[window.workspace.settings.fontHeaderSize]
    private val bodyFamily get() = Fonts.getFamily("Inter")["Regular"]
    private val bodyFont get() = bodyFamily[window.workspace.settings.fontSize]
    private val fontAwesomeFamily = Fonts.getFamily("Fa")["Regular"]
    private val fontAwesome get() = this.fontAwesomeFamily[window.workspace.settings.fontHeaderSize]

    private val arrowCount = 5
    private val execSegmentCount = 10
    private val execSegmentLength = 10f // Length of each segment in the exec link
    private val execGapRatio = 0.5f // Ratio of gap to segment length

    inline fun renderBackground(drawList: ImDrawList, clipBounds: Vector4f, crossinline body: () -> Unit) {
        //Handle the clipping of anything outside of our canvas bounds
        drawList.pushClipRect(clipBounds.x, clipBounds.y, clipBounds.z, clipBounds.w, false)
        body()
        drawList.popClipRect()
    }

    fun renderLinks(drawList: ImDrawList, links: Collection<Link>) {
        for (link in links) {
            val sourceEdge = window.workspace.getEdge(link.from) ?: continue
            val sourceNode = window.workspace.getNode(sourceEdge.owner) ?: continue
            val targetEdge = window.workspace.getEdge(link.to) ?: continue
            val targetNode = window.workspace.getNode(targetEdge.owner) ?: continue

            val sourceBounds = context.getNodeBounds(sourceNode)
            val targetBounds = context.getNodeBounds(targetNode)

            val sourcePos = context.getEdgePosition(sourceNode, sourceEdge, sourceBounds)
            val targetPos = context.getEdgePosition(targetNode, targetEdge, targetBounds)

            val sourceColor = ImColor.rgba(sourceNode.color.x, sourceNode.color.y, sourceNode.color.z, 255)
            val targetColor = ImColor.rgba(targetNode.color.x, targetNode.color.y, targetNode.color.z, 255)

            if (sourceEdge.type == "exec" || targetEdge.type == "exec") {
                drawExecLink(drawList, sourcePos, targetPos, sourceColor, targetColor, context.zoom, window.currentTime)
            } else {
                drawDataLink(drawList, sourcePos, targetPos, sourceColor, targetColor, context.zoom)
            }

            // Hover effect
            if (window.hoveredLink?.uid == link.uid || context.isLinkInSelectionBox(link)) {
                val midX = (sourcePos.x + targetPos.x) / 2
                val controlPoint1 = Vector2f(midX, sourcePos.y)
                val controlPoint2 = Vector2f(midX, targetPos.y)
                drawList.addBezierCubic(
                    sourcePos.x,
                    sourcePos.y,
                    controlPoint1.x,
                    controlPoint1.y,
                    controlPoint2.x,
                    controlPoint2.y,
                    targetPos.x,
                    targetPos.y,
                    ImColor.rgba(69, 163, 230, 185), // Blue-ish highlight for hovered links
                    4f * context.zoom, // Thicker line for hovered links
                    50
                )
            }

            // Add visual indication for selected links
            if (context.isLinkSelected(link)) {
                val midX = (sourcePos.x + targetPos.x) / 2
                val controlPoint1 = Vector2f(midX, sourcePos.y)
                val controlPoint2 = Vector2f(midX, targetPos.y)
                drawList.addBezierCubic(
                    sourcePos.x,
                    sourcePos.y,
                    controlPoint1.x,
                    controlPoint1.y,
                    controlPoint2.x,
                    controlPoint2.y,
                    targetPos.x,
                    targetPos.y,
                    ImColor.rgba(255, 255, 0, 255), // Yellow highlight for selected links
                    4f * context.zoom, // Thicker line for selected links
                    50
                )
            }
        }
    }

    fun renderNodes(drawList: ImDrawList, nodes: Collection<Node>) {
        for (node in nodes) {
            val nodeBounds = context.getNodeBounds(node)
            val headerBounds = context.getHeaderBounds(node)
            val rawColor: Vector4i = node.color
            val nodeColor = ImColor.rgba(rawColor.x, rawColor.y, rawColor.z, rawColor.w)
            renderNodeBody(drawList, node, nodeBounds, nodeColor)
            renderNodeHeader(drawList, node, headerBounds, nodeBounds)
            renderEdges(drawList, node, nodeBounds)

            context.handleNode(node, nodeBounds, headerBounds)
        }

        renderToolTips()

        // Render the selection context overlay
    }

    inline fun renderButton(
        font: ImFont,
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

    }

    private fun renderNodeHeader(drawList: ImDrawList, node: Node, titleBounds: Vector4f, nodeBounds: Vector4f) {
        val paddingX = 12f * context.zoom
        val paddingY = 5f * context.zoom

        val icon = node.icon.toChar().toString()
        val iconSize = 24f * context.zoom
        val maxX = nodeBounds.z // Use node bounds for max width

        // Header background
        drawList.addRectFilled(
            nodeBounds.x - paddingX / 2f,
            titleBounds.y - paddingY / 2f,
            maxX + paddingX / 2f,
            titleBounds.w + paddingY / 2f,
            ImColor.rgba(70, 70, 70, 255),
            10f * context.zoom
        )

        // Header border
        drawList.addRect(
            nodeBounds.x - paddingX / 2f,
            titleBounds.y - paddingY / 2f,
            maxX + paddingX / 2f,
            titleBounds.w + paddingY / 2f,
            ImColor.rgba(0, 0, 0, 255),
            10f * context.zoom,
            ImDrawFlags.None,
            2f * context.zoom
        )

        // Render icon
        drawShadowedText(
            drawList,
            fontAwesome,
            iconSize,
            nodeBounds.x + paddingX / 2f - 4 * context.zoom,
            nodeBounds.y - 15f * context.zoom,
            ImColor.rgba(255, 255, 255, 255),
            icon,
            scale = 1f,
            offsetY = 2.5f * context.zoom,
            offsetX = 2.5f * context.zoom
        )

        // Icon separator line
        drawList.addLine(
            nodeBounds.x + iconSize - 5 * context.zoom,
            titleBounds.y - paddingY / 2f,
            nodeBounds.x + iconSize - 5 * context.zoom,
            titleBounds.w + paddingY / 2f,
            ImColor.rgba(0, 0, 0, 255),
            2f * context.zoom
        )

        // Calculate available width for text
        val availableWidth = maxX - (nodeBounds.x + iconSize + 2f * context.zoom)
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
                10f * context.zoom
            )
        }

        // Render node name
        headerFont.use {
            val textSize = ImGui.calcTextSize(fullText)

            if (textSize.x > availableWidth) {
                // Truncate text if it's too long
                displayText = truncateText(fullText, availableWidth)
            }

            val textX = nodeBounds.x + iconSize + 2f * context.zoom
            val textY = titleBounds.y - 1 * context.zoom

            // Render truncated or full text
            drawShadowedText(
                drawList,
                headerFont,
                window.workspace.settings.fontHeaderSize.toFloat() * 1f,
                textX,
                textY,
                ImColor.rgba(255, 255, 255, 255),
                displayText,
                scale = 1f,
                offsetX = 3f * context.zoom,
                offsetY = 3f * context.zoom
            )
        }


    }

    private fun renderEdges(drawList: ImDrawList, node: Node, nodeBounds: Vector4f) {
        val edges = window.workspace.graph.getEdges(node)
        val inputEdges = edges.filter { it.direction == "input" }
        val outputEdges = edges.filter { it.direction == "output" }

        val edgeSpacing = 20f * context.zoom
        val edgeStartY = nodeBounds.y + 30f * context.zoom  // Start below the header

        // Render input edges
        inputEdges.forEachIndexed { index, edge ->
            val yPos = edgeStartY + index * edgeSpacing
            renderEdge(drawList, node, edge, nodeBounds, Vector2f(nodeBounds.x - 10f * context.zoom, yPos), true)
        }

        // Render output edges
        outputEdges.forEachIndexed { index, edge ->
            val yPos = edgeStartY + index * edgeSpacing
            renderEdge(drawList, node, edge, nodeBounds, Vector2f(nodeBounds.z + 10f * context.zoom, yPos), false)
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
        val edgeRadius = 4f * context.zoom
        val isConnected = window.workspace.graph.links.any { it.from == edge.uid || it.to == edge.uid }

        if (edge.type == "exec" && edge.name == "exec_in" || edge.name == "exec_out" || edge.name == "exec") {


            //render the triangle
            val triangleSize = 8f * context.zoom
            val triangleX =
                if (edge.direction == "input") pos.x + 18f * context.zoom else pos.x - triangleSize - 18f * context.zoom
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
            drawList.addCircle(pos.x, pos.y, edgeRadius, color, 12, 1.5f * context.zoom)
        }

        // Render edge label and input field for non-exec types
        if (edge.type == "exec" && (edge.name == "exec_in" || edge.name == "exec_out" || edge.name == "exec")) {
            // Do nothing
        } else {
            bodyFont.use {
                val labelWidth = ImGui.calcTextSize(edge.name).x
                val inputWidth = 60f * context.zoom // Adjust this value as needed
                val spacing = 5f * context.zoom

                val totalWidth = labelWidth + inputWidth + spacing
                val startX =
                    if (isInput) pos.x + edgeRadius * 2 + spacing else pos.x - totalWidth - edgeRadius * 2 - spacing

                val labelX = if (isInput) startX else startX + inputWidth + spacing
                val inputX = if (isInput) startX + labelWidth + spacing else startX

                val textY = pos.y - ImGui.getTextLineHeight() / 2
                val labelPadding = 1f * context.zoom
                // Render text container
                if (edge.direction == "input") drawList.addRectFilled(
                    startX - labelPadding,
                    textY - labelPadding,
                    startX + labelWidth + labelPadding,
                    textY + ImGui.getTextLineHeight() + labelPadding,
                    ImColor.rgba(40, 40, 40, 200),
                    2f * context.zoom
                )
                else drawList.addRectFilled(
                    startX + inputWidth + spacing - labelPadding,
                    textY - labelPadding,
                    startX + totalWidth + labelPadding,
                    textY + ImGui.getTextLineHeight() + labelPadding,
                    ImColor.rgba(40, 40, 40, 200),
                    2f * context.zoom
                )

                // Render edge label
                drawList.addText(
                    bodyFont,
                    window.workspace.settings.fontSize.toFloat(),
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
        if (context.isPointOverEdge(Vector2f(mousePos.x, mousePos.y), pos)) {
            drawList.addCircle(
                pos.x,
                pos.y,
                edgeRadius + 2f * context.zoom,
                ImColor.rgba(255, 255, 255, 200),
                12,
                1.5f * context.zoom
            )

            if (ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
                context.startEdgeDrag(node, edge)
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
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 0f, 2f * context.zoom)
        ImGui.pushStyleVar(ImGuiStyleVar.CellPadding, 0f, 0f)
        bodyFamily[(12 * context.zoom).toInt()].use {

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
                        drawList, boolValue, x, y + 2 * context.zoom, width, ImGui.getTextLineHeight(), "##value"
                    ) {
                        edge.properties["value"] = edge.value.apply {
                            this["default"] = boolValue
                            Client { it.send(EdgePropertyUpdate(edge.uid, this)) }
                        }
                    }
                }

                "color" -> {
                    var colorValue = (currentValue.castOr { Property.String("#FFFFFFFF") }).toVecColor()
                    val color = colorValue.get()
                    val colorPickerSize = 20f * context.zoom
                    val colorPickerPos = Vector2f(x, y + 2 * context.zoom)
                    val colorPickerBounds = Vector4f(
                        colorPickerPos.x,
                        colorPickerPos.y,
                        colorPickerPos.x + colorPickerSize,
                        colorPickerPos.y + colorPickerSize
                    )

                    drawList.addRectFilled(
                        colorPickerBounds.x,
                        colorPickerBounds.y,
                        colorPickerBounds.z,
                        colorPickerBounds.w,
                        ImColor.rgba(color.x, color.y, color.z, color.w)
                    )

                    if (isPointOverRect(Vector2f(ImGui.getMousePos().x, ImGui.getMousePos().y), colorPickerBounds)) {
                        ImGui.setMouseCursor(ImGuiMouseCursor.Hand)
                        drawList.addRectFilled(
                            colorPickerBounds.x,
                            colorPickerBounds.y,
                            colorPickerBounds.z,
                            colorPickerBounds.w,
                            ImColor.rgba(100, 100, 100, 200)
                        )

                        if (ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
                            ImGui.openPopup("ColorPicker")
                        }
                    }

                    if (ImGui.beginPopup("ColorPicker")) {
                        val floatColor = colorValue.toFloatArray()
                        val colorPickerPos = ImGui.getCursorScreenPos()
                        if (ImGui.colorPicker4("##color", floatColor)) {
                            colorValue =
                                Property.Vec4f(Vector4f(floatColor[0], floatColor[1], floatColor[2], floatColor[3]))
                            edge.properties["value"] = edge.value.apply {
                                //compute the #RRGGBBAA value
                                this["default"] = colorValue.toHexStringProperty()
                                Client { it.send(EdgePropertyUpdate(edge.uid, this)) }
                            }
                        }
                        if (ImGui.isItemDeactivatedAfterEdit()) {
                            edge.properties["value"] = edge.value.apply {
                                //compute the #RRGGBBAA value
                                this["default"] = colorValue.toHexStringProperty()
                                Client { it.send(EdgePropertyUpdate(edge.uid, this)) }
                            }
                            ImGui.closeCurrentPopup()
                        }
                        ImGui.endPopup()
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

    private fun renderNodeBody(drawList: ImDrawList, node: Node, bounds: Vector4f, color: Int) {
        // Adjust bounds to account for edge offset
        val edgeOffset = 10f * context.zoom
        val adjustedBounds = Vector4f(
            bounds.x, bounds.y, bounds.z, bounds.w
        )

        // Main body
        drawList.addRectFilled(
            adjustedBounds.x, adjustedBounds.y, adjustedBounds.z, adjustedBounds.w, color, 10f * context.zoom
        )

        // Hover effect
        if (window.hoveredNode?.uid == node.uid || context.isNodeInSelectionBox(node)) {
            drawList.addRect(
                adjustedBounds.x - 1f * context.zoom,
                adjustedBounds.y - 1f * context.zoom,
                adjustedBounds.z + 1f * context.zoom,
                adjustedBounds.w + 1f * context.zoom,
                ImColor.rgba(69, 163, 230, 185),
                10f * context.zoom,
                ImDrawFlags.None,
                2f * context.zoom
            )
        }

        // Selection outline
        if (context.isNodeSelected(node)) {
            //selectedNodes.add(node.uid)
            drawList.addRect(
                adjustedBounds.x - 1.5f * context.zoom,
                adjustedBounds.y - 1.5f * context.zoom,
                adjustedBounds.z + 1.5f * context.zoom,
                adjustedBounds.w + 1.5f * context.zoom,
                ImColor.rgba(255, 255, 255, 255),
                10f * context.zoom,
                ImDrawFlags.None,
                2f * context.zoom
            )
        } else {
            //selectedNodes.remove(node.uid)
        }
    }

    fun renderSelectionBox(drawList: ImDrawList) {
        val selection = context.getSelection()
        val start = selection?.first ?: return
        val end = selection.second ?: return
        drawList.addRect(
            start.x, start.y, end.x, end.y, ImColor.rgba(100, 100, 255, 100), 0f, ImDrawFlags.None, 2f
        )
        drawList.addRectFilled(
            start.x, start.y, end.x, end.y, ImColor.rgba(100, 100, 255, 30)
        )
    }

    private fun renderToolTips() {
        if (context.isDraggingNode) return
        // Show tooltip for hovered node
        if (context.hoveredTitleBar != null) {
            val node = window.workspace.getNode(context.hoveredTitleBar!!) ?: return
            val nodeType = listener<Schemas>(Endpoint.Side.CLIENT).library.get("${node.type}/${node.name}") ?: return
            val description: Property<String> = nodeType["description"].cast()
            val color = node.color
            drawTooltip(node.icon.toChar().toString(), description.get())
        }

        if (context.hoveredPin != null) {
            val edge = context.hoveredPin!!.second
            val description = edge.description
            val color = getEdgeColor(edge.type)
            drawTooltip(FontAwesome.Info, description, color)
        }
    }

    fun drawDataLink(
        drawList: ImDrawList, startPos: Vector2f, endPos: Vector2f, startColor: Int, endColor: Int, zoom: Float
    ) {
        val midX = (startPos.x + endPos.x) / 2
        val controlPoint1 = Vector2f(midX, startPos.y)
        val controlPoint2 = Vector2f(midX, endPos.y)

        val totalLength = getBezierLength(startPos, controlPoint1, controlPoint2, endPos)
        val scaledSegmentLength = execSegmentLength * zoom
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
                    drawList.addLine(lastPoint.x, lastPoint.y, point.x, point.y, color, 2f * zoom)
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


    fun drawExecLink(
        drawList: ImDrawList,
        startPos: Vector2f,
        endPos: Vector2f,
        startColor: Int,
        endColor: Int,
        zoom: Float,
        time: Float
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
            2f * zoom,
            50
        )

        // Draw animated triangles
        for (i in 0 until arrowCount) {
            val t = ((time * 0.5f + i.toFloat() / arrowCount) % 1.0f)
            val arrowPos = getBezierPoint(startPos, controlPoint1, controlPoint2, endPos, t)
            val arrowTangent = getBezierTangent(startPos, controlPoint1, controlPoint2, endPos, t)

            val arrowSize = (6f + 4f * kotlin.math.sin(t * kotlin.math.PI)).coerceIn(4.0, 10.0) * zoom
            val arrowColor = lerpColor(startColor, endColor, t)

            drawArrow(drawList, arrowPos, arrowTangent, arrowSize.toFloat(), arrowColor)
        }
    }

    fun drawShadowedText(
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

    fun drawTooltip(
        icon: String,
        text: String,
        iconFontSize: Int = 28,
        textFontSize: Int = 24,
        iconColor: Int = ImColor.rgba(33, 150, 243, 255)
    ) {
        val drawList = ImGui.getForegroundDrawList()
        val iconFont = fontAwesomeFamily[iconFontSize]
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
        val checkSize = 12f * context.zoom
        val checkPadding = 2f * context.zoom
        val checkX = x + checkPadding
        val checkY = y + (height - checkSize) / 2
        val checkBounds = Vector4f(checkX, checkY, checkX + checkSize, checkY + checkSize)

        //Renders the checkbox
        drawList.addRectFilled(
            checkBounds.x, checkBounds.y, checkBounds.z, checkBounds.w, ImColor.rgba(40, 40, 40, 200)
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
                checkBounds.x, checkBounds.y, checkBounds.z, checkBounds.w, ImColor.rgba(100, 100, 100, 200)
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

    /**
     * A helper method to assist with the drawing of the grids
     */
    fun drawGrid(
        drawList: ImDrawList,
        position: Vector2f,
        bounds: Vector4f,
        zoom: Float,
        step: Float = 20f,
        color: Int = ImColor.rgba(33, 33, 33, 120)
    ) {
        val zoomedStep = step * zoom
        var x = fmodf(position.x, zoomedStep)
        while (x < bounds.z - bounds.x) {
            drawList.addLine(
                bounds.x + x, bounds.y, bounds.x + x, bounds.w, color
            )
            x += zoomedStep
        }
        var y = fmodf(position.y, zoomedStep)
        while (y < bounds.w - bounds.y) {
            drawList.addLine(
                bounds.x, bounds.y + y, bounds.z, bounds.y + y, color
            )
            y += zoomedStep
        }
    }

    fun truncateText(text: String, availableWidth: Float): String {
        val ellipsis = "..."
        var truncated = text
        while (ImGui.calcTextSize(truncated + ellipsis).x > availableWidth && truncated.length > 1) {
            truncated = truncated.dropLast(1)
        }
        return truncated + ellipsis
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

    fun renderEdgeDrag(drawList: ImDrawList, draggedEdge: Pair<Node, Edge>?, dragStartPos: Vector2f?) {
        if (draggedEdge != null && dragStartPos != null) {
            val (sourceNode, sourceEdge) = draggedEdge
            val nodeBounds = context.getNodeBounds(sourceNode)
            val sourcePos = context.getEdgePosition(sourceNode, sourceEdge, nodeBounds)
            val mousePos = ImGui.getMousePos()

            // Draw the dragging line
            drawList.addBezierCubic(
                sourcePos.x,
                sourcePos.y,
                sourcePos.x + 50f * context.zoom,
                sourcePos.y,
                mousePos.x - 50f * context.zoom,
                mousePos.y,
                mousePos.x,
                mousePos.y,
                ImColor.rgba(255, 255, 255, 200),
                2f * context.zoom,
                20
            )
        }
    }

    fun renderMousePosText(drawList: ImDrawList, bounds: Vector4f, mousePos: Vector2f) {
        val mouseWorldPos = context.convertToWorldCoordinates(mousePos)
        val nodePos: Vector2f? = window.hoveredNode?.let { Vector2f(it.x, it.y) }

        drawList.addText(
            bounds.x + 5,
            bounds.w - 15,
            ImColor.rgba(1f, 1f, 1f, 1f),
            String.format("Center: %.0f, %.0f", window.workspace.settings.center.x, window.workspace.settings.center.y)
        )

        if (mousePos.x in bounds.x..bounds.z && mousePos.y in bounds.y..bounds.w) {
            drawList.addText(
                bounds.x + 5,
                bounds.w - 30,
                ImColor.rgba(1f, 1f, 1f, 1f),
                String.format("Mouse: %.0f, %.0f", mouseWorldPos.x, mouseWorldPos.y)
            )

            nodePos?.let {
                drawList.addText(
                    bounds.x + 5,
                    bounds.w - 45,
                    ImColor.rgba(1f, 1f, 1f, 1f),
                    String.format("Hovered Node: %.0f, %.0f", it.x, it.y)
                )
            }
        }
    }

}