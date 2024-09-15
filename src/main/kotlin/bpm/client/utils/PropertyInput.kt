package bpm.client.utils

import imgui.flag.ImGuiCol
import imgui.flag.ImGuiInputTextFlags
import bpm.common.logging.KotlinLogging
import bpm.client.font.Fonts
import bpm.client.runtime.Platform
import bpm.common.property.*
import bpm.common.utils.FontAwesome
import imgui.*
import imgui.flag.ImGuiMouseCursor
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
        accentColor: Int = ImColor.rgba(100, 100, 200, 255),
        font: ImFont = Fonts["Inter-Bold-24"]
    ): Boolean {
        val icon = getPropertyIcon(property)
        val iconWidth = 20f

        // Draw custom background
        drawList.addRectFilled(x - 5f, y - 2f, x + width, y + height + 6f, backgroundColor, 10f)
        //Draws a line above the input
        drawList.addLine(x - 15f, y - 5f, x + width + 35f, y - 5f, ImColor.rgba(0, 0, 0, 255), 1f)

        // Draw custom icon
        val iconType = when (property) {
            is Property.String -> "string"
            is Property.Int -> "int"
            is Property.Float -> "float"
            is Property.Boolean -> "boolean"
            is Property.Vec2f -> "vec2f"
            is Property.Vec3f -> "vec3f"
            is Property.Vec4f -> "vec4f"
            is Property.Vec4i -> "vec4i"
            else -> "default"
        }
        drawCustomIcon(drawList, x, y, iconType)

        val inputX = x + iconWidth
        val inputWidth = width - iconWidth

        ImGui.pushID(label)
        ImGui.setCursorScreenPos(inputX, y - 5f)
        return font.use {

            //        //draw separator line between icon and input
            drawList.addLine(
                ImGui.getCursorScreenPos().x + 10,
                ImGui.getCursorScreenPos().y + 2f,
                ImGui.getCursorScreenPos().x + 10,
                ImGui.getCursorScreenPos().y + 30f,
                ImColor.rgba(100, 100, 100, 255),
                2f
            )
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
                is Property.String -> renderStringInput(drawList, label, property, inputWidth)
                is Property.Int -> renderIntInput(drawList, label, property, inputWidth)
                is Property.Float -> renderFloatInput(drawList, label, property, inputWidth)
                is Property.Boolean -> renderBooleanInput(drawList, label, property, inputWidth, height)
                is Property.Vec2f -> renderVec2fInput(drawList, label, property, inputWidth)
                is Property.Vec3f -> renderVec3fInput(drawList, label, property, inputWidth)
                is Property.Vec4f -> renderVec4fInput(drawList, label, property, inputWidth)
                is Property.Vec4i -> renderVec4iInput(drawList, label, property, inputWidth)
                else -> false
            }

            ImGui.popStyleColor(7)
            ImGui.popStyleVar(3)
            ImGui.popID()
            changed
        }

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

    private fun renderStringInput(
        drawList: ImDrawList,
        label: String,
        property: Property.String,
        width: Float
    ): Boolean {
//        drawList.addText(
//            fontAwesomeFamily[40],
//            40f,
//            ImGui.getCursorScreenPos().x - 16f,
//            ImGui.getCursorScreenPos().y - 11f,
//            ImColor.rgba(255, 255, 255, 255),
//            FontAwesome.Font
//        )
//

        ImGui.pushItemWidth(width)
        ImGui.setCursorScreenPos(ImGui.getCursorScreenPos().x + 10, ImGui.getCursorScreenPos().y)
        val changed = ImGui.inputText(label, buffer, ImGuiInputTextFlags.AlwaysOverwrite)
        if (changed) {
            property.set(buffer.toString())
        }
        return changed
    }

    private fun renderIntInput(drawList: ImDrawList, label: String, property: Property.Int, width: Float): Boolean {
        ImGui.pushItemWidth(width - 15)
        ImGui.setCursorScreenPos(ImGui.getCursorScreenPos().x + 10, ImGui.getCursorScreenPos().y)
        val value = ImInt(property.get())
        val changed = ImGui.inputInt(label, value)
        if (changed) {
            property.set(value.get())
        }
        return changed
    }

    private var isDraggingFloat = false
    private var dragLockX: Float? = null
    private var targetItem: Int = 0
    private var prevMousePos: ImVec2 = ImVec2()

    private fun wrapMousePosEx(axisesMask: Int, wrapRect: ImVec4) {
        val mousePos = ImGui.getMousePos()
        val pMouse = ImVec2(mousePos.x, mousePos.y)

        for (axis in 0..1) {
            if ((axisesMask and (1 shl axis)) == 0) continue
            val mouse = if (axis == 0) pMouse.x else pMouse.y
            fun setMouse(value: Float) {
                if (axis == 0) pMouse.x = value else pMouse.y = value
            }
            if (mouse >= wrapRect.w) {
                setMouse(wrapRect.x + 1f)
            } else if (mouse <= wrapRect.x) {
                setMouse(wrapRect.w - 1f)
            }
        }

        if (pMouse.x != mousePos.x || pMouse.y != mousePos.y) {
            Platform.setMousePosition(pMouse.x.toDouble(), pMouse.y.toDouble())
        }
    }

    private fun wrapMousePos(axisesMask: Int) {
        val viewport = ImGui.getMainViewport()
        val wrapRect = ImVec4(
            viewport.pos.x,
            viewport.pos.y,
            viewport.pos.x + viewport.size.x - 1f,
            viewport.pos.y + viewport.size.y - 1f
        )
        wrapMousePosEx(axisesMask, wrapRect)
    }

    private fun activeItemLockMousePos(id: Int) {
        if (ImGui.isItemActive()) {
            if (targetItem == 0) {
                targetItem = id
                prevMousePos = ImGui.getMousePos()
            }
            wrapMousePos(1 shl 0) // ImGuiAxis_X
            ImGui.setMouseCursor(ImGuiMouseCursor.None)
        } else if (targetItem > 0 && targetItem == id && ImGui.isItemDeactivated()) {
            Platform.setMousePosition(prevMousePos.x.toDouble(), prevMousePos.y.toDouble())
            targetItem = 0
        }
    }

    private fun renderFloatInput(drawList: ImDrawList, label: String, property: Property.Float, width: Float): Boolean {
        ImGui.pushItemWidth(width - 15)
        val cursorPos = ImGui.getCursorScreenPos()
        ImGui.setCursorScreenPos(cursorPos.x + 10, cursorPos.y)
        val value = floatArrayOf(property.get())
        val changed = ImGui.dragFloat(label, value, 0.1f)

        if (changed) {
            property.set(value[0])
        }

        return changed
    }

    private fun renderBooleanInput(
        drawList: ImDrawList,
        label: String,
        property: Property.Boolean,
        width: Float,
        height: Float
    ): Boolean {
        val value = ImBoolean(property.get())
        val cursorPos = ImGui.getCursorScreenPos()
        cursorPos.set(cursorPos.x, cursorPos.y + 7f)
        val toggleWidth = 40f
        val toggleHeight = 20f
        //If hovered, change mouse
        if (ImGui.isMouseHoveringRect(cursorPos.x, cursorPos.y, cursorPos.x + width, cursorPos.y + height)) {
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand)
        }

        // Draw "True" or "False" text on the left
        val textColor = ImColor.rgba(220, 220, 220, 255)
        drawList.addText(
            Fonts["Inter-Bold-24"],
            24f,
            cursorPos.x + 15f,
            cursorPos.y - 2f,
            textColor,
            if (value.get()) "True" else "False"
        )

        // Draw custom toggle switch
        val toggleX = cursorPos.x + (width - toggleWidth) - 10f
        val toggleY = cursorPos.y + (height - toggleHeight) / 2
        drawList.addRectFilled(
            toggleX,
            toggleY,
            toggleX + toggleWidth,
            toggleY + toggleHeight,
            ImColor.rgba(100, 100, 100, 255),
            toggleHeight / 2
        )

        val knobSize = toggleHeight - 4f
        val knobX = if (value.get()) toggleX + toggleWidth - knobSize - 2f else toggleX + 2f
        drawList.addCircleFilled(
            knobX + knobSize / 2,
            toggleY + toggleHeight / 2,
            knobSize / 2,
            if (value.get()) ImColor.rgba(50, 205, 50, 255) else ImColor.rgba(220, 20, 60, 255)
        )

        // Handle input
        ImGui.invisibleButton(label, width, height)
        if (ImGui.isItemClicked()) {
            value.set(!value.get())
            property.set(value.get())
            return true
        }

        return false
    }

    private fun renderVec2fInput(
        drawList: ImDrawList,
        label: String,
        property: Property.Vec2f,
        width: Float
    ): Boolean {
        val value = floatArrayOf(property.get().x, property.get().y)
        val changed = ImGui.inputFloat2(label, value)
        if (changed) {
            property.set(Vector2f(value[0], value[1]))
        }
        return changed
    }

    private fun renderVec3fInput(
        drawList: ImDrawList,
        label: String,
        property: Property.Vec3f,
        width: Float
    ): Boolean {
        val value = floatArrayOf(property.get().x, property.get().y, property.get().z)
        ImGui.pushItemWidth(width - 15)
        ImGui.setCursorScreenPos(ImGui.getCursorScreenPos().x + 10, ImGui.getCursorScreenPos().y)
        val changed = ImGui.inputFloat3(label, value)

        if (changed) {
            property.set(Vector3f(value[0], value[1], value[2]))
        }
        return changed
    }

    private fun renderVec4fInput(
        drawList: ImDrawList,
        label: String,
        property: Property.Vec4f,
        width: Float
    ): Boolean {
        val value = floatArrayOf(property.get().x, property.get().y, property.get().z, property.get().w)
        val changed = ImGui.inputFloat4(label, value)
        if (changed) {
            property.set(Vector4f(value[0], value[1], value[2], value[3]))
        }
        return changed
    }

    private fun renderVec4iInput(
        drawList: ImDrawList,
        label: String,
        property: Property.Vec4i,
        width: Float
    ): Boolean {
        val value = intArrayOf(property.get().x, property.get().y, property.get().z, property.get().w)
        val changed = ImGui.inputInt4(label, value)
        if (changed) {
            property.set(Vector4i(value[0], value[1], value[2], value[3]))
        }
        return changed
    }


    private fun drawCustomIcon(drawList: ImDrawList, x: Float, y: Float, iconType: String) {
        val iconSize = 24f
        val centerX = x + iconSize / 2
        val centerY = y + iconSize / 2

        when (iconType) {
            "string" -> drawStringIcon(drawList, centerX, centerY, iconSize)
            "int" -> drawIntIcon(drawList, centerX, centerY, iconSize)
            "float" -> drawFloatIcon(drawList, centerX, centerY, iconSize)
            "boolean" -> drawBooleanIcon(drawList, centerX, centerY, iconSize)
            "vec2f" -> drawVec2fIcon(drawList, centerX, centerY, iconSize)
            "vec3f" -> drawVec3fIcon(drawList, centerX, centerY, iconSize)
            "vec4f" -> drawVec4fIcon(drawList, centerX, centerY, iconSize)
            "vec4i" -> drawVec4iIcon(drawList, centerX, centerY, iconSize)
            else -> drawDefaultIcon(drawList, centerX, centerY, iconSize)
        }
    }

    private fun drawStringIcon(drawList: ImDrawList, centerX: Float, centerY: Float, size: Float) {
        val baseColor = ImColor.rgba(65, 105, 225, 255) // Royal Blue
        val accentColor = ImColor.rgba(255, 255, 255, 255) // White
        val backgroundColor = ImColor.rgba(30, 30, 30, 255) // Dark background

        // Draw a dark circular background
        drawList.addCircleFilled(centerX, centerY, size / 2, backgroundColor)

        // Draw a blue ring
        drawList.addCircle(centerX, centerY, size / 2 - 1, baseColor, 32, 2f)

        // Draw quotation marks
        drawList.addText(
            fontAwesome,
            20f,
            centerX - size / 8 - 1f,
            centerY - size / 2,
            accentColor,
            FontAwesome.QuoteLeft
        )
//        drawList.addText(centerX , centerY - size / 4, accentColor, "\"")

        // Add subtle glow effect
        for (i in 1..3) {
            val alpha = 100 - i * 30
            drawList.addCircle(centerX, centerY, size / 2 + i, ImColor.rgba(65, 105, 225, alpha), 32, 1f)
        }
    }


    private fun drawIntIcon(drawList: ImDrawList, centerX: Float, centerY: Float, size: Float) {
        val baseColor = ImColor.rgba(0, 180, 0, 255) // Bright green color
        val accentColor = ImColor.rgba(0, 220, 0, 255) // Lighter green for accents
        val backgroundColor = ImColor.rgba(30, 30, 30, 255) // Dark background

        // Draw a dark circular background
        drawList.addCircleFilled(centerX, centerY, size / 2, backgroundColor)

        // Draw a green ring
        drawList.addCircle(centerX, centerY, size / 2 - 1, baseColor, 32, 2f)
        //Draws a hash tag like italics symbol
        val left = ImVec2(centerX - size / 4, centerY - size / 4)
        val right = ImVec2(centerX + size / 4, centerY + size / 4)
        drawList.addLine(left.x - 1, left.y + 3, right.x, left.y + 3, ImColor.rgba(220, 220, 220, 255), 2f)
        drawList.addLine(left.x - 1, left.y + 8, right.x, left.y + 8, ImColor.rgba(220, 220, 220, 255), 2f)
        drawList.addLine(left.x + 3, left.y - 1, left.x + 1, right.y, ImColor.rgba(220, 220, 220, 255), 2f)
        drawList.addLine(right.x - 3, left.y - 1, right.x - 5, right.y, ImColor.rgba(220, 220, 220, 255), 2f)
        //Draw lines across the hash tag


        // Add subtle glow effect
        for (i in 1..3) {
            val alpha = 100 - i * 30
            drawList.addCircle(centerX, centerY, size / 2 + i, ImColor.rgba(0, 255, 0, alpha), 32, 1f)
        }
    }

    private fun drawFloatIcon(drawList: ImDrawList, centerX: Float, centerY: Float, size: Float) {
        val baseColor = ImColor.rgba(30, 144, 255, 255) // Dodger Blue
        val accentColor = ImColor.rgba(255, 255, 255, 255) // White
        val backgroundColor = ImColor.rgba(30, 30, 30, 255) // Dark background

// Draw a dark circular background
        drawList.addCircleFilled(centerX, centerY, size / 2, backgroundColor)

        // Draw a blue ring
        drawList.addCircle(centerX, centerY, size / 2 - 1, baseColor, 32, 2f)
        val size = 18f
        // Draw a sine wave
        val wavePoints = Array(21) { i ->
            val x = centerX - size / 2 + i * size / 20
            val y = centerY + (Math.sin(i * Math.PI / 5) * size / 6).toFloat()
            ImVec2(x, y)
        }
        drawList.addPolyline(wavePoints, 21, accentColor, 0, 2f)

        // Add subtle glow effect
        for (i in 1..3) {
            val alpha = 100 - i * 30
            drawList.addCircle(centerX, centerY, size / 2 + i, ImColor.rgba(30, 144, 255, alpha), 32, 1f)
        }
    }

    private fun drawBooleanIcon(drawList: ImDrawList, centerX: Float, centerY: Float, size: Float) {
        // Purple purple background
        val backgroundColor = ImColor.rgba(138, 43, 226, 255)


        // Draw a dark circular background
        drawList.addCircleFilled(centerX, centerY, size / 2, backgroundColor)
        val innerColor = ImColor.rgba(255, 255, 255, 255)
        // Draw a split-color ring
        drawList.addCircle(centerX, centerY, size / 2 - 1, innerColor, 32, 2f)
        drawList.addText(
            fontAwesome,
            22f,
            centerX - size / 4,
            centerY - size / 2,
            ImColor.rgba(255, 255, 255, 255),
            FontAwesome.ToggleOn
        )

        // Add subtle glow effect
        for (i in 1..3) {
            val alpha = 100 - i * 30
            val color = ImColor.rgba(110, 0, 255, alpha)
            drawList.addCircle(centerX, centerY, size / 2 + i, color, 32, 1f)
        }
    }

    private fun drawVec2fIcon(drawList: ImDrawList, centerX: Float, centerY: Float, size: Float) {
        val xColor = ImColor.rgba(220, 20, 60, 255) // Crimson
        val yColor = ImColor.rgba(65, 105, 225, 255) // Royal Blue
        val tipColor = ImColor.rgba(255, 215, 0, 255) // Gold

        // Draw x and y axes
        drawList.addLine(centerX - size / 2, centerY, centerX + size / 2, centerY, xColor, 2f)
        drawList.addLine(centerX, centerY + size / 2, centerX, centerY - size / 2, yColor, 2f)

        // Draw arrowheads
        drawList.addTriangleFilled(
            centerX + size / 2, centerY,
            centerX + size / 3, centerY - size / 8,
            centerX + size / 3, centerY + size / 8,
            xColor
        )
        drawList.addTriangleFilled(
            centerX, centerY - size / 2,
            centerX - size / 8, centerY - size / 3,
            centerX + size / 8, centerY - size / 3,
            yColor
        )

        // Draw a point
        drawList.addCircleFilled(centerX + size / 4, centerY - size / 4, 3f, tipColor)
    }

    private fun drawVec3fIcon(drawList: ImDrawList, centerX: Float, centerY: Float, size: Float) {
        val xColor = ImColor.rgba(220, 20, 60, 255) // Crimson
        val yColor = ImColor.rgba(65, 105, 225, 255) // Royal Blue
        val zColor = ImColor.rgba(50, 205, 50, 255) // Lime Green

        // Draw a 3D coordinate system
        drawList.addLine(centerX, centerY, centerX + size / 2, centerY + size / 4, xColor, 2f)
        drawList.addLine(centerX, centerY, centerX - size / 3, centerY - size / 2, yColor, 2f)
        drawList.addLine(centerX, centerY, centerX - size / 3, centerY + size / 2, zColor, 2f)

        // Draw arrowheads
        drawList.addTriangleFilled(
            centerX + size / 2, centerY + size / 4,
            centerX + size / 3, centerY + size / 8,
            centerX + size / 3, centerY + size / 3,
            xColor
        )
        drawList.addTriangleFilled(
            centerX - size / 3, centerY - size / 2,
            centerX - size / 4, centerY - size / 3,
            centerX - size / 5, centerY - size / 3,
            yColor
        )
        drawList.addTriangleFilled(
            centerX - size / 3, centerY + size / 2,
            centerX - size / 4, centerY + size / 3,
            centerX - size / 5, centerY + size / 3,
            zColor
        )
    }

    private fun drawVec4fIcon(drawList: ImDrawList, centerX: Float, centerY: Float, size: Float) {
        val colors = arrayOf(
            ImColor.rgba(220, 20, 60, 255), // Crimson
            ImColor.rgba(65, 105, 225, 255), // Royal Blue
            ImColor.rgba(50, 205, 50, 255), // Lime Green
            ImColor.rgba(255, 215, 0, 255)  // Gold
        )

        // Draw four interlocking circles
        for (i in 0 until 4) {
            val angle = Math.PI / 2 * i
            val x = (centerX + size / 4 * Math.cos(angle)).toFloat()
            val y = (centerY + size / 4 * Math.sin(angle)).toFloat()
            drawList.addCircle(x, y, size / 3, colors[i], 32, 2f)
        }

        // Add a central connecting dot
        drawList.addCircleFilled(centerX, centerY, 3f, ImColor.rgba(255, 255, 255, 255))
    }

    private fun drawVec4iIcon(drawList: ImDrawList, centerX: Float, centerY: Float, size: Float) {
        val baseColor = ImColor.rgba(138, 43, 226, 255) // Blue Violet
        val numberColor = ImColor.rgba(255, 255, 255, 255) // White

        // Draw a rounded square background
        drawList.addRectFilled(
            centerX - size / 2, centerY - size / 2,
            centerX + size / 2, centerY + size / 2,
            baseColor, size / 8
        )

        // Draw four numbers in each corner
        val numberSize = size / 4
        drawList.addText(centerX - size / 3, centerY - size / 3, numberColor, "1")
        drawList.addText(centerX + size / 5, centerY - size / 3, numberColor, "2")
        drawList.addText(centerX - size / 3, centerY + size / 6, numberColor, "3")
        drawList.addText(centerX + size / 5, centerY + size / 6, numberColor, "4")

        // Add connecting lines
        drawList.addLine(
            centerX - size / 6,
            centerY - size / 6,
            centerX + size / 6,
            centerY + size / 6,
            numberColor,
            1f
        )
        drawList.addLine(
            centerX + size / 6,
            centerY - size / 6,
            centerX - size / 6,
            centerY + size / 6,
            numberColor,
            1f
        )
    }

    private fun drawDefaultIcon(drawList: ImDrawList, centerX: Float, centerY: Float, size: Float) {
        val baseColor = ImColor.rgba(128, 128, 128, 255) // Gray
        val accentColor = ImColor.rgba(255, 255, 255, 255) // White

        // Draw a gear-like shape
        val numTeeth = 8
        val outerRadius = size / 2
        val innerRadius = size / 3
        val points = mutableListOf<ImVec2>()

        for (i in 0 until numTeeth * 2) {
            val angle = Math.PI * i / numTeeth
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            points.add(
                ImVec2(
                    (centerX + radius * Math.cos(angle)).toFloat(),
                    (centerY + radius * Math.sin(angle)).toFloat()
                )
            )
        }

        drawList.addConvexPolyFilled(points.toTypedArray(), points.size, baseColor)
        drawList.addCircleFilled(centerX, centerY, size / 6, accentColor)
    }
}