package bpm.client.render.markdown

import bpm.common.utils.FontAwesome
import imgui.*
import imgui.flag.ImGuiMouseCursor
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MarkdownRenderer {


    private var cursorX = 0f
    private var cursorY = 0f
    private var indentLevel = 1
    private var currentColor = ImVec4(1f, 1f, 1f, 1f)
    private val colorStack = mutableListOf<ImVec4>()
    private lateinit var drawList: ImDrawList
    private val font get() = MarkdownFont.current

    var scale = 1.0f
    private var baseFontSize = 24f
    private var baseLineHeight = 1.5f
    private var baseSpacing = 5f
    private var indentWidth = 20f
    private var padding = 10f
    private var cornerRadius = 5f
    private var shadowOffset = 5f
    private var lineHeight = 1.5f
    private var lastRenderedElement: Element? = null
    private val codeBlockStates = mutableMapOf<String, CodeBlockState>()
    private val animationDuration = 0.3f // seconds

    private data class CodeBlockState(
        var isMinimized: Boolean = false,
        var animationProgress: Float = 1f,
        var targetHeight: Float = 0f,
        var currentHeight: Float = 0f,
        var codeText: String = "",
        var language: String = ""
    ) {

        val lines: List<String>
            get() = codeText.lines()
    }

    fun render(html: String) {
        val io = ImGui.getIO()
        drawList = ImGui.getWindowDrawList()

        //Example icon rendering
        MarkdownFont.push()
        font.isIcon = true
        font.fontSize = 24
        drawList.addText(
            font.font,
            font.fontSize.toFloat(),
            cursorX,
            cursorY,
            ImColor.rgba(255, 255, 255, 255),
            FontAwesome.MagnifyingGlass
        )
        MarkdownFont.pop()

        // Calculate scale based on screen width
        baseFontSize = 16f * scale
        baseLineHeight = 1.5f * scale
        baseSpacing = 5f * scale

        // Update scaled values
        indentWidth = 20f * scale
        padding = 10f * scale
        cornerRadius = 5f * scale
        shadowOffset = 5f * scale


        // Draw background and shadow
        val startPos = ImVec2(ImGui.getCursorScreenPosX() - padding, ImGui.getCursorScreenPosY() - padding)
        val endPos = ImVec2(startPos.x + ImGui.getWindowContentRegionMaxX() + padding * 2, cursorY + padding)
        val size = ImVec2(endPos.x - startPos.x, endPos.y - startPos.y)

        drawList.addRectFilled(
            startPos.x + padding,
            startPos.y + padding,
            endPos.x - padding * 2,
            endPos.y - padding * 2,
            ImColor.rgba(31, 39, 44, 255),
            20f,
        )

//        // If hovered
//        if (ImGui.isMouseHoveringRect(startPos.x, startPos.y, endPos.x, endPos.y)) {
//            drawList.addRectFilled(
//                startPos.x + padding,
//                startPos.y + padding,
//                endPos.x - padding * 2,
//                endPos.y - padding * 2,
//                ImColor.rgba(33, 33, 33, 255),
//                20f,
//            )
//        }

        cursorX = ImGui.getCursorScreenPosX()
        cursorY = ImGui.getCursorScreenPosY()

        val doc = Jsoup.parse(html)
        renderElement(doc.body())

        cursorY += 40f
        ImGui.setCursorScreenPos(cursorX, cursorY)
    }


    private fun renderChildren(element: Element) {
        element.childNodes().forEach { node ->
            when (node) {
                is Element -> renderElement(node)
                is TextNode -> renderText(node.text(), isFirstLine = true, contentStartX = cursorX)
            }
        }
    }

    private fun renderParagraph(element: Element) {
        if (lastRenderedElement?.tagName()?.startsWith("h") == true) {
            cursorY -= 10f * scale
        }

        val currentColor = ImVec4(currentColor.x, currentColor.y, currentColor.z, currentColor.w)
        this.currentColor = ImVec4(135f / 255f, 135f / 255f, 135f / 255f, 1f)
        if (cursorX > ImGui.getCursorScreenPosX() + padding) {
            renderNewLine()
        }
        MarkdownFont.push()
        font.isMedium = true
        font.fontSize = 18
        cursorX += 5f * scale
        renderChildren(element)
        MarkdownFont.pop()
        this.currentColor = currentColor
//        renderNewLine()
        // Add extra space after paragraphs
        cursorY += baseSpacing

        if (lastRenderedElement?.tagName()?.startsWith("h") == true) {
            cursorY += 15f * scale
        }
    }

    private fun renderHeading(element: Element) {
        MarkdownFont.push()


        if (cursorX > ImGui.getCursorScreenPosX() + padding) {
            renderNewLine()
        }
        cursorX += 5f * scale

        val level = element.tagName().substring(1).toInt()
        val fontSize = when (level) {
            1 -> {
                font.isExtraBold = true
                baseFontSize * 2f
            }

            2 -> {
                font.isBold = true
                baseFontSize * 1.5f
            }

            3 -> {
                font.isSemiBold = true
                baseFontSize * 1.25f
            }

            else -> {
                font.isMedium = true
                baseFontSize * 1.1f
            }
        }

        font.fontSize = fontSize.toInt()

        ImGui.pushFont(font.font)
        val headingText = element.text()
        val textSize = ImGui.calcTextSize(headingText)
        ImGui.popFont()


        var startX = cursorX
        var startY = cursorY


        // Render link icon


        //If the heading is hovered, show the link icon
        if (ImGui.isMouseHoveringRect(cursorX, cursorY, cursorX + textSize.x, cursorY + textSize.y)) {
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand)
            startX += 25f * scale

            MarkdownFont.push()
            font.isIcon = true


            val iconSize = fontSize * 1.2f
            val iconX = startX - iconSize
            val iconY = startY + (textSize.y - iconSize) / 2

            val isHovered = ImGui.isMouseHoveringRect(iconX, iconY, iconX + iconSize, iconY + iconSize)
            val iconColor = if (ImGui.isMouseDown(0)) ImColor.rgba(100, 149, 237, 255) else ImColor.rgba(
                100, 100, 100, 255
            )
            // Handle click on the link icon
            if (isHovered && ImGui.isMouseClicked(0)) {
                val normalizedTitle = headingText.lowercase().replace(" ", "_")
                val encodedTitle = URLEncoder.encode(normalizedTitle, StandardCharsets.UTF_8.toString())
                onHeadingClicked("#$encodedTitle")
            }



            drawList.addText(
                font.font, iconSize, iconX, iconY, iconColor, FontAwesome.Link
            )



            MarkdownFont.pop()
        }
        cursorY += textSize.y




        cursorY = startY
        drawList.addText(
            font.font,
            fontSize,
            startX + 5f,
            cursorY + 5f,
            ImColor.rgba(20, 20, 20, 255),
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


        MarkdownFont.pop()

        // Add extra space after headings
        cursorY += baseSpacing
        //Restore starting font
    }

    // Add this property to store the callback function
    private var onHeadingClicked: (String) -> Unit = {}

    // Add this method to set the callback function
    fun setOnHeadingClickedCallback(callback: (String) -> Unit) {
        onHeadingClicked = callback
    }

    private fun renderText(text: String, isFirstLine: Boolean = false, contentStartX: Float = cursorX) {
        ImGui.pushFont(font.font)
        val words = text.split(" ")
        var spaceLeft = ImGui.getWindowWidth() - (cursorX - ImGui.getCursorScreenPosX()) - padding * 2

        words.forEach { word ->
            val wordSize = ImGui.calcTextSize(word)
            val wordWidth = wordSize.x * scale
            val spaceSize = ImGui.calcTextSize(" ").x * scale
            if (wordWidth > spaceLeft) {
                cursorY += font.fontSize * lineHeight
                cursorX = if (isFirstLine) contentStartX else ImGui.getCursorScreenPosX() + padding
                spaceLeft = ImGui.getWindowWidth() - padding * 2 - indentLevel * indentWidth
            }

            drawList.addText(
                font.font,
                font.fontSize * scale,
                cursorX,
                cursorY,
                ImColor.rgba(currentColor.x, currentColor.y, currentColor.z, currentColor.w),
                word
            )

            cursorX += wordWidth + spaceSize * scale
            spaceLeft -= wordWidth + spaceSize * scale
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
        val linkText = element.text()
        val textSize = ImGui.calcTextSize(linkText)

        val startX = cursorX

        withColor(linkColor) {
            renderChildren(element)
        }

        val endX = cursorX

        val textWidth = endX - startX
        val textHeight = font.fontSize * scale

        drawList.addLine(
            cursorX - textWidth,
            cursorY + textHeight,
            cursorX,
            cursorY + textHeight,
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

    private val listIndentation = 20f * scale
    private val listItemSpacing = 5f * scale

    private fun renderList(element: Element) {
        //If the last element was a heading, subtract some space
        if (lastRenderedElement?.tagName()?.startsWith("h") == true) {
            cursorY -= 15f * scale
        }

        if (cursorX > ImGui.getCursorScreenPosX() + padding) {
            renderNewLine()
        }
        indentLevel++
        cursorX += listIndentation
        cursorY -= 15f
        element.children().forEach { child ->
            if (child.tagName() == "li") {
                renderListItem(child)
            }
        }
        cursorY += 10f
        indentLevel--
        cursorX -= listIndentation
        // Add extra space after lists
        cursorY += baseSpacing
    }

    private fun renderListItem(element: Element, isSubList: Boolean = false) {
        if (cursorX > ImGui.getCursorScreenPosX() + padding) {
            renderNewLine()
        }
        val startX = ImGui.getCursorScreenPosX() + padding + indentLevel * indentWidth
        cursorX = startX
        val listItemStartY = cursorY

        // Render bullet point or number
        val bulletOffset = 20f * scale
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
                cursorX - 20f * scale,
                cursorY,
                ImColor.rgba(currentColor.x, currentColor.y, currentColor.z, currentColor.w),
                "$index."
            )
        }

        // Render list item content
        cursorX += bulletOffset - 15f * scale
        val contentStartX = cursorX
        var maxY = cursorY
        var isFirstLine = true

        element.childNodes().forEach { node ->
            when (node) {
                is Element -> {
                    when (node.tagName()) {
                        "ul", "ol" -> {
                            renderList(node)
                            maxY = maxOf(maxY, cursorY)
                        }

                        else -> {
                            if (node.tagName() == "p") {
                                cursorX -= 5f * scale
                                renderChildren(node)
                                cursorY += font.fontSize
                            } else {
                                renderElement(node)
                            }
                            maxY = maxOf(maxY, cursorY)
                        }
                    }
                }

                is TextNode -> {
                    renderText(node.text(), isFirstLine, contentStartX)
                    maxY = maxOf(maxY, cursorY)
                    isFirstLine = false
                }
            }
        }

        // Ensure we move to the next line after the list item
        if (!isSubList) {
            cursorY = (maxY + font.fontSize * (lineHeight - 1) * scale) - 2.5f
            cursorX = startX
        }
    }

    // Helper function for smooth interpolation
    private fun lerp(start: Float, end: Float, t: Float): Float {
        return start + t * (end - start)
    }

    private var borderRadius = 0f

    private fun renderCodeBlock(element: Element) {
        MarkdownFont.push()
        val state = codeBlockStates.getOrPut(element.text()) {
            val codeElement = element.selectFirst("code")
            val codeText = codeElement?.html() ?: element.html()
            val language = codeElement?.className()?.replace("language-", "") ?: ""
            CodeBlockState(
                codeText = codeText,
                language = language
            )
        }
        font.fontSize = 16

        if (cursorX > ImGui.getCursorScreenPosX() + padding) {
            renderNewLine()
        }

        cursorX += 6f * scale

        // Calculate the exact height of the code block
        val lineCount = state.lines.size
        val lineHeight = font.fontSize * 1.2f * scale
        val topBarHeight = font.fontSize * 1.5f * scale
        val codeBlockPadding = padding * 2 // Add some padding at the top and bottom of the code block
        state.targetHeight = lineCount * lineHeight + topBarHeight + codeBlockPadding

        // Smoothly animate the code block height
        state.currentHeight = lerp(state.currentHeight, state.targetHeight, ImGui.getIO().deltaTime * 5f)

        val highlightedWidth = ImGui.getWindowContentRegionMaxX() - padding * 2
        val highlightedStartX = ImGui.getCursorScreenPosX() + padding
        val highlightedEndX = highlightedStartX + highlightedWidth
        val highlightedEndY = cursorY + state.currentHeight
        val highlightedColor = ImVec4(0.1f, 0.1f, 0.1f, 1f)

        // Render the code block background
        drawList.addRectFilled(
            highlightedStartX,
            cursorY,
            highlightedEndX,
            highlightedEndY,
            ImColor.rgba(highlightedColor.x, highlightedColor.y, highlightedColor.z, highlightedColor.w),
            borderRadius
        )

        // Render top bar
        val topBarStartX = ImGui.getCursorScreenPosX() + padding
        val topBarEndX = ImGui.getWindowContentRegionMaxX() - padding
        val topBarColor = ImVec4(0.1f, 0.1f, 0.1f, 1f)

        drawList.addRectFilled(
            topBarStartX,
            cursorY,
            topBarEndX,
            cursorY + topBarHeight,
            ImColor.rgba(topBarColor.x, topBarColor.y, topBarColor.z, topBarColor.w),
            borderRadius
        )
        cursorY += topBarHeight

        // Render language label
        if (state.language.isNotEmpty()) {
            val labelColor = ImVec4(0.7f, 0.7f, 0.7f, 1f)
            drawList.addText(
                font.font,
                font.fontSize * scale,
                cursorX + padding / 2,
                cursorY - topBarHeight + padding / 2,
                ImColor.rgba(labelColor.x, labelColor.y, labelColor.z, labelColor.w),
                state.language
            )
        }

        // Highlight and render the code
        highlightCode(drawList, state.codeText, state.language)

        // Update cursor position after rendering the code block
        cursorX = ImGui.getCursorScreenPosX() + padding
        cursorY += state.currentHeight - topBarHeight - padding * 2

        // Ensure we move to the next line after the code block
        renderNewLine()

        MarkdownFont.pop()
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

        MarkdownFont.push()

//        ImGui.pushFont(font.font)
        val textSize = ImGui.calcTextSize(element.text())
        val textWidth = textSize.x * scale
        val textHeight = textSize.y * scale
//        ImGui.popFont()

        val availableWidth = ImGui.getWindowContentRegionMaxX() - cursorX - padding * 2
        if (textSize.x > availableWidth) {
            renderNewLine()
        }

        val startX = cursorX
        val startY = cursorY

        // Draw background with drop shadow
        drawList.addRectFilled(
            startX - padding * 0.5f + shadowOffset,
            startY - padding * 0.5f + shadowOffset,
            startX + textWidth + padding * 0.5f + shadowOffset,
            startY + textHeight + padding * 0.5f + shadowOffset,
            ImGui.colorConvertFloat4ToU32(0f, 0f, 0f, 0.3f),
            cornerRadius * 0.5f
        )
        drawList.addRectFilled(
            startX - padding * 0.5f,
            startY - padding * 0.5f,
            startX + textWidth + padding * 2f,
            startY + textHeight + padding * 0.5f,
            ImColor.rgba(38, 44, 54, 255),
            cornerRadius * 0.5f
        )

        drawList.addText(
            font.font,
            font.fontSize.toFloat(),
            startX,
            startY,
            ImColor.rgba(textColor.x, textColor.y, textColor.z, textColor.w),
            element.text()
        )

        cursorX += textWidth + padding
        // Don't update cursorY here to keep following text on the same line

        MarkdownFont.pop()
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
            cursorY -= baseSpacing // Move up to be closer to the heading
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
        } else {
            cursorY += 5f * scale // Add a small space after the line when it follows a heading
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

    private enum class ImageAlignment { LEFT, CENTER, RIGHT
    }

    private inline fun withColor(color: ImVec4, block: () -> Unit) {
        colorStack.add(currentColor)
        currentColor = color
        block()
        currentColor = colorStack.removeAt(colorStack.lastIndex)
    }


    private val YAML_PATTERNS = listOf(
        Triple("key", "^\\s*(\\w+):\\s*".toRegex(), ImVec4(0.8f, 0.4f, 0.4f, 1f)),
        Triple("string", "\"([^\"]*)\"|'([^']*)'".toRegex(), ImVec4(0.4f, 0.8f, 0.4f, 1f)),
        Triple("number", "\\b\\d+(\\.\\d+)?\\b".toRegex(), ImVec4(0.4f, 0.6f, 0.8f, 1f)),
        Triple("boolean", "\\b(true|false)\\b".toRegex(RegexOption.IGNORE_CASE), ImVec4(0.8f, 0.6f, 0.4f, 1f)),
        Triple("comment", "#.*$".toRegex(), ImVec4(0.5f, 0.5f, 0.5f, 1f))
    )

    private val LUA_PATTERNS = listOf(
        // Comments (multi-line and single-line)
        Triple("comment", Regex("--.*$|--\\[\\[.*?\\]\\]", RegexOption.DOT_MATCHES_ALL), ImVec4(0.4f, 0.6f, 0.4f, 1f)),

        // Strings (including long strings)
        Triple(
            "string",
            "\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'|\\[\\[.*?\\]\\]".toRegex(RegexOption.DOT_MATCHES_ALL),
            ImVec4(0.9f, 0.6f, 0.3f, 1f)
        ),

        // Keywords (moved to higher priority and adjusted regex)
        Triple(
            "keyword",
            "\\b(and|break|do|else|elseif|end|false|for|function|if|in|local|nil|not|or|repeat|return|then|true|until|while)\\b".toRegex(),
            ImVec4(0.9f, 0.4f, 0.4f, 1f)
        ),

        // Function definitions
        Triple(
            "function",
            Regex("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?=\\()"),
            ImVec4(0.4f, 0.7f, 0.9f, 1f)
        ),

        // Built-in functions and libraries
        Triple(
            "builtin",
            Regex("\\b(assert|collectgarbage|dofile|error|getmetatable|ipairs|load|loadfile|next|pairs|pcall|print|rawequal|rawget|rawlen|rawset|require|select|setmetatable|tonumber|tostring|type|xpcall)\\b"),
            ImVec4(0.5f, 0.8f, 0.8f, 1f)
        ),

        // Numbers (including hexadecimal)
        Triple(
            "number",
            "\\b(0x[a-fA-F0-9]+|\\d+(\\.\\d*)?([eE][+-]?\\d+)?)\\b".toRegex(),
            ImVec4(0.6f, 0.6f, 0.9f, 1f)
        ),
        // Operators
        Triple("operator", "[+\\-*/%^#=<>]|==|~=|<=|>=|\\.\\.|\\.\\.\\.".toRegex(), ImVec4(0.8f, 0.8f, 0.4f, 1f)),

        // Punctuation
        Triple("punctuation", "[(){}\\[\\],;:]".toRegex(), ImVec4(0.7f, 0.7f, 0.7f, 1f)),


        )


    private val DEFAULT_COLOR = ImVec4(0.9f, 0.9f, 0.9f, 1f)


    fun highlightCode(drawList: ImDrawList, code: String, language: String): Float {
        val patterns = when (language.lowercase()) {
            "yaml" -> YAML_PATTERNS
            "lua" -> LUA_PATTERNS
            else -> emptyList()
        }
        return highlightCode(drawList, code, patterns)
    }


    private fun highlightCode(
        drawList: ImDrawList,
        code: String,
        patterns: List<Triple<String, Regex, ImVec4>>
    ): Float {
        MarkdownFont.push()
        font.fontSize = (16 * scale).toInt()
        val font = MarkdownFont.current.font
        val fontSize = MarkdownFont.current.fontSize.toFloat()
        val startX = cursorX
        val startY = cursorY
        var currentY = startY
        val lineHeight = fontSize * 1.2f
        var height = 0f

        code.lines().forEach { line ->
            var currentX = startX
            var remainingLine = line

            while (remainingLine.isNotEmpty()) {
                var didPreMatch = false
                val match = patterns.firstNotNullOfOrNull { (_, regex, color) ->
                    regex.find(remainingLine)?.let { Triple(it.value, it.range, color) }
                }

                if (match != null) {
                    val (text, range, color) = match
                    // Draw any text before the match
                    if (range.first > 0) {
                        //Match all the text before the match
                        while (remainingLine.isNotEmpty()) {
                            val text = remainingLine.takeWhile { it != ' ' }
                            drawList.addText(
                                font,
                                fontSize,
                                currentX,
                                currentY,
                                ImColor.rgba(DEFAULT_COLOR),
                                text
                            )
                            currentX += ImGui.calcTextSize(text).x * scale
                            remainingLine = remainingLine.substring(text.length)
                            if (remainingLine.isNotEmpty()) {
                                drawList.addText(
                                    font,
                                    fontSize,
                                    currentX,
                                    currentY,
                                    ImColor.rgba(DEFAULT_COLOR),
                                    " "
                                )
                                currentX += ImGui.calcTextSize(" ").x * scale
                                remainingLine = remainingLine.substring(1)
                            }
                        }
                    }
                    // Draw the matched text
                    drawList.addText(font, fontSize, currentX, currentY, ImColor.rgba(color), text)
                    currentX += ImGui.calcTextSize(text).x * scale
                    remainingLine = remainingLine.substring(range.last + 1)
                    height += ImGui.calcTextSize(text).y
                } else {
                    // Draw any remaining text
                    drawList.addText(
                        font,
                        fontSize,
                        currentX,
                        currentY,
                        ImColor.rgba(DEFAULT_COLOR),
                        remainingLine
                    )
                    height += ImGui.calcTextSize(remainingLine).y * 1.2f
                    break
                }
            }
            currentY += lineHeight
        }
        MarkdownFont.pop()

        return height - padding
    }
}

