package bpm.client.render.markdown

import bpm.client.font.Fonts
import bpm.client.utils.use
import bpm.common.logging.KotlinLogging
import imgui.ImDrawList
import imgui.ImFont
import imgui.ImGui
import imgui.ImVec2

class MarkdownFont {

    var isBold: Boolean = false
    var isItalic: Boolean = false
    var isSemiBold: Boolean = false
    var isExtraBold: Boolean = false
    var isMedium: Boolean = false
    var isIcon: Boolean = false
    var fontSize: Int = 22
    var color: Int = 0xFFFFFFFF.toInt()

    private val inter = Fonts.getFamily("Inter")
    private val iconFont = Fonts.getFamily("Fa")

    val font: ImFont
        get() {
            if (isIcon) {
                return iconFont["Regular"][fontSize]
            }
            val type = when {
                isExtraBold -> "ExtraBold"
                isMedium -> "Medium"
                isSemiBold -> "SemiBold"
                isBold && isItalic -> "BoldItalic"
                isBold -> "Bold"
                isItalic -> "Italic"
                else -> "Regular"
            }
            return inter[type][fontSize]
        }

    private fun copyFrom(other: MarkdownFont) {
        isBold = other.isBold
        isItalic = other.isItalic
        isSemiBold = other.isSemiBold
        isExtraBold = other.isExtraBold
        isMedium = other.isMedium
        isIcon = other.isIcon
        fontSize = other.fontSize
        color = other.color
    }

    private fun reset() {
        isBold = false
        isItalic = false
        isSemiBold = false
        isExtraBold = false
        isMedium = false
        isIcon = false
        fontSize = 22
        color = 0xFFFFFFFF.toInt()
    }
    //draws text on the screen, returns the size of the text
    fun drawText(drawList: ImDrawList, text: String, x: Float, y: Float): ImVec2 {
        val size = font.use { ImGui.calcTextSize(text, true) }
        drawList.addText(font, fontSize.toFloat(), x, y, color, text)
        return size
    }

    companion object {

        private val fontStack = Array(32) { MarkdownFont() }
        private var pointer = 0
        private val logger = KotlinLogging.logger { }

        val current: MarkdownFont
            get() = fontStack[pointer]

        fun push() {
            if (pointer < fontStack.lastIndex) {
                fontStack[pointer + 1].copyFrom(fontStack[pointer])
                ImGui.pushFont(fontStack[pointer + 1].font)
                pointer++
            } else {
                logger.warn { "Font stack overflow, push ignored" }
            }
        }

        fun pop(count: Int = 1) {
            pointer = (pointer - count).coerceAtLeast(0)
            if (pointer == 0) {
                current.reset()
            }
            for (i in 0 until count) {
                ImGui.popFont()
            }
        }
    }
}