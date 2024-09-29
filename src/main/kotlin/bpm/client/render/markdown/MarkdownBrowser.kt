package bpm.client.render.markdown

import bpm.client.docs.Docs
import bpm.client.font.Fonts
import bpm.client.utils.use
import bpm.common.utils.FontAwesome
import bpm.common.vm.ComplexLuaTranspiler
import imgui.*
import imgui.callback.ImGuiInputTextCallback
import imgui.flag.*
import imgui.internal.ImRect
import imgui.type.ImString
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.util.*

class MarkdownBrowser(private val docs: Docs) {

    private val flavour = CommonMarkFlavourDescriptor()
    private val parser = MarkdownParser(flavour)
    private var currentFile: String? = null
    private var currentHtml: String? = null
    private val history = LinkedList<String>()
    private var currentIndex = -1
    private val iconFont = Fonts.getFamily("Fa")["Regular"][18]
    private val boldFont = Fonts.getFamily("Inter")["Bold"][18]
    private val regularFont = Fonts.getFamily("Inter")["Regular"][18]
    private var isNavInputFocused = false
    private var isNavInputHovered = false
    private var navInputAnimation = 0f
    private val bufferString = ImString(200)
    private var autocompleteText = ""
    private var isSidebarVisible = true
    private var sidebarAnimation = 1f
    private val sidebarWidth = 250f
    private val markdownFiles = mutableMapOf<String, Any>() // Can contain String (file content) or Map (folder)
    private var expandedFolders = mutableSetOf<String>()
    private val folderAnimations = mutableMapOf<String, Float>()
    private val hoverAnimation = mutableMapOf<String, Float>()
    private var hoveredItem: String? = null
    private var isMouseInWindow = true
    private var autocompleteOptions = listOf<String>()
    private var currentPath = ""


    init {
        // Initialize the markdownFiles with the doc tree from Docs
        markdownFiles.putAll(docs.getDocTree())
    }


    fun reloadDocs() {
        docs.reloadDocs()
        markdownFiles.clear()
        markdownFiles.putAll(docs.getDocTree())
        // Optionally, you might want to reset the current view or update the UI
        // after reloading the docs
    }


    fun addMarkdownFile(path: String, content: String) {
        val parts = path.split("/")
        var current: MutableMap<String, Any> = markdownFiles as MutableMap<String, Any>
        for (i in 0 until parts.size - 1) {
            current = current.getOrPut(parts[i]) { mutableMapOf<String, Any>() } as MutableMap<String, Any>
        }
        current[parts.last()] = content
    }


    fun render() {
        val windowFlags = ImGuiWindowFlags.NoDecoration or
                ImGuiWindowFlags.NoMove or
                ImGuiWindowFlags.NoResize

        ImGui.setNextWindowPos(0f, 0f)
        ImGui.setNextWindowSize(ImGui.getIO().displaySizeX, ImGui.getIO().displaySizeY)

        ImGui.begin("Markdown Browser", windowFlags)

        renderNavBar()
        isMouseInWindow = ImGui.isWindowHovered(ImGuiHoveredFlags.RootAndChildWindows)

        renderSidebarAndContent()

        ImGui.end()
    }


    private fun renderNavBar() {
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 10f, 10f)
        val drawList = ImGui.getWindowDrawList()
        ImGui.dummy(0f, 10f) // Add some space before the navbar
        val startPos = ImGui.getCursorScreenPos()

        // Background for the entire navbar
        drawList.addRectFilled(
            startPos.x,
            startPos.y,
            startPos.x + ImGui.getWindowWidth(),
            startPos.y + 50f,
            ImColor.rgb(30, 30, 30),
            10f
        )

        ImGui.setCursorScreenPos(startPos.x + 10f, startPos.y + 10f)
        renderHamburgerButton()
        ImGui.sameLine()
        renderNavButton(FontAwesome.LeftLong, canGoBack()) { goBack() }
        ImGui.sameLine()
        renderNavButton(FontAwesome.RightLong, canGoForward()) { goForward() }
        ImGui.sameLine()

        val maxNavInputWidth = ImGui.getContentRegionAvailX() - 20f // Leave some padding
        renderExpandingNavInput("File Name", currentFile ?: "", maxNavInputWidth, this::handleNavInput)

        ImGui.popStyleVar()
        ImGui.dummy(0f, 60f) // Add some space after the navbar
    }


    private fun renderNavButton(icon: String, enabled: Boolean = true, onClick: () -> Unit) {
        val drawList = ImGui.getWindowDrawList()
        val pos = ImGui.getCursorScreenPos()
        val size = 30f
        val buttonColor = if (enabled) ImColor.rgb(50, 50, 50) else ImColor.rgb(40, 40, 40)
        val textColor = if (enabled) ImColor.rgb(200, 200, 200) else ImColor.rgb(100, 100, 100)

        drawList.addRectFilled(pos.x, pos.y, pos.x + size, pos.y + size, buttonColor, 5f)
        drawList.addText(iconFont, 18f, pos.x + 6, pos.y + 6, textColor, icon)

        if (ImGui.invisibleButton("##$icon", size, size) && enabled) {
            onClick()
        }

        if (ImGui.isItemHovered() && enabled) {
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand)
        }
    }

    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t.coerceIn(0f, 1f)
    }

    private fun renderExpandingNavInput(label: String, value: String, maxWidth: Float, onChange: (String) -> Unit) {
        val drawList = ImGui.getWindowDrawList()
        val pos = ImGui.getCursorScreenPos()

        // Calculate animation progress
        val targetAnimation = when {
            isNavInputFocused -> 1f
            isNavInputHovered -> 0.5f
            else -> 0f
        }
        navInputAnimation = lerp(navInputAnimation, targetAnimation, ImGui.getIO().deltaTime * 4f)

        val minWidth = 200f
        val animatedWidth = lerp(minWidth, maxWidth, navInputAnimation)

        // Render the custom rounded rect
        val rectColor = when {
            isNavInputFocused -> ImColor.rgb(60, 60, 60)
            isNavInputHovered -> ImColor.rgb(50, 50, 50)
            else -> ImColor.rgb(40, 40, 40)
        }
        drawList.addRectFilled(
            pos.x,
            pos.y,
            pos.x + animatedWidth,
            pos.y + 30f,
            rectColor,
            5f
        )

        // Render the search icon
        drawList.addText(iconFont, 18f, pos.x + 8, pos.y + 6, ImColor.rgb(150, 150, 150), FontAwesome.MagnifyingGlass)

        // Render the file name or input
        bufferString.set(value)
        ImGui.setNextItemWidth(animatedWidth - 40f)
        ImGui.setCursorScreenPos(pos.x + 20f, pos.y - 2.5f)

        ImGui.pushStyleColor(ImGuiCol.FrameBg, 0)
        ImGui.pushStyleColor(ImGuiCol.Text, ImColor.rgb(200, 200, 200))

        val inputFlags = ImGuiInputTextFlags.AutoSelectAll or ImGuiInputTextFlags.CallbackCompletion or ImGuiInputTextFlags.CallbackEdit
        if (ImGui.inputText("##$label-nav", bufferString, inputFlags, inputTextCallback)) {
            onChange(bufferString.get())
        }

        ImGui.popStyleColor(2)

        // Check if the input is focused or hovered
        isNavInputFocused = ImGui.isItemActive() || ImGui.isItemFocused()
        isNavInputHovered = ImGui.isItemHovered()

        // Render autocomplete text
        if (isNavInputFocused && autocompleteText.isNotEmpty()) {
            val fullText = bufferString.get() + autocompleteText
            val autoCompletePos = ImVec2(
                pos.x + ImGui.calcTextSize(bufferString.get()).x + 32f,
                pos.y + 7.5f
            )
            drawList.addText(autoCompletePos.x, autoCompletePos.y, ImColor.rgb(100, 100, 100), autocompleteText)
        }

        // Render placeholder text when not focused and empty
        if (!isNavInputFocused && bufferString.get().isEmpty()) {
            drawList.addText(
                pos.x + 40f,
                pos.y + 5f,
                ImColor.rgb(100, 100, 100),
                "Search or enter file name"
            )
        }

        // Change cursor to text input cursor when hovered
        if (isNavInputHovered) {
            ImGui.setMouseCursor(ImGuiMouseCursor.TextInput)
        }

        ImGui.setCursorScreenPos(pos.x, pos.y - 10f)
    }

    private val inputTextCallback = object : ImGuiInputTextCallback() {
        override fun accept(data: ImGuiInputTextCallbackData) {
            when (data.eventFlag) {
                ImGuiInputTextFlags.CallbackCompletion -> {
                    if (autocompleteText.isNotEmpty()) {
                        data.insertChars(data.cursorPos, autocompleteText)
                        currentPath = data.buf.toString()
                        autocompleteText = ""
                        updateAutocomplete()
                    }
                }

                ImGuiInputTextFlags.CallbackEdit -> {
                    currentPath = data.buf.toString()
                    updateAutocomplete()
                }
            }
        }
    }


    private fun updateAutocomplete() {
        val input = currentPath.lowercase()
        val parts = input.split("/")

        var current: Map<String, Any> = markdownFiles
        var currentPathPrefix = ""

        for (i in 0 until parts.size - 1) {
            currentPathPrefix += parts[i] + "/"
            current = current[parts[i]] as? Map<String, Any> ?: return
        }

        val lastPart = parts.last()
        autocompleteOptions = current.keys
            .filter { it.lowercase().startsWith(lastPart) }
            .map { currentPathPrefix + it }

        autocompleteText = if (autocompleteOptions.isNotEmpty()) {
            autocompleteOptions.first().substring(input.length)
        } else {
            ""
        }
    }


    private fun renderHamburgerButton() {
        val drawList = ImGui.getWindowDrawList()
        val pos = ImGui.getCursorScreenPos()
        val size = 30f
        val buttonColor = ImColor.rgb(50, 50, 50)
        val lineColor = ImColor.rgb(200, 200, 200)

        drawList.addRectFilled(pos.x, pos.y, pos.x + size, pos.y + size, buttonColor, 5f)

        // Draw hamburger icon
        val lineWidth = 18f
        val lineHeight = 2f
        val lineSpacing = 5f
        for (i in 0..2) {
            val lineY = pos.y + (size - 3 * lineHeight - 2 * lineSpacing) / 2 + i * (lineHeight + lineSpacing)
            drawList.addRectFilled(
                pos.x + (size - lineWidth) / 2,
                lineY,
                pos.x + (size + lineWidth) / 2,
                lineY + lineHeight,
                lineColor
            )
        }

        if (ImGui.invisibleButton("##hamburger", size, size)) {
            isSidebarVisible = !isSidebarVisible
        }

        if (ImGui.isItemHovered()) {
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand)
        }
    }

    private fun renderSidebarAndContent() {
        // Animate sidebar
        if (isSidebarVisible) {
            sidebarAnimation = (sidebarAnimation + ImGui.getIO().deltaTime * 4f).coerceAtMost(1f)
        } else {
            sidebarAnimation = (sidebarAnimation - ImGui.getIO().deltaTime * 4f).coerceAtLeast(0f)
        }

        val animatedSidebarWidth = sidebarWidth * sidebarAnimation

        // Render sidebar
        if (sidebarAnimation > 0f) {
            ImGui.setCursorPos(10f, 80f) // Position below navbar
            renderSidebar(animatedSidebarWidth)
        }

        // Render content
        ImGui.setCursorPos(animatedSidebarWidth + 30f, 80f) // Add spacing between sidebar and content
        val contentWidth = ImGui.getWindowWidth() - animatedSidebarWidth - 40f
        renderContent(contentWidth)
    }


    private fun renderSidebar(width: Float) {
        val drawList = ImGui.getWindowDrawList()
        val pos = ImGui.getCursorScreenPos()
        val height = ImGui.getWindowHeight() - pos.y - 10f // Extend to bottom of screen

        // Render custom border
        val borderColor = ImColor.rgb(50, 50, 50)
        drawList.addRect(pos.x, pos.y, pos.x + width, pos.y + height, borderColor, 10f, ImDrawFlags.RoundCornersAll, 2f)

        ImGui.pushClipRect(pos.x, pos.y, pos.x + width, pos.y + height, true)

        ImGui.beginChild("Sidebar", width, height, false, ImGuiWindowFlags.NoScrollbar)

        ImGui.pushFont(Fonts.getFamily("Inter")["SemiBold"][20])
        ImGui.setCursorScreenPos(pos.x + 20f, pos.y + 20f)
        ImGui.text("File Explorer")
        ImGui.popFont()
        ImGui.dummy(0f, 10f)

        val separatorColor = ImColor.rgb(60, 60, 60)
        drawList.addLine(
            pos.x + 20f,
            ImGui.getCursorScreenPosY(),
            pos.x + width - 20f,
            ImGui.getCursorScreenPosY(),
            separatorColor
        )

        ImGui.dummy(0f, 20f) // Add padding after separator

        renderFileTree(markdownFiles, "", drawList, pos.x, width)

        ImGui.endChild()

        ImGui.popClipRect()
    }


    private fun renderFileTree(
        files: Map<String, Any>,
        path: String,
        drawList: ImDrawList,
        startX: Float,
        width: Float,
        depth: Int = 0
    ) {
        for ((name, content) in files) {
            val fullPath = if (path.isEmpty()) name else "$path/$name"
            val isFolder = content is Map<*, *>
            val isExpanded = fullPath in expandedFolders
            val icon = when {
                isFolder && isExpanded -> FontAwesome.FolderOpen
                isFolder -> FontAwesome.Folder
                else -> FontAwesome.FileLines
            }
            val iconColor = if (isFolder) ImColor.rgb(255, 200, 0) else ImColor.rgb(200, 200, 200)

            val padding = 20f
            val indentation = 20f * depth
            val itemStartX = startX + padding + indentation
            ImGui.setCursorPosX(itemStartX)

            val itemStartY = ImGui.getCursorScreenPosY()
            val itemHeight = 30f
            val itemWidth = width - padding * 2 - indentation

            // Render hover and focus effect
            val isHovered = ImGui.isMouseHoveringRect(
                itemStartX - padding,
                itemStartY,
                itemStartX + itemWidth,
                itemStartY + itemHeight
            )
            val isFocused = fullPath == currentFile

            if (isHovered && isMouseInWindow) {
                hoveredItem = fullPath
            } else if (!isMouseInWindow || !isHovered) {
                if (hoveredItem == fullPath) {
                    hoveredItem = null
                }
            }

            hoverAnimation[fullPath] = hoverAnimation.getOrDefault(fullPath, 0f).let {
                if (fullPath == hoveredItem) (it + ImGui.getIO().deltaTime * 4f).coerceAtMost(1f)
                else (it - ImGui.getIO().deltaTime * 4f).coerceAtLeast(0f)
            }

            val hoverAlpha = hoverAnimation[fullPath] ?: 0f
            if (hoverAlpha > 0f || isFocused) {
                val color = if (isFocused)
                    ImColor.rgba(100, 100, 100, (255 * 0.5f).toInt())
                else
                    ImColor.rgba(80, 80, 80, (255 * 0.3f * hoverAlpha).toInt())
                drawList.addRectFilled(
                    itemStartX - padding,
                    itemStartY,
                    itemStartX + itemWidth,
                    itemStartY + itemHeight,
                    color,
                    5f
                )
            }

            // Draw the icon
            drawList.addText(iconFont, 16f, itemStartX, itemStartY + 7f, iconColor, icon)

            // Draw the file/folder name with proper spacing
            val textPadding = 25f
            val textStartX = itemStartX + textPadding

            if (isFolder) {
                val arrowIcon = if (isExpanded) FontAwesome.ChevronDown else FontAwesome.ChevronRight
                drawList.addText(
                    iconFont,
                    16f,
                    itemStartX + itemWidth - textPadding,
                    itemStartY + 7f,
                    ImColor.rgb(220, 220, 220),
                    arrowIcon
                )

                drawList.addText(
                    regularFont,
                    16f,
                    textStartX,
                    itemStartY + 7f,
                    ImColor.rgb(220, 220, 220),
                    name
                )

                if (ImGui.invisibleButton("##$fullPath", itemWidth, itemHeight)) {
                    if (isExpanded) {
                        expandedFolders.remove(fullPath)
                    } else {
                        expandedFolders.add(fullPath)
                        folderAnimations[fullPath] = 0f
                    }
                }

                if (isExpanded) {
                    renderFolderContents(content as Map<String, Any>, fullPath, drawList, startX, width, depth + 1)
                }
            } else {
                val font = if (isFocused) boldFont else regularFont
                drawList.addText(
                    font,
                    16f,
                    textStartX,
                    itemStartY + 7f,
                    ImColor.rgb(220, 220, 220),
                    name
                )
                if (ImGui.invisibleButton("##$fullPath", itemWidth, itemHeight)) {
                    loadFile(fullPath)
                }
            }

            if (ImGui.isItemHovered()) {
                ImGui.setMouseCursor(ImGuiMouseCursor.Hand)
            }

            ImGui.dummy(0f, 5f) // Add some vertical spacing between items
        }
    }


    class CodeFenceTagRenderer : HtmlGenerator.TagRenderer {

        private var inCodeFence = false
        private var codeLanguage: String? = null

        override fun closeTag(tagName: CharSequence): CharSequence {
            return when {
                inCodeFence && tagName == "pre" -> {
                    inCodeFence = false
                    codeLanguage = null
                    "</code></pre>"
                }

                else -> "</$tagName>"
            }
        }

        override fun openTag(
            node: ASTNode,
            tagName: CharSequence,
            vararg attributes: CharSequence?,
            autoClose: Boolean
        ): CharSequence {
            return when {
                tagName == "pre" && attributes.any { it?.startsWith("class=\"language-") == true } -> {
                    inCodeFence = true
                    codeLanguage = attributes.firstOrNull { it?.startsWith("class=\"language-") == true }
                        ?.removePrefix("class=\"language-")?.removeSuffix("\"").toString()
                    "<pre><code${codeLanguage?.let { " class=\"language-$it\"" } ?: ""}>"
                }

                else -> buildString {
                    append("<$tagName")
                    attributes.filterNotNull().forEach { append(" $it") }
                    if (autoClose) append(" />") else append(">")
                }
            }
        }

        override fun printHtml(html: CharSequence): CharSequence {
            return if (inCodeFence) {
                html.toString().replace("```", "")
            } else {
                html
            }
        }
    }

    private fun renderFolderContents(
        folder: Map<String, Any>,
        path: String,
        drawList: ImDrawList,
        startX: Float,
        width: Float,
        depth: Int
    ) {
        val animation = folderAnimations.getOrPut(path) { 1f }
        folderAnimations[path] = (animation + ImGui.getIO().deltaTime * 4f).coerceAtMost(1f)

        ImGui.pushStyleVar(ImGuiStyleVar.Alpha, animation)
        renderFileTree(folder, path, drawList, startX, width, depth)
        ImGui.popStyleVar()
    }

    private val renderer = HtmlRenderer()

    private fun renderContent(width: Float) {
        val height = ImGui.getWindowHeight() - ImGui.getCursorPosY() - 10f

        ImGui.pushStyleColor(ImGuiCol.ChildBg, ImColor.rgb(18, 18, 18))
        ImGui.pushStyleVar(ImGuiStyleVar.ChildRounding, 10f)

        // Use ImGuiWindowFlags.AlwaysVerticalScrollbar to ensure scrollbar is always visible
        ImGui.beginChild(
            "Content",
            width,
            height,
            false,
            ImGuiWindowFlags.HorizontalScrollbar or ImGuiWindowFlags.AlwaysVerticalScrollbar
        )

        currentFile?.let {
            // Remove the nested child window and set cursor position once
            ImGui.setCursorPos(10f, 10f)

            currentHtml?.let { html ->
                renderer.render(html)

                // Ensure the content area extends to cover all rendered content
                val lastItemRect = ImGui.getItemRectMax()
                ImGui.dummy(0f, ImGui.getScrollMaxY() - lastItemRect.y + 20f)
            }
        }

        ImGui.endChild()
        ImGui.popStyleVar()
        ImGui.popStyleColor()
    }


    private fun getFileContent(path: String): String? {
        return docs.getDocContent(path)
    }

    private fun loadFile(fileName: String) {
        if (fileName != currentFile && isValidPath(fileName)) {
            currentFile = fileName
            if (currentIndex < history.size - 1) {
                history.subList(currentIndex + 1, history.size).clear()
            }
            history.add(fileName)
            currentIndex = history.size - 1

            // Reset currentHtml to trigger content update
            currentHtml = null

            val content = getFileContent(fileName)
            content?.let {
                val parsedTree = parser.buildMarkdownTreeFromString(it)
                var generatedHtml = HtmlGenerator(it, parsedTree, flavour).generateHtml()

                // Post-process the generated HTML
                generatedHtml = postProcessHtml(generatedHtml)

                currentHtml = generatedHtml
            }

            if (isFolder(fileName)) {
                expandedFolders.add(fileName)
            }

            bufferString.set(fileName)
        }
    }

    private fun postProcessHtml(html: String): String {
        var processedHtml = html

        // Replace unclosed code blocks
        val codeBlockRegex = Regex("<pre><code([^>]*)>(.*?)```\\s*", RegexOption.DOT_MATCHES_ALL)
        processedHtml = processedHtml.replace(codeBlockRegex) { matchResult ->
            val attributes = matchResult.groupValues[1]
            val content = matchResult.groupValues[2].trim()
            "<pre><code$attributes>$content</code></pre>"
        }

        // Close any remaining unclosed code and pre tags
        processedHtml = processedHtml.replace(Regex("<pre>(?!.*</pre>)"), "<pre></pre>")
        processedHtml = processedHtml.replace(Regex("<code>(?!.*</code>)"), "<code></code>")

        // Remove any leftover triple backticks
        processedHtml = processedHtml.replace("```", "")

        return processedHtml
    }

    private fun isValidPath(path: String): Boolean {
        var current: Any? = markdownFiles
        for (part in path.split("/")) {
            if (part.isEmpty()) continue
            current = (current as? Map<*, *>)?.get(part)
            if (current == null) return false
        }
        return true
    }

    private fun isFolder(path: String): Boolean {
        var current: Any? = markdownFiles
        for (part in path.split("/")) {
            if (part.isEmpty()) continue
            current = (current as? Map<*, *>)?.get(part)
            if (current == null) return false
        }
        return current is Map<*, *>
    }


    private fun canGoBack(): Boolean = currentIndex > 0
    private fun canGoForward(): Boolean = currentIndex < history.size - 1

    private fun handleNavInput(input: String) {
        currentPath = input
        if (isValidPath(input)) {
            loadFile(input)
        }
    }

    private fun goBack() {
        if (canGoBack()) {
            currentIndex--
            currentFile = history[currentIndex]
            bufferString.set(currentFile ?: "")
        }
    }

    private fun goForward() {
        if (canGoForward()) {
            currentIndex++
            currentFile = history[currentIndex]
            bufferString.set(currentFile ?: "")
        }
    }
}