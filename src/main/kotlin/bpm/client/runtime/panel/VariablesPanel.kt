package bpm.client.runtime.panel

import bpm.client.font.Fonts
import bpm.client.runtime.ClientRuntime
import bpm.client.runtime.ClientRuntime.logger
import bpm.client.runtime.Platform
import bpm.client.runtime.windows.CanvasContext
import bpm.client.utils.PropertyInput
import bpm.client.utils.toVec2f
import bpm.client.utils.use
import bpm.common.network.Client
import bpm.common.property.Property
import bpm.common.utils.FontAwesome
import bpm.common.workspace.packets.NodeType.*
import bpm.common.workspace.packets.VariableCreateRequest
import bpm.common.workspace.packets.VariableDeleteRequest
import bpm.common.workspace.packets.VariableUpdateRequest
import imgui.ImColor
import imgui.ImDrawList
import imgui.ImGui
import imgui.flag.ImGuiMouseButton
import imgui.flag.ImGuiMouseCursor
import imgui.type.ImString
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.Vector4i

data object VariablesPanel : Panel("Variables", "\uf1ec") {

    private val canvasContext by lazy { Client.installed<CanvasContext>() }
    private val workspace get() = ClientRuntime.workspace ?: error("Workspace not available")
    private val font = Fonts.getFamily("Inter")["Regular"][32]
    private val faFont = Fonts.getFamily("Fa")["Regular"][18]

    private var variableValueBuffer = ImString(256)
    private val variableNameBuffer = ImString(256)
    private val variableTypeBuffer = ImString(256)

    private var draggedVariable: Triple<String, Property<*>, String>? = null

    override fun renderBody(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        ImGui.setNextWindowPos(position.x, position.y)
        ImGui.setNextWindowSize(size.x - 20f, size.y - 40f)
        ImGui.pushClipRect(position.x, position.y, position.x + size.x, position.y + size.y - 100f, true)
        if (ImGui.beginChild("VariablesScrollRegion", size.x - 20f, size.y - 100f, true)) {
            workspace.graph.variables.forEach { (name, variable) ->
                renderVariable(drawList, name, variable, Vector2f(size.x - 40f, 70f))
                ImGui.dummy(0f, 10f)
            }
        }
        ImGui.endChild()
        ImGui.popClipRect()

        renderAddVariable(drawList, Vector2f(position.x + 10f, position.y + size.y))
        handleVariableDragging(ImGui.getForegroundDrawList())

    }

    private fun renderVariable(
        drawList: ImDrawList,
        name: String,
        variable: Property<*>,
        size: Vector2f
    ) {
        val startCursorPos = ImGui.getCursorScreenPos().toVec2f

        // Background
        drawList.addRectFilled(
            startCursorPos.x,
            startCursorPos.y,
            startCursorPos.x + size.x,
            startCursorPos.y + size.y,
            ImColor.rgba(60, 60, 60, 255)
        )

        // Delete button
        renderDeleteButton(drawList, startCursorPos, name)

        // Variable name
        renderVariableName(drawList, startCursorPos, name, size)

        // Value input
        handleVariableInput(drawList, name, variable, startCursorPos, size.x, 5f)


        // Get and Set buttons
        renderGetSetButtons(drawList, startCursorPos, name, variable, size)

        ImGui.setCursorScreenPos(startCursorPos.x, startCursorPos.y + size.y)
    }

    private fun renderDeleteButton(drawList: ImDrawList, startCursorPos: Vector2f, name: String) {
        ImGui.setCursorScreenPos(startCursorPos.x + 5f, startCursorPos.y + 5f)
        if (ImGui.invisibleButton("delete_$name", 25f, 25f)) {
            deleteVariable(name)
        }
        if (ImGui.isItemHovered()) {
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand)
        }
        faFont.use {
            drawList.addText(
                faFont,
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
        textFont.use {
            drawList.addText(
                textFont,
                18f,
                centerX,
                startCursorPos.y + 10f,
                ImColor.rgba(255, 255, 255, 255),
                name
            )
        }
    }

    private fun renderAddVariable(drawList: ImDrawList, position: Vector2f) {
        ImGui.setCursorPos(position.x, panelHeight - 30f)
        if (ImGui.invisibleButton("Add Variable", panelWidth, 30f)) {
            ImGui.openPopup("Add Variable")
        }
        if (ImGui.isItemHovered()) {
            //render dropshadow for the add variable button
            drawList.addRectFilledMultiColor(
                position.x + 5f,
                panelHeight - 35f,
                position.x + panelWidth - 35f,
                panelHeight - 5f,
                ImGui.colorConvertFloat4ToU32(0.2f, 0.2f, 0.2f, 1f).toLong(),
                ImGui.colorConvertFloat4ToU32(0.2f, 0.2f, 0.2f, 1f).toLong(),
                ImGui.colorConvertFloat4ToU32(0.3f, 0.3f, 0.3f, 1f).toLong(),
                ImGui.colorConvertFloat4ToU32(0.3f, 0.3f, 0.3f, 1f).toLong()
            )
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand)
        }

        drawList.addRectFilledMultiColor(
            position.x,
            panelHeight - 40f,
            position.x + panelWidth - 40f,
            panelHeight - 10f,
            ImGui.colorConvertFloat4ToU32(0.3f, 0.3f, 0.3f, 1f).toLong(),
            ImGui.colorConvertFloat4ToU32(0.3f, 0.3f, 0.3f, 1f).toLong(),
            ImGui.colorConvertFloat4ToU32(0.2f, 0.2f, 0.2f, 1f).toLong(),
            ImGui.colorConvertFloat4ToU32(0.2f, 0.2f, 0.2f, 1f).toLong()
        )

        //Draw the add variable text in the center
        textFont.use {
            val textSize = ImGui.calcTextSize("Add Variable")
            val textPosition = Vector2f(position.x + ((panelWidth - 40) - textSize.x) / 2, panelHeight - 35f)
            drawList.addText(
                textFont,
                24f,
                textPosition.x + 10f,
                textPosition.y - 5f,
                ImGui.colorConvertFloat4ToU32(1f, 1f, 1f, 1f),
                "Add Variable"
            )
        }

        if (ImGui.isItemClicked(0)) {
            ImGui.openPopup("Add Variable")
        }
        if (ImGui.beginPopup("Add Variable")) {
            ImGui.inputText("Name##variable", variableNameBuffer)
            ImGui.inputText("Type##variable", variableTypeBuffer)

            if (ImGui.button("Add")) {
                addVariable(variableNameBuffer.get(), variableTypeBuffer.get())
                ImGui.closeCurrentPopup()
            }
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
        val buttonWidth = 40f
        val buttonHeight = 20f
        val buttonSpacing = 5f
        val totalButtonWidth = buttonWidth * 2 + buttonSpacing

        val getButtonPos = Vector2f(
            startCursorPos.x + size.x - totalButtonWidth - 10f,
            startCursorPos.y + 10f
        )
        val setButtonPos = Vector2f(
            getButtonPos.x + buttonWidth + buttonSpacing,
            getButtonPos.y
        )

        // Get button
        drawButton(drawList, "Get", getButtonPos, buttonWidth, buttonHeight)
        if (ImGui.isMouseClicked(ImGuiMouseButton.Left) && isMouseOver(getButtonPos, buttonWidth, buttonHeight)) {
            draggedVariable = Triple(name, variable, "Get")
        }

        // Set button
        drawButton(drawList, "Set", setButtonPos, buttonWidth, buttonHeight)
        if (ImGui.isMouseClicked(ImGuiMouseButton.Left) && isMouseOver(setButtonPos, buttonWidth, buttonHeight)) {
            draggedVariable = Triple(name, variable, "Set")
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
        drawList.addText(textFont, 14f, textPos.x, textPos.y, ImColor.rgba(255, 255, 255, 255), label)
    }

    private fun isMouseOver(pos: Vector2f, width: Float, height: Float): Boolean {
        val mousePos = ImGui.getMousePos()
        return mousePos.x >= pos.x && mousePos.x <= pos.x + width &&
                mousePos.y >= pos.y && mousePos.y <= pos.y + height
    }

    private fun handleVariableInput(
        drawList: ImDrawList,
        name: String,
        variable: Property<*>,
        boxPosition: Vector2f,
        boxWidth: Float,
        yOffset: Float
    ) {
        // Render the property input, ensuring it doesn't interfere with the action menu
        val updated = PropertyInput.render(
            drawList,
            "##$name",
            variable,
            boxPosition.x + 10f,
            boxPosition.y + 35f + yOffset,
            boxWidth - 40f  // Reduced width to avoid overlapping with action menu
        )

        if (updated) {
            Client {
                it.send(VariableUpdateRequest(name, Property.Object().apply {
                    this["type"] = Property.String(variable::class.simpleName!!)
                    this["value"] = variable
                }))
            }
        }
    }

    private fun handleVariableDragging(drawList: ImDrawList) {
        val draggedVar = draggedVariable
        if (draggedVar != null && ImGui.isMouseDragging(ImGuiMouseButton.Left)) {
            val mousePos = ImGui.getMousePos()
            val (name, _, nodeType) = draggedVar

            // Draw a preview of the node header
            val headerWidth = 150f
            val headerHeight = 30f
            drawList.addRectFilled(
                mousePos.x, mousePos.y,
                mousePos.x + headerWidth, mousePos.y + headerHeight,
                ImColor.rgba(80, 80, 80, 200)
            )
            textFont.use {
                drawList.addText(
                    textFont, 14f,
                    mousePos.x + 5f, mousePos.y + 5f,
                    ImColor.rgba(255, 255, 255, 255),
                    "$nodeType $name"
                )
            }
        } else if (draggedVar != null && ImGui.isMouseReleased(ImGuiMouseButton.Left)) {
            val mousePos = Platform.getMousePosition()
            val (name, _, nodeType) = draggedVar

            when (nodeType) {
                "Get" -> canvasContext.createVariableNode(GetVariable, mousePos, name)
                "Set" -> canvasContext.createVariableNode(SetVariable, mousePos, name)
            }
            draggedVariable = null
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