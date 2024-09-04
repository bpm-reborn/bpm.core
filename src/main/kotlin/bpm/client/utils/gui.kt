package bpm.client.utils

import imgui.*
import org.joml.*
import org.lwjgl.glfw.GLFW
import imgui.ImGui
import imgui.ImDrawList
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiKey
import imgui.flag.ImGuiMouseCursor
import imgui.flag.ImGuiStyleVar
import imgui.internal.ImRect
import imgui.type.ImString
import bpm.client.font.Fonts
import bpm.client.runtime.Platform
import bpm.client.runtime.ClientRuntime
import bpm.common.utils.FontAwesome
import org.joml.Vector2f
import kotlin.math.*

val windowHandle: Long get() = ImGui.getWindowViewport().platformHandle

fun isMouseDown(mouseButton: Int): Boolean {
    val state = GLFW.glfwGetMouseButton(windowHandle, mouseButton)
    return state == GLFW.GLFW_PRESS || state == GLFW.GLFW_REPEAT
}

fun isKeyPressed(key: Int): Boolean {
    return GLFW.glfwGetKey(windowHandle, key) == GLFW.GLFW_PRESS
}

fun isKeyReleased(key: Int): Boolean {
    return GLFW.glfwGetKey(windowHandle, key) == GLFW.GLFW_RELEASE
}

fun isKeyDown(key: Int): Boolean {
    return GLFW.glfwGetKey(windowHandle, key) != GLFW.GLFW_RELEASE
}

val mousePosition: Vector2f
    get() {
        val x = ImGui.getMousePos().x
        val y = ImGui.getMousePos().y
        return Vector2f(x, y)
    }

val ImVec2.toVec2f: Vector2f
    get() {
        return Vector2f(x, y)
    }

val Vector4i.imColor: Int
    get() {
        return ImColor.rgba(x, y, z, w)
    }

fun ImFont.sizeOf(
    text: String,
    size: Float = this.fontSize,
    maxWidth: Float = 200f,
    wrapWidth: Float = maxWidth * 2f,
): Vector2f {
    val vec = ImVec2()
    this.calcTextSizeA(vec, size, maxWidth, wrapWidth, text)
    return Vector2f(vec.x, vec.y)
}

fun ImFont.textSize(text: String): ImVec2 = this.use {
    ImGui.calcTextSize(text)
}


inline fun <T : Any> ImFont.use(block: (ImFont) -> T): T {
    ImGui.pushFont(this)
    val value = block(this)
    ImGui.popFont()
    return value
}

data class TextInputState(
    var text: String = "",
    var cursorPosition: Int = 0,
    var selectionStart: Int = 0,
    var selectionEnd: Int = 0,
    var isFocused: Boolean = false,
    var placeHolder: String = "",
    var textOffset: Float = 0f
)

enum class InputType { TEXT, NUMBER
}

fun handleUniversalTextInput(
    drawList: ImDrawList,
    state: TextInputState,
    font: imgui.ImFont,
    fontSize: Float,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    textColor: Int,
    backgroundColor: Int,
    selectionColor: Int,
    inputType: InputType = InputType.TEXT,
    placeholder: String = "",
    multiline: Boolean = true
): TextInputState {
    val newState = state.copy()

    // Ensure cursor position is within bounds
    newState.cursorPosition = newState.cursorPosition.coerceIn(0, newState.text.length)
    newState.selectionStart = newState.selectionStart.coerceIn(0, newState.text.length)
    newState.selectionEnd = newState.selectionEnd.coerceIn(0, newState.text.length)

    val cornerRadius = 4f
    val lines = newState.text.split("\n")
    val longestLineString = lines.maxByOrNull { it.length } ?: ""
    val textSize = ImGui.calcTextSize(longestLineString)

    val contentHeight = if (multiline) maxOf(height, lines.size * fontSize + 10) else height
    var expandedWidth = maxOf(width, textSize.x + 20)

    if (newState.text.isEmpty() && placeholder.isNotEmpty()) {
        expandedWidth = maxOf(width, ImGui.calcTextSize(placeholder).x + 20)
    }

    val expandedHeight = if (multiline) contentHeight else height
    val expandedX = x
    val expandedY = y

    // Use window draw list for single-line inputs
    val actualDrawList = drawList

    // Calculate text offset for single-line scrolling
    if (!multiline) {
        val visibleTextWidth = width - 10 // Subtract padding
        if (textSize.x > visibleTextWidth) {
            val cursorX = ImGui.calcTextSize(newState.text.substring(0, newState.cursorPosition)).x
            if (cursorX - newState.textOffset > visibleTextWidth) {
                newState.textOffset = cursorX - visibleTextWidth
            } else if (cursorX < newState.textOffset) {
                newState.textOffset = cursorX
            }
            newState.textOffset = newState.textOffset.coerceIn(0f, textSize.x - visibleTextWidth)
        } else {
            newState.textOffset = 0f
        }
    }

    // Handle mouse interactions
    val mousePos = ImGui.getMousePos()
    if (ImGui.isMouseClicked(0) && mousePos.x >= expandedX && mousePos.x <= expandedX + expandedWidth &&
        mousePos.y >= expandedY && mousePos.y <= expandedY + expandedHeight
    ) {
        newState.isFocused = true
        newState.cursorPosition = getTextPositionFromMouse(
            newState.text,
            font,
            fontSize,
            expandedX,
            expandedY,
            mousePos,
            newState.textOffset,
            multiline
        )
        newState.selectionStart = newState.cursorPosition
        newState.selectionEnd = newState.cursorPosition
    }

    if (newState.isFocused && ImGui.isMouseDragging(0)) {
        val endPos = getTextPositionFromMouse(
            newState.text, font, fontSize, expandedX, expandedY, mousePos, newState.textOffset, multiline
        )
        newState.selectionEnd = endPos
        newState.cursorPosition = endPos
    }

    // Handle keyboard input
    if (newState.isFocused) {
        when {
            ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.Backspace)) -> handleBackspace(newState)
            ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.Delete)) -> handleDelete(newState)
            ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.LeftArrow)) -> {
                if (Platform.isKeyDown(ClientRuntime.Key.LEFT_CONTROL)) {
                    handleCtrlLeftArrow(newState)
                } else {
                    handleLeftArrow(newState)
                }
            }

            ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.RightArrow)) -> {
                if (Platform.isKeyDown(ClientRuntime.Key.LEFT_CONTROL)) {
                    handleCtrlRightArrow(newState)
                } else {
                    handleRightArrow(newState)
                }
            }

            ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.UpArrow)) -> handleUpArrow(newState)
            ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.DownArrow)) -> handleDownArrow(newState)
            ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.Enter)) -> {
                if (multiline && inputType != InputType.NUMBER) handleEnter(newState) else newState.isFocused = false
            }

            ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.Home)) -> handleHome(newState)
            ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.End)) -> handleEnd(newState)
            Platform.isKeyDown(ClientRuntime.Key.LEFT_CONTROL) && ImGui.isKeyPressed(GLFW.GLFW_KEY_A) -> handleSelectAll(
                newState
            )

            Platform.isKeyDown(ClientRuntime.Key.LEFT_CONTROL) && ImGui.isKeyPressed(GLFW.GLFW_KEY_C) -> handleCopy(newState)
            Platform.isKeyDown(ClientRuntime.Key.LEFT_CONTROL) && ImGui.isKeyPressed(GLFW.GLFW_KEY_V) -> handlePaste(newState)
            Platform.isKeyDown(ClientRuntime.Key.LEFT_CONTROL) && ImGui.isKeyPressed(GLFW.GLFW_KEY_X) -> handleCut(newState)
            else -> {
                // Handle text input
                for (key in 32..126) { // ASCII printable characters
                    if (ImGui.isKeyPressed(key)) {
                        val char = if (Platform.isKeyDown(ClientRuntime.Key.LEFT_SHIFT) || Platform.isKeyDown(ClientRuntime.Key.RIGHT_SHIFT)) {
                            key.toChar().uppercaseChar()
                        } else {
                            key.toChar().lowercaseChar()
                        }

                        if (inputType == InputType.NUMBER) {
                            if (char.isDigit() || (char == '.' && !newState.text.contains('.')) || (char == '-' && newState.cursorPosition == 0 && !newState.text.startsWith(
                                    '-'
                                ))
                            ) {
                                insertCharacter(newState, char)
                            }
                        } else {
                            insertCharacter(newState, char)
                        }
                    }
                }
            }
        }
    }

    // Rendering
    if (!newState.isFocused) {
        ImGui.getWindowDrawList().addRectFilled(x, y, x + width, y + height, backgroundColor, cornerRadius)
        ImGui.getWindowDrawList().pushClipRect(x, y, x + width, y + height)
    } else {
        actualDrawList.addRectFilled(
            expandedX + 2, expandedY + 2,
            expandedX + expandedWidth + 2, expandedY + expandedHeight + 2,
            ImColor.rgba(0, 0, 0, 100), cornerRadius
        )
        actualDrawList.addRectFilled(
            expandedX, expandedY,
            expandedX + expandedWidth, expandedY + expandedHeight,
            ImColor.rgba(60, 60, 60, 255), cornerRadius
        )
        actualDrawList.addRect(
            expandedX, expandedY,
            expandedX + expandedWidth, expandedY + expandedHeight,
            ImColor.rgba(100, 100, 200, 255), cornerRadius, 0, 1.5f
        )
    }

    // Render selection
    if (newState.selectionStart != newState.selectionEnd) {
        val startPos = minOf(newState.selectionStart, newState.selectionEnd)
        val endPos = maxOf(newState.selectionStart, newState.selectionEnd)
        if (multiline) {
            val startLine = newState.text.substring(0, startPos).count { it == '\n' }
            val endLine = newState.text.substring(0, endPos).count { it == '\n' }
            val startX = ImGui.calcTextSize(newState.text.substring(0, startPos).substringAfterLast('\n')).x
            val endX = ImGui.calcTextSize(newState.text.substring(0, endPos).substringAfterLast('\n')).x

            for (i in startLine..endLine) {
                val lineY = expandedY + 5 + i * fontSize
                val lineStartX = if (i == startLine) expandedX + 5 + startX else expandedX + 5
                val lineEndX = if (i == endLine) expandedX + 5 + endX else expandedX + expandedWidth - 5
                actualDrawList.addRectFilled(lineStartX, lineY, lineEndX, lineY + fontSize, selectionColor)
            }
        } else {
            val startX = ImGui.calcTextSize(newState.text.substring(0, startPos)).x
            val endX = ImGui.calcTextSize(newState.text.substring(0, endPos)).x
            actualDrawList.addRectFilled(
                expandedX + 5 + startX - newState.textOffset, expandedY + 5,
                expandedX + 5 + endX - newState.textOffset, expandedY + 5 + fontSize,
                selectionColor
            )
        }
    }

    // Render text or placeholder
    if (newState.text.isNotEmpty()) {
        if (multiline) {
            //if it's not focused we render just the first line
            if (!newState.isFocused)
                actualDrawList.pushClipRect(
                    expandedX + 5,
                    expandedY + 8,
                    expandedX + width - 5,
                    expandedY + height
                )
            lines.forEachIndexed { index, line ->
                actualDrawList.addText(font, fontSize, expandedX + 5, expandedY + 5 + index * fontSize, textColor, line)
            }
            if (!newState.isFocused)
                actualDrawList.popClipRect()
        } else {
            actualDrawList.pushClipRect(expandedX + 5, expandedY + 5, expandedX + width - 5, expandedY + height - 5)
            actualDrawList.addText(
                font,
                fontSize,
                expandedX + 5 - newState.textOffset,
                expandedY + 5,
                textColor,
                newState.text
            )
            actualDrawList.popClipRect()
        }
    } else if (placeholder.isNotEmpty()) {
        actualDrawList.addText(
            font,
            fontSize,
            expandedX + 5,
            expandedY + 5,
            ImColor.rgba(150, 150, 150, 180),
            placeholder
        )
    }


    // Render cursor
    if (newState.isFocused && ((ImGui.getTime() * 2).toInt() % 2 == 0)) {
        if (multiline) {
            val cursorLine = newState.text.substring(0, newState.cursorPosition).count { it == '\n' }
            val cursorX = expandedX + 5 + ImGui.calcTextSize(
                newState.text.substring(0, newState.cursorPosition).substringAfterLast('\n')
            ).x
            val cursorY = expandedY + 5 + cursorLine * fontSize

            actualDrawList.addLine(cursorX, cursorY, cursorX, cursorY + fontSize, textColor)
        } else {
            val cursorX = expandedX + 5 + ImGui.calcTextSize(
                newState.text.substring(
                    0,
                    newState.cursorPosition
                )
            ).x - newState.textOffset
            actualDrawList.addLine(cursorX, expandedY + 5, cursorX, expandedY + height - 5, textColor)
        }
    }

    // Update the clipping rectangle
    if (!newState.isFocused) {
        ImGui.getWindowDrawList().popClipRect()
    }
    actualDrawList.pushClipRect(expandedX, expandedY, expandedX + expandedWidth, expandedY + expandedHeight + 100f)

    return newState
}

fun getTextPositionFromMouse(
    text: String,
    font: ImFont,
    fontSize: Float,
    x: Float,
    y: Float,
    mousePos: ImVec2,
    textOffset: Float = 0f,
    multiline: Boolean = true
): Int {
    val lines = text.split("\n")
    val clickedLine = if (multiline) {
        ((mousePos.y - y - 5) / fontSize).toInt().coerceIn(0, lines.size - 1)
    } else {
        0
    }
    val lineStart = lines.take(clickedLine).sumOf { it.length + 1 }
    val relativeX = mousePos.x - x - 5 + textOffset

    val lineText = lines[clickedLine]
    var bestPosition = lineStart
    var bestDistance = Float.MAX_VALUE

    for (i in 0..lineText.length) {
        val subString = lineText.substring(0, i)
        val textWidth = ImGui.calcTextSize(subString).x
        val distance = kotlin.math.abs(textWidth - relativeX)
        if (distance < bestDistance) {
            bestDistance = distance
            bestPosition = lineStart + i
        }
    }

    return bestPosition
}

private fun handleCtrlLeftArrow(state: TextInputState) {
    val text = state.text
    var newPosition = state.cursorPosition

    // Skip spaces
    while (newPosition > 0 && text[newPosition - 1].isWhitespace()) {
        newPosition--
    }

    // Find the start of the current word
    while (newPosition > 0 && !text[newPosition - 1].isWhitespace()) {
        newPosition--
    }

    state.cursorPosition = newPosition
    if (!Platform.isKeyDown(ClientRuntime.Key.LEFT_SHIFT)) {
        state.selectionStart = state.cursorPosition
        state.selectionEnd = state.cursorPosition
    } else {
        state.selectionEnd = state.cursorPosition
    }
}

private fun handleCtrlRightArrow(state: TextInputState) {
    val text = state.text
    var newPosition = state.cursorPosition

    // Find the end of the current word
    while (newPosition < text.length && !text[newPosition].isWhitespace()) {
        newPosition++
    }

    // Skip spaces
    while (newPosition < text.length && text[newPosition].isWhitespace()) {
        newPosition++
    }

    state.cursorPosition = newPosition
    if (!Platform.isKeyDown(ClientRuntime.Key.LEFT_SHIFT)) {
        state.selectionStart = state.cursorPosition
        state.selectionEnd = state.cursorPosition
    } else {
        state.selectionEnd = state.cursorPosition
    }
}

private fun handleEnter(state: TextInputState) {
    insertCharacter(state, '\n')
}

private fun handleUpArrow(state: TextInputState) {
    val currentLine = state.text.substring(0, state.cursorPosition).count { it == '\n' }
    if (currentLine > 0) {
        val lineStart = state.text.substring(0, state.cursorPosition).lastIndexOf('\n', state.cursorPosition - 2) + 1
        val columnInCurrentLine = state.cursorPosition - lineStart
        val previousLineStart = state.text.substring(0, lineStart - 1).lastIndexOf('\n') + 1
        val previousLineEnd = lineStart - 1
        state.cursorPosition = minOf(previousLineStart + columnInCurrentLine, previousLineEnd)
    }
}

private fun handleDownArrow(state: TextInputState) {
    val lines = state.text.split("\n")
    val currentLine = state.text.substring(0, state.cursorPosition).count { it == '\n' }
    if (currentLine < lines.size - 1) {
        val lineStart = state.text.substring(0, state.cursorPosition).lastIndexOf('\n') + 1
        val columnInCurrentLine = state.cursorPosition - lineStart
        val nextLineStart = state.text.indexOf('\n', state.cursorPosition) + 1
        val nextLineEnd = state.text.indexOf('\n', nextLineStart).let { if (it == -1) state.text.length else it }
        state.cursorPosition = minOf(nextLineStart + columnInCurrentLine, nextLineEnd)
    }
}


private fun handleSelectAll(state: TextInputState) {
    state.selectionStart = 0
    state.selectionEnd = state.text.length
    state.cursorPosition = state.text.length
}

private fun handleCopy(state: TextInputState) {
    if (state.selectionStart != state.selectionEnd) {
        val selectedText = state.text.substring(
            minOf(state.selectionStart, state.selectionEnd), maxOf(state.selectionStart, state.selectionEnd)
        )
        ImGui.setClipboardText(selectedText)
    }
}

private fun handlePaste(state: TextInputState) {
    val pastedText = ImGui.getClipboardText()
    if (pastedText.isNotEmpty()) {
        if (state.selectionStart != state.selectionEnd) {
            deleteSelectedText(state)
        }
        state.text = state.text.substring(
            0, state.cursorPosition
        ) + pastedText + state.text.substring(state.cursorPosition)
        state.cursorPosition += pastedText.length
        state.selectionStart = state.cursorPosition
        state.selectionEnd = state.cursorPosition
    }
}

private fun handleCut(state: TextInputState) {
    if (state.selectionStart != state.selectionEnd) {
        val selectedText = state.text.substring(
            minOf(state.selectionStart, state.selectionEnd), maxOf(state.selectionStart, state.selectionEnd)
        )
        ImGui.setClipboardText(selectedText)
        deleteSelectedText(state)
    }
}

private fun insertCharacter(state: TextInputState, char: Char) {
    if (state.selectionStart != state.selectionEnd) {
        deleteSelectedText(state)
    }
    state.text = state.text.substring(0, state.cursorPosition) + char + state.text.substring(state.cursorPosition)
    state.cursorPosition++
    state.selectionStart = state.cursorPosition
    state.selectionEnd = state.cursorPosition
}

private fun handleBackspace(state: TextInputState) {
    if (state.selectionStart != state.selectionEnd) {
        deleteSelectedText(state)
    } else if (state.cursorPosition > 0) {
        state.text = state.text.substring(0, state.cursorPosition - 1) + state.text.substring(state.cursorPosition)
        state.cursorPosition--
        state.selectionStart = state.cursorPosition
        state.selectionEnd = state.cursorPosition
    }
}

private fun handleDelete(state: TextInputState) {
    if (state.selectionStart != state.selectionEnd) {
        deleteSelectedText(state)
    } else if (state.cursorPosition < state.text.length) {
        state.text = state.text.substring(0, state.cursorPosition) + state.text.substring(state.cursorPosition + 1)
    }
}

private fun handleLeftArrow(state: TextInputState) {
    if (state.cursorPosition > 0) state.cursorPosition--
    if (!Platform.isKeyDown(ClientRuntime.Key.LEFT_SHIFT)) {
        state.selectionStart = state.cursorPosition
        state.selectionEnd = state.cursorPosition
    } else {
        state.selectionEnd = state.cursorPosition
    }
}

private fun handleRightArrow(state: TextInputState) {
    if (state.cursorPosition < state.text.length) state.cursorPosition++
    if (!Platform.isKeyDown(ClientRuntime.Key.LEFT_SHIFT)) {
        state.selectionStart = state.cursorPosition
        state.selectionEnd = state.cursorPosition
    } else {
        state.selectionEnd = state.cursorPosition
    }
}

private fun handleHome(state: TextInputState) {
    val currentLineStart = state.text.lastIndexOf('\n', state.cursorPosition - 1) + 1
    state.cursorPosition = currentLineStart
    if (!Platform.isKeyDown(ClientRuntime.Key.LEFT_SHIFT)) {
        state.selectionStart = state.cursorPosition
        state.selectionEnd = state.cursorPosition
    } else {
        state.selectionEnd = state.cursorPosition
    }
}

private fun handleEnd(state: TextInputState) {
    state.cursorPosition = state.text.length
    if (!Platform.isKeyDown(ClientRuntime.Key.LEFT_SHIFT)) {
        state.selectionStart = state.cursorPosition
        state.selectionEnd = state.cursorPosition
    } else {
        state.selectionEnd = state.cursorPosition
    }
}

private fun deleteSelectedText(state: TextInputState) {
    val start = minOf(state.selectionStart, state.selectionEnd)
    val end = maxOf(state.selectionStart, state.selectionEnd)
    state.text = state.text.substring(0, start) + state.text.substring(end)
    state.cursorPosition = start
    state.selectionStart = start
    state.selectionEnd = start
}

object CustomInputField {

    private val bodyFont by lazy { Fonts.getFamily("Inter")["Regular"][14] }

    fun render(
        drawList: ImDrawList,
        label: String,
        value: ImString,
        rect: ImRect,
        backgroundColor: Int = ImColor.rgba(45, 45, 45, 255),
        textColor: Int = ImColor.rgba(220, 220, 220, 255),
        placeholderColor: Int = ImColor.rgba(150, 150, 150, 180)
    ): Boolean {
        var changed = false

        // Draw background with rounded corners
        drawList.addRectFilled(
            rect.min.x, rect.min.y, rect.max.x, rect.max.y, backgroundColor, 4f
        )

        // Handle input field focus
        if (ImGui.isMouseClicked(0) && rect.contains(ImGui.getMousePos())) {
            ImGui.setKeyboardFocusHere()
        }

        // Render input field
        ImGui.setCursorPos(rect.min.x + 5, rect.min.y + 3)
        ImGui.pushItemWidth(rect.max.x - rect.min.x - 10)
        ImGui.pushStyleColor(ImGuiCol.FrameBg, 0)
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 0f, 5f)

        if (ImGui.inputText("##$label", value)) {
            changed = true
        }

        ImGui.popStyleColor()
        ImGui.popStyleVar()
        ImGui.popItemWidth()

        // Render placeholder text if the input is empty
        if (value.get().isBlank()) {
            bodyFont.use {
                drawList.addText(
                    bodyFont, 14f, rect.min.x + 5, rect.min.y + 8, placeholderColor, label
                )
            }
        }

        return changed
    }
}

// New data classes for different property types
data class IntInputState(
    var value: Int = 0,
    var text: String = "0",
    var cursorPosition: Int = 0,
    var selectionStart: Int = 0,
    var selectionEnd: Int = 0,
    var isFocused: Boolean = false,
    var placeHolder: String = "0",
    var isDragging: Boolean = false,
    var dragStartMouseX: Float = 0f,
    var dragStartValue: Int = 0,
    var dragStartMouseY: Float = 0f,
    var lastMouseX: Float = 0f,
    var accelerationRate: Float = 0f,
    var minValue: Int = -1000,
    var maxValue: Int = 1000
)


data class FloatInputState(
    var value: Float = 0f,
    var text: String = "0.0",
    var cursorPosition: Int = 0,
    var selectionStart: Int = 0,
    var selectionEnd: Int = 0,
    var isFocused: Boolean = false,
    var draggable: Boolean = false,
    var mouseStartPos: Vector2f = Vector2f()
)

data class BooleanInputState(
    var value: Boolean = false, var isFocused: Boolean = false
)

data class Vec2fInputState(
    val x: FloatInputState = FloatInputState(),
    var y: FloatInputState = FloatInputState()
)

data class Vec3fInputState(
    var value: Vector3f = Vector3f(),
    var text: String = "0.0, 0.0, 0.0",
    var cursorPosition: Int = 0,
    var selectionStart: Int = 0,
    var selectionEnd: Int = 0,
    var isFocused: Boolean = false
)

data class Vec4fInputState(
    var value: Vector4f = Vector4f(),
    var text: String = "0.0, 0.0, 0.0, 0.0",
    var cursorPosition: Int = 0,
    var selectionStart: Int = 0,
    var selectionEnd: Int = 0,
    var isFocused: Boolean = false
)

data class Vec4iInputState(
    var value: Vector4i = Vector4i(),
    var text: String = "0, 0, 0, 0",
    var cursorPosition: Int = 0,
    var selectionStart: Int = 0,
    var selectionEnd: Int = 0,
    var isFocused: Boolean = false
)


// Slider state helper
data class SliderIntState(
    var value: Int = 0,
    var isDragging: Boolean = false,
    var lastMouseX: Float = 0f,
    var accelerationRate: Float = 0f
)
//fun handleSlider(
//    drawList: ImDrawList,
//    state: SliderState,
//    font: ImFont,
//    fontSize: Float,
//    label: String,
//    x: Float,
//    y: Float,
//    width: Float,
//    height: Float,
//    min: Float,
//    max: Float,
//    textColor: Int,
//    backgroundColor: Int,
//    hoverColor: Int,
//    activeColor: Int
//): SliderState {
//    val newState = state.copy()
//    val labelSize = ImGui.calcTextSize(label)
//    val centerX = x + width / 2
//    val centerY = y + height / 2
//
//    // Draw background
//    drawList.addRectFilled(x, y, x + width, y + height, backgroundColor, 4f)
//
//    // Handle hover effect
//    val isHovered = ImGui.isMouseHoveringRect(x, y, x + width, y + height)
//    if (isHovered) {
//        val padding = 2f
//        drawList.addRectFilled(x - padding, y - padding, x + width + padding, y + height + padding, hoverColor, 4f)
//    }
//
//    // Handle dragging
//    if (ImGui.isMouseClicked(0) && isHovered) {
//        newState.isDragging = true
//        newState.lastMouseX = ImGui.getMousePos().x
//    }
//
//    if (newState.isDragging) {
//        if (ImGui.isMouseDown(0)) {
//            val mouseX = ImGui.getMousePos().x
//            val delta = mouseX - newState.lastMouseX
//
//            // Adjust speed based on modifier keys
//            val speed = when {
//                Platform.isKeyDown(Runtime.Key.LEFT_SHIFT) -> 0.1f
//                Platform.isKeyDown(Runtime.Key.LEFT_CONTROL) -> 10f
//                else -> 1f
//            }
//
//            newState.accelerationRate = delta * speed
//            newState.value = (newState.value + newState.accelerationRate).coerceIn(min, max)
//
//            // Set mouse position to center of slider
//            Platform.setMousePosition(centerX.toDouble(), centerY.toDouble())
//        } else {
//            newState.isDragging = false
//            newState.accelerationRate = 0f
//        }
//    }
//
//    // Calculate progress
//    val progress = (newState.value - min) / (max - min)
//
//    // Draw colored lines
//    val lineY = y + height / 2
//    if (newState.accelerationRate > 0) {
//        drawList.addLine(centerX, lineY, x + width, lineY, ImColor.rgba(0, 255, 0, 255), 2f)
//        drawList.addLine(x, lineY, centerX, lineY, ImColor.rgba(128, 128, 128, 255), 2f)
//    } else if (newState.accelerationRate < 0) {
//        drawList.addLine(x, lineY, centerX, lineY, ImColor.rgba(255, 0, 0, 255), 2f)
//        drawList.addLine(centerX, lineY, x + width, lineY, ImColor.rgba(128, 128, 128, 255), 2f)
//    } else {
//        drawList.addLine(x, lineY, x + width, lineY, ImColor.rgba(128, 128, 128, 255), 2f)
//    }
//
//    // Draw label
//    drawList.addText(
//        font,
//        fontSize,
//        centerX - labelSize.x / 2,
//        centerY - labelSize.y / 2,
//        textColor,
//        label
//    )
//
//    // Draw acceleration rate
//    if (newState.isDragging) {
//        val accelerationText = String.format("%.2f", newState.accelerationRate)
//        val accelerationTextSize = ImGui.calcTextSize(accelerationText)
//        val accelerationY = y - accelerationTextSize.y - 5
//        drawList.addText(
//            font,
//            fontSize,
//            if (newState.accelerationRate > 0) x + width - accelerationTextSize.x else x,
//            accelerationY,
//            if (newState.accelerationRate > 0) ImColor.rgba(0, 255, 0, 255) else ImColor.rgba(255, 0, 0, 255),
//            accelerationText
//        )
//    }
//
//    // Draw value
//    val valueText = String.format("%.2f", newState.value)
//    val valueTextSize = ImGui.calcTextSize(valueText)
//    drawList.addText(
//        font,
//        fontSize,
//        x + width - valueTextSize.x,
//        y + height,
//        textColor,
//        valueText
//    )
//
//    return newState
//}


//fun slider(
//    drawList: ImDrawList,
//    state: SliderState,
//    label: String,
//    x: Float,
//    y: Float,
//    width: Float,
//    height: Float,
//    min: Int,
//    max: Int,
//    textColor: Int,
//    backgroundColor: Int,
//    hoverColor: Int,
//    activeColor: Int
//): SliderState {
//    val newState = state.copy()
//    val centerX = x + width / 2
//    val centerY = y + height / 2
//
//    // Draw background
//    drawList.addRectFilled(x, y, x + width, y + height, backgroundColor, 4f)
//
//    // Handle hover effect
//    val isHovered = ImGui.isMouseHoveringRect(x, y, x + width, y + height)
//    if (isHovered) {
//        drawList.addRectFilled(x, y, x + width, y + height, hoverColor, 4f)
//    }
//
//    // Handle dragging
//    if (ImGui.isMouseClicked(0) && isHovered) {
//        newState.isDragging = true
//        newState.lastMouseX = ImGui.getMousePos().x
//    }
//
//    if (newState.isDragging) {
//        if (ImGui.isMouseDown(0)) {
//            val mouseX = ImGui.getMousePos().x
//            val delta = mouseX - newState.lastMouseX
//
//            // Adjust speed based on modifier keys
//            val speed = when {
//                Platform.isKeyDown(Runtime.Key.LEFT_SHIFT) -> 0.01f
//                Platform.isKeyDown(Runtime.Key.LEFT_CONTROL) -> 1f
//                else -> 0.1f
//            }
//
//            newState.accelerationRate += delta * speed
//            newState.value = (newState.value + newState.accelerationRate.roundToInt()).coerceIn(min, max)
//
//            // Set mouse position to center of slider
//            Platform.setMousePosition(centerX.toDouble(), centerY.toDouble())
//            newState.lastMouseX = centerX
//        } else {
//            newState.isDragging = false
//            newState.accelerationRate = 0f
//        }
//    }
//
//    // Calculate progress
//    val progress = (newState.value - min).toFloat() / (max - min)
//
//    // Draw slider bar
//    val barY = y + height / 2
//    drawList.addRectFilled(x, barY - 2, x + width, barY + 2, ImColor.rgba(60, 60, 60, 255))
//    drawList.addRectFilled(x, barY - 2, x + width * progress, barY + 2, activeColor)
//
//    // Draw handle
//    val handleRadius = height / 2 - 2
//    val handleX = x + width * progress
//    drawList.addCircleFilled(handleX, barY, handleRadius, textColor)
//
//    return newState
//}

/*
fun slider(
    drawList: ImDrawList,
    state: SliderIntState,
    label: String,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    min: Float,
    max: Float,
    textColor: Int,
    backgroundColor: Int,
    hoverColor: Int,
    activeColor: Int
): SliderIntState {
    val newState = state.copy()
    val centerX = x + width / 2
    val centerY = y + height / 2

    // Draw background
    drawList.addRectFilled(x, y, x + width, y + height, backgroundColor, 4f)

    // Handle hover effect
    val isHovered = ImGui.isMouseHoveringRect(x, y, x + width, y + height)
    if (isHovered) {
        val padding = 2f
        drawList.addRectFilled(x - padding, y - padding, x + width + padding, y + height + padding, hoverColor, 4f)
    }

    // Handle dragging
    if (ImGui.isMouseClicked(0) && isHovered) {
        newState.isDragging = true
        newState.lastMouseX = ImGui.getMousePos().x
    }

    if (newState.isDragging) {
        if (ImGui.isMouseDown(0)) {
            val mouseX = ImGui.getMousePos().x
            val delta = mouseX - newState.lastMouseX

            // Adjust speed based on modifier keys
            val speed = when {
                Platform.isKeyDown(Runtime.Key.LEFT_SHIFT) -> 0.01f
                Platform.isKeyDown(Runtime.Key.LEFT_CONTROL) -> 1f
                else -> 0.1f
            }

            newState.accelerationRate += delta * speed
            newState.value = (newState.value + newState.accelerationRate.roundToInt()).coerceIn(
                min.toFloat(),
                max.toFloat()
            )

            // Set mouse position to center of slider
            Platform.setMousePosition(centerX.toDouble(), centerY.toDouble())
            newState.lastMouseX = centerX
        } else {
            newState.isDragging = false
            newState.accelerationRate = 0f
        }
    }

    // Calculate progress
    val progress = (newState.value - min).toFloat() / (max - min)

    // Draw slider bar
    val barY = y + height / 2
    drawList.addRectFilled(x, barY - 2, x + width, barY + 2, ImColor.rgba(60, 60, 60, 255))
    drawList.addRectFilled(x, barY - 2, x + width * progress, barY + 2, activeColor)

    // Draw label
    drawList.addText(x, y - 20, textColor, label)

    // Draw value
    val valueText = newState.value.toString()
    val valueTextSize = ImGui.calcTextSize(valueText)
    drawList.addText(
        x + width - valueTextSize.x,
        y - 20,
        textColor,
        valueText
    )

    // Draw - and + buttons
    val buttonSize = 20f
    val buttonY = y + height + 5

    // Minus button
    if (renderButton(drawList, "-", x, buttonY, buttonSize, buttonSize, textColor, ImColor.rgba(80, 80, 80, 255))) {
        newState.value = (newState.value - 1).coerceIn(min.toFloat(), max.toFloat())
    }

    // Plus button
    if (renderButton(
            drawList,
            "+",
            x + width - buttonSize,
            buttonY,
            buttonSize,
            buttonSize,
            textColor,
            ImColor.rgba(80, 80, 80, 255)
        )
    ) {
        newState.value = (newState.value + 1).coerceIn(min.toFloat(), max.toFloat())
    }

    return newState
}
*/
fun slider(
    drawList: ImDrawList,
    state: SliderState,
    label: String,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    min: Int,
    max: Int,
    textColor: Int,
    backgroundColor: Int,
    hoverColor: Int,
    activeColor: Int
): SliderState {


    val newState = state.copy()
    val centerX = x + width / 2
    val centerY = y + height / 2

    // Draw background
    drawList.addRectFilled(x, y, x + width, y + height, backgroundColor, 4f)

    // Handle hover effect
    val isHovered = ImGui.isMouseHoveringRect(x, y, x + width, y + height)
    if (isHovered) {
        drawList.addRectFilled(x, y, x + width, y + height, hoverColor, 4f)
    }

    // Handle dragging
    if (ImGui.isMouseClicked(0) && isHovered) {
        newState.isDragging = true
        newState.lastMouseX = ImGui.getMousePos().x
    }

    if (newState.isDragging) {
        if (ImGui.isMouseDown(0)) {
            val mouseX = ImGui.getMousePos().x
            val delta = mouseX - newState.lastMouseX

            val isShiftHeld = Platform.isKeyDown(ClientRuntime.Key.LEFT_SHIFT)
            val isControlHeld = Platform.isKeyDown(ClientRuntime.Key.LEFT_CONTROL)
            if (isShiftHeld) {
                // Precise movement without acceleration when Shift is held
                newState.value = (newState.value + delta.toInt()).coerceIn(min, max)
            } else {
                // Adjust speed based on modifier keys
                val speed = when {
                    isControlHeld -> 0.1f
                    else -> 0.01f
                }

                newState.accelerationRate += delta * speed
                newState.value = (newState.value + newState.accelerationRate.roundToInt()).coerceIn(min, max)
            }

            // Set mouse position to center of slider
            Platform.setMousePosition(centerX.toDouble(), centerY.toDouble())
            newState.lastMouseX = centerX
        } else {
            newState.isDragging = false
            newState.accelerationRate = 0f
        }
    }

    // Calculate progress
    val progress = (newState.value - min).toFloat() / (max - min)

    // Draw slider bar
    val barY = y + height / 2
    drawList.addRectFilled(x, barY - 2, x + width, barY + 2, ImColor.rgba(60, 60, 60, 255))
    drawList.addRectFilled(x, barY - 2, x + width * progress, barY + 2, activeColor)

    // Draw handle
    val handleRadius = height / 2 - 2
    val handleX = x + width * progress
    drawList.addCircleFilled(handleX, barY, handleRadius, textColor)

    return newState
}


// Helper function to render a button
fun renderButton(
    drawList: ImDrawList,
    label: String,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    textColor: Int,
    backgroundColor: Int
): Boolean {
    val isHovered = ImGui.isMouseHoveringRect(x, y, x + width, y + height)
    val buttonColor = if (isHovered) ImColor.rgba(100, 100, 100, 255) else backgroundColor

    drawList.addRectFilled(x, y, x + width, y + height, buttonColor, 4f)
    val textSize = ImGui.calcTextSize(label)
    val textX = x + (width - textSize.x) / 2
    val textY = y + (height - textSize.y) / 2
    drawList.addText(textX, textY, textColor, label)

    return isHovered && Platform.isMouseDown(ClientRuntime.MouseButton.LEFT)
}

var sliderState = SliderIntState()

data class SliderState(
    var value: Int = 0,
    var isDragging: Boolean = false,
    var lastMouseX: Float = 0f,
    var accelerationRate: Float = 0f
)

/*fun handleIntInput(
    drawList: ImDrawList,
    state: IntInputState,
    font: imgui.ImFont,
    fontSize: Float,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    textColor: Int,
    backgroundColor: Int,
    selectionColor: Int,
    accentColor: Int,
    placeholder: String = ""
): IntInputState {
    val newState = state.copy()
    val textInputHeight = height * 0.6f
    val dragInputHeight = height * 0.4f
    var y = y
    // Text input
    val textState = TextInputState(
        newState.text.ifEmpty { newState.value.toString() },
        newState.cursorPosition,
        newState.selectionStart,
        newState.selectionEnd,
        newState.isFocused
    )

    // Parse the text input
    newState.value = newState.text.toIntOrNull() ?: newState.value

    y += textInputHeight + 10f
    // Drag input
    val dragRect = ImRect(x, y + textInputHeight, x + width, y + height)
    drawList.addRectFilled(dragRect.min.x, dragRect.min.y, dragRect.max.x, dragRect.max.y, backgroundColor)

    if (ImGui.isMouseHoveringRect(
            dragRect.min.x,
            dragRect.min.y,
            dragRect.max.x,
            dragRect.max.y
        ) && ImGui.isMouseDown(0)
    ) {
        if (!newState.isDragging) {
            ImGui.setMouseCursor(ImGuiMouseCursor.None)
            newState.isDragging = true
            newState.dragStartMouseX = ImGui.getMousePos().x
            newState.dragStartMouseY = ImGui.getMousePos().y
            newState.dragStartValue = newState.value
        }

        val dragDelta = ImGui.getMouseDragDelta(0).x
        val dragSpeed = when {
            Platform.isKeyDown(Runtime.Key.LEFT_SHIFT) -> 0.1f
            Platform.isKeyDown(Runtime.Key.LEFT_CONTROL) -> 10f
            else -> 1f
        }

        newState.value = (newState.dragStartValue + (dragDelta * dragSpeed).toInt()).coerceIn(
            Int.MIN_VALUE,
            Int.MAX_VALUE
        )
        newState.text = newState.value.toString()

        // Reset mouse position to start of drag
        Platform.setMousePosition(newState.dragStartMouseX.toDouble(), newState.dragStartMouseY.toDouble())

    } else if (newState.isDragging) {
        newState.isDragging = false
        ImGui.setMouseCursor(ImGuiMouseCursor.Arrow)

    }

    // Draw drag handle
    val handleWidth = 10f
    val handleX = x + (width - handleWidth) * (newState.value - Int.MIN_VALUE).toFloat() / (Int.MAX_VALUE - Int.MIN_VALUE).toFloat()
    drawList.addRectFilled(handleX, dragRect.min.y, handleX + handleWidth, dragRect.max.y, accentColor)

    // Display current value
    val valueText = newState.value.toString()
    val textSize = ImGui.calcTextSize(valueText)
    drawList.addText(
        font,
        fontSize,
        x + width - textSize.x - 5,
        y + textInputHeight + (dragInputHeight - fontSize) / 2,
        textColor,
        valueText
    )
    y -= textInputHeight + 5f
    val updatedTextState = handleUniversalTextInput(
        drawList,
        textState,
        font,
        fontSize,
        x,
        y,
        width,
        textInputHeight,
        textColor,
        backgroundColor,
        selectionColor,
        InputType.NUMBER,
        placeholder
    )

    newState.text = updatedTextState.text
    newState.cursorPosition = updatedTextState.cursorPosition
    newState.selectionStart = updatedTextState.selectionStart
    newState.selectionEnd = updatedTextState.selectionEnd
    newState.isFocused = updatedTextState.isFocused


    return newState
}*/
/*
fun handleIntInput(
    drawList: ImDrawList,
    state: IntInputState,
    font: imgui.ImFont,
    fontSize: Float,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    textColor: Int,
    backgroundColor: Int,
    selectionColor: Int,
    accentColor: Int,
    placeholder: String = ""
): IntInputState {
    val newState = state.copy()
    val textInputHeight = height * 0.6f
    val sliderHeight = height * 0.4f
    val padding = 20f  // Add some padding between text input and slider

    // Text input
    val textState = TextInputState(
        newState.text,
        newState.cursorPosition,
        newState.selectionStart,
        newState.selectionEnd,
        newState.isFocused,
        newState.placeHolder
    )

    val updatedTextState = handleUniversalTextInput(
        ImGui.getForegroundDrawList(),
        textState,
        font,
        fontSize,
        x,
        y,
        width,
        20f,
        textColor,
        backgroundColor,
        selectionColor,
        InputType.NUMBER,
        placeholder
    )

    newState.text = updatedTextState.text
    newState.cursorPosition = updatedTextState.cursorPosition
    newState.selectionStart = updatedTextState.selectionStart
    newState.selectionEnd = updatedTextState.selectionEnd
    newState.isFocused = updatedTextState.isFocused
    newState.placeHolder = updatedTextState.placeHolder

    // Parse the text input
    newState.value = newState.text.toIntOrNull() ?: 0

    // Slider
    val sliderState = SliderState(newState.value, newState.isDragging, newState.lastMouseX, newState.accelerationRate)
    val updatedSliderState = slider(
        drawList,
        sliderState,
        "",
        x,
        y + textInputHeight + padding,  // Position slider below text input with padding
        width,
        sliderHeight,
        newState.minValue,
        newState.maxValue,
        textColor,
        backgroundColor,
        ImColor.rgba(80, 80, 80, 255),
        accentColor
    )

    // Minus button
    val buttonSize = 20f
    val buttonY = y + height + 5

    if (renderButton(
            drawList,
            "-",
            x,
            buttonY + 15f,
            buttonSize,
            buttonSize,
            textColor,
            ImColor.rgba(80, 80, 80, 255)
        )
    ) {
        newState.value = (newState.value - 1).coerceIn(newState.minValue, newState.maxValue)
        newState.text = newState.value.toString()
    }

    // Plus button
    if (renderButton(
            drawList,
            "+",
            x + width - buttonSize,
            buttonY + 15f,
            buttonSize,
            buttonSize,
            textColor,
            ImColor.rgba(80, 80, 80, 255)
        )
    ) {
        newState.value = (newState.value + 1).coerceIn(newState.minValue, newState.maxValue)
        newState.text = newState.value.toString()
    }

    // Update state from slider if it changed
    if (updatedSliderState.value != newState.value) {
        newState.value = updatedSliderState.value
        newState.text = newState.value.toString()
    }
    newState.isDragging = updatedSliderState.isDragging
    if (newState.isDragging) {
        ImGui.setMouseCursor(ImGuiMouseCursor.None)
    }
    newState.lastMouseX = updatedSliderState.lastMouseX
    newState.accelerationRate = updatedSliderState.accelerationRate

    return newState
}*/

/*fun handleFloatInput(
    drawList: ImDrawList,
    state: InputState,
    font: ImFont,
    fontSize: Float,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    textColor: Int,
    backgroundColor: Int,
    accentColor: Int,
    placeholder: String = ""
): InputState {
    val updatedState = handleUniversalInput(
        drawList, state, font, fontSize, x, y, width, height,
        textColor, backgroundColor, accentColor, InputType.NUMBER, placeholder
    )

    // Ensure the value is updated when in text input mode
    if (updatedState.numberInputMode == NumberInputMode.TEXT) {
        updatedState.value = updatedState.text.toFloatOrNull() ?: updatedState.value
    }

    return updatedState
}*/



fun handleIntInput(
    drawList: ImDrawList,
    state: InputState,
    font: ImFont,
    fontSize: Float,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    textColor: Int,
    backgroundColor: Int,
    accentColor: Int,
    placeholder: String = ""
): InputState {
    state.decimalPlaces = 0
    val updatedState = handleUniversalInput(
        drawList, state, font, fontSize, x, y, width, height,
        textColor, backgroundColor, accentColor, InputType.NUMBER, placeholder
    )

    // Round the float value to the nearest integer
    updatedState.value = updatedState.value.roundToInt().toFloat()
    updatedState.text = updatedState.value.toInt().toString()

    return updatedState
}


enum class NumberInputMode { DRAG, TEXT }

data class InputState(
    var text: String = "",
    var cursorPosition: Int = 0,
    var selectionStart: Int = 0,
    var selectionEnd: Int = 0,
    var isFocused: Boolean = false,
    var placeHolder: String = "",
    var value: Float = 0f,
    var isDragging: Boolean = false,
    var dragStartMouseX: Float = 0f,
    var dragStartValue: Float = 0f,
    var dragSensitivity: Float = 0.01f,
    var decimalPlaces: Int = 2,
    var isTextMode: Boolean = false
)

fun handleUniversalInput(
    drawList: ImDrawList,
    state: InputState,
    font: ImFont,
    fontSize: Float,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    textColor: Int,
    backgroundColor: Int,
    accentColor: Int,
    inputType: InputType = InputType.TEXT,
    placeholder: String = ""
): InputState {
    val newState = state.copy()
    val isHovered = ImGui.isMouseHoveringRect(x, y, x + width, y + height)

    // Draw background
    drawList.addRectFilled(x, y, x + width, y + height, backgroundColor, 4f)

    when (inputType) {
        InputType.TEXT -> handleTextInput(
            newState,
            drawList,
            font,
            fontSize,
            x,
            y,
            width,
            height,
            textColor,
            placeholder
        )

        InputType.NUMBER -> handleNumberInput(
            newState,
            drawList,
            font,
            fontSize,
            x,
            y,
            width,
            height,
            textColor,
            accentColor,
            isHovered
        )
    }

    // Handle focus
    if (ImGui.isMouseClicked(0) && isHovered) {
        newState.isFocused = true
        newState.isTextMode = true
        ImGui.setKeyboardFocusHere()
    } else if (ImGui.isMouseClicked(0) && !isHovered) {
        newState.isFocused = false
        newState.isTextMode = false
    }

    return newState
}


private fun handleDragInput(state: InputState, mousePos: ImVec2, x: Float, y: Float, width: Float, height: Float) {
    if (state.isDragging) {
        if (ImGui.isMouseDown(0)) {
            val delta = ImGui.getMouseDragDeltaX()
            state.value += delta * state.dragSensitivity
            state.text = String.format("%.2f", state.value)
            //Reset delta
//            ImGui.resetMouseDragDelta(0)

            // Set mouse position to center of slider
            Platform.setMousePosition((x + width / 2).toDouble(), (y + height / 2).toDouble())
            ImGui.setMouseCursor(ImGuiMouseCursor.None)
        } else {
            state.isDragging = false
        }
    } else if (ImGui.isMouseClicked(0) && ImGui.isMouseHoveringRect(x, y, x + width, y + height)) {
        state.isDragging = true
        state.dragStartMouseX = mousePos.x
        state.dragStartValue = state.value
        ImGui.setMouseCursor(ImGuiMouseCursor.None)

    }
}


private fun handleTextInput(
    state: InputState,
    drawList: ImDrawList,
    font: ImFont,
    fontSize: Float,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    textColor: Int,
    placeholder: String
) {
    // Draw text or placeholder
    if (state.text.isNotEmpty()) {
        drawList.addText(font, fontSize, x + 5, y + (height - fontSize) / 2, textColor, state.text)
    } else if (placeholder.isNotEmpty()) {
        drawList.addText(
            font,
            fontSize,
            x + 5,
            y + (height - fontSize) / 2,
            ImColor.rgba(128, 128, 128, 255),
            placeholder
        )
    }

    if (state.isFocused) {
        handleKeyboardInput(state)
        drawCursor(state, drawList, font, fontSize, x, y, height, textColor)
    }
}


private fun handleNumberInput(
    state: InputState,
    drawList: ImDrawList,
    font: ImFont,
    fontSize: Float,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    textColor: Int,
    accentColor: Int,
    isHovered: Boolean
) {
    val mousePos = ImGui.getMousePos()

    if (isHovered && ImGui.isMouseClicked(0)) {
        state.isDragging = true
        state.dragStartMouseX = mousePos.x
        state.dragStartValue = state.value
    }

    if (state.isDragging) {
        if (ImGui.isMouseDown(0)) {
            val delta = (mousePos.x - state.dragStartMouseX) * state.dragSensitivity
            state.value += delta

            // Adjust sensitivity based on modifier keys
            state.dragSensitivity = when {
                Platform.isKeyDown(ClientRuntime.Key.LEFT_SHIFT) -> 0.001f
                Platform.isKeyDown(ClientRuntime.Key.LEFT_CONTROL) -> 0.1f
                else -> 0.01f
            }

            println("state.text: ${state.text}, state.value: ${state.value}")
            // Update text representation
            state.text = formatNumberValue(state.value, state.decimalPlaces)
            // Reset mouse position to start of drag
            Platform.setMousePosition((x + width / 2).toDouble(), (y + height / 2).toDouble())
            ImGui.setMouseCursor(ImGuiMouseCursor.None)
        } else {
            state.isDragging = false
            ImGui.setMouseCursor(ImGuiMouseCursor.Arrow)
        }
    } else if (state.isFocused) {
        handleKeyboardInput(state)
    }

    // Draw value
    drawList.addText(font, fontSize, x + 5, y + (height - fontSize) / 2, textColor, state.text)

    // Draw drag indicator
    if (isHovered || state.isDragging) {
        drawList.addRectFilled(x + width - 5, y, x + width, y + height, accentColor)
    }

    if (state.isFocused && !state.isDragging) {
        drawCursor(state, drawList, font, fontSize, x, y, height, textColor)
    }
}

private fun drawCursor(
    state: InputState,
    drawList: ImDrawList,
    font: ImFont,
    fontSize: Float,
    x: Float,
    y: Float,
    height: Float,
    textColor: Int
) {
    if ((ImGui.getTime() * 2).toInt() % 2 == 0) {
        if (state.cursorPosition >= state.text.length) {
            drawList.addLine(
                x + 5 + ImGui.calcTextSize(state.text).x,
                y + 2,
                x + 5 + ImGui.calcTextSize(state.text).x,
                y + height - 2,
                textColor
            )
        } else {
            val cursorX = x + 5 + ImGui.calcTextSize(state.text.substring(0, state.cursorPosition)).x
            drawList.addLine(cursorX, y + 2, cursorX, y + height - 2, textColor)
        }
    }
}

private fun formatNumberValue(value: Float, decimalPlaces: Int): String {
    return if (decimalPlaces == 0) {
        value.toInt().toString()
    } else {
        String.format("%.${decimalPlaces}f", value)
    }
}

private fun handleKeyboardInput(state: InputState) {
    for (key in 32..126) { // ASCII printable characters
        if (ImGui.isKeyPressed(key)) {
            val char = if (Platform.isKeyDown(ClientRuntime.Key.LEFT_SHIFT) || Platform.isKeyDown(ClientRuntime.Key.RIGHT_SHIFT)) {
                key.toChar().uppercaseChar()
            } else {
                key.toChar().lowercaseChar()
            }
            if (char.isDigit() || char == '.' || (char == '-' && state.cursorPosition == 0)) {
                insertCharacters(state, char)
            }
        }
    }

    // Handle backspace
    if (ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.Backspace)) && state.cursorPosition > 0 && state.cursorPosition <= state.text.length) {
        state.text = state.text.removeRange(state.cursorPosition - 1, state.cursorPosition)
        state.cursorPosition--
    }

    // Handle delete
    if (ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.Delete)) && state.cursorPosition < state.text.length) {
        state.text = state.text.removeRange(state.cursorPosition, state.cursorPosition + 1)
    }

    // Handle arrow keys
    if (ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.LeftArrow)) && state.cursorPosition > 0) {
        state.cursorPosition--
    }
    if (ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.RightArrow)) && state.cursorPosition < state.text.length) {
        state.cursorPosition++
    }

    // Update value from text
    state.value = state.text.toFloatOrNull() ?: state.value
}


private fun insertCharacters(state: InputState, char: Char) {
    if (state.cursorPosition <= state.text.length) {
        state.text = state.text.substring(0, state.cursorPosition) + char + state.text.substring(state.cursorPosition)
        state.cursorPosition++
    }
}

private fun handleDragInput(
    state: InputState,
    mousePos: ImVec2,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    isHovered: Boolean
) {
    if (isHovered && ImGui.isMouseClicked(0)) {
        state.isDragging = true
        state.dragStartMouseX = mousePos.x
        state.dragStartValue = state.value
        ImGui.setMouseCursor(ImGuiMouseCursor.None)
    }

    if (state.isDragging) {
        if (ImGui.isMouseDown(0)) {
            val delta = (mousePos.x - state.dragStartMouseX) * state.dragSensitivity
            state.value = state.dragStartValue + delta

            // Adjust sensitivity based on modifier keys
            state.dragSensitivity = when {
                Platform.isKeyDown(ClientRuntime.Key.LEFT_SHIFT) -> 0.001f
                Platform.isKeyDown(ClientRuntime.Key.LEFT_CONTROL) -> 0.1f
                else -> 0.01f
            }

            // Update text representation
            state.text = String.format("%.2f", state.value)

            // Reset mouse position to start of drag
            Platform.setMousePosition((x + width / 2).toDouble(), (y + height / 2).toDouble())
            ImGui.setMouseCursor(ImGuiMouseCursor.None)
        } else {
            state.isDragging = false
            ImGui.setMouseCursor(ImGuiMouseCursor.Arrow)
        }
    }
}


private fun insertCharacter(state: InputState, char: Char) {
    state.text = state.text.substring(0, state.cursorPosition) + char + state.text.substring(state.cursorPosition)
    state.cursorPosition++
}

fun handleFloatInput(
    drawList: ImDrawList,
    state: InputState,
    font: ImFont,
    fontSize: Float,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    textColor: Int,
    backgroundColor: Int,
    accentColor: Int,
    placeholder: String = ""
): InputState {
    state.decimalPlaces = 2
    val updatedState = handleUniversalInput(
        drawList, state, font, fontSize, x, y, width, height,
        textColor, backgroundColor, accentColor, InputType.NUMBER, placeholder
    )

    // Ensure the value is updated when in text input mode
    updatedState.value = updatedState.text.toFloatOrNull() ?: updatedState.value

    return updatedState
}


//fun handleFloatInput(
//    drawList: ImDrawList,
//    state: FloatInputState,
//    font: imgui.ImFont,
//    fontSize: Float,
//    x: Float,
//    y: Float,
//    width: Float,
//    height: Float,
//    textColor: Int,
//    backgroundColor: Int,
//    selectionColor: Int,
//    accentColor: Int,
//    placeholder: String = ""
//): FloatInputState {
//    val newState = state.copy()
//    val textState = TextInputState(
//        newState.text, newState.cursorPosition, newState.selectionStart, newState.selectionEnd, newState.isFocused
//    )
//
//
//    // Render a small draggable area
//    val dragX = x + width - height
//    drawList.addRectFilled(dragX, y, dragX + height, y + height, accentColor, 4f)
//    drawList.addText(font, fontSize, dragX + 5, y + 5, textColor, "↕")
//
//    if (ImGui.isMouseDragging(0) && ImGui.isMouseHoveringRect(dragX, y, dragX + height, y + height)) {
//        newState.draggable = true
//        newState.mouseStartPos = mousePosition
//    }
//
//    val updatedTextState = handleUniversalTextInput(
//        drawList,
//        textState,
//        font,
//        fontSize,
//        x,
//        y,
//        width - height,
//        height,
//        textColor,
//        backgroundColor,
//        selectionColor,
//        InputType.NUMBER,
//        placeholder
//    )
//
//    newState.text = updatedTextState.text
//    newState.cursorPosition = updatedTextState.cursorPosition
//    newState.selectionStart = updatedTextState.selectionStart
//    newState.selectionEnd = updatedTextState.selectionEnd
//    newState.isFocused = updatedTextState.isFocused
//
//
//    if (newState.draggable) {
//        var force = 0.01f
//        if (Platform.isKeyDown(Runtime.Key.LEFT_SHIFT)) {
//            force = 0.001f
//        }
//        if (Platform.isKeyDown(Runtime.Key.LEFT_CONTROL)) {
//            force = 0.1f
//        }
//
//        val dragDelta = ImGui.getMouseDragDelta(0).y
//        newState.value += dragDelta * force
//        newState.text = String.format("%.2f", newState.value)
//        //Move the mouse back to the center of the draggable area
//        //Hide the mouse cursor
//        ImGui.setMouseCursor(ImGuiMouseCursor.None)
//        Platform.setMousePosition(newState.mouseStartPos.x.toDouble(), newState.mouseStartPos.y.toDouble())
//    }
//
//
//
//
//    if (!ImGui.isMouseDown(0) && newState.draggable) {
//        newState.draggable = false
//        //reset the mouse cursor
//    }
//
//    newState.value = newState.text.toFloatOrNull() ?: 0f
//    return newState
//}

fun handleBooleanInput(
    drawList: ImDrawList,
    state: BooleanInputState,
    font: imgui.ImFont,
    fontSize: Float,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    textColor: Int,
    backgroundColor: Int,
    activeColor: Int
): BooleanInputState {
    val newState = state.copy()

    val toggleWidth = min(width, height * 2)
    val knobSize = height - 4

    // Draw background
    drawList.addRectFilled(x, y, x + toggleWidth, y + height, backgroundColor, height / 2)

    // Draw knob
    val knobX = if (newState.value) x + toggleWidth - knobSize - 2 else x + 2
    drawList.addCircleFilled(
        knobX + knobSize / 2, y + height / 2, knobSize / 2, if (newState.value) activeColor else textColor
    )

    // Handle click
    if (ImGui.isMouseClicked(0) && ImGui.isMouseHoveringRect(x, y, x + toggleWidth, y + height)) {
        newState.value = !newState.value
        newState.isFocused = true
    }

    // Add text label
    val labelX = x + toggleWidth + 5
    drawList.addText(
        font, fontSize, labelX, y + (height - fontSize) / 2, textColor, if (newState.value) "ON" else "OFF"
    )

    return newState
}

fun handleVec2fInput(
    drawList: ImDrawList,
    state: Vector2f,
    font: ImFont,
    fontSize: Float,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    textColor: Int,
    backgroundColor: Int,
    accentColor: Int
): Vector2f {
    val newState = Vector2f(state)
    val componentWidth = width / 2 - 5

    // X component
    drawList.addText(font, fontSize, x, y - fontSize - 2, textColor, "X:")
    val xState = InputState(value = newState.x, text = newState.x.toString())
    val updatedXState = handleFloatInput(
        drawList,
        xState,
        font,
        fontSize,
        x,
        y,
        componentWidth,
        height,
        textColor,
        backgroundColor,
        accentColor,
        "0.0"
    )
    newState.x = updatedXState.value

    // Y component
    drawList.addText(font, fontSize, x + componentWidth + 5, y - fontSize - 2, textColor, "Y:")
    val yState = InputState(value = newState.y, text = newState.y.toString())
    val updatedYState = handleFloatInput(
        drawList,
        yState,
        font,
        fontSize,
        x + componentWidth + 5,
        y,
        componentWidth,
        height,
        textColor,
        backgroundColor,
        accentColor,
        "0.0"
    )
    newState.y = updatedYState.value

    return newState
}


fun handleVec3fInput(
    drawList: ImDrawList,
    state: Vector3f,
    font: ImFont,
    fontSize: Float,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    textColor: Int,
    backgroundColor: Int,
    accentColor: Int
): Vector3f {
    val newState = Vector3f(state)
    val componentWidth = width / 3 - 5

    // X component
    drawList.addText(font, fontSize, x, y - fontSize - 2, textColor, "X:")
    val xState = InputState(value = newState.x, text = newState.x.toString())
    val updatedXState = handleFloatInput(
        drawList,
        xState,
        font,
        fontSize,
        x,
        y,
        componentWidth,
        height,
        textColor,
        backgroundColor,
        accentColor,
        "0.0"
    )
    newState.x = updatedXState.value

    // Y component
    drawList.addText(font, fontSize, x + componentWidth + 5, y - fontSize - 2, textColor, "Y:")
    val yState = InputState(value = newState.y, text = newState.y.toString())
    val updatedYState = handleFloatInput(
        drawList,
        yState,
        font,
        fontSize,
        x + componentWidth + 5,
        y,
        componentWidth,
        height,
        textColor,
        backgroundColor,
        accentColor,
        "0.0"
    )
    newState.y = updatedYState.value

    // Z component
    drawList.addText(font, fontSize, x + 2 * (componentWidth + 5), y - fontSize - 2, textColor, "Z:")
    val zState = InputState(value = newState.z, text = newState.z.toString())
    val updatedZState = handleFloatInput(
        drawList,
        zState,
        font,
        fontSize,
        x + 2 * (componentWidth + 5),
        y,
        componentWidth,
        height,
        textColor,
        backgroundColor,
        accentColor,
        "0.0"
    )
    newState.z = updatedZState.value

    return newState
}

fun handleVec4fInput(
    drawList: ImDrawList,
    state: Vector4f,
    font: ImFont,
    fontSize: Float,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    textColor: Int,
    backgroundColor: Int,
    accentColor: Int
): Vector4f {
    val newState = Vector4f(state)
    val componentWidth = width / 4 - 5

    // X component
    drawList.addText(font, fontSize, x, y - fontSize - 2, textColor, "X:")
    val xState = InputState(value = newState.x, text = newState.x.toString())
    val updatedXState = handleFloatInput(
        drawList,
        xState,
        font,
        fontSize,
        x,
        y,
        componentWidth,
        height,
        textColor,
        backgroundColor,
        accentColor,
        "0.0"
    )
    newState.x = updatedXState.value

    // Y component
    drawList.addText(font, fontSize, x + componentWidth + 5, y - fontSize - 2, textColor, "Y:")
    val yState = InputState(value = newState.y, text = newState.y.toString())
    val updatedYState = handleFloatInput(
        drawList,
        yState,
        font,
        fontSize,
        x + componentWidth + 5,
        y,
        componentWidth,
        height,
        textColor,
        backgroundColor,
        accentColor,
        "0.0"
    )
    newState.y = updatedYState.value

    // Z component
    drawList.addText(font, fontSize, x + 2 * (componentWidth + 5), y - fontSize - 2, textColor, "Z:")
    val zState = InputState(value = newState.z, text = newState.z.toString())
    val updatedZState = handleFloatInput(
        drawList,
        zState,
        font,
        fontSize,
        x + 2 * (componentWidth + 5),
        y,
        componentWidth,
        height,
        textColor,
        backgroundColor,
        accentColor,
        "0.0"
    )
    newState.z = updatedZState.value

    // W component
    drawList.addText(font, fontSize, x + 3 * (componentWidth + 5), y - fontSize - 2, textColor, "W:")
    val wState = InputState(value = newState.w, text = newState.w.toString())
    val updatedWState = handleFloatInput(
        drawList,
        wState,
        font,
        fontSize,
        x + 3 * (componentWidth + 5),
        y,
        componentWidth,
        height,
        textColor,
        backgroundColor,
        accentColor,
        "0.0"
    )
    newState.w = updatedWState.value

    return newState
}


fun handleVec4iInput(
    drawList: ImDrawList,
    state: Vector4i,
    font: ImFont,
    fontSize: Float,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    textColor: Int,
    backgroundColor: Int,
    accentColor: Int
): Vector4i {
    val newState = Vector4i(state)
    val componentWidth = width / 4 - 5

    // X component
    drawList.addText(font, fontSize, x, y - fontSize - 2, textColor, "X:")
    val xState = InputState(value = newState.x.toFloat(), text = newState.x.toString())
    val updatedXState = handleFloatInput(
        drawList,
        xState,
        font,
        fontSize,
        x,
        y,
        componentWidth,
        height,
        textColor,
        backgroundColor,
        accentColor,
        "0"
    )
    newState.x = updatedXState.value.toInt()

    // Y component
    drawList.addText(font, fontSize, x + componentWidth + 5, y - fontSize - 2, textColor, "Y:")
    val yState = InputState(value = newState.y.toFloat(), text = newState.y.toString())
    val updatedYState = handleFloatInput(
        drawList,
        yState,
        font,
        fontSize,
        x + componentWidth + 5,
        y,
        componentWidth,
        height,
        textColor,
        backgroundColor,
        accentColor,
        "0"
    )
    newState.y = updatedYState.value.toInt()

    // Z component
    drawList.addText(font, fontSize, x + 2 * (componentWidth + 5), y - fontSize - 2, textColor, "Z:")
    val zState = InputState(value = newState.z.toFloat(), text = newState.z.toString())
    val updatedZState = handleFloatInput(
        drawList,
        zState,
        font,
        fontSize,
        x + 2 * (componentWidth + 5),
        y,
        componentWidth,
        height,
        textColor,
        backgroundColor,
        accentColor,
        "0"
    )
    newState.z = updatedZState.value.toInt()

    // W component
    drawList.addText(font, fontSize, x + 3 * (componentWidth + 5), y - fontSize - 2, textColor, "W:")
    val wState = InputState(value = newState.w.toFloat(), text = newState.w.toString())
    val updatedWState = handleFloatInput(
        drawList,
        wState,
        font,
        fontSize,
        x + 3 * (componentWidth + 5),
        y,
        componentWidth,
        height,
        textColor,
        backgroundColor,
        accentColor,
        "0"
    )
    newState.w = updatedWState.value.toInt()

    return newState
}

// An action is a simple element that can be clicked, in a wheel menu.
//data class Action(
//    val label: String,
//    val icon: String,
//    val hoverColor: Int = ImColor.rgba(33, 150, 243, 255),
//    val onClick: (Action) -> Unit
//)

// The action menu state contains the position of the menu, the icon to display when closed and open, the size of the icons, and the actions to display.
//data class ActionMenuState(
//    var x: Float,
//    var y: Float,
//    val closedIcon: String = FontAwesome.Gear,
//    val openIcon: String = FontAwesome.Gear,
//    var iconSize: Float = 16f,
//    val actions: Set<Action> = emptySet(),
//    var isOpen: Boolean = false
//)
//
//private val Icon by lazy { Fonts.getFamily("FontAwesome")["Regular"] }
//private val Bold by lazy { Fonts.getFamily("Inter")["Bold"] }
//private val Regular by lazy { Fonts.getFamily("Inter")["Regular"] }
//
//
//// A wheel menu that has hover effects and can be clicked. When clicked, and the mouse is held down, the menu will stay open,
//// the mouse will be hidden with the cursor at the center of the menu, and the menu will close when the mouse is released.
//// When the menu is open, the actions will be displayed in a circle around the center of the menu.
//// When the menu is closed, only the icon will be displayed.
//fun handleActionMenu(
//    drawList: ImDrawList,
//    state: ActionMenuState,
//    fontSize: Int,
//    textColor: Int,
//    backgroundColor: Int,
//    accentColor: Int
//): ActionMenuState {
//    val newState = state.copy()
//    //Icon font
//    val iconFont = Icon[fontSize]
//    val boldFont = Bold[fontSize + 2]
//    val regularFont = Regular[fontSize]
//
//    // Draw the icon
//    val icon = if (newState.actions.isEmpty()) newState.closedIcon else newState.openIcon
//    val iconWidth = iconFont.sizeOf(icon).x
//    drawList.addText(
//        bpm.client.utils.Icon[fontSize],
//        fontSize.toFloat(), newState.x, newState.y, textColor, icon
//    )
//    val isHovered = ImGui.isMouseHoveringRect(
//        newState.x,
//        newState.y,
//        newState.x + iconWidth,
//        newState.y + fontSize
//    )
//    val isMouseDown = ImGui.isMouseDown(0)
//    // Handle click
//    if (isHovered && isMouseDown) {
//        newState.isOpen = true
//        ImGui.setMouseCursor(ImGuiMouseCursor.None)
//        Platform.setMousePosition(newState.x + iconWidth / 2.0, newState.y + fontSize / 2.0)
//    }
//
//    if (!isMouseDown && newState.isOpen) {
//        newState.isOpen = false
//        ImGui.setMouseCursor(ImGuiMouseCursor.Arrow)
//    }
//
//    // Draw the actions
//    if (newState.isOpen) {
//        val actionCount = newState.actions.size
//        val angleStep = 2 * PI / actionCount
//        val radius = 50f
//        val actionSize = 40f
//        val actionIconSize = 24f
//        val actionIconPadding = 5f
//        val actionTextPadding = 5f
//        val actionTextSize = 14f
//        val actionTextHeight = ImGui.calcTextSize("A").y
//        val actionTextWidth = actionSize - 2 * actionTextPadding
//        val actionTextRect = ImRect(0f, 0f, actionTextWidth, actionTextHeight)
//        val actionIconRect = ImRect(0f, 0f, actionIconSize, actionIconSize)
//        val actionRect = ImRect(0f, 0f, actionSize, actionSize)
//        val center = ImVec2(newState.x + iconWidth / 2, newState.y + fontSize / 2)
//        var index = 0
//        for ((label, action) in newState.actions) {
//            val angle = index * angleStep
//            val actionX = center.x + radius * cos(angle) - actionSize / 2
//            val actionY = center.y + radius * sin(angle) - actionSize / 2
//            val isActionHovered = ImGui.isMouseHoveringRect(
//                actionX.toFloat(), actionY.toFloat(),
//                (actionX + actionSize).toFloat(), (actionY + actionSize).toFloat()
//            )
//            val actionColor = if (isActionHovered) action.hoverColor else backgroundColor
//            drawList.addRectFilled(
//                actionX.toFloat(), actionY.toFloat(),
//                (actionX + actionSize).toFloat(), (actionY + actionSize).toFloat(), actionColor, 4f
//            )
//            drawList.addText(
//                boldFont, actionTextSize, (actionX + actionTextPadding).toFloat(),
//                (actionY + actionTextPadding).toFloat(), textColor, label
//            )
//            drawList.addText(
//                iconFont, actionIconSize, (actionX + actionSize / 2 - actionIconSize / 2).toFloat(),
//                (actionY + actionSize / 2 - actionIconSize / 2).toFloat(), textColor, action.icon
//            )
//            if (isActionHovered && ImGui.isMouseClicked(0)) {
//                action.onClick()
//            }
//            index++
//        }
//    }
//
//
//    return newState
//}

fun ClosedFloatingPointRange<Float>.step(step: Float): Sequence<Float> {
    val sequence = generateSequence(start) { previous ->
        if (previous + step > endInclusive) null
        else previous + step
    }
    return sequence.takeWhile { it <= endInclusive }
}


data class WheelAction(
    var label: String,
    val icon: String,
    val color: Int = ImColor.rgba(60, 60, 60, 255),
    val hoverColor: Int = ImColor.rgba(100, 100, 100, 255),
    val onClick: () -> Unit
)

class ActionWheelMenu(
    internal var centerX: Float,
    internal var centerY: Float,
    private var baseRadius: Float = 40f,
    private var baseFontSize: Float = 14f,
    internal val actions: List<WheelAction>,
    private var innerCircleScale: Float = 1f,
    private var outerCircleScale: Float = 1f,
    private var actionIconScale: Float = 1f,
    private var labelScale: Float = 1f,
    private var innerScale: Float = 0.7f,
    private val isTopLevel: Boolean = true,
    private val icon: String = FontAwesome.Gear
) {

    private var isOpen = false
    private val iconFont by lazy { Fonts.getFamily("Fa")["Regular"] }
    private val labelFont by lazy { Fonts.getFamily("Inter")["Regular"] }
    private var originalMousePos: Vector2f? = null
    var selectedActionIndex: Int = -1
    var isActionSelected: Boolean = false
        private set
    private var waitingForSecondRelease = false


    private val scaledInnerRadius get() = baseRadius * innerCircleScale
    private val scaledOuterRadius get() = baseRadius * outerCircleScale
    private val scaledActionIconFontSize get() = baseFontSize * actionIconScale
    private val scaledLabelFontSize get() = baseFontSize * labelScale

    fun reposition(centerX: Float, centerY: Float) {
        this.centerX = centerX
        this.centerY = centerY
    }

    fun render(drawList: ImDrawList): Boolean {
        var actionTriggered = false
        if (isTopLevel) {
            actionTriggered = renderTopLevelMenu(drawList)
        } else {
            actionTriggered = renderSubMenu(drawList)
        }
        return actionTriggered
    }


    private fun renderTopLevelMenu(drawList: ImDrawList): Boolean {
        var actionTriggered = false
        if (!isOpen) {
            renderIcon(drawList)
            if (ImGui.isMouseClicked(0) && isMouseOverIcon()) {
                isOpen = true
                originalMousePos = Platform.getMousePosition()
                ImGui.setMouseCursor(ImGuiMouseCursor.None)
                Platform.setMousePosition(centerX.toDouble(), centerY.toDouble())
            }
        } else {
            renderWheel(drawList)
            handleMouseBehavior()

            if (ImGui.isMouseReleased(0)) {
                if (selectedActionIndex != -1) {
                    isActionSelected = true
                    actions[selectedActionIndex].onClick()
                    actionTriggered = true
                }
                isOpen = false
                restoreMousePosition()
            }
        }
        return actionTriggered
    }

    private fun renderSubMenu(drawList: ImDrawList): Boolean {
        var actionTriggered = false
        renderWheel(drawList)
        handleMouseBehavior()

        if (!waitingForSecondRelease && ImGui.isMouseReleased(0)) {
            waitingForSecondRelease = true
        } else if (waitingForSecondRelease && ImGui.isMouseReleased(0)) {
            if (selectedActionIndex != -1) {
                isActionSelected = true
                actions[selectedActionIndex].onClick()
                actionTriggered = true
            }
            waitingForSecondRelease = false
        }

        return actionTriggered
    }

    private fun renderIcon(drawList: ImDrawList) {
        val iconColor = ImColor.rgba(220, 220, 220, 255)
        val fontIcon = iconFont[scaledActionIconFontSize.toInt()]

        fontIcon.use {
            val iconSize = fontIcon.calcTextSizeA(scaledActionIconFontSize, Float.MAX_VALUE, 0f, icon)
            drawList.addText(
                fontIcon,
                scaledActionIconFontSize,
                centerX - iconSize.x / 2,
                centerY - iconSize.y / 2,
                iconColor,
                icon
            )
        }
    }

    private fun renderWheel(drawList: ImDrawList) {
        drawList.addCircleFilled(centerX, centerY, scaledOuterRadius, ImColor.rgba(30, 30, 30, 200))

        val angleStep = 2 * PI / actions.size
        actions.forEachIndexed { index, action ->
            val startAngle = index * angleStep - PI / 2
            val endAngle = (index + 1) * angleStep - PI / 2
            val isSelected = index == selectedActionIndex

            renderPieSection(drawList, action, startAngle, endAngle, isSelected)
            renderActionIcon(drawList, action, startAngle + angleStep / 2)
        }

        if (selectedActionIndex != -1) {
            renderSelectedLabel(drawList, actions[selectedActionIndex])
        }
    }

    private fun renderPieSection(
        drawList: ImDrawList,
        action: WheelAction,
        startAngle: Double,
        endAngle: Double,
        isSelected: Boolean
    ) {
        val color = if (isSelected) action.hoverColor else action.color
        drawList.pathClear()
        drawList.pathLineTo(centerX, centerY)
        for (angle in (startAngle.toFloat()..endAngle.toFloat()).step(0.1f)) {
            drawList.pathLineTo(
                centerX + scaledInnerRadius * cos(angle),
                centerY + scaledInnerRadius * sin(angle)
            )
        }
        drawList.pathFillConvex(color)
    }

    private fun renderActionIcon(drawList: ImDrawList, action: WheelAction, angle: Double) {
        val x = centerX + (scaledInnerRadius * innerScale) * cos(angle).toFloat()
        val y = centerY + (scaledInnerRadius * innerScale) * sin(angle).toFloat()

        iconFont[scaledActionIconFontSize.toInt()].use { fontIcon ->
            val iconSize = fontIcon.calcTextSizeA(scaledActionIconFontSize, Float.MAX_VALUE, 0f, action.icon)
            drawList.addText(
                fontIcon,
                scaledActionIconFontSize,
                x - iconSize.x / 2,
                y - iconSize.y / 2,
                ImColor.rgba(220, 220, 220, 255),
                action.icon
            )
        }
    }

    private fun renderSelectedLabel(drawList: ImDrawList, action: WheelAction) {
        labelFont[scaledLabelFontSize.toInt()].use { fontLabel ->
            val labelSize = fontLabel.calcTextSizeA(scaledLabelFontSize, Float.MAX_VALUE, 0f, action.label)
            val labelX = centerX - labelSize.x / 2
            val labelY = centerY + scaledOuterRadius + 25 * labelScale

            drawList.addRectFilled(
                labelX - 5 * labelScale,
                labelY - 2 * labelScale,
                labelX + labelSize.x + 5 * labelScale,
                labelY + labelSize.y + 2 * labelScale,
                ImColor.rgba(40, 40, 40, 220)
            )
            drawList.addText(
                fontLabel,
                scaledLabelFontSize,
                labelX,
                labelY,
                ImColor.rgba(220, 220, 220, 255),
                action.label
            )
        }
    }


    private fun handleMouseBehavior() {
        val mousePos = Platform.getMousePosition()
        val dx = mousePos.x - centerX
        val dy = mousePos.y - centerY
        val distanceFromCenter = sqrt(dx * dx + dy * dy)

        if (distanceFromCenter > scaledInnerRadius) {
            val angle = atan2(dy, dx)
            val newX = centerX + scaledInnerRadius * cos(angle)
            val newY = centerY + scaledInnerRadius * sin(angle)
            Platform.setMousePosition(newX.toDouble(), newY.toDouble())
        }

        val mouseAngle = (atan2(dy, dx) + PI * 2.5) % (PI * 2)
        selectedActionIndex = (mouseAngle / (PI * 2 / actions.size)).toInt() % actions.size
        ImGui.setMouseCursor(ImGuiMouseCursor.None)
    }

    private fun restoreMousePosition() {
        originalMousePos?.let {
            Platform.setMousePosition(it.x.toDouble(), it.y.toDouble())
            ImGui.setMouseCursor(ImGuiMouseCursor.Arrow)
        }
        originalMousePos = null
        selectedActionIndex = -1
        isActionSelected = false
    }

    private fun isMouseOverIcon(): Boolean {
        val mousePos = ImGui.getMousePos()
        val dx = mousePos.x - centerX
        val dy = mousePos.y - centerY
        return sqrt(dx * dx + dy * dy) <= scaledActionIconFontSize / 2
    }
}

private fun ImRect.contains(mousePos: ImVec2?): Boolean {
    return mousePos != null && mousePos.x >= min.x && mousePos.x <= max.x && mousePos.y >= min.y && mousePos.y <= max.y
}

