package bpm.client.utils

import imgui.ImColor
import imgui.ImDrawList
import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiInputTextFlags
import bpm.common.logging.KotlinLogging
import bpm.client.font.Fonts
import bpm.common.property.*
import bpm.common.utils.FontAwesome
import imgui.flag.ImGuiStyleVar
import imgui.type.ImBoolean
import imgui.type.ImFloat
import imgui.type.ImInt
import imgui.type.ImString
import org.joml.*

object PropertyInput {

    private val fontAwesomeFamily = Fonts.getFamily("Fa")["Regular"]
    private val fontAwesome get() = fontAwesomeFamily[16]
    private val logger = KotlinLogging.logger {}

    fun render(
        drawList: ImDrawList,
        label: String,
        property: Property<*>,
        x: Float,
        y: Float,
        width: Float = 200f,
        height: Float = 20f,
        backgroundColor: Int = ImColor.rgba(45, 45, 45, 255),
        textColor: Int = ImColor.rgba(220, 220, 220, 255),
        accentColor: Int = ImColor.rgba(100, 100, 200, 255)
    ): Boolean {
        val icon = getPropertyIcon(property)
        val iconWidth = 20f

        // Draw custom background
        drawList.addRectFilled(x, y, x + width, y + height, backgroundColor)

        // Draw icon
        fontAwesome.use {
            drawList.addText(fontAwesome, 16f, x, y + 2, textColor, icon)
        }

        val inputX = x + iconWidth
        val inputWidth = width - iconWidth

        ImGui.pushID(label)
        ImGui.setCursorScreenPos(inputX, y)

        // Set colors for ImGui elements
        ImGui.pushStyleColor(ImGuiCol.FrameBg, 0) // Transparent background
        ImGui.pushStyleColor(ImGuiCol.Text, textColor)
        ImGui.pushStyleColor(ImGuiCol.TextSelectedBg, accentColor)
        ImGui.pushStyleColor(ImGuiCol.Border, 0) // Transparent border
        //Gray button background
        ImGui.pushStyleColor(ImGuiCol.Button, ImColor.rgba(60, 60, 60, 255))
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, ImColor.rgba(70, 70, 70, 255))
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, ImColor.rgba(80, 80, 80, 255))

        ImGui.pushStyleVar(ImGuiStyleVar.CellPadding, 6f, 6f)
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 6f)
        ImGui.pushStyleVar(ImGuiStyleVar.ChildRounding, 6f)

        val changed = when (property) {
            is Property.String -> renderStringInput(label, property, inputWidth)
            is Property.Int -> renderIntInput(label, property, inputWidth)
            is Property.Float -> renderFloatInput(label, property, inputWidth)
            is Property.Boolean -> renderBooleanInput(label, property, inputWidth, height)
            is Property.Vec2f -> renderVec2fInput(label, property, inputWidth)
            is Property.Vec3f -> renderVec3fInput(label, property, inputWidth)
            is Property.Vec4f -> renderVec4fInput(label, property, inputWidth)
            is Property.Vec4i -> renderVec4iInput(label, property, inputWidth)
            else -> false
        }

        ImGui.popStyleColor(7)
        ImGui.popStyleVar(3)
        ImGui.popID()

        return changed
    }

    private fun getPropertyIcon(property: Property<*>): String {
        return when (property) {
            is Property.String -> FontAwesome.Font
            is Property.Int -> FontAwesome.Hashtag
            is Property.Float -> FontAwesome.Percent
            is Property.Boolean -> FontAwesome.ToggleOn
            is Property.Vec2f -> FontAwesome.ArrowsUpDown
            is Property.Vec3f -> FontAwesome.Cube
            is Property.Vec4f -> FontAwesome.Cubes
            is Property.Vec4i -> FontAwesome.Paintbrush
            else -> FontAwesome.Question
        }
    }

    val buffer = ImString(256)

    private fun renderStringInput(label: String, property: Property.String, width: Float): Boolean {
        val changed = ImGui.inputText(label, buffer, ImGuiInputTextFlags.AlwaysOverwrite)
        if (changed) {
            property.set(buffer.toString())
        }
        return changed
    }

    private fun renderIntInput(label: String, property: Property.Int, width: Float): Boolean {
        val value = ImInt(property.get())
        ImGui.pushItemWidth(width)
        val changed = ImGui.inputInt(label, value)
        if (changed) {
            property.set(value.get())
        }
        return changed
    }

    private fun renderFloatInput(label: String, property: Property.Float, width: Float): Boolean {
        val value = ImFloat(property.get())
        val changed = ImGui.inputFloat(label, value)
        if (changed) {
            property.set(value.get())
        }
        return changed
    }

    private fun renderBooleanInput(label: String, property: Property.Boolean, width: Float, height: Float): Boolean {
        val value = ImBoolean(property.get())
        val changed = ImGui.checkbox(label, value)
        if (changed) {
            property.set(value.get())
        }
        return changed
    }

    private fun renderVec2fInput(label: String, property: Property.Vec2f, width: Float): Boolean {
        val value = floatArrayOf(property.get().x, property.get().y)
        val changed = ImGui.inputFloat2(label, value)
        if (changed) {
            property.set(Vector2f(value[0], value[1]))
        }
        return changed
    }

    private fun renderVec3fInput(label: String, property: Property.Vec3f, width: Float): Boolean {
        val value = floatArrayOf(property.get().x, property.get().y, property.get().z)
        val changed = ImGui.inputFloat3(label, value)
        if (changed) {
            property.set(Vector3f(value[0], value[1], value[2]))
        }
        return changed
    }

    private fun renderVec4fInput(label: String, property: Property.Vec4f, width: Float): Boolean {
        val value = floatArrayOf(property.get().x, property.get().y, property.get().z, property.get().w)
        val changed = ImGui.inputFloat4(label, value)
        if (changed) {
            property.set(Vector4f(value[0], value[1], value[2], value[3]))
        }
        return changed
    }

    private fun renderVec4iInput(label: String, property: Property.Vec4i, width: Float): Boolean {
        val value = intArrayOf(property.get().x, property.get().y, property.get().z, property.get().w)
        val changed = ImGui.inputInt4(label, value)
        if (changed) {
            property.set(Vector4i(value[0], value[1], value[2], value[3]))
        }
        return changed
    }
}