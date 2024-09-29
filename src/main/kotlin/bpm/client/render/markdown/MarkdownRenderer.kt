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
    private var headingAnchors = mutableMapOf<String, Float>()
    private var scrolledAnchor: String? = null
    private var reset = false

    fun reset() {
//        indentLevel = 1
//        currentColor = ImVec4(1f, 1f, 1f, 1f)
//        colorStack.clear()
//        lastRenderedElement = null
        codeBlockStates.forEach { t, u ->
            u.animationProgress = 0f
            u.currentHeight = 0f
        }

        headingAnchors.clear()
        reset = true
    }

    private data class CodeBlockState(
        var isMinimized: Boolean = false,
        var animationProgress: Float = 0f,
        var targetHeight: Float = 0f,
        var currentHeight: Float = 0f,
        var codeText: String = "",
        var language: String = "",
        var isHovered: Boolean = false,
        var isCopied: Boolean = false,
        var copiedAnimationProgress: Float = 0f
    ) {

        val lines: List<String>
            get() = codeText.lines()
    }

    fun render(html: String) {

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

        cursorX = ImGui.getCursorScreenPosX()
        cursorY = ImGui.getCursorScreenPosY()

        val doc = Jsoup.parse(html)
        renderElement(doc.body())

        cursorY += 40f
        ImGui.setCursorScreenPos(cursorX, cursorY)
        if (reset) {
            ImGui.setScrollY(0f)
            reset = false
        }
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

    fun scrollToAnchor(anchor: String) {
        scrolledAnchor = anchor
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
        ImGui.setCursorScreenPos(ImGui.getCursorScreenPosX(), cursorY)
        // Render link icon
        // Generate an ID for the heading
        val headingId = element.text().lowercase().replace(" ", "-")
        //Convert to relative position
        val screenPos = ImGui.getCursorPos()
        if (headingId == scrolledAnchor) {
            ImGui.setScrollY(screenPos.y - 20)
            scrolledAnchor = null
        }
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
            if (ImGui.isMouseClicked(0)) {
                val normalizedTitle = headingText.lowercase().replace(" ", "-")
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
    private var onLinkClickedCallback: (String) -> Unit = {}
    // Add this method to set the callback function
    fun setOnHeadingClickedCallback(callback: (String) -> Unit) {
        onHeadingClicked = callback
    }

    fun setOnLinkClickedCallback(callback: (String) -> Unit) {
        onLinkClickedCallback = callback
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
        val href = element.attr("href")
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
            ImGui.setTooltip(href)
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand)
            if (ImGui.isMouseClicked(0)) {
                onLinkClickedCallback(href)
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
        font.fontSize = (16 * scale).toInt()

        if (cursorX > ImGui.getCursorScreenPosX() + padding) {
            renderNewLine()
        }

        cursorX += 6f * scale

        // Calculate the exact height of the code block
        val lineCount = state.lines.size
        val lineHeight = font.fontSize * 1.2f
        val topBarHeight = font.fontSize * 1.5f * scale
        val codeBlockPadding = padding * 2 // Add some padding at the top and bottom of the code block
        state.targetHeight = lineCount * lineHeight + codeBlockPadding

        val highlightedWidth = ImGui.getWindowContentRegionMaxX() - padding * 2
        val highlightedStartX = ImGui.getCursorScreenPosX() + padding
        val highlightedEndX = highlightedStartX + highlightedWidth
        var highlightedEndY = (cursorY + state.currentHeight)
        val highlightedColor = ImVec4(0.1f, 0.1f, 0.1f, 1f)

        // Check if the code block is hovered
        val isHovered = ImGui.isMouseHoveringRect(
            highlightedStartX + padding,
            cursorY,
            highlightedEndX - padding * 2,
            highlightedEndY
        )

        // Smoothly animate the hover state
        val animationSpeed = 5f
        if (isHovered) {
            state.animationProgress = lerp(state.animationProgress, 1f, ImGui.getIO().deltaTime * animationSpeed)
        } else {
            state.animationProgress = lerp(state.animationProgress, 0f, ImGui.getIO().deltaTime * animationSpeed)
        }

        // Render the code block background
        drawList.addRectFilled(
            highlightedStartX + padding,
            cursorY,
            highlightedEndX - padding * 2,
            highlightedEndY,
            ImColor.rgba(highlightedColor.x, highlightedColor.y, highlightedColor.z, highlightedColor.w),
            20f
        )

        // Render top bar with animation
        if (state.animationProgress > 0f) {
            MarkdownFont.push()

            font.isMedium = true
            font.fontSize = (18f * scale).toInt()

            val width = ImGui.calcTextSize(if (state.isCopied) "Copied" else state.language).x * scale

            val topBarWidth = width + padding * 2 + 20f * scale + padding * 2

            val topBarHeight = 30f * scale
            val topBarColor = ImVec4(0.175f, 0.167f, 0.185f, state.animationProgress)
            val topBarStartX = highlightedEndX - padding * 4 - topBarWidth
            val topBarStartY = cursorY + state.currentHeight - topBarHeight - padding * 2
            val topBarEndX = highlightedEndX - padding * 4
            val topBarEndY = topBarStartY + topBarHeight

            val isTopBarHovered = ImGui.isMouseHoveringRect(topBarStartX, topBarStartY, topBarEndX, topBarEndY)

            if (isTopBarHovered) {
                ImGui.setMouseCursor(ImGuiMouseCursor.Hand)
                if (ImGui.isMouseClicked(0)) {
                    ImGui.setClipboardText(state.codeText)
                    state.isCopied = true
                    state.copiedAnimationProgress = 1f
                }
            } else if (!isHovered) {
                state.isCopied = false
            }

            // Animate the "Copied" text
            if (state.isCopied) {
                state.copiedAnimationProgress = lerp(state.copiedAnimationProgress, 1f, ImGui.getIO().deltaTime * 5f)
            } else {
                state.copiedAnimationProgress = lerp(state.copiedAnimationProgress, 0f, ImGui.getIO().deltaTime * 5f)
            }

            drawList.addRectFilled(
                topBarStartX,
                topBarStartY,
                topBarEndX,
                topBarEndY,
                ImColor.rgba(50, 56, 66, (255 * state.animationProgress).toInt()),
                10f
            )

            // Render language label or "Copied" text
            if (state.language.isNotEmpty()) {
                val labelColor = ImVec4(0.7f, 0.7f, 0.7f, state.animationProgress)
                font.isMedium = true
                font.fontSize = 16
                val displayText = lerp(state.language, "Copied", state.copiedAnimationProgress)

                drawList.addText(
                    font.font,
                    font.fontSize * scale,
                    topBarStartX + padding * state.animationProgress,
                    topBarStartY + (topBarHeight - font.fontSize * scale) / 2,
                    ImColor.rgba(labelColor.x, labelColor.y, labelColor.z, labelColor.w * state.animationProgress),
                    displayText
                )
            }

            MarkdownFont.pop()

            // Render copy button
            val copyButtonSize = (20f * scale) * state.animationProgress
            val copyButtonX = topBarEndX - copyButtonSize - padding
            val copyButtonY = topBarStartY + (topBarHeight - copyButtonSize) / 2
            drawList.addRectFilled(
                copyButtonX,
                copyButtonY,
                copyButtonX + copyButtonSize,
                copyButtonY + copyButtonSize,
                ImColor.rgba(100, 100, 100, (255 * state.animationProgress).toInt()),
                5f
            )
            MarkdownFont.push()
            font.isIcon = true
            font.fontSize = (24 * state.animationProgress * scale).toInt()
            drawList.addText(
                font.font,
                font.fontSize.toFloat(),
                copyButtonX + 5 * scale,
                copyButtonY - 3 * scale,
                ImColor.rgba(255, 255, 255, (255 * state.animationProgress).toInt()),
                FontAwesome.Copy
            )
            MarkdownFont.pop()

            // Handle copy button click
            if (ImGui.isMouseHoveringRect(
                    copyButtonX,
                    copyButtonY,
                    copyButtonX + copyButtonSize,
                    copyButtonY + copyButtonSize
                ) &&
                ImGui.isMouseClicked(0)
            ) {
                ImGui.setClipboardText(state.codeText)
            }
        }

        // Smoothly animate the code block height
        state.currentHeight = lerp(state.currentHeight, state.targetHeight, ImGui.getIO().deltaTime * animationSpeed)

        cursorX += padding * 2
        cursorY += padding

        // Highlight and render the code
        highlightCode(drawList, state.codeText, state.language)
        cursorX -= padding * 2

        // Update cursor position after rendering the code block
        cursorX = ImGui.getCursorScreenPosX() + padding
        cursorY += state.currentHeight

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

    private fun lerp(start: String, end: String, t: Float): String {
        return if (t < 0.25f) start else end
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


    private fun highlightCode(drawList: ImDrawList, code: String, language: String): Float {
        val tokenizer = when (language.lowercase()) {
            "yaml" -> YamlTokenizer()
            "lua" -> LuaTokenizer()
            else -> DefaultTokenizer()
        }

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
            val tokens = tokenizer.tokenize(line)

            tokens.forEach { token ->
                val color = getColorForToken(token)
                drawList.addText(font, fontSize, currentX, currentY, ImColor.rgba(color), token.value)
                currentX += ImGui.calcTextSize(token.value).x
                height += ImGui.calcTextSize(token.value).y
            }

            currentY += lineHeight
        }

        MarkdownFont.pop()
        return height - padding
    }


    private fun getColorForToken(token: Token): ImVec4 {
        return when (token.type) {
            TokenType.KEYWORD -> ImVec4(0.9f, 0.4f, 0.4f, 1f)
            TokenType.STRING -> ImVec4(0.4f, 0.8f, 0.4f, 1f)
            TokenType.NUMBER -> ImVec4(0.4f, 0.6f, 0.8f, 1f)
            TokenType.COMMENT -> ImVec4(0.5f, 0.5f, 0.5f, 1f)
            TokenType.OPERATOR -> ImVec4(0.8f, 0.8f, 0.4f, 1f)
            TokenType.IDENTIFIER -> ImVec4(0.7f, 0.7f, 0.9f, 1f)
            TokenType.PUNCTUATION -> ImVec4(0.7f, 0.7f, 0.7f, 1f)
            TokenType.WHITESPACE -> ImVec4(1f, 1f, 1f, 1f)
            TokenType.YAML_KEY -> ImVec4(0.9f, 0.6f, 0.3f, 1f)
            TokenType.YAML_COLON -> ImVec4(0.9f, 0.9f, 0.2f, 1f)
            TokenType.YAML_LIST_ITEM -> ImVec4(0.5f, 0.8f, 0.9f, 1f)
            TokenType.YAML_BUILTIN_TYPE -> ImVec4(0.6f, 0.4f, 0.8f, 1f)
            TokenType.YAML_ANCHOR -> ImVec4(0.8f, 0.4f, 0.6f, 1f)
            TokenType.YAML_ALIAS -> ImVec4(0.6f, 0.8f, 0.4f, 1f)
            else -> ImVec4(0.9f, 0.9f, 0.9f, 1f)
        }
    }
}

enum class TokenType {
    KEYWORD, STRING, NUMBER, COMMENT, OPERATOR, IDENTIFIER, PUNCTUATION, WHITESPACE,
    YAML_KEY, YAML_COLON, YAML_LIST_ITEM, YAML_BUILTIN_TYPE, YAML_ANCHOR, YAML_ALIAS, OTHER
}

data class Token(val type: TokenType, val value: String)

class YamlTokenizer : Tokenizer {

    private val builtinTypes = setOf("int", "float", "string", "boolean", "null", "date", "time", "timestamp")

    override fun tokenize(line: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var remaining = line
        var indentLevel = 0

        // Handle indentation
        while (remaining.startsWith(" ")) {
            indentLevel++
            tokens.add(Token(TokenType.WHITESPACE, " "))
            remaining = remaining.substring(1)
        }

        while (remaining.isNotEmpty()) {
            when {
                remaining.startsWith("#") -> {
                    tokens.add(Token(TokenType.COMMENT, remaining))
                    remaining = ""
                }

                remaining.startsWith("- ") -> {
                    tokens.add(Token(TokenType.YAML_LIST_ITEM, "-"))
                    tokens.add(Token(TokenType.WHITESPACE, " "))
                    remaining = remaining.substring(2)
                }

                remaining.startsWith("&") -> {
                    val anchor = remaining.takeWhile { !it.isWhitespace() && it != ':' }
                    tokens.add(Token(TokenType.YAML_ANCHOR, anchor))
                    remaining = remaining.substring(anchor.length)
                }

                remaining.startsWith("*") -> {
                    val alias = remaining.takeWhile { !it.isWhitespace() && it != ':' }
                    tokens.add(Token(TokenType.YAML_ALIAS, alias))
                    remaining = remaining.substring(alias.length)
                }

                remaining.contains(":") -> {
                    val parts = remaining.split(":", limit = 2)
                    tokens.add(Token(TokenType.YAML_KEY, parts[0].trim()))
                    tokens.add(Token(TokenType.YAML_COLON, ":"))
                    remaining = parts[1]
                }

                else -> {
                    val value = remaining.takeWhile { !it.isWhitespace() }
                    val type = when {
                        value.startsWith("\"") && value.endsWith("\"") -> TokenType.STRING
                        value.startsWith("'") && value.endsWith("'") -> TokenType.STRING
                        value.toDoubleOrNull() != null -> TokenType.NUMBER
                        value == "true" || value == "false" -> TokenType.KEYWORD
                        value in builtinTypes -> TokenType.YAML_BUILTIN_TYPE
                        else -> TokenType.OTHER
                    }
                    tokens.add(Token(type, value))
                    remaining = remaining.substring(value.length)
                }
            }

            // Handle any trailing whitespace
            val space = remaining.takeWhile { it.isWhitespace() }
            if (space.isNotEmpty()) {
                tokens.add(Token(TokenType.WHITESPACE, space))
                remaining = remaining.substring(space.length)
            }
        }

        return tokens
    }
}


interface Tokenizer {

    fun tokenize(line: String): List<Token>
}

class DefaultTokenizer : Tokenizer {

    override fun tokenize(line: String): List<Token> {
        return line.split(" ").flatMap { word ->
            listOf(
                Token(TokenType.OTHER, word),
                Token(TokenType.WHITESPACE, " ")
            )
        }.dropLast(1) // Remove the last space
    }
}

class LuaTokenizer : Tokenizer {

    private val keywords = setOf(
        "and", "break", "do", "else", "elseif", "end", "false", "for", "function",
        "if", "in", "local", "nil", "not", "or", "repeat", "return", "then", "true", "until", "while"
    )
    private val operators = setOf(
        "+",
        "-",
        "*",
        "/",
        "%",
        "^",
        "#",
        "==",
        "~=",
        "<=",
        ">=",
        "<",
        ">",
        "=",
        "(",
        ")",
        "{",
        "}",
        "[",
        "]",
        ";",
        ":",
        ",",
        ".",
        "..",
        "..."
    )

    override fun tokenize(line: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var remaining = line

        while (remaining.isNotEmpty()) {
            when {
                remaining[0].isWhitespace() -> {
                    val space = remaining.takeWhile { it.isWhitespace() }
                    tokens.add(Token(TokenType.WHITESPACE, space))
                    remaining = remaining.substring(space.length)
                }

                remaining.startsWith("--") -> {
                    tokens.add(Token(TokenType.COMMENT, remaining))
                    remaining = ""
                }

                remaining.startsWith("\"") || remaining.startsWith("'") -> {
                    val endIndex = remaining.indexOf(remaining[0], 1)
                    if (endIndex != -1) {
                        tokens.add(Token(TokenType.STRING, remaining.substring(0, endIndex + 1)))
                        remaining = remaining.substring(endIndex + 1)
                    } else {
                        tokens.add(Token(TokenType.STRING, remaining))
                        remaining = ""
                    }
                }

                remaining[0].isDigit() -> {
                    val number = remaining.takeWhile { it.isDigit() || it == '.' }
                    tokens.add(Token(TokenType.NUMBER, number))
                    remaining = remaining.substring(number.length)
                }

                remaining[0].isLetter() || remaining[0] == '_' -> {
                    val identifier = remaining.takeWhile { it.isLetterOrDigit() || it == '_' }
                    val type = if (identifier in keywords) TokenType.KEYWORD else TokenType.IDENTIFIER
                    tokens.add(Token(type, identifier))
                    remaining = remaining.substring(identifier.length)
                }

                else -> {
                    val op = operators.find { remaining.startsWith(it) }
                    if (op != null) {
                        tokens.add(Token(TokenType.OPERATOR, op))
                        remaining = remaining.substring(op.length)
                    } else {
                        tokens.add(Token(TokenType.OTHER, remaining[0].toString()))
                        remaining = remaining.substring(1)
                    }
                }
            }
        }

        return tokens
    }
}