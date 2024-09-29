package bpm.client.render.markdown

import bpm.client.docs.Docs
import bpm.client.font.Fonts
import bpm.common.utils.FontAwesome
import imgui.*
import imgui.callback.ImGuiInputTextCallback
import imgui.flag.*
import imgui.type.ImString
import java.util.*

class MarkdownBrowser(private val docs: Docs) {

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
    private var isNavInputExpanded = false
    private var navInputWidth = 200f
    private var navInputHeight = 30f
    private var navInputColor = ImVec4(0.16f, 0.16f, 0.16f, 1f)
    private val minNavInputWidth: Float
        get() {
            if(bufferString.isEmpty) return 210f

            val text = bufferString.get()
            val width = ImGui.calcTextSize(text).x
            return (width + 60f).coerceAtLeast(100f)
        }
    private val minNavInputHeight = 30f
    private val hoveredNavInputHeight = 34f
    private val focusedNavInputHeight = 38f
    private val navBarHeight = 50f
    private val leftButtonsWidth = 120f // Adjust based on your actual left buttons width
    private val rightButtonsWidth = 120f // Adjust based on your actual right buttons width
    private var wasNavInputFocused = false
    private var focusStartTime = 0L


    private var zoomLevel = 1.0f
    private val minZoom = 0.5f
    private val maxZoom = 2.0f

    private var zoomButtonsAnimation = 0f

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
        val windowFlags = ImGuiWindowFlags.NoDecoration or ImGuiWindowFlags.NoMove or ImGuiWindowFlags.NoResize

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
            startPos.y + navBarHeight,
            ImColor.rgb(30, 30, 30),
            10f
        )

        ImGui.setCursorScreenPos(startPos.x + 20f, startPos.y + 10f)
        renderHamburgerButton()
        ImGui.sameLine()
        renderNavButton(FontAwesome.LeftLong, canGoBack()) { goBack() }
        ImGui.sameLine()
        renderNavButton(FontAwesome.RightLong, canGoForward()) { goForward() }
        ImGui.sameLine()

        val navBarWidth = ImGui.getWindowWidth() - 20f // 10f padding on each side
        val availableWidth = navBarWidth - leftButtonsWidth - rightButtonsWidth
        val maxNavInputWidth = availableWidth - 60f // Leave some padding

        renderExpandingNavInput("File Name", currentFile ?: "", startPos, availableWidth, maxNavInputWidth)

        renderZoomButtons(startPos, navBarWidth)

        ImGui.popStyleVar()
        ImGui.dummy(0f, 60f) // Add some space after the navbar
    }

    private fun renderZoomButtons(startPos: ImVec2, navBarWidth: Float) {
        val zoomButtonsStartX = startPos.x + navBarWidth - rightButtonsWidth - 10f
        val zoomButtonsStartY = startPos.y + 10f
        val drawList = ImGui.getWindowDrawList()
        // Render background for zoom buttons
        drawList.addRectFilled(
            zoomButtonsStartX,
            zoomButtonsStartY,
            zoomButtonsStartX + rightButtonsWidth,
            zoomButtonsStartY + 30f,
            ImColor.rgb(50, 50, 50),
            5f
        )

        ImGui.setCursorScreenPos(zoomButtonsStartX + 5f, zoomButtonsStartY)
        renderNavButton(FontAwesome.MagnifyingGlassMinus, zoomLevel > minZoom) { zoomOut() }
        ImGui.sameLine()
        renderNavButton(FontAwesome.MagnifyingGlassPlus, zoomLevel < maxZoom) { zoomIn() }
        ImGui.sameLine()
        renderNavButton(FontAwesome.RotateRight, true) { resetZoom() }
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

    private fun zoomOut() {
        zoomLevel = (zoomLevel - 0.1f).coerceAtLeast(minZoom)
        updateZoom()
    }

    private fun zoomIn() {
        zoomLevel = (zoomLevel + 0.1f).coerceAtMost(maxZoom)
        updateZoom()
    }

    private fun resetZoom() {
        zoomLevel = 1.0f
        updateZoom()
    }

    private fun updateZoom() {
        // Update the zoom level in your MarkdownRenderer
        renderer.scale = zoomLevel
    }

    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t.coerceIn(0f, 1f)
    }

    private fun lerpColor(a: ImVec4, b: ImVec4, t: Float): ImVec4 {
        return ImVec4(
            lerp(a.x, b.x, t), lerp(a.y, b.y, t), lerp(a.z, b.z, t), lerp(a.w, b.w, t)
        )
    }


    private fun renderExpandingNavInput(
        label: String, value: String, startPos: ImVec2, availableWidth: Float, maxWidth: Float
    ) {
        val drawList = ImGui.getWindowDrawList()

        // Handle focus state changes
        if (isNavInputFocused && !wasNavInputFocused) {
            focusStartTime = System.currentTimeMillis()
            isNavInputExpanded = true
        }
        wasNavInputFocused = isNavInputFocused


        //Only click if not hovering our search input, we unexpand it
        if (ImGui.isMouseClicked(0) && !isNavInputHovered) {
            isNavInputExpanded = false
        }

        // Calculate animation progress
        val focusAnimationDuration = 200 // milliseconds
        val timeSinceFocus = System.currentTimeMillis() - focusStartTime
        val focusProgress = (timeSinceFocus.toFloat() / focusAnimationDuration).coerceIn(0f, 1f)

        val targetAnimation = when {
            isNavInputFocused -> focusProgress
            isNavInputHovered -> 0.5f
            else -> 0f
        }
        navInputAnimation = lerp(navInputAnimation, targetAnimation, ImGui.getIO().deltaTime * 4f)

        // Animate width and height
        val targetWidth = when {
            isNavInputExpanded -> maxWidth
            isNavInputHovered -> maxWidth / 2
            else -> minNavInputWidth
        }
        navInputWidth = lerp(navInputWidth, targetWidth, ImGui.getIO().deltaTime * 4f)

        navInputHeight = when {
            isNavInputFocused -> lerp(hoveredNavInputHeight, focusedNavInputHeight, focusProgress)
            isNavInputHovered -> hoveredNavInputHeight
            else -> minNavInputHeight
        }

        // Animate color
        val targetColor = when {
            isNavInputFocused -> ImVec4(0.24f, 0.24f, 0.24f, 1f)
            isNavInputHovered -> ImVec4(0.20f, 0.20f, 0.20f, 1f)
            else -> ImVec4(0.16f, 0.16f, 0.16f, 1f)
        }
        navInputColor = lerpColor(navInputColor, targetColor, ImGui.getIO().deltaTime * 4f)

        val startX = startPos.x + leftButtonsWidth + (availableWidth - navInputWidth) / 2
        val startY = startPos.y + (navBarHeight - navInputHeight) / 2

        // Render the custom rounded rect
        drawList.addRectFilled(
            startX, startY, startX + navInputWidth, startY + navInputHeight, ImColor.rgba(
                (navInputColor.x * 255).toInt(),
                (navInputColor.y * 255).toInt(),
                (navInputColor.z * 255).toInt(),
                (navInputColor.w * 255).toInt()
            ), 5f
        )

        // Render the search icon
        drawList.addText(
            iconFont,
            18f,
            startX + 8,
            startY + (navInputHeight - 18f) / 2,
            ImColor.rgb(150, 150, 150),
            FontAwesome.MagnifyingGlass
        )

        // Render the file name or input
        bufferString.set(value)
        ImGui.setNextItemWidth(navInputWidth - 40f)

        if (isNavInputExpanded && !isNavInputFocused) {
            // Render centered text when expanded but not focused
            val textSize = ImGui.calcTextSize(value)
            val textX = startX + (navInputWidth - textSize.x) / 2
            val textY = startY + (navInputHeight - textSize.y) / 2
            drawList.addText(textX, textY, ImColor.rgb(200, 200, 200), value)
        } else {
            ImGui.setCursorScreenPos(startX + 30f, startY + (navInputHeight) / 2 - 18f)
            ImGui.pushStyleColor(ImGuiCol.FrameBg, 0)
            ImGui.pushStyleColor(ImGuiCol.Text, ImColor.rgb(200, 200, 200))

            val inputFlags = ImGuiInputTextFlags.AutoSelectAll or ImGuiInputTextFlags.CallbackCompletion or
                    ImGuiInputTextFlags.CallbackEdit or ImGuiInputTextFlags.CallbackAlways
            ImGui.inputText("##$label-nav", bufferString, inputFlags, inputTextCallback)

            ImGui.popStyleColor(2)
        }

        // Check if the input is focused or hovered
        isNavInputFocused = ImGui.isItemActive() || ImGui.isItemFocused()
        isNavInputHovered = ImGui.isItemHovered()

        // Handle unfocusing logic
        if (!isNavInputFocused && !isNavInputHovered && ImGui.isMouseClicked(0)) {
            isNavInputExpanded = false
        }

        // Render autocomplete text
        if (isNavInputFocused && autocompleteText.isNotEmpty()) {
            val fullText = bufferString.get() + autocompleteText
            val autoCompletePos = ImVec2(
                startX + ImGui.calcTextSize(bufferString.get()).x + 41f, startY + (navInputHeight - 16f) / 2
            )
            drawList.addText(autoCompletePos.x, autoCompletePos.y, ImColor.rgb(100, 100, 100), autocompleteText)
        }

        // Render placeholder text when not focused and empty
        if (!isNavInputFocused && !isNavInputExpanded && bufferString.get().isEmpty()) {
            drawList.addText(
                startX + 40f,
                startY + (navInputHeight - 18f) / 2,
                ImColor.rgb(100, 100, 100),
                "Search or enter file name"
            )
        }

        // Change cursor to text input cursor when hovered
        if (isNavInputHovered) {
            ImGui.setMouseCursor(ImGuiMouseCursor.TextInput)
        }
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
                        handleNavInput(currentPath)
                    }
                }

                ImGuiInputTextFlags.CallbackEdit -> {
                    currentPath = data.buf.toString()
                    updateAutocomplete()
                    handleNavInput(currentPath)
                }

                ImGuiInputTextFlags.CallbackAlways -> {
                    if (ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.Enter))) {
                        isNavInputFocused = false
                        isNavInputExpanded = false
                        //Unfocus the input
                        ImGui.setKeyboardFocusHere(1)
                    }
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
        autocompleteOptions = current.keys.filter { it.lowercase().startsWith(lastPart) }.map { currentPathPrefix + it }

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
                pos.x + (size - lineWidth) / 2, lineY, pos.x + (size + lineWidth) / 2, lineY + lineHeight, lineColor
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
            pos.x + 20f, ImGui.getCursorScreenPosY(), pos.x + width - 20f, ImGui.getCursorScreenPosY(), separatorColor
        )

        ImGui.dummy(0f, 20f) // Add padding after separator

        renderFileTree(markdownFiles, "", drawList, pos.x, width)

        ImGui.endChild()

        ImGui.popClipRect()
    }


    private fun renderFileTree(
        files: Map<String, Any>, path: String, drawList: ImDrawList, startX: Float, width: Float, depth: Int = 0
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
                itemStartX - padding, itemStartY, itemStartX + itemWidth, itemStartY + itemHeight
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
                val color = if (isFocused) ImColor.rgba(100, 100, 100, (255 * 0.5f).toInt())
                else ImColor.rgba(80, 80, 80, (255 * 0.3f * hoverAlpha).toInt())
                drawList.addRectFilled(
                    itemStartX - padding, itemStartY, itemStartX + itemWidth, itemStartY + itemHeight, color, 5f
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
                    regularFont, 16f, textStartX, itemStartY + 7f, ImColor.rgb(220, 220, 220), name
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
                    font, 16f, textStartX, itemStartY + 7f, ImColor.rgb(220, 220, 220), name
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


    private fun renderFolderContents(
        folder: Map<String, Any>, path: String, drawList: ImDrawList, startX: Float, width: Float, depth: Int
    ) {
        val animation = folderAnimations.getOrPut(path) { 1f }
        folderAnimations[path] = (animation + ImGui.getIO().deltaTime * 4f).coerceAtMost(1f)

        ImGui.pushStyleVar(ImGuiStyleVar.Alpha, animation)
        renderFileTree(folder, path, drawList, startX, width, depth)
        ImGui.popStyleVar()
    }

    private val renderer = MarkdownRenderer()

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
//                val lastItemRect = ImGui.getItemRectMax()
//                ImGui.dummy(0f, renderer.height)
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
                currentHtml = content
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