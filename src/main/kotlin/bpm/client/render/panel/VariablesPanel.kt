package bpm.client.render.panel

import bpm.client.runtime.ClientRuntime
import bpm.client.runtime.ClientRuntime.logger
import bpm.client.runtime.windows.CanvasContext
import bpm.client.runtime.windows.CanvasGraphics
import bpm.client.utils.PropertyInput
import bpm.client.utils.toVec2f
import bpm.client.utils.use
import bpm.common.network.Client
import bpm.common.network.Endpoint
import bpm.common.network.listener
import bpm.common.property.Property
import bpm.common.schemas.Schemas
import bpm.common.utils.FontAwesome
import bpm.common.workspace.graph.Node
import bpm.common.workspace.packets.NodeType
import bpm.common.workspace.packets.NodeType.*
import bpm.common.workspace.packets.VariableCreateRequest
import bpm.common.workspace.packets.VariableDeleteRequest
import bpm.common.workspace.packets.VariableUpdateRequest
import imgui.ImColor
import imgui.ImDrawList
import imgui.ImGui
import imgui.flag.*
import imgui.type.ImString
import net.minecraft.client.gui.GuiGraphics
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.Vector4i

object VariablesPanel : Panel("Variables", "\uf1ec") {


    private val canvasContext by lazy { Client.installed<CanvasContext>() }
    private val workspace get() = ClientRuntime.workspace ?: error("Workspace not available")

    private var variableValueBuffer = ImString(256)
    private val variableNameBuffer = ImString(256)
    private val variableTypeBuffer = ImString(256)
    private val searchBuffer = ImString(256)
    private var draggedVariable: Triple<String, Property<*>, String>? = null
    private val accentColor = ImColor.rgba(0, 122, 255, 255)


    override fun renderBody(
        drawList: ImDrawList,
        position: Vector2f,
        size: Vector2f
    ) {
        // Filter variables based on search input
        val filteredVariables = filterVariables(workspace.graph.variables)

        filteredVariables.forEach { (name, variable) ->
            renderVariable(drawList, name, variable, Vector2f(size.x - 30f, 70f))
            ImGui.dummy(0f, 10f)
        }
    }


    private fun filterVariables(variables: Map<String, Property<*>>): Map<String, Property<*>> {
        val searchTerm = searchBuffer.get().lowercase()
        return if (searchTerm.isEmpty()) {
            variables
        } else {
            variables.filter { (name, _) ->
                name.lowercase().contains(searchTerm)
            }
        }
    }

//    override fun renderAfter(graphics: CanvasGraphics, drawList: ImDrawList, position: Vector2f, size: Vector2f) {
//        val pos = Vector2f(position.x, position.y + size.y - 60f)
//        ImGui.setNextWindowPos(pos.x - 10f, pos.y)
//        val panelWidth = size.x
//        ImGui.pushClipRect(pos.x, pos.y, pos.x + panelWidth, pos.y + 60f, false)
//        if (ImGui.beginChild(
//                "Variablesscrolling",
//                panelWidth,
//                60f,
//                false,
//                ImGuiWindowFlags.NoScrollbar or ImGuiWindowFlags.NoScrollWithMouse
//            )
//        ) {
//
//        }
//        ImGui.endChild()
//        ImGui.popClipRect()
//    }

    override fun renderFooterContent(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        renderSearchAndAddBar(drawList, position, Vector2f(panelWidth - 20f, 40f))
        handleVariableDragging(graphics, ImGui.getForegroundDrawList())
    }


    private fun renderSearchAndAddBar(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
//        val barHeight = 40f
//        val barY = position.y + 10f
//        position.x += 15f
//        // Background for the entire bar
//        drawList.addRectFilled(
//            position.x,
//            barY,
//            position.x + size.x,
//            barY + barHeight,
//            ImColor.rgba(40, 40, 40, 255),
//            10f
//        )
//
//        // Search input


        renderSearchInput(drawList, position, size)
        // Add variable button
        renderAddVariableButton(
            drawList,
            Vector2f(position.x + size.x - 35f, position.y + 10f),
            Vector2f(30f, 30f)
        )
    }


    private fun renderVariable(
        drawList: ImDrawList,
        name: String,
        variable: Property<*>,
        size: Vector2f
    ) {
        val startCursorPos = ImGui.getCursorScreenPos().toVec2f
        //Add margin if the height is less than 70
        startCursorPos.x += 15f
        startCursorPos.y += 15f
        // Background
        drawList.addRectFilled(
            startCursorPos.x,
            startCursorPos.y,
            startCursorPos.x + size.x,
            startCursorPos.y + size.y,
            ImColor.rgba(60, 60, 60, 255),
            5f
        )

        // Delete button
        renderDeleteButton(drawList, startCursorPos, name)

        // Variable name
        renderVariableName(drawList, startCursorPos, name, size)

        // Value input
        handleVariableInput(drawList, name, variable, startCursorPos, size.x - 25f, 0f)


        // Get and Set buttons
        renderGetSetButtons(drawList, startCursorPos, name, variable, size)

        ImGui.setCursorScreenPos(startCursorPos.x, startCursorPos.y + size.y)
    }


    private fun renderSearchInput(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        val searchBarHeight = 30f
        val searchBarY = position.y + 10f

        // Search bar background
        drawList.addRectFilled(
            position.x + 5f,
            searchBarY,
            position.x + size.x - 5f,
            searchBarY + searchBarHeight,
            ImColor.rgba(60, 60, 60, 255),
            5f
        )

        // Search icon
        iconFam[20].use {
            drawList.addText(
                it,
                20f,
                position.x + 15f,
                searchBarY + 5f,
                ImColor.rgba(150, 150, 150, 255),
                FontAwesome.AlignCenter
            )
        }

        // Search input
        bodyFam[18].use {
            ImGui.setCursorScreenPos(position.x + 25f, position.y + 12)
            ImGui.pushItemWidth(size.x - 30f)
            ImGui.pushStyleColor(ImGuiCol.FrameBg, ImColor.rgba(60, 60, 60, 0).toInt())
            ImGui.pushStyleColor(ImGuiCol.Text, ImColor.rgba(200, 200, 200, 255).toInt())
            ImGui.pushStyleColor(ImGuiCol.Border, ImColor.rgba(60, 60, 60, 255).toInt())
            ImGui.inputText("##search", searchBuffer)
            ImGui.popStyleColor(3)
            ImGui.popItemWidth()
        }

    }

    private fun renderDeleteButton(drawList: ImDrawList, startCursorPos: Vector2f, name: String) {
        ImGui.setCursorScreenPos(startCursorPos.x + 5f, startCursorPos.y + 5f)
        if (ImGui.invisibleButton("delete_$name", 25f, 25f)) {
            deleteVariable(name)
        }
        if (ImGui.isItemHovered()) {
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand)
        }
        iconFam.header.use {
            drawList.addText(
                iconFam.header,
                18f,
                startCursorPos.x + 10f,
                startCursorPos.y + 8f,
                ImColor.rgba(255, 255, 255, 255),
                FontAwesome.Trash
            )
        }
    }

    private fun renderVariableName(drawList: ImDrawList, startCursorPos: Vector2f, name: String, size: Vector2f) {
        val textSize = ImGui.calcTextSize(name)
        val centerX = startCursorPos.x + (size.x - textSize.x) / 2
        boldFam.header.use {
            drawList.addText(
                boldFam.header,
                18f,
                centerX,
                startCursorPos.y + 10f,
                ImColor.rgba(255, 255, 255, 255),
                name
            )
        }
    }

    private fun renderAddVariableButton(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        val isHovered = isMouseOver(position, size.x, size.y)
        val buttonColor = if (isHovered) buttonHoverColor else buttonColor

        drawList.addRectFilled(
            position.x,
            position.y,
            position.x + size.x,
            position.y + size.y,
            buttonColor,
            5f
        )

        iconFam.header.use {
            drawList.addText(
                iconFam.header,
                24f,
                position.x + size.x / 2 - 7f,
                position.y + size.y / 2 - 8f,
                ImColor.rgba(255, 255, 255, 255),
                FontAwesome.Plus
            )
        }

        if (isHovered) {
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand)
            graphics.renderTooltip("Add Variable")
        }

        if (ImGui.isMouseClicked(ImGuiMouseButton.Left) && isHovered) {
            ImGui.openPopup("Add Variable")
        }

        renderAddVariablePopup()
    }

    private fun renderAddVariablePopup() {
        if (ImGui.beginPopup("Add Variable")) {
            ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 8f, 8f)
            ImGui.pushStyleColor(ImGuiCol.PopupBg, ImColor.rgba(50, 50, 50, 255).toInt())
            ImGui.pushStyleColor(ImGuiCol.Text, ImColor.rgba(200, 200, 200, 255).toInt())
            ImGui.pushStyleColor(ImGuiCol.FrameBg, ImColor.rgba(70, 70, 70, 255).toInt())

            ImGui.inputText("Name##variable", variableNameBuffer)

            ImGui.pushItemWidth(ImGui.calcItemWidth())
            if (ImGui.beginCombo("Type##variable", variableTypeBuffer.get())) {
                listOf("String", "Int", "Float", "Bool", "Vec2f", "Vec3f", "Vec4f", "Color").forEach { type ->
                    if (ImGui.selectable(type, type == variableTypeBuffer.get())) {
                        variableTypeBuffer.set(type)
                    }
                }
                ImGui.endCombo()
            }
            ImGui.popItemWidth()

            if (ImGui.button("Add")) {
                addVariable(variableNameBuffer.get(), variableTypeBuffer.get())
                ImGui.closeCurrentPopup()
            }

            ImGui.popStyleColor(3)
            ImGui.popStyleVar()
            ImGui.endPopup()
        }
    }

    private fun renderValueInput(startCursorPos: Vector2f, name: String, size: Vector2f) {
        ImGui.setCursorScreenPos(startCursorPos.x + 10f, startCursorPos.y + 35f)
        ImGui.pushItemWidth(size.x - 20f)
        if (ImGui.inputText("##value_$name", variableValueBuffer)) {
            variableValueBuffer.get().trim().let {
                // TODO: Parse the input and set the variable value
                logger.info("Variable value changed to: $it")
            }
        }
        ImGui.popItemWidth()
    }

    private fun renderGetSetButtons(
        drawList: ImDrawList,
        startCursorPos: Vector2f,
        name: String,
        variable: Property<*>,
        size: Vector2f
    ) {
        val buttonSize = 30f
        val buttonSpacing = 5f
        val totalButtonHeight = buttonSize * 2 + buttonSpacing

        val getButtonPos = Vector2f(
            startCursorPos.x + size.x - buttonSize - 5f,
            startCursorPos.y + (size.y - totalButtonHeight) / 2
        )
        val setButtonPos = Vector2f(
            getButtonPos.x,
            getButtonPos.y + buttonSize + buttonSpacing
        )

        // Get button
        drawIconButton(drawList, FontAwesome.ArrowRight, getButtonPos, buttonSize)
        if (ImGui.isMouseClicked(ImGuiMouseButton.Left) && isMouseOver(getButtonPos, buttonSize, buttonSize)) {
            draggedVariable = Triple(name, variable, "Get")
            isDragging = true
        }


        // Set button
        drawIconButton(drawList, FontAwesome.ArrowLeft, setButtonPos, buttonSize)
        if (ImGui.isMouseClicked(ImGuiMouseButton.Left) && isMouseOver(setButtonPos, buttonSize, buttonSize)) {
            draggedVariable = Triple(name, variable, "Set")
            isDragging = true
        }

        //If the mouse is released and we're still over our button, don't create a node
        if (ImGui.isMouseReleased(ImGuiMouseButton.Left) && (isMouseOver(
                getButtonPos,
                buttonSize,
                buttonSize
            ) || isMouseOver(setButtonPos, buttonSize, buttonSize))
        ) {
            draggedVariable = null
            isDragging = false
        }
    }

    private fun drawButton(drawList: ImDrawList, label: String, pos: Vector2f, width: Float, height: Float) {
        drawList.addRectFilled(
            pos.x, pos.y, pos.x + width, pos.y + height,
            ImColor.rgba(100, 100, 100, 255)
        )
        val textSize = ImGui.calcTextSize(label)
        val textPos = Vector2f(
            pos.x + (width - textSize.x) / 2,
            pos.y + (height - textSize.y) / 2
        )
        drawList.addText(boldFam.header, 14f, textPos.x, textPos.y, ImColor.rgba(255, 255, 255, 255), label)
    }

    private fun drawIconButton(drawList: ImDrawList, icon: String, pos: Vector2f, size: Float) {
        val isHovered = isMouseOver(pos, size, size)
        //If hovered, render a drop shadow
        if (isHovered) {
            drawList.addRectFilled(
                pos.x + 2, pos.y + 2, pos.x + size + 2, pos.y + size + 2,
                ImColor.rgba(0, 0, 0, 100),
                10f
            )
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand)
            graphics.renderTooltip("Drag to create a new node")
        }
        drawList.addRectFilled(
            pos.x, pos.y, pos.x + size, pos.y + size,
            ImColor.rgba(27, 29, 28, 255),
            10f
        )
        iconFam.header.use {

            val textSize = ImGui.calcTextSize(icon)
            val textPos = Vector2f(
                pos.x + (textSize.x / 2) + 1,
                pos.y + 5F
            )
            drawList.addText(iconFam.header, 18f, textPos.x, textPos.y, ImColor.rgba(255, 255, 255, 255), icon)

        }
    }


    private fun handleVariableInput(
        drawList: ImDrawList,
        name: String,
        variable: Property<*>,
        boxPosition: Vector2f,
        boxWidth: Float,
        yOffset: Float
    ) {
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 8f, 4f)
        ImGui.pushStyleColor(ImGuiCol.FrameBg, buttonColor.toInt())
        ImGui.pushStyleColor(ImGuiCol.FrameBgHovered, buttonHoverColor.toInt())
        ImGui.pushStyleColor(ImGuiCol.FrameBgActive, accentColor.toInt())
        //Remove the input background
        val updated = PropertyInput.render(
            drawList,
            "##$name",
            variable,
            boxPosition.x + 15f,
            boxPosition.y + 40f + yOffset,  // Raised up by adjusting this Y-coordinate
            boxWidth - 40f
        )

        ImGui.popStyleColor(3)
        ImGui.popStyleVar()

        if (updated) {
            Client {
                it.send(VariableUpdateRequest(name, Property.Object().apply {
                    this["type"] = Property.String(variable::class.simpleName!!)
                    this["value"] = variable
                }))
            }
        }
    }

    private fun getNode(type: NodeType): Node? {
        val library = listener<Schemas>(Endpoint.Side.CLIENT).library
        val type = when (type) {
            GetVariable -> library["Variables/Get Variable"]
            SetVariable -> library["Variables/Set Variable"]
        }
        if (type == null) {
            logger.error("Node type not found in library")
            return null
        }
        return listener<Schemas>(Endpoint.Side.CLIENT).createFromType(workspace, type, Vector2f(0f, 0f), true)
    }

    private fun handleVariableDragging(graphics: CanvasGraphics, drawList: ImDrawList) {
        val draggedVar = draggedVariable
        if (draggedVar != null && ImGui.isMouseDragging(ImGuiMouseButton.Left)) {
            val mousePos = ImGui.getMousePos()
            val (name, _, nodeType) = draggedVar
            val node = if (nodeType == "Get") getNode(GetVariable) else getNode(SetVariable)
            if (node == null) {
                logger.error("Failed to create node")
                return
            }
            // Draw a preview of the node header
            val headerWidth = 150f
            val headerHeight = 30f
            val titleBounds = Vector4f(
                mousePos.x + 20f,
                mousePos.y,
                mousePos.x + headerWidth,
                mousePos.y + headerHeight
            )
            val nodeBounds = Vector4f(mousePos.x + 20f, mousePos.y, mousePos.x + headerWidth, mousePos.y + headerHeight)
            renderNodeHeader(
                graphics,
                drawList,
                node,
                titleBounds,
                nodeBounds
            )


            ImGui.setMouseCursor(ImGuiMouseCursor.None)
        } else if (draggedVar != null && ImGui.isMouseReleased(ImGuiMouseButton.Left)) {
            val mousePos = ImGui.getMousePos().toVec2f
            val (name, _, nodeType) = draggedVar

            when (nodeType) {
                "Get" -> canvasContext.createVariableNode(GetVariable, mousePos, name)
                "Set" -> canvasContext.createVariableNode(SetVariable, mousePos, name)
            }
            draggedVariable = null
            isDragging = false
        }
    }

    fun renderNodeHeader(
        graphics: CanvasGraphics,
        drawList: ImDrawList,
        node: Node,
        titleBounds: Vector4f,
        nodeBounds: Vector4f
    ) {
        val paddingX = 12f
        val paddingY = 5f

        val icon = node.icon.toChar().toString()
        val iconSize = 24f
        val maxX = nodeBounds.z // Use node bounds for max width

        // Header background
        drawList.addRectFilled(
            nodeBounds.x - paddingX / 2f,
            titleBounds.y - paddingY / 2f,
            maxX + paddingX / 2f,
            titleBounds.w + paddingY / 2f,
            ImColor.rgba(70, 70, 70, 255),
            10f
        )

        // Header border
        drawList.addRect(
            nodeBounds.x - paddingX / 2f,
            titleBounds.y - paddingY / 2f,
            maxX + paddingX / 2f,
            titleBounds.w + paddingY / 2f,
            ImColor.rgba(0, 0, 0, 255),
            10f,
            ImDrawFlags.None,
            2f
        )

        // Render icon
        graphics.drawShadowedText(
            drawList,
            iconFam.header,
            iconSize,
            nodeBounds.x + paddingX / 2f - 4,
            nodeBounds.y,
            ImColor.rgba(255, 255, 255, 255),
            icon,
            scale = 1f,
            offsetY = 2.5f,
            offsetX = 2.5f
        )

        // Icon separator line
        drawList.addLine(
            nodeBounds.x + iconSize - 5,
            titleBounds.y - paddingY / 2f,
            nodeBounds.x + iconSize - 5,
            titleBounds.w + paddingY / 2f,
            ImColor.rgba(0, 0, 0, 255),
            2f
        )

        // Calculate available width for text
        val availableWidth = maxX - (nodeBounds.x + iconSize + 2f)
        val fullText = node.name
        var displayText = fullText

        // Render node name
        bodyFam.title.use {
            val textSize = ImGui.calcTextSize(fullText)

            if (textSize.x > availableWidth) {
                // Truncate text if it's too long
                displayText = graphics.truncateText(fullText, availableWidth)
            }

            val textX = nodeBounds.x + iconSize + 7f
            val textY = nodeBounds.y + 5f

            // Render truncated or full text
            graphics.drawShadowedText(
                drawList,
                bodyFam.title,
                20f,
                textX,
                textY,
                ImColor.rgba(255, 255, 255, 255),
                displayText,
                scale = 1f,
                offsetX = 3f,
                offsetY = 3f
            )
        }
    }


    private fun deleteVariable(name: String) {
        Client {
            it.send(VariableDeleteRequest(name))
        }
    }

    private fun addVariable(name: String, type: String) {
        val property = when (type) {
            "String" -> Property.String("")
            "Int" -> Property.Int(0)
            "Float" -> Property.Float(0f)
            "Bool" -> Property.Boolean(false)
            "Vec2f" -> Property.Vec2f(Vector2f(0f, 0f))
            "Vec3f" -> Property.Vec3f(Vector3f(0f, 0f, 0f))
            "Vec4f" -> Property.Vec4f(Vector4f(0f, 0f, 0f, 0f))
            "Color" -> Property.Vec4i(Vector4i(255, 255, 255, 255))
            else -> return
        }
        Client {
            it.send(VariableCreateRequest(name, Property.Object().apply {
                this["type"] = Property.String(type)
                this["value"] = property
            }))
        }
    }
}