package bpm.client.utils

import imgui.ImColor
import imgui.ImDrawList
import imgui.ImGui
import bpm.common.logging.KotlinLogging
import bpm.client.font.Fonts
import bpm.common.property.*
import bpm.common.utils.FontAwesome
import bpm.common.utils.Random
import org.joml.*

object PropertyInput {

    private val buffers = mutableMapOf<String, Any>()
    private val fontAwesomeFamily = Fonts.getFamily("Fa")["Regular"]
    private val fontAwesome get() = fontAwesomeFamily[16]
    private var focusedProperty: String? = null
    private var lastClickTime: Long = 0
    private var lastClickedProperty: String? = null
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
        fontAwesome.use {
            drawList.addText(fontAwesome, 16f, x, y + 2, textColor, icon)
        }

        val inputX = x + iconWidth
        val inputWidth = width - iconWidth

        val changed = when (property) {
            is Property.String -> renderStringInput(
                drawList,
                label,
                property,
                inputX,
                y,
                inputWidth,
                height,
                backgroundColor,
                textColor,
                accentColor
            )

            is Property.Int -> renderIntInput(
                drawList,
                label,
                property,
                inputX,
                y,
                inputWidth,
                height,
                backgroundColor,
                textColor,
                accentColor
            )

            is Property.Float -> renderFloatInput(
                drawList,
                label,
                property,
                inputX,
                y,
                inputWidth,
                height,
                backgroundColor,
                textColor,
                accentColor
            )

            is Property.Boolean -> renderBooleanInput(
                drawList,
                label,
                property,
                inputX,
                y,
                height,
                backgroundColor,
                textColor,
                accentColor
            )

            is Property.Vec2f -> renderVec2fInput(
                drawList,
                label,
                property,
                inputX,
                y,
                inputWidth,
                height,
                backgroundColor,
                textColor,
                accentColor
            )

            is Property.Vec3f -> renderVec3fInput(
                drawList,
                label,
                property,
                inputX,
                y,
                inputWidth,
                height,
                backgroundColor,
                textColor,
                accentColor
            )

            is Property.Vec4f -> renderVec4fInput(
                drawList,
                label,
                property,
                inputX,
                y,
                inputWidth,
                height,
                backgroundColor,
                textColor,
                accentColor
            )

            is Property.Vec4i -> renderVec4iInput(
                drawList,
                label,
                property,
                inputX,
                y,
                inputWidth,
                height,
                backgroundColor,
                textColor,
                accentColor
            )

            else -> false
        }

        handleFocus(label, x, y, width, height)

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

    private fun handleFocus(label: String, x: Float, y: Float, width: Float, height: Float) {
        val mousePos = ImGui.getMousePos()
        val isHovered = mousePos.x >= x && mousePos.x <= x + width && mousePos.y >= y && mousePos.y <= y + height

        if (ImGui.isMouseClicked(0)) {
            if (isHovered) {
                val currentTime = System.currentTimeMillis()
                if (label == lastClickedProperty && currentTime - lastClickTime < 300) {
                    //select

                    focusedProperty = label
                } else {
                    focusedProperty = label
                }
                lastClickTime = currentTime
                lastClickedProperty = label
            } else if (focusedProperty == label) {
                focusedProperty = null
            }
        }
    }

    private fun renderStringInput(
        drawList: ImDrawList,
        label: String,
        property: Property.String,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        backgroundColor: Int,
        textColor: Int,
        accentColor: Int
    ): Boolean {
        val state = buffers.getOrPut("$label-string") { TextInputState(property.get()) } as TextInputState
        if (state.text != property.get()) {
            state.text = property.get()
            //Set the cursor to the end of the text
            state.cursorPosition = state.text.length
        }
        val font = ImGui.getFont()
        val fontSize = ImGui.getFontSize()
        val selectionColor = ImColor.rgba(100, 100, 200, 100)
        val wasFocused = state.isFocused
        state.isFocused = focusedProperty == label
        val updatedState = handleUniversalTextInput(
            ImGui.getForegroundDrawList(), state, font, fontSize.toFloat(), x, y, width, height,
            textColor, backgroundColor, selectionColor, InputType.TEXT, state.placeHolder
        )
        if (!wasFocused && (state.isFocused || updatedState.isFocused)) {
            // Reset cursor position when focused to the end of the text
//            updatedState.placeHolder = ""
        }

        buffers["$label-string"] = updatedState
        if (updatedState.text.isBlank() && updatedState.placeHolder.isBlank()) {
            updatedState.placeHolder = Random.randomSentence()
        }
        if (updatedState.text != property.get()) {
            property.set(updatedState.text)
            //regenerate the place holder when focused
            updatedState.placeHolder = ""
            return true
        }
        return false
    }

    private fun renderIntInput(
        drawList: ImDrawList,
        label: String,
        property: Property.Int,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        backgroundColor: Int,
        textColor: Int,
        accentColor: Int
    ): Boolean {
        val state = when (val existingState = buffers["$label-int"]) {
            is InputState -> existingState
            else -> InputState(
                text = property.get().toString(),
                value = property.get().toFloat()
            )
        }

//        if (state.text != property.get().toString() || state.value.toInt() != property.get()) {
//            state.text = property.get().toString()
//            state.value = property.get().toFloat()
//            state.cursorPosition = state.text.length
//        }

        state.isFocused = focusedProperty == label
        val placeholder = ""

        val updatedState = handleIntInput(
            drawList, state, ImGui.getFont(), ImGui.getFontSize().toFloat(), x, y, width, height,
            textColor, backgroundColor, accentColor, placeholder
        )




        buffers["$label-int"] = updatedState

        if (updatedState.value.toInt() != property.get()) {
            try {
                property.set(updatedState.value.toInt())

                return true
            } catch (e: Exception) {
                // Log error or handle exception
            }
        }
        return false
    }

    private fun renderFloatInput(
        drawList: ImDrawList,
        label: String,
        property: Property.Float,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        backgroundColor: Int,
        textColor: Int,
        accentColor: Int
    ): Boolean {
        val state = when (val existingState = buffers["$label-float"]) {
            is InputState -> existingState
            else -> InputState(
                text = property.get().toString(),
                value = property.get().toFloat()
            )
        }

        state.isFocused = focusedProperty == label
        val placeholder = ""

        val updatedState = handleFloatInput(
            drawList, state, ImGui.getFont(), ImGui.getFontSize().toFloat(), x, y, width, height,
            textColor, backgroundColor, accentColor, placeholder
        )
        buffers["$label-float"] = updatedState

        if (updatedState.value != property.get()) {
            try {
                property.set(updatedState.value)
                return true
            } catch (e: Exception) {
                // Log error or handle exception
            }
        }
        return false
    }


    private fun renderBooleanInput(
        drawList: ImDrawList,
        label: String,
        property: Property.Boolean,
        x: Float,
        y: Float,
        height: Float,
        backgroundColor: Int,
        textColor: Int,
        accentColor: Int
    ): Boolean {
        val state = buffers.getOrPut("$label-boolean") { BooleanInputState(property.get()) } as BooleanInputState

        state.isFocused = focusedProperty == label

        val updatedState = handleBooleanInput(
            drawList, state, fontAwesome, 18f, x, y, height * 2, height, textColor, backgroundColor, accentColor
        )

        buffers["$label-boolean"] = updatedState

        if (updatedState.value != property.get()) {
            property.set(updatedState.value)
            return true
        }
        return false
    }

    private val scratchVec2f = Vector2f()
    private fun renderVec2fInput(
        drawList: ImDrawList,
        label: String,
        property: Property.Vec2f,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        backgroundColor: Int,
        textColor: Int,
        accentColor: Int
    ): Boolean {
        val stateX = buffers.getOrPut("$label-vec2-x") {
            InputState(
                text = property.get().x.toString(),
                value = property.get().x
            )
        } as InputState

        val stateY = buffers.getOrPut("$label-vec2-y") {
            InputState(
                text = property.get().y.toString(),
                value = property.get().y
            )
        } as InputState

        val componentWidth = width / 2 - 5f

        // X component
        drawList.addText(
            ImGui.getFont(),
            ImGui.getFontSize().toFloat(),
            x,
            y - ImGui.getFontSize() - 2,
            textColor,
            "X:"
        )
        val updatedStateX = handleFloatInput(
            drawList, stateX, ImGui.getFont(), ImGui.getFontSize().toFloat(), x, y, componentWidth, height,
            textColor, backgroundColor, accentColor, "0.0"
        )

        // Y component
        drawList.addText(
            ImGui.getFont(),
            ImGui.getFontSize().toFloat(),
            x + componentWidth + 5,
            y - ImGui.getFontSize() - 2,
            textColor,
            "Y:"
        )
        val updatedStateY = handleFloatInput(
            drawList, stateY, ImGui.getFont(),
            ImGui.getFontSize().toFloat(), x + componentWidth + 5, y, componentWidth, height,
            textColor, backgroundColor, accentColor, "0.0"
        )

        buffers["$label-vec2-x"] = updatedStateX
        buffers["$label-vec2-y"] = updatedStateY

        val newVec2f = Vector2f(updatedStateX.value, updatedStateY.value)
        if (newVec2f != property.get()) {
            property.set(newVec2f)
            return true
        }
        return false
    }

    private fun renderVec3fInput(
        drawList: ImDrawList,
        label: String,
        property: Property.Vec3f,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        backgroundColor: Int,
        textColor: Int,
        accentColor: Int
    ): Boolean {
        val stateX = buffers.getOrPut("$label-vec3-x") {
            InputState(
                text = property.get().x.toString(),
                value = property.get().x
            )
        } as InputState

        val stateY = buffers.getOrPut("$label-vec3-y") {
            InputState(
                text = property.get().y.toString(),
                value = property.get().y
            )
        } as InputState

        val stateZ = buffers.getOrPut("$label-vec3-z") {
            InputState(
                text = property.get().z.toString(),
                value = property.get().z
            )
        } as InputState

        val componentWidth = width / 3 - 5f

        // X component
        drawList.addText(
            ImGui.getFont(),
            ImGui.getFontSize().toFloat(),
            x,
            y - ImGui.getFontSize() - 2,
            textColor,
            "X:"
        )
        val updatedStateX = handleFloatInput(
            drawList, stateX, ImGui.getFont(), ImGui.getFontSize().toFloat(), x, y, componentWidth, height,
            textColor, backgroundColor, accentColor, "0.0"
        )

        // Y component
        drawList.addText(
            ImGui.getFont(),
            ImGui.getFontSize().toFloat(),
            x + componentWidth + 5,
            y - ImGui.getFontSize() - 2,
            textColor,
            "Y:"
        )
        val updatedStateY = handleFloatInput(
            drawList, stateY, ImGui.getFont(),
            ImGui.getFontSize().toFloat(), x + componentWidth + 5, y, componentWidth, height,
            textColor, backgroundColor, accentColor, "0.0"
        )

        // Z component
        drawList.addText(
            ImGui.getFont(),
            ImGui.getFontSize().toFloat(),
            x + 2 * componentWidth + 10,
            y - ImGui.getFontSize() - 2,
            textColor,
            "Z:"
        )
        val updatedStateZ = handleFloatInput(
            drawList, stateZ, ImGui.getFont(),
            ImGui.getFontSize().toFloat(), x + 2 * componentWidth + 10, y, componentWidth, height,
            textColor, backgroundColor, accentColor, "0.0"
        )

        buffers["$label-vec3-x"] = updatedStateX
        buffers["$label-vec3-y"] = updatedStateY
        buffers
        buffers["$label-vec3-z"] = updatedStateZ

        val newVec3f = Vector3f(updatedStateX.value, updatedStateY.value, updatedStateZ.value)
        if (newVec3f != property.get()) {
            property.set(newVec3f)
            return true
        }
        return false
    }

    private fun renderVec4fInput(
        drawList: ImDrawList,
        label: String,
        property: Property.Vec4f,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        backgroundColor: Int,
        textColor: Int,
        accentColor: Int
    ): Boolean {
        val stateX = buffers.getOrPut("$label-vec4-x") {
            InputState(
                text = property.get().x.toString(),
                value = property.get().x
            )
        } as InputState

        val stateY = buffers.getOrPut("$label-vec4-y") {
            InputState(
                text = property.get().y.toString(),
                value = property.get().y
            )
        } as InputState

        val stateZ = buffers.getOrPut("$label-vec4-z") {
            InputState(
                text = property.get().z.toString(),
                value = property.get().z
            )
        } as InputState

        val stateW = buffers.getOrPut("$label-vec4-w") {
            InputState(
                text = property.get().w.toString(),
                value = property.get().w
            )
        } as InputState

        val componentWidth = width / 4 - 5f

        // X component
        drawList.addText(
            ImGui.getFont(),
            ImGui.getFontSize().toFloat(),
            x,
            y - ImGui.getFontSize() - 2,
            textColor,
            "X:"
        )
        val updatedStateX = handleFloatInput(
            drawList, stateX, ImGui.getFont(), ImGui.getFontSize().toFloat(), x, y, componentWidth, height,
            textColor, backgroundColor, accentColor, "0.0"
        )

        // Y component
        drawList.addText(
            ImGui.getFont(),
            ImGui.getFontSize().toFloat(),
            x + componentWidth + 5,
            y - ImGui.getFontSize() - 2,
            textColor,
            "Y:"
        )
        val updatedStateY = handleFloatInput(
            drawList, stateY, ImGui.getFont(),
            ImGui.getFontSize().toFloat(), x + componentWidth + 5, y, componentWidth, height,
            textColor, backgroundColor, accentColor, "0.0"
        )

        // Z component
        drawList.addText(
            ImGui.getFont(),
            ImGui.getFontSize().toFloat(),
            x + 2 * componentWidth + 10,
            y - ImGui.getFontSize() - 2,
            textColor,
            "Z:"
        )
        val updatedStateZ = handleFloatInput(
            drawList,
            stateZ,
            ImGui.getFont(),
            ImGui.getFontSize().toFloat(),
            x + 2 * componentWidth + 10,
            y,
            componentWidth,
            height,
            textColor,
            backgroundColor,
            accentColor,
            "0.0"
        )

        // W component
        drawList.addText(
            ImGui.getFont(),
            ImGui.getFontSize().toFloat(),
            x + 3 * componentWidth + 15,
            y - ImGui.getFontSize() - 2,
            textColor,
            "W:"
        )
        val updatedStateW = handleFloatInput(
            drawList,
            stateW,
            ImGui.getFont(),
            ImGui.getFontSize().toFloat(),
            x + 3 * componentWidth + 15,
            y,
            componentWidth,
            height,
            textColor,
            backgroundColor,
            accentColor,
            "0.0"
        )

        buffers["$label-vec4-x"] = updatedStateX
        buffers["$label-vec4-y"] = updatedStateY
        buffers["$label-vec4-z"] = updatedStateZ
        buffers["$label-vec4-w"] = updatedStateW

        val newVec4f = Vector4f(updatedStateX.value, updatedStateY.value, updatedStateZ.value, updatedStateW.value)
        if (newVec4f != property.get()) {
            property.set(newVec4f)
            return true
        }
        return false
    }


    private fun renderVec4iInput(
        drawList: ImDrawList,
        label: String,
        property: Property.Vec4i,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        backgroundColor: Int,
        textColor: Int,
        accentColor: Int
    ): Boolean {
        val stateX = buffers.getOrPut("$label-vec4-x") {
            InputState(
                text = property.get().x.toString(),
                value = property.get().x.toFloat()
            )
        } as InputState

        val stateY = buffers.getOrPut("$label-vec4-y") {
            InputState(
                text = property.get().y.toString(),
                value = property.get().y.toFloat()
            )
        } as InputState

        val stateZ = buffers.getOrPut("$label-vec4-z") {
            InputState(
                text = property.get().z.toString(),
                value = property.get().z.toFloat()
            )
        } as InputState

        val stateW = buffers.getOrPut("$label-vec4-w") {
            InputState(
                text = property.get().w.toString(),
                value = property.get().w.toFloat()
            )
        } as InputState

        val componentWidth = width / 4 - 5f

        // X component
        drawList.addText(
            ImGui.getFont(),
            ImGui.getFontSize().toFloat(),
            x,
            y - ImGui.getFontSize() - 2,
            textColor,
            "X:"
        )
        val updatedStateX = handleFloatInput(
            drawList, stateX, ImGui.getFont(), ImGui.getFontSize().toFloat(), x, y, componentWidth, height,
            textColor, backgroundColor, accentColor, "0.0"
        )

        // Y component
        drawList.addText(
            ImGui.getFont(),
            ImGui.getFontSize().toFloat(),
            x + componentWidth + 5,
            y - ImGui.getFontSize() - 2,
            textColor,
            "Y:"
        )
        val updatedStateY = handleFloatInput(
            drawList, stateY, ImGui.getFont(),
            ImGui.getFontSize().toFloat(), x + componentWidth + 5, y, componentWidth, height,
            textColor, backgroundColor, accentColor, "0.0"
        )

        // Z component
        drawList.addText(
            ImGui.getFont(),
            ImGui.getFontSize().toFloat(),
            x + 2 * componentWidth + 10,
            y - ImGui.getFontSize() - 2,
            textColor,
            "Z:"
        )
        val updatedStateZ = handleFloatInput(
            drawList,
            stateZ,
            ImGui.getFont(),
            ImGui.getFontSize().toFloat(),
            x + 2 * componentWidth + 10, y,
            componentWidth,
            height,
            textColor,
            backgroundColor,
            accentColor,
            "0.0"
        )

        // W component
        drawList.addText(
            ImGui.getFont(),
            ImGui.getFontSize().toFloat(),
            x + 3 * componentWidth + 15,
            y - ImGui.getFontSize() - 2,
            textColor,
            "W:"
        )
        val updatedStateW = handleFloatInput(
            drawList,
            stateW,
            ImGui.getFont(),
            ImGui.getFontSize().toFloat(),
            x + 3 * componentWidth + 15,
            y,
            componentWidth,
            height,
            textColor,
            backgroundColor,
            accentColor,
            "0.0"
        )

        buffers["$label-vec4-x"] = updatedStateX
        buffers["$label-vec4-y"] = updatedStateY
        buffers["$label-vec4-z"] = updatedStateZ
        buffers["$label-vec4-w"] = updatedStateW

        val newVec4i = Vector4i(
            updatedStateX.value.toInt(),
            updatedStateY.value.toInt(),
            updatedStateZ.value.toInt(),
            updatedStateW.value.toInt()
        )
        if (newVec4i != property.get()) {
            property.set(newVec4i)
            return true
        }
        return false

    }

}