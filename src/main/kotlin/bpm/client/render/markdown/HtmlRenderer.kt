package bpm.client.render.markdown

import bpm.client.font.Fonts
import imgui.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

class HtmlRenderer {

    data class HtmlFont(var isBold: Boolean, var isItalic: Boolean, var fontSize: Int) {
        private val family = Fonts.getFamily("Inter")

        val font: ImFont
            get() {
                val type = when {
                    isBold && isItalic -> "BoldItalic"
                    isBold -> "Bold"
                    isItalic -> "Italic"
                    else -> "Regular"
                }
                return family[type][fontSize]
            }
    }

    private var cursorX = 0f
    private var cursorY = 0f
    private var indentLevel = 0
    private var currentColor = ImVec4(1f, 1f, 1f, 1f)
    private val colorStack = mutableListOf<ImVec4>()
    private lateinit var drawList: ImDrawList
    private val font = HtmlFont(false, false, 16)

    private var scale = 1f
    private var baseFontSize = 16f
    private var baseLineHeight = 1.5f
    private var baseSpacing = 5f
    private var indentWidth = 20f
    private var padding = 10f
    private var cornerRadius = 5f
    private var shadowOffset = 5f
    private var lineHeight = 1.5f
    private var lastRenderedElement: Element? = null

    fun render(html: String) {
        val io = ImGui.getIO()
        val screenWidth = io.displaySize.x
        val screenHeight = io.displaySize.y

        // Calculate scale based on screen width
        scale = 1.0f
        baseFontSize = 16f * scale
        baseLineHeight = 1.5f * scale
        baseSpacing = 5f * scale

        // Update scaled values
        indentWidth = 20f * scale
        padding = 10f * scale
        cornerRadius = 5f * scale
        shadowOffset = 5f * scale

        drawList = ImGui.getWindowDrawList()

        // Draw background and shadow
        val startPos = ImVec2(ImGui.getCursorScreenPosX() - padding, ImGui.getCursorScreenPosY() - padding)
        val endPos = ImVec2(startPos.x + ImGui.getWindowContentRegionMaxX() + padding * 2, cursorY + padding)
        val size = ImVec2(endPos.x - startPos.x, endPos.y - startPos.y)

        drawList.addRectFilled(
            startPos.x + padding,
            startPos.y + padding,
            endPos.x - padding * 2,
            endPos.y - padding * 2,
            ImColor.rgba(33, 33, 33, 255),
            20f,
        )

        // If hovered
        if (ImGui.isMouseHoveringRect(startPos.x, startPos.y, endPos.x, endPos.y)) {
            drawList.addRectFilled(
                startPos.x + padding,
                startPos.y + padding,
                endPos.x - padding * 2,
                endPos.y - padding * 2,
                ImColor.rgba(33, 33, 33, 255),
                20f,
            )
        }

        cursorX = ImGui.getCursorScreenPosX()
        cursorY = ImGui.getCursorScreenPosY()

        val doc = Jsoup.parse(html)
        renderElement(doc.body())

        ImGui.setCursorScreenPos(cursorX, cursorY)
    }



    private fun renderChildren(element: Element) {
        element.childNodes().forEach { node ->
            when (node) {
                is Element -> renderElement(node)
                is TextNode -> renderText(node.text())
            }
        }
    }

    private fun renderParagraph(element: Element) {
        val currentColor = ImVec4(currentColor.x, currentColor.y, currentColor.z, currentColor.w)
        this.currentColor = ImVec4(135f / 255f, 135f / 255f, 135f / 255f, 1f)
        if (cursorX > ImGui.getCursorScreenPosX() + padding) {
            renderNewLine()
        }
        cursorY -= padding
        renderChildren(element)
        this.currentColor = currentColor
        renderNewLine()
        cursorY += padding
    }

    private fun renderHeading(element: Element) {
        if (cursorX > ImGui.getCursorScreenPosX() + padding) {
            renderNewLine()
        }

        val level = element.tagName().substring(1).toInt()
        val fontSize = when (level) {
            1 -> baseFontSize * 2f
            2 -> baseFontSize * 1.5f
            3 -> baseFontSize * 1.25f
            else -> baseFontSize * 1.1f
        }
        font.fontSize = fontSize.toInt()

        ImGui.pushFont(font.font)
        val headingText = element.text()
        val textSize = ImGui.calcTextSize(headingText)
        ImGui.popFont()

        val startX = cursorX
        val startY = cursorY

        cursorY += textSize.y

        if (level == 1) {
            val windowWidth = ImGui.getWindowWidth()
            drawList.addLine(
                startX,
                startY + textSize.y + 5f * scale,
                startX + windowWidth - padding * 4,
                startY + textSize.y + 5f * scale,
                ImColor.rgba(currentColor.x, currentColor.y, currentColor.z, currentColor.w),
                1f * scale
            )
        }

        cursorY = startY
        drawList.addText(
            font.font,
            fontSize,
            startX + shadowOffset,
            cursorY + shadowOffset,
            ImColor.rgba(20, 20, 20, 80),
            headingText
        )

        drawList.addText(
            font.font,
            fontSize,
            startX,
            cursorY,
            ImColor.rgba(currentColor.x, currentColor.y, currentColor.z, currentColor.w),
            headingText
        )

        cursorY += textSize.y
        cursorX = ImGui.getCursorScreenPosX() + padding

        font.fontSize = (16 * scale).toInt() // Reset font size
        lastRenderedElement = element
    }

    private fun renderText(text: String) {
        ImGui.pushFont(font.font)
        val words = text.split(" ")
        var spaceLeft = ImGui.getWindowWidth() - (cursorX - ImGui.getCursorScreenPosX()) - padding * 2

        words.forEach { word ->
            val wordSize = ImGui.calcTextSize(word)
            val spaceSize = ImGui.calcTextSize(" ").x * scale
            if (wordSize.x > spaceLeft) {
                cursorY += baseFontSize * lineHeight
                cursorX = ImGui.getCursorScreenPosX() + padding + indentLevel * indentWidth
                spaceLeft = ImGui.getWindowWidth() - padding * 2 - indentLevel * indentWidth
            }

            drawList.addText(
                font.font,
                baseFontSize,
                cursorX,
                cursorY,
                ImColor.rgba(currentColor.x, currentColor.y, currentColor.z, currentColor.w),
                word
            )

            cursorX += wordSize.x + spaceSize
            spaceLeft -= wordSize.x + spaceSize
        }
        ImGui.popFont()
    }

    private fun renderBold(element: Element) {
        font.isBold = true
        cursorX -= 3.5f * scale
        renderChildren(element)
        cursorX += 2f * scale
        font.isBold = false
    }

    private fun renderItalic(element: Element) {
        font.isItalic = true
        cursorX -= 3.5f * scale
        renderChildren(element)
        cursorX += 2f * scale
        font.isItalic = false
    }

    private fun renderLink(element: Element) {
        val linkColor = ImVec4(0.2f, 0.8f, 1f, 1f)
        cursorX -= 3f * scale

        withColor(linkColor) {
            renderChildren(element)
        }

        ImGui.pushFont(font.font)
        val linkText = element.text()
        val textSize = ImGui.calcTextSize(linkText)
        ImGui.popFont()
        cursorX -= 4f * scale

        drawList.addLine(
            cursorX - textSize.x,
            cursorY + textSize.y,
            cursorX,
            cursorY + textSize.y,
            ImColor.rgba(linkColor.x, linkColor.y, linkColor.z, linkColor.w),
            1f * scale
        )

        if (ImGui.isMouseHoveringRect(cursorX - textSize.x, cursorY, cursorX, cursorY + textSize.y)) {
            ImGui.setTooltip(element.attr("href"))
            if (ImGui.isMouseClicked(0)) {
                // Handle link click (e.g., open URL in browser)
            }
        }
    }

    private fun renderImage(element: Element) {
        val src = element.attr("src")
        val alt = element.attr("alt")
        val windowWidth = ImGui.getWindowWidth()

        if (MarkdownGif.isGif(src)) {
            renderGif(src, alt)
        } else {
            renderStaticImage(src, alt)
        }
    }

    private fun renderGif(src: String, alt: String) {
        val gifData = MarkdownGif.loadGif(src) ?: return
        val imageSize = ImVec2(gifData.width * scale, gifData.height * scale)
        val windowWidth = ImGui.getWindowWidth()

        val scaleRatio = if (imageSize.x > windowWidth - padding * 2) {
            (windowWidth - padding * 2) / imageSize.x - 0.1f
        } else {
            0.9f
        }

        imageSize.x *= scaleRatio
        imageSize.y *= scaleRatio

        val startX = calculateImageStartX(windowWidth, imageSize.x, alt)
        val startY = cursorY + padding * 2
        if (cursorX > ImGui.getCursorScreenPosX() + padding) {
            renderNewLine()
        }

        MarkdownGif.renderGif(src, drawList, startX, startY, scaleRatio * scale)

        cursorY += imageSize.y + padding
        cursorX = ImGui.getCursorScreenPosX() + padding
    }

    private fun renderStaticImage(src: String, alt: String) {
        val img = MarkdownImages.getImage(src) as? MarkdownImages.ImageData ?: return
        val imageSize = ImVec2(img.width * scale, img.height * scale)
        val windowWidth = ImGui.getWindowWidth()

        val scaleRatio = if (imageSize.x > windowWidth - padding * 2) {
            (windowWidth - padding * 2) / imageSize.x - 0.1f
        } else {
            0.9f
        }

        imageSize.x *= scaleRatio
        imageSize.y *= scaleRatio

        val startX = calculateImageStartX(windowWidth, imageSize.x, alt)
        val startY = cursorY + padding * 2
        if (cursorX > ImGui.getCursorScreenPosX() + padding) {
            renderNewLine()
        }

        drawList.addImage(
            img.textureId,
            startX + shadowOffset,
            startY + shadowOffset,
            startX + imageSize.x + shadowOffset,
            startY + imageSize.y + shadowOffset,
            0f,
            0f,
            1f,
            1f,
            ImGui.colorConvertFloat4ToU32(0f, 0f, 0f, 0.3f)
        )

        drawList.addImage(
            img.textureId,
            startX,
            startY,
            startX + imageSize.x,
            startY + imageSize.y,
            0f,
            0f,
            1f,
            1f,
            ImColor.rgba(currentColor.x, currentColor.y, currentColor.z, currentColor.w)
        )

        cursorY += imageSize.y + padding
        cursorX = ImGui.getCursorScreenPosX() + padding
    }

    private fun renderList(element: Element) {
        if (cursorX > ImGui.getCursorScreenPosX() + padding) {
            renderNewLine()
        }
        indentLevel++
        renderChildren(element)
        indentLevel--
    }

    private fun renderListItem(element: Element) {
        if (cursorX > ImGui.getCursorScreenPosX() + padding) {
            renderNewLine()
        }
        cursorX += indentLevel * indentWidth
        if (element.parent()?.tagName() != "ol") {
            drawList.addCircleFilled(
                cursorX - 10f * scale,
                cursorY + font.fontSize * 0.5f * scale,
                2f * scale,
                ImColor.rgba(currentColor.x, currentColor.y, currentColor.z, currentColor.w)
            )
        } else {
            val index = element.elementSiblingIndex() + 1
            drawList.addText(
                font.font,
                font.fontSize * scale,
                cursorX,
                cursorY,
                ImColor.rgba(currentColor.x, currentColor.y, currentColor.z, currentColor.w),
                "$index."
            )
            cursorX += 20f * scale
        }

        renderChildren(element)
        cursorX = ImGui.getCursorScreenPosX() + padding
        renderNewLine()
    }

    private fun renderCodeBlock(element: Element) {
        if (cursorX > ImGui.getCursorScreenPosX() + padding) {
            renderNewLine()
        }

        cursorX += 6f * scale

        val textColor = ImVec4(0.8f, 0.8f, 0.8f, 1f)
        val bgColor = ImVec4(0.2f, 0.2f, 0.2f, 0.5f)

        // Find the <code> element within the <pre> tag
        val codeElement = element.selectFirst("code")
        val codeText = codeElement?.html() ?: element.html()

        // Extract language if specified
        val language = codeElement?.className()?.replace("language-", "") ?: ""

        ImGui.pushFont(font.font)
        val codeLines = codeText.lines()
        val maxLineWidth = codeLines.maxOfOrNull { ImGui.calcTextSize(it).x } ?: 0f
        val blockHeight = codeLines.size * font.fontSize * lineHeight * scale
        ImGui.popFont()
        val width = ImGui.getContentRegionAvail().x

        // Draw background with drop shadow
        drawList.addRectFilled(
            cursorX - padding + shadowOffset,
            cursorY + shadowOffset,
            cursorX + width - padding * 2 + shadowOffset - 6f * scale,
            cursorY + blockHeight + padding * 2 + shadowOffset,
            ImGui.colorConvertFloat4ToU32(0f, 0f, 0f, 0.3f),
            cornerRadius
        )
        drawList.addRectFilled(
            cursorX - padding,
            cursorY,
            cursorX + width - padding * 2 - 6f * scale,
            cursorY + blockHeight + padding * 2,
            ImColor.rgba(bgColor.x, bgColor.y, bgColor.z, bgColor.w),
            cornerRadius
        )

        // Render language label if available
        if (language.isNotEmpty()) {
            val labelColor = ImVec4(0.5f, 0.5f, 0.5f, 1f)
            drawList.addText(
                font.font,
                font.fontSize * 0.8f * scale,
                cursorX,
                cursorY + padding * 0.5f,
                ImColor.rgba(labelColor.x, labelColor.y, labelColor.z, labelColor.w),
                language
            )
        }

        // Render code lines
        var lineY = cursorY + padding + (if (language.isNotEmpty()) font.fontSize * scale else 0f)
        codeLines.forEach { line ->
            drawList.addText(
                font.font,
                font.fontSize * scale,
                cursorX,
                lineY,
                ImColor.rgba(textColor.x, textColor.y, textColor.z, textColor.w),
                line
            )
            lineY += font.fontSize * lineHeight * scale
        }

        // Update cursor position after rendering the code block
        cursorY += blockHeight + padding * 2 + (if (language.isNotEmpty()) font.fontSize * scale else 0f)
        cursorX = ImGui.getCursorScreenPosX() + padding

        // Ensure we move to the next line after the code block
        renderNewLine()
    }
    private fun renderElement(element: Element) {
        when (element.tagName()) {
            "body" -> renderChildren(element)
            "p" -> renderParagraph(element)
            "h1", "h2", "h3", "h4", "h5", "h6" -> renderHeading(element)
            "strong", "b" -> renderBold(element)
            "em", "i" -> renderItalic(element)
            "a" -> renderLink(element)
            "img" -> renderImage(element)
            "ul", "ol" -> renderList(element)
            "li" -> renderListItem(element)
            "pre" -> renderCodeBlock(element)
            "code" -> {
                if (element.parent()?.tagName() != "pre") {
                    renderInlineCode(element)
                }
            }
            "br" -> renderNewLine()
            "hr" -> renderHorizontalRule()
            else -> renderChildren(element)
        }
    }

    private fun renderInlineCode(element: Element) {
        val textColor = ImVec4(0.8f, 0.8f, 0.8f, 1f)
        val bgColor = ImVec4(0.2f, 0.2f, 0.2f, 0.5f)

        ImGui.pushFont(font.font)
        val textSize = ImGui.calcTextSize(element.text())
        ImGui.popFont()

        if (cursorX + textSize.x > ImGui.getCursorScreenPosX() + ImGui.getWindowContentRegionMinX() - padding) {
            renderNewLine()
        }

        // Draw background with drop shadow
        drawList.addRectFilled(
            cursorX - padding * 0.5f + shadowOffset,
            cursorY - padding * 0.5f + shadowOffset,
            cursorX + textSize.x + padding * 0.5f + shadowOffset,
            cursorY + textSize.y + padding * 0.5f + shadowOffset,
            ImGui.colorConvertFloat4ToU32(0f, 0f, 0f, 0.3f),
            cornerRadius * 0.5f
        )
        drawList.addRectFilled(
            cursorX - padding * 0.5f,
            cursorY - padding * 0.5f,
            cursorX + textSize.x + padding * 0.5f,
            cursorY + textSize.y + padding * 0.5f,
            ImColor.rgba(bgColor.x, bgColor.y, bgColor.z, bgColor.w),
            cornerRadius * 0.5f
        )

        drawList.addText(
            font.font,
            font.fontSize * scale,
            cursorX,
            cursorY,
            ImColor.rgba(textColor.x, textColor.y, textColor.z, textColor.w),
            element.text()
        )

        cursorX += textSize.x + padding
    }

    private fun renderNewLine() {
        cursorY += font.fontSize * lineHeight * scale
        cursorX = ImGui.getCursorScreenPosX() + padding
    }

    private fun renderHorizontalRule() {
        if (cursorX > ImGui.getCursorScreenPosX() + padding) {
            renderNewLine()
        }
        val width = ImGui.getContentRegionAvail().x

        // Check if the previous element was a heading
        if (lastRenderedElement?.tagName()?.startsWith("h") == true) {
            // Render the line directly under the heading
            cursorY -= 5f * scale
        } else {
            cursorY += 10f * scale
        }

        drawList.addLine(
            cursorX,
            cursorY,
            cursorX + width - padding * 2,
            cursorY,
            ImGui.colorConvertFloat4ToU32(0.5f, 0.5f, 0.5f, 1f),
            1f * scale
        )

        if (lastRenderedElement?.tagName()?.startsWith("h") != true) {
            cursorY += 10f * scale
        }

        lastRenderedElement = null
    }

    private fun calculateImageStartX(windowWidth: Float, imageWidth: Float, alt: String): Float {
        val alignment = when {
            alt.contains("left", ignoreCase = true) -> ImageAlignment.LEFT
            alt.contains("right", ignoreCase = true) -> ImageAlignment.RIGHT
            else -> ImageAlignment.CENTER
        }

        return when (alignment) {
            ImageAlignment.LEFT -> ImGui.getCursorScreenPosX() + padding
            ImageAlignment.RIGHT -> ImGui.getCursorScreenPosX() + windowWidth - imageWidth - padding * 3
            ImageAlignment.CENTER -> ImGui.getCursorScreenPosX() + (windowWidth - imageWidth) / 2 - padding * 2
        }
    }

    private enum class ImageAlignment {
        LEFT, CENTER, RIGHT
    }

    private inline fun withColor(color: ImVec4, block: () -> Unit) {
        colorStack.add(currentColor)
        currentColor = color
        block()
        currentColor = colorStack.removeAt(colorStack.lastIndex)
    }
}