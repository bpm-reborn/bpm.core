package bpm.client.render.markdown

import bpm.client.font.Fonts
import imgui.*
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiHoveredFlags
import imgui.flag.ImGuiWindowFlags
import imgui.internal.ImRect
import imgui.type.ImBoolean
import kotlin.math.max
import kotlin.math.min

class MarkdownRenderer() {

    data class MarkdownFont(var isBold: Boolean, var isItalic: Boolean, var fontSize: Int) {

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
    private val font = MarkdownFont(false, false, 16)

    private val open = ImBoolean(true)
    private val BREAKPOINT_LARGE = 2561
    private val BREAKPOINT_MEDIUM = 1921
    private val BREAKPOINT_SMALL = 1281

    private var scale = 1f
    private var baseFontSize = 16f
    private var baseLineHeight = 1.5f
    private var baseSpacing = 5f
    private var indentWidth = 20f
    private var padding = 10f
    private var cornerRadius = 5f
    private var shadowOffset = 5f
    private var lineHeight = 1.5f
    private var lastRenderedNode: MarkdownNode? = null

    fun render(node: MarkdownNode) {
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


//        ImGui.setNextWindowPos(windowPosX, windowPosY + padding, ImGuiCond.Always)
//        ImGui.setNextWindowSize(windowWidth + padding, windowHeight - padding * 2, ImGuiCond.Always)

//        if (ImGui.begin(
//                "Markdown", open,
//                ImGuiWindowFlags.NoTitleBar or
//                        ImGuiWindowFlags.NoResize or
//                        ImGuiWindowFlags.NoMove or ImGuiWindowFlags.NoBackground or ImGuiWindowFlags.NoScrollbar
//            )
//        ) {
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


        //if hovered
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


        // Begin a child window for scrolling

        cursorX = ImGui.getCursorScreenPosX()
        cursorY = ImGui.getCursorScreenPosY()

        renderNode(node)
        ImGui.setCursorScreenPos(cursorX, cursorY)

//        }

//        ImGui.end()

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

    private fun renderNode(node: MarkdownNode) {
        when (node) {
            is MarkdownNode.Document -> renderDocument(node)
            is MarkdownNode.Paragraph -> renderParagraph(node)
            is MarkdownNode.Heading -> renderHeading(node)
            is MarkdownNode.Text -> renderText(node)
            is MarkdownNode.Bold -> renderBold(node)
            is MarkdownNode.Italic -> renderItalic(node)
            is MarkdownNode.Link -> renderLink(node)
            is MarkdownNode.Image -> renderImage(node)
            is MarkdownNode.List -> renderList(node)
            is MarkdownNode.ListItem -> renderListItem(node)
            is MarkdownNode.CodeBlock -> renderCodeBlock(node)
            is MarkdownNode.InlineCode -> renderInlineCode(node)
            is MarkdownNode.NewLine -> renderNewLine()
            is MarkdownNode.HorizontalRule -> renderHorizontalRule()
            is MarkdownNode.CustomColor -> renderCustomColor(node)
        }
    }

    private fun renderAST(node: MarkdownNode) {
        if (ImGui.begin("Markdown AST", open, ImGuiWindowFlags.NoTitleBar or ImGuiWindowFlags.NoResize)) {
            drawList = ImGui.getWindowDrawList()
            cursorX = ImGui.getCursorScreenPosX() + padding
            cursorY = ImGui.getCursorScreenPosY() + padding
            ImGui.text(node.toString())
        }
        ImGui.end()
    }

    private fun renderDocument(document: MarkdownNode.Document) {
        document.children.forEach { renderNode(it) }
    }

    private fun renderParagraph(paragraph: MarkdownNode.Paragraph) {
        val currentColor = ImVec4(currentColor.x, currentColor.y, currentColor.z, currentColor.w)
        this.currentColor = ImVec4(135f / 255f, 135f / 255f, 135f / 255f, 1f)
        if (cursorX > ImGui.getCursorScreenPosX() + padding) {
            renderNewLine()
        }
        cursorY -= padding
        paragraph.children.forEach { renderNode(it) }
        this.currentColor = currentColor
        renderNewLine()
        cursorY += padding
    }


    private fun renderHeading(heading: MarkdownNode.Heading) {
        if (cursorX > ImGui.getCursorScreenPosX() + padding) {
            renderNewLine()
        }

        val fontSize = when (heading.level) {
            1 -> baseFontSize * 2f
            2 -> baseFontSize * 1.5f
            3 -> baseFontSize * 1.25f
            else -> baseFontSize * 1.1f
        }
        font.fontSize = fontSize.toInt()

        ImGui.pushFont(font.font)
        val headingText = heading.children.filterIsInstance<MarkdownNode.Text>().joinToString("") { it.content }
        val textSize = ImGui.calcTextSize(headingText)
        ImGui.popFont()

        var startX = cursorX
        var startY = cursorY

        cursorY += textSize.y


        //If it's a heading 1, draw a nice line under it that extends to the end of the line window
        if (heading.level == 1) {
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
            fontSize.toFloat(),
            startX + shadowOffset,
            cursorY + shadowOffset,
            ImColor.rgba(20, 20, 20, 80),
            headingText
        )

        drawList.addText(
            font.font,
            fontSize.toFloat(),
            startX,
            cursorY,
            ImColor.rgba(currentColor.x, currentColor.y, currentColor.z, currentColor.w),
            headingText
        )

        cursorY += textSize.y
        cursorX = ImGui.getCursorScreenPosX() + padding

        font.fontSize = (16 * scale).toInt() // Reset font size
        lastRenderedNode = heading
    }

    private fun renderGif(image: MarkdownNode.Image) {
        val windowWidth = ImGui.getWindowWidth()
        val gifData = MarkdownImages.getImage(image.url) as? MarkdownGif.GifData ?: return

        val imageSize = ImVec2(gifData.width * scale, gifData.height * scale)
        val scaleRatio = if (imageSize.x > windowWidth - padding * 2) {
            (windowWidth - padding * 2) / imageSize.x - 0.1f
        } else {
            0.9f
        }

        imageSize.x *= scaleRatio
        imageSize.y *= scaleRatio

        val startX = calculateImageStartX(windowWidth, imageSize.x, image.alt)
        val startY = cursorY + padding * 2
        if (cursorX > ImGui.getCursorScreenPosX() + padding) {
            renderNewLine()
        }

        MarkdownGif.renderGif(image.url, drawList, startX, startY, scaleRatio * scale)

        cursorY += imageSize.y + padding
        cursorX = ImGui.getCursorScreenPosX() + padding
    }

    private fun renderStaticImage(image: MarkdownNode.Image) {
        val img = MarkdownImages.getImage(image.url) as? MarkdownImages.ImageData ?: return
        val imageSize = ImVec2(img.width * scale, img.height * scale)
        val windowWidth = ImGui.getWindowWidth()

        val scaleRatio = if (imageSize.x > windowWidth - padding * 2) {
            (windowWidth - padding * 2) / imageSize.x - 0.1f
        } else {
            0.9f
        }

        imageSize.x *= scaleRatio
        imageSize.y *= scaleRatio

        val startX = calculateImageStartX(windowWidth, imageSize.x, image.alt)
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
    private fun renderText(text: MarkdownNode.Text) {
        ImGui.pushFont(font.font)
        val words = text.content.split(" ")
        var spaceLeft = ImGui.getWindowWidth() - (cursorX - ImGui.getCursorScreenPosX()) - padding * 2

        words.forEach { word ->
            val wordSize = ImGui.calcTextSize(word)
            val spaceSize = ImGui.calcTextSize(" ").x * scale
            if (wordSize.x > spaceLeft) {
                // Move to the next line
                cursorY += baseFontSize * lineHeight
                cursorX = ImGui.getCursorScreenPosX() + padding + indentLevel * indentWidth
                spaceLeft = ImGui.getWindowWidth() - padding * 2 - indentLevel * indentWidth
            }

            // Draw the word
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

    private fun renderBold(bold: MarkdownNode.Bold) {
        font.isBold = true
        cursorX -= 3.5f * scale
        bold.children.forEach { renderNode(it) }
        cursorX += 2f * scale
        font.isBold = false
    }

    private fun renderItalic(italic: MarkdownNode.Italic) {
        font.isItalic = true
        cursorX -= 3.5f * scale
        italic.children.forEach { renderNode(it) }
        cursorX += 2f * scale
        font.isItalic = false
    }

    private fun renderLink(link: MarkdownNode.Link) {
        val linkColor = ImVec4(0.2f, 0.8f, 1f, 1f)
        cursorX -= 3f * scale

        withColor(linkColor) {
            link.children.forEach { renderNode(it) }
        }

        ImGui.pushFont(font.font)
        val linkText = link.children.filterIsInstance<MarkdownNode.Text>().joinToString("") { it.content }
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
            ImGui.setTooltip(link.url)
            if (ImGui.isMouseClicked(0)) {
                // Handle link click (e.g., open URL in browser)
            }
        }
    }


    private fun renderImage(image: MarkdownNode.Image) {
        if (MarkdownGif.isGif(image.url)) {
            renderGif(image)
        } else {
            renderStaticImage(image)
        }
    }

    private var ordered = false
    private var listIndex = 1

    private fun renderList(list: MarkdownNode.List) {
        if (cursorX > ImGui.getCursorScreenPosX() + padding) {
            renderNewLine()
        }
        indentLevel++
        ordered = list.ordered
        list.items.forEachIndexed() { index, item ->
            listIndex = index
            renderNode(item)
        }
        indentLevel--
    }

    private fun renderListItem(item: MarkdownNode.ListItem) {
        if (cursorX > ImGui.getCursorScreenPosX() + padding) {
            renderNewLine()
        }
        cursorX += indentLevel * indentWidth
        if (!ordered) {
            drawList.addCircleFilled(
                cursorX - 10f * scale,
                cursorY + font.fontSize * 0.5f * scale,
                2f * scale,
                ImColor.rgba(currentColor.x, currentColor.y, currentColor.z, currentColor.w)
            )
        } else {
            drawList.addText(
                font.font,
                font.fontSize * scale,
                cursorX,
                cursorY,
                ImColor.rgba(currentColor.x, currentColor.y, currentColor.z, currentColor.w),
                "${listIndex + 1}."
            )
            cursorX += 20f * scale
        }

        item.children.forEach { renderNode(it) }
        cursorX = ImGui.getCursorScreenPosX() + padding
        renderNewLine()
    }

    private fun renderCodeBlock(codeBlock: MarkdownNode.CodeBlock) {
        if (cursorX > ImGui.getCursorScreenPosX() + padding) {
            renderNewLine()
        }

        cursorX += 6f * scale

        val textColor = ImVec4(0.8f, 0.8f, 0.8f, 1f)
        val bgColor = ImVec4(0.2f, 0.2f, 0.2f, 0.5f)

        ImGui.pushFont(font.font)
        val codeLines = codeBlock.content.lines()
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

        // Render code lines
        var lineY = cursorY + padding
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

        cursorY += blockHeight
        cursorX = ImGui.getCursorScreenPosX() + padding
    }

    private fun renderInlineCode(inlineCode: MarkdownNode.InlineCode) {
        val textColor = ImVec4(0.8f, 0.8f, 0.8f, 1f)
        val bgColor = ImVec4(0.2f, 0.2f, 0.2f, 0.5f)

        ImGui.pushFont(font.font)
        val textSize = ImGui.calcTextSize(inlineCode.content)
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
            inlineCode.content
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

        // Check if the previous node was a heading
        if (lastRenderedNode is MarkdownNode.Heading) {
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

        if (lastRenderedNode !is MarkdownNode.Heading) {
            cursorY += 10f * scale
        }

        lastRenderedNode = MarkdownNode.HorizontalRule
    }

    private var insideColor = false

    private fun renderCustomColor(customColor: MarkdownNode.CustomColor) {
        val color = parseColor(customColor.color)
        if (insideColor) {
            cursorX -= 3f * scale
        }

        withColor(color) {
            insideColor = true
            customColor.children.forEach { renderNode(it) }
            insideColor = false
        }
    }


    private fun parseColor(colorString: String): ImVec4 {
        return when (colorString.toLowerCase()) {
            "red" -> ImVec4(231f / 255f, 76f / 255f, 60f / 255f, 1f)
            "green" -> ImVec4(46f / 255f, 204f / 255f, 113f / 255f, 1f)
            "blue" -> ImVec4(52f / 255f, 152f / 255f, 219f / 255f, 1f)
            "yellow" -> ImVec4(241f / 255f, 196f / 255f, 15f / 255f, 1f)
            "purple" -> ImVec4(155f / 255f, 89f / 255f, 182f / 255f, 1f)
            else -> ImVec4(0.876f, 0.876f, 0.876f, 1f)
        }
    }

    private inline fun withColor(color: ImVec4, block: () -> Unit) {
        colorStack.add(currentColor)
        currentColor = color
        block()
        currentColor = colorStack.removeAt(colorStack.lastIndex)
    }
}