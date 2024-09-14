package bpm.client.runtime.panel

import bpm.client.font.Fonts
import bpm.client.runtime.ClientRuntime
import bpm.client.runtime.ClientRuntime.logger
import bpm.client.utils.use
import bpm.common.network.Client
import bpm.common.property.Property
import bpm.common.utils.FontAwesome
import bpm.common.workspace.packets.VariableCreateRequest
import imgui.ImColor
import imgui.ImDrawList
import imgui.ImGui
import imgui.flag.ImGuiMouseCursor
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImString
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.Vector4i

data object VariablesPanel : Panel("Variables", "\uf1ec") {

    private val workspace get() = ClientRuntime.workspace ?: error("Workspace not available")
    private val font = Fonts.getFamily("Inter")["Regular"][32]
    private val faFont = Fonts.getFamily("Fa")["Regular"][18]

    private var variableValueBuffer = ImString(256)
    private val variableNameBuffer = ImString(256)
    private val variableTypeBuffer = ImString(256)

    override fun renderBody(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        ImGui.setNextWindowPos(position.x, position.y)
        ImGui.setNextWindowSize(size.x - 20f, size.y - 40f)
        ImGui.pushClipRect(position.x, position.y, position.x + size.x, position.y + size.y - 100f, true)
        if (ImGui.beginChild("VariablesScrollRegion", size.x - 20f, size.y - 100f, true)) {

            workspace.graph.variables.forEach { (name, variable) ->
                renderVariable(drawList, name, variable, Vector2f(size.x - 40f, 70f))
                ImGui.dummy(0f, 10f) // Add some space between variables
            }
        }
        ImGui.endChild()
        ImGui.popClipRect()

        renderAddVariable(drawList, Vector2f(position.x, position.y + size.y))
    }

    private fun renderVariable(
        drawList: ImDrawList,
        name: String,
        variable: Property<*>,
        size: Vector2f
    ) {
        val startCursorPos = ImGui.getCursorScreenPos()

        // Background
        drawList.addRectFilled(
            startCursorPos.x,
            startCursorPos.y,
            startCursorPos.x + size.x,
            startCursorPos.y + size.y,
            ImGui.colorConvertFloat4ToU32(0.3f, 0.3f, 0.3f, 1f)
        )

        // Delete button (left side)
        ImGui.setCursorScreenPos(startCursorPos.x + 5f, startCursorPos.y + 5f)
        if (ImGui.invisibleButton("delete_$name", 25f, 25f)) {
            // TODO: Implement delete functionality
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
                ImGui.colorConvertFloat4ToU32(1f, 1f, 1f, 1f),
                "\uf1f8"  // FontAwesome trash icon
            )
        }

        // Name (centered)
        val textSize = ImGui.calcTextSize(name)
        val centerX = startCursorPos.x + (size.x - textSize.x) / 2
        textFont.use {
            drawList.addText(
                textFont,
                18f,
                centerX,
                startCursorPos.y + 10f,
                ImGui.colorConvertFloat4ToU32(1f, 1f, 1f, 1f),
                name
            )
        }

        // Value input
        ImGui.setCursorScreenPos(startCursorPos.x + 10f, startCursorPos.y + 35f)
        ImGui.pushItemWidth(size.x - 50f)
        if (ImGui.inputText("##value_$name", variableValueBuffer)) {
            variableValueBuffer.get().trim().let {
                //TODO: Parse the input and set the variable value
                logger.info("Variable value changed to: $it")
            }
        }
        ImGui.popItemWidth()

        // Additional buttons (stacked on right side)
        val buttonSize = 25f
        val buttonSpacing = 5f
        val rightSideX = startCursorPos.x + size.x - buttonSize - 5f

        // Top button
        ImGui.setCursorScreenPos(rightSideX, startCursorPos.y + 5f)
        if (ImGui.invisibleButton("button1_$name", buttonSize, buttonSize)) {
            // TODO: Implement button 1 functionality
        }
        if (ImGui.isItemHovered()) {
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand)
        }
        faFont.use {
            drawList.addText(
                faFont,
                18f,
                rightSideX + 5f,
                startCursorPos.y + 8f,
                ImGui.colorConvertFloat4ToU32(1f, 1f, 1f, 1f),
                "\uf013"  // FontAwesome cog icon
            )
        }

        // Bottom button
        ImGui.setCursorScreenPos(rightSideX, startCursorPos.y - buttonSize - buttonSpacing)
        if (ImGui.invisibleButton("button2_$name", buttonSize, buttonSize)) {
            // TODO: Implement button 2 functionality
        }
        if (ImGui.isItemHovered()) {
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand)
        }
        faFont.use {
            drawList.addText(
                faFont,
                18f,
                rightSideX + 5f,
                startCursorPos.y + buttonSize + buttonSpacing + 8f,
                ImGui.colorConvertFloat4ToU32(1f, 1f, 1f, 1f),
                "\uf0c9"  // FontAwesome bars icon
            )
        }

        // Move cursor to the end of this variable's area
        ImGui.setCursorScreenPos(startCursorPos.x, startCursorPos.y + size.y)
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
            it.send(VariableCreateRequest(name,
                Property.Object().apply {
                    this["type"] = Property.String(type)
                    this["value"] = property
                }
            ))
        }
    }
}