package bpm.client.runtime.windows

import imgui.*
import imgui.flag.*
import imgui.internal.ImRect
import bpm.common.logging.KotlinLogging
import bpm.client.font.Fonts
import bpm.client.runtime.Platform
import bpm.client.runtime.ClientRuntime
import bpm.client.utils.TextInputState
import bpm.client.utils.handleUniversalTextInput
import bpm.common.utils.FontAwesome
import bpm.client.utils.use
import bpm.common.network.Client
import bpm.common.property.Property
import bpm.common.property.PropertyMap
import bpm.common.property.cast
import bpm.common.property.castOr
import bpm.common.schemas.Schemas
import bpm.common.type.NodeLibrary
import bpm.common.workspace.Workspace
import bpm.common.workspace.graph.Link
import bpm.common.workspace.graph.Node
import bpm.common.workspace.packets.LinkDeleteRequest
import bpm.common.workspace.packets.NodeDeleteRequest
import org.joml.Vector2f
import java.util.*
import kotlin.math.max

class CustomActionMenu(private val workspace: Workspace, private val canvasCtx: CanvasContext) {

    private var isOpen = false
    private var animationProgress = 0f
    private val animationDuration = 0.2f
    private var menuWidth = 250f
    private var menuHeight = 400f
    private val maxMenuWidth = 300f
    private val maxMenuHeight = 500f
    private val minMenuWidth = 250f
    private val minMenuHeight = 100f
    private var menuPosition = ImVec2()
    private var searchText = ""
    private var hoveredNodeType = ""
    private var scrollPosition = 0f
    private var targetScrollPosition = 0f
    private var lastContentHeight = 0f
    private var contentHeightSmoothingFactor = 0.2f


    private val backgroundColor = ImColor.rgba(30, 30, 30, 240)
    private val searchBarColor = ImColor.rgba(45, 45, 45, 255)
    private val textColor = ImColor.rgba(220, 220, 220, 255)
    private val accentColor = ImColor.rgba(100, 65, 165, 255)
    private val hoverColor = ImColor.rgba(70, 70, 70, 255)
    private val folderColor = ImColor.rgba(60, 60, 60, 255)
    private val nodeLibrary: NodeLibrary get() = Client.installed<Schemas>().library
    private val bodyFont get() = Fonts.getFamily("Inter")["Regular"][14]
    private val iconFont get() = Fonts.getFamily("Fa")["Bold"][18]

    private var isSearchBarFocused = false
    private var isSearchBarHovered = false
    private var cursorPosition = 0
    private var selectionStart = 0
    private var selectionEnd = 0
    private val keyLastPressTime = mutableMapOf<ClientRuntime.Key, Long>()
    private val inputDelay = 250L
    private val expandedFolders = mutableSetOf<String>()

    private var clipboard = mutableListOf<Node>()
    private val logger = KotlinLogging.logger { }
    private var selectedLinks: Set<Link> = emptySet()

    fun copyToClipboard(nodes: List<Node>) {
        clipboard.clear()
        //We need to copy the nodes, updating their positions to be relative to the mouse,
        //and then add them to the clipboard
        for (node in nodes) {
            val copy = node.properties.copy().cast<PropertyMap>()
            copy["x"].cast<Property.Float>().set(node.x - menuPosition.x)
            copy["y"].cast<Property.Float>().set(node.y - menuPosition.y)
            clipboard.add(Node(copy))
            logger.debug { "Copied node ${node.name} to clipboard" }
        }
    }

    fun hasClipboardContent(): Boolean {
        return clipboard.isNotEmpty()
    }

    private var cursorBlinkTime = 0f
    private val cursorBlinkDuration = 0.53f
    private var searchBarScrollOffset = 0f

    private data class FolderItem(val name: String, val items: MutableList<Any> = mutableListOf())
    private data class NodeItem(val name: String, val group: String) {

        var nodeIcon: String = FontAwesome.CompactDisc
        val nodeType: String = "$group/$name"
    }

    private lateinit var folderStructure: MutableList<Any>

    private var selectedNodes: Set<Node> = emptySet()
    private var isNodeMenu = false

    init {
        buildFolderStructure()
    }

    fun render(drawList: ImDrawList) {
        if (!isOpen) return

        updateAnimation()

        // Recalculate content size and adjust menu size
        val contentSize = calculateContentSize()
        menuWidth = contentSize.x.coerceIn(minMenuWidth, maxMenuWidth)
        menuHeight = contentSize.y.coerceIn(minMenuHeight, maxMenuHeight)

        // Adjust menu position if it goes out of window bounds
        val windowSize = ImVec2()
        ImGui.getWindowSize(windowSize)
        menuPosition.x = max(menuPosition.x, 0f)
        menuPosition.y = max(menuPosition.y, 0f)

        val scaledHeight = menuHeight * animationProgress
        drawList.addRectFilled(
            menuPosition.x,
            menuPosition.y,
            menuPosition.x + menuWidth,
            menuPosition.y + scaledHeight,
            backgroundColor,
            10f
        )

        if (animationProgress > 0.5f) {
            if (isNodeMenu) {
                renderNodeMenu(drawList)
            } else {
                renderSearchBar(drawList)
                renderNodeTypes(drawList)
            }
        }

        handleInput()
    }

    //"Delete node" if one node is selected, "Delete link, if one link is selected, "Delete nodes & links" if multiple nodes are selected

    private val selectionText: String
        get() {
            val nodeCount = selectedNodes.size
            val linkCount = selectedLinks.size

            return when {
                nodeCount == 0 && linkCount == 0 -> "No items selected"
                nodeCount == 1 && linkCount == 0 -> "Selected: 1 node (${selectedNodes.first().name})"
                nodeCount == 0 && linkCount == 1 -> "Selected: 1 link"
                nodeCount > 1 && linkCount == 0 -> "Selected: $nodeCount nodes"
                nodeCount == 0 && linkCount > 1 -> "Selected: $linkCount links"
                nodeCount == 1 && linkCount == 1 -> "Selected: 1 node and 1 link"
                else -> "Selected: $nodeCount nodes and $linkCount links"
            }
        }


    private fun renderNodeMenu(drawList: ImDrawList) {
        val padding = 10f
        var yPos = menuPosition.y + padding

        // Render selection info
        bodyFont.use {

            drawList.addText(
                bodyFont, 16f, menuPosition.x + padding, yPos, textColor, selectionText
            )
        }
        yPos += 30f


        val nodes = selectedNodes.size
        val links = selectedLinks.size
        // Render action buttons
        renderActionButton(drawList, "Delete       (Del)", FontAwesome.Trash, yPos) {
            deleteSelectedNodes()
            deleteSelectedLinks()
        }
        yPos += 40f

        renderActionButton(drawList, "Cut             (Ctrl+X)", FontAwesome.Scissors, yPos) {
            cutSelectedNodes()
        }
        yPos += 40f

        renderActionButton(drawList, "Copy          (Ctrl+C)", FontAwesome.Copy, yPos) {
            copySelectedNodes()
        }
        yPos += 40f
        // Only show paste if there's something in the clipboard
        if (hasClipboardContent()) {
            renderActionButton(drawList, "Paste     (Ctrl+V)", FontAwesome.Paste, yPos) {
                pasteNodes()
            }
        }


    }

    fun pasteFromClipboard(position: Vector2f) {
        for (node in clipboard) {
//            val worldPos = canvasCtx.convertToWorldCoordinates(position)
//            val copy = node.properties.copy().cast<PropertyMap>()
//            copy["x"].cast<Property.Float>().set(worldPos.x)
//            copy["y"].cast<Property.Float>().set(worldPos.y)
            logger.debug { "TODO: this needs to take the links into account. Must implement link selection first" }
        }
    }

    private fun deleteSelectedLinks() {
        selectedLinks.forEach { link ->
            canvasCtx.client.send(LinkDeleteRequest(link.uid))
        }
    }

    private fun renderActionButton(drawList: ImDrawList, label: String, icon: String, yPos: Float, action: () -> Unit) {
        val padding = 10f
        val buttonWidth = menuWidth - 2 * padding
        val buttonHeight = 30f
        val xPos = menuPosition.x + padding

        val isHovered = ImGui.isMouseHoveringRect(xPos, yPos, xPos + buttonWidth, yPos + buttonHeight)
        val buttonColor = if (isHovered) hoverColor else folderColor

        drawList.addRectFilled(xPos, yPos, xPos + buttonWidth, yPos + buttonHeight, buttonColor, 5f)

        iconFont.use {
            drawList.addText(
                iconFont, 14f, xPos + 10f, yPos + 8f, textColor, icon
            )
        }

        bodyFont.use {
            drawList.addText(
                bodyFont, 14f, xPos + 35f, yPos + 8f, textColor, label
            )
        }

        if (isHovered && ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
            action()
            close()
        }
    }

    private fun deleteNode(nodeId: UUID) {
        //colects all the links that are connected to the node
        val links = workspace.graph.getLinks(nodeId)
        for (link in links) {
            Client { it.send(LinkDeleteRequest(link.uid)) }
        }
        Client { it.send(NodeDeleteRequest(nodeId)) }
    }

    private fun deleteSelectedNodes() {
        selectedNodes.forEach { node ->
            deleteNode(node.uid)
        }

    }

    private fun cutSelectedNodes() {
        copySelectedNodes()
        deleteSelectedNodes()
    }

    private fun copySelectedNodes() {
        copyToClipboard(selectedNodes.toList())
    }

    private fun pasteNodes() {
        val worldPos = canvasCtx.convertToWorldCoordinates(Vector2f(menuPosition.x, menuPosition.y))
        pasteFromClipboard(worldPos)
    }

    //    private fun renderSearchBar(drawList: ImDrawList) {
//        val searchBarHeight = 30f
//        drawList.addRectFilled(
//            menuPosition.x + 10f,
//            menuPosition.y + 10f,
//            menuPosition.x + menuWidth - 10f,
//            menuPosition.y + 10f + searchBarHeight,
//            searchBarColor,
//            5f
//        )
//
//        bodyFont.use {
//            val visibleWidth = menuWidth - 20f
//            val textWidth = ImGui.calcTextSize(searchText).x
//
//            if (isSearchBarFocused) {
//                val cursorX = ImGui.calcTextSize(searchText.substring(0, cursorPosition)).x
//                if (cursorX - searchBarScrollOffset > visibleWidth) {
//                    searchBarScrollOffset = cursorX - visibleWidth
//                } else if (cursorX < searchBarScrollOffset) {
//                    searchBarScrollOffset = cursorX
//                }
//            }
//
//            drawList.pushClipRect(
//                menuPosition.x + 10f, menuPosition.y + 10f, menuPosition.x + menuWidth - 10f, menuPosition.y + 40f
//            )
//
//            if (searchText.isEmpty() && !isSearchBarFocused) {
//                drawList.addText(
//                    bodyFont,
//                    14f,
//                    menuPosition.x + 15f,
//                    menuPosition.y + 18f,
//                    ImColor.rgba(150, 150, 150, 180),
//                    "Search..."
//                )
//            } else {
//                drawList.addText(
//                    bodyFont,
//                    14f,
//                    menuPosition.x + 15f - searchBarScrollOffset,
//                    menuPosition.y + 18f,
//                    textColor,
//                    searchText
//                )
//
//                if (isSearchBarFocused && shouldShowCursor()) {
//                    val cursorX = menuPosition.x + 15f + ImGui.calcTextSize(
//                        searchText.substring(
//                            0, cursorPosition
//                        )
//                    ).x - searchBarScrollOffset
//                    drawList.addLine(
//                        cursorX, menuPosition.y + 15f, cursorX, menuPosition.y + 35f, textColor
//                    )
//                }
//
//                if (selectionStart != selectionEnd) {
//                    val startX = menuPosition.x + 15f + ImGui.calcTextSize(
//                        searchText.substring(
//                            0, selectionStart
//                        )
//                    ).x - searchBarScrollOffset
//                    val endX = menuPosition.x + 15f + ImGui.calcTextSize(
//                        searchText.substring(
//                            0, selectionEnd
//                        )
//                    ).x - searchBarScrollOffset
//                    drawList.addRectFilled(
//                        startX, menuPosition.y + 15f, endX, menuPosition.y + 35f, ImColor.rgba(100, 100, 255, 100)
//                    )
//                }
//            }
//            drawList.popClipRect()
//        }
//    }
    private var searchInputState = TextInputState()
    private fun renderSearchBar(drawList: ImDrawList) {
        val searchBarHeight = 30f
        drawList.addRectFilled(
            menuPosition.x + 10f,
            menuPosition.y + 10f,
            menuPosition.x + menuWidth - 10f,
            menuPosition.y + 10f + searchBarHeight,
            searchBarColor,
            5f
        )

        bodyFont.use {
            searchInputState = handleUniversalTextInput(
                drawList,
                searchInputState,
                bodyFont,
                14f,
                menuPosition.x + 15f,
                menuPosition.y + 15f,
                menuWidth - 30f,
                searchBarHeight - 10f,
                textColor,
                ImColor.rgba(0, 0, 0, 0),
                ImColor.rgba(100, 100, 255, 100)
            )

            searchText = searchInputState.text
        }
    }


//    private fun renderNodeTypes(drawList: ImDrawList) {
//        val startY = menuPosition.y + 50f
//        val endY = menuPosition.y + menuHeight - 10f
//        val clipRect = ImRect(menuPosition.x, startY, menuPosition.x + menuWidth, endY)
//        drawList.pushClipRect(clipRect.min.x, clipRect.min.y, clipRect.max.x, clipRect.max.y)
//
//        var yOffset = startY - scrollPosition
//
//        if (searchText.isEmpty()) {
//            renderFolderStructure(drawList, folderStructure, 0, yOffset, startY, endY)
//        } else {
//            renderFilteredNodes(drawList, yOffset, startY, endY)
//        }
//
//        drawList.popClipRect()
//
//        renderScrollbar(drawList, startY, endY)
//    }

    private fun renderNodeTypes(drawList: ImDrawList) {
        val startY = menuPosition.y + 50f
        val endY = menuPosition.y + menuHeight - 10f
        val clipRect = ImRect(menuPosition.x, startY, menuPosition.x + menuWidth, endY)
        drawList.pushClipRect(clipRect.min.x, clipRect.min.y, clipRect.max.x, clipRect.max.y)

        var yOffset = startY - scrollPosition

        if (searchText.isEmpty()) {
            renderFolderStructure(drawList, folderStructure, 0, yOffset, startY, endY)
        } else {
            renderFilteredNodes(drawList, yOffset, startY, endY)
        }

        drawList.popClipRect()

        // Only render scrollbar if content height significantly exceeds menu height
        if (lastContentHeight > menuHeight + 20f) {
            renderScrollbar(drawList, startY, endY)
        }
    }


    private fun renderFolderStructure(
        drawList: ImDrawList, items: List<Any>, depth: Int, yOffset: Float, startY: Float, endY: Float
    ): Float {
        var currentYOffset = yOffset
        items.forEach { item ->
            when (item) {
                is FolderItem -> {
                    if (currentYOffset + 30f > startY && currentYOffset < endY) {
                        renderFolder(drawList, item, depth, currentYOffset)
                    }
                    currentYOffset += 30f
                    if (expandedFolders.contains(item.name)) {
                        currentYOffset = renderFolderStructure(
                            drawList, item.items, depth + 1, currentYOffset, startY, endY
                        )
                    }
                }

                is NodeItem -> {
                    if (currentYOffset + 30f > startY && currentYOffset < endY) {
                        renderNode(drawList, item, depth, currentYOffset)
                    }
                    currentYOffset += 30f
                }
            }
        }
        return currentYOffset
    }

    private fun renderFolder(drawList: ImDrawList, folder: FolderItem, depth: Int, yOffset: Float) {
        val isExpanded = expandedFolders.contains(folder.name)
        val icon = if (isExpanded) FontAwesome.ChevronDown else FontAwesome.ChevronRight

        val isHovered = ImGui.isMouseHoveringRect(
            menuPosition.x, yOffset, menuPosition.x + menuWidth, yOffset + 30f
        )

        val backgroundColor = if (isHovered) hoverColor else folderColor
        drawList.addRectFilled(
            menuPosition.x, yOffset, menuPosition.x + menuWidth, yOffset + 30f, backgroundColor
        )

        iconFont.use {
            drawList.addText(
                iconFont, 14f, menuPosition.x + 15f + depth * 20f, yOffset + 8f, textColor, icon
            )
        }

        bodyFont.use {
            drawList.addText(
                bodyFont, 14f, menuPosition.x + 35f + depth * 20f, yOffset + 8f, textColor, folder.name
            )
        }

        if (isHovered) {
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand)
        }

        if (isHovered && ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
            if (isExpanded) {
                expandedFolders.remove(folder.name)
            } else {
                expandedFolders.add(folder.name)
            }
            ImGui.getIO().setMouseDown(0, false)
        }
    }

//    private fun renderNode(drawList: ImDrawList, node: NodeItem, depth: Int, yOffset: Float) {
//        val isHovered = node.name == hoveredNodeType || ImGui.isMouseHoveringRect(
//            menuPosition.x, yOffset, menuPosition.x + menuWidth, yOffset + 30f
//        )
//
//        if (isHovered) {
//            drawList.addRectFilled(
//                menuPosition.x, yOffset, menuPosition.x + menuWidth, yOffset + 30f, hoverColor
//            )
//            Platform.setMouseIcon(Platform.MouseIcon.HAND)
//        }
//
//        bodyFont.use {
//            drawList.addText(
//                bodyFont, 14f, menuPosition.x + 35f + depth * 20f, yOffset + 8f, textColor, node.name
//            )
//        }
//
//        if (isHovered && ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
//            createNode(Vector2f(ImGui.getMousePos().x, ImGui.getMousePos().y), node.nodeType)
//            close()
//        }
//    }

    private fun renderNode(drawList: ImDrawList, node: NodeItem, depth: Int, yOffset: Float) {
        val isHovered = node.name == hoveredNodeType || ImGui.isMouseHoveringRect(
            menuPosition.x, yOffset, menuPosition.x + menuWidth, yOffset + 30f
        )

        if (isHovered) {
            drawList.addRectFilled(
                menuPosition.x, yOffset, menuPosition.x + menuWidth, yOffset + 30f, hoverColor
            )
            Platform.setMouseIcon(Platform.MouseIcon.HAND)
        }

        // Render node icon


        drawList.addText(
            iconFont, 18f, menuPosition.x + 15f + depth * 20f, yOffset + 8f, textColor, node.nodeIcon
        )

        // Render node name
        drawList.addText(
            bodyFont, 14f, menuPosition.x + 40f + depth * 20f, yOffset + 8f, textColor, node.name
        )

        if (isHovered && ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
            createNode(Vector2f(ImGui.getMousePos().x, ImGui.getMousePos().y), node.nodeType)
            close()
        }
    }


    private fun renderFilteredNodes(drawList: ImDrawList, yOffset: Float, startY: Float, endY: Float) {
        var currentYOffset = yOffset
        val filteredNodes = nodeLibrary.filter {
            it.nodeTypeName.contains(searchText, ignoreCase = true) || it.group.contains(searchText, ignoreCase = true)
        }

        filteredNodes.forEach { nodeType ->
            if (currentYOffset + 30f > startY && currentYOffset < endY) {
                renderNode(drawList, NodeItem(nodeType.name, nodeType.group), 0, currentYOffset)
            }
            currentYOffset += 30f
        }
    }

//    private fun renderScrollbar(drawList: ImDrawList, startY: Float, endY: Float) {
//        val contentHeight = calculateContentHeight()
//        if (contentHeight > menuHeight - 60f) {
//            val scrollbarHeight = (menuHeight - 60f) / contentHeight * (menuHeight - 60f)
//            val scrollbarY = startY + (scrollPosition / contentHeight) * (menuHeight - 60f)
//            drawList.addRectFilled(
//                menuPosition.x + menuWidth - 8f,
//                scrollbarY,
//                menuPosition.x + menuWidth - 2f,
//                scrollbarY + scrollbarHeight,
//                accentColor,
//                3f
//            )
//        }
//    }

    private fun renderScrollbar(drawList: ImDrawList, startY: Float, endY: Float) {
        val visibleHeight = endY - startY
        if (lastContentHeight > visibleHeight) {
            val scrollbarHeight = (visibleHeight / lastContentHeight) * visibleHeight
            val scrollbarY = startY + (scrollPosition / lastContentHeight) * visibleHeight
            drawList.addRectFilled(
                menuPosition.x + menuWidth - 8f,
                scrollbarY,
                menuPosition.x + menuWidth - 2f,
                scrollbarY + scrollbarHeight,
                accentColor,
                3f
            )
        }
    }


//    private fun calculateContentHeight(): Float {
//        return if (searchText.isEmpty()) {
//            calculateFolderStructureHeight(folderStructure)
//        } else {
//            nodeLibrary.count {
//                it.nodeTypeName.contains(searchText, ignoreCase = true) || it.group.contains(
//                    searchText,
//                    ignoreCase = true
//                )
//            } * 30f
//        }
//    }

    private fun calculateContentHeight(): Float {
        return if (searchText.isEmpty()) {
            calculateFolderStructureHeight(folderStructure)
        } else {
            nodeLibrary.count {
                it.nodeTypeName.contains(searchText, ignoreCase = true) || it.group.contains(
                    searchText,
                    ignoreCase = true
                )
            } * 30f
        }
    }

//    private fun calculateFolderStructureHeight(items: List<Any>): Float {
//        var height = 0f
//        items.forEach { item ->
//            height += 30f
//            if (item is FolderItem && expandedFolders.contains(item.name)) {
//                height += calculateFolderStructureHeight(item.items)
//            }
//        }
//        return height
//    }

    private fun calculateFolderStructureHeight(items: List<Any>): Float {
        var height = 0f
        items.forEach { item ->
            when (item) {
                is FolderItem -> {
                    height += 30f
                    if (expandedFolders.contains(item.name)) {
                        height += calculateFolderStructureHeight(item.items)
                    }
                }

                is NodeItem -> height += 30f
            }
        }
        return height
    }

    private fun handleInput() {
        val mousePos = ImGui.getMousePos()
        val isMouseOverMenu = mousePos.x >= menuPosition.x && mousePos.x <= menuPosition.x + menuWidth && mousePos.y >= menuPosition.y && mousePos.y <= menuPosition.y + menuHeight * animationProgress

        if (isMouseOverMenu) {
            handleSearchBarInput(mousePos)
            handleNodeTypeSelection(mousePos)
            handleScrolling()
        } else {
            if (ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
                close()
            }
        }

        if (isMouseOverMenu) {
            Platform.setMouseIcon(Platform.MouseIcon.ARROW)
        }

        handleKeyboardNavigation()
    }

    private fun handleSearchBarInput(mousePos: ImVec2) {
        val searchBarRect = ImRect(
            menuPosition.x + 10f, menuPosition.y + 10f, menuPosition.x + menuWidth - 10f, menuPosition.y + 40f
        )

        isSearchBarHovered = searchBarRect.contains(mousePos)

        if (ImGui.isMouseClicked(ImGuiMouseButton.Left) && isSearchBarHovered) {
            isSearchBarFocused = true
            ImGui.setKeyboardFocusHere()
        } else if (ImGui.isMouseClicked(ImGuiMouseButton.Left) && !isSearchBarHovered) {
            isSearchBarFocused = false
        }

        if (ImGui.isMouseClicked(ImGuiMouseButton.Right) && isSearchBarHovered) {
            searchText = ""
            searchInputState = TextInputState()
        }
        if (isSearchBarHovered) {
            ImGui.setMouseCursor(ImGuiMouseCursor.TextInput)
        }
    }


//    private fun handleSearchBarInput(mousePos: ImVec2) {
//        val searchBarRect = ImRect(
//            menuPosition.x + 10f, menuPosition.y + 10f, menuPosition.x + menuWidth - 10f, menuPosition.y + 40f
//        )
//
//        isSearchBarHovered = searchBarRect.contains(mousePos)
//
//        if (ImGui.isMouseClicked(ImGuiMouseButton.Left) && isSearchBarHovered) {
//            isSearchBarFocused = true
//            cursorPosition = calculateCursorPosition(mousePos.x - menuPosition.x - 15f)
//            selectionStart = cursorPosition
//            selectionEnd = cursorPosition
//        } else if (ImGui.isMouseClicked(ImGuiMouseButton.Left) && !isSearchBarHovered) {
//            isSearchBarFocused = false
//        }
//
//        if (isSearchBarFocused) {
//            handleKeyPresses()
//        }
//
//        if (ImGui.isMouseClicked(ImGuiMouseButton.Right) && isSearchBarHovered) {
//            searchText = ""
//        }
//        if (isSearchBarHovered) {
//            ImGui.setMouseCursor(ImGuiMouseCursor.TextInput)
//        }
//
//
//    }

    private fun handleKeyPresses() {
        val currentTime = System.currentTimeMillis()

        for (key in ClientRuntime.Key.values()) {
            if (Platform.isKeyDown(key)) {
                val lastPressTime = keyLastPressTime.getOrDefault(key, 0L)
                if (currentTime - lastPressTime > inputDelay) {
                    handleKeyInput(key)
                    keyLastPressTime[key] = currentTime
                }
            } else {
                keyLastPressTime.remove(key)
            }
        }
    }

    private fun handleKeyInput(key: ClientRuntime.Key) {
        when (key) {
            ClientRuntime.Key.BACKSPACE -> handleBackspace()
            ClientRuntime.Key.DELETE -> handleDelete()
            ClientRuntime.Key.LEFT -> handleLeftArrow()
            ClientRuntime.Key.RIGHT -> handleRightArrow()
            ClientRuntime.Key.ENTER -> createSelectedNode()
            ClientRuntime.Key.SPACE -> insertCharacter(' ')
            else -> handleCharacterInput(key)
        }
    }

    private fun handleCharacterInput(key: ClientRuntime.Key) {
        val char = key.toString().singleOrNull()
        if (char != null && (char.isLetterOrDigit() || char.isWhitespace())) {
            insertCharacter(char)
        }
    }

    private fun insertCharacter(char: Char) {
        if (selectionStart != selectionEnd) {
            deleteSelectedText()
        }
        searchText = searchText.substring(0, cursorPosition) + char + searchText.substring(cursorPosition)
        cursorPosition++
        selectionStart = cursorPosition
        selectionEnd = cursorPosition
    }

    private fun handleBackspace() {
        if (selectionStart != selectionEnd) {
            deleteSelectedText()
        } else if (cursorPosition > 0) {
            searchText = searchText.substring(0, cursorPosition - 1) + searchText.substring(cursorPosition)
            cursorPosition--
            selectionStart = cursorPosition
            selectionEnd = cursorPosition
        }
    }

    private fun handleDelete() {
        if (selectionStart != selectionEnd) {
            deleteSelectedText()
        } else if (cursorPosition < searchText.length) {
            searchText = searchText.substring(0, cursorPosition) + searchText.substring(cursorPosition + 1)
        }
    }

    private fun handleLeftArrow() {
        if (cursorPosition > 0) cursorPosition--
        if (!Platform.isKeyDown(ClientRuntime.Key.LEFT_SHIFT)) {
            selectionStart = cursorPosition
            selectionEnd = cursorPosition
        }
    }

    private fun handleRightArrow() {
        if (cursorPosition < searchText.length) cursorPosition++
        if (!Platform.isKeyDown(ClientRuntime.Key.LEFT_SHIFT)) {
            selectionStart = cursorPosition
            selectionEnd = cursorPosition
        }
    }

    private fun deleteSelectedText() {
        val start = minOf(selectionStart, selectionEnd)
        val end = maxOf(selectionStart, selectionEnd)
        searchText = searchText.substring(0, start) + searchText.substring(end)
        cursorPosition = start
        selectionStart = start
        selectionEnd = start
    }

    private fun handleNodeTypeSelection(mousePos: ImVec2) {
        val startY = menuPosition.y + 50f
        val endY = menuPosition.y + menuHeight - 10f
        val filteredNodeTypes = nodeLibrary.filter {
            it.nodeTypeName.contains(searchText, ignoreCase = true)
        }

        var yOffset = startY - scrollPosition
        filteredNodeTypes.forEach { nodeType ->
            if (yOffset + 30f > startY && yOffset < endY) {
                if (mousePos.x >= menuPosition.x && mousePos.x <= menuPosition.x + menuWidth && mousePos.y >= yOffset && mousePos.y <= yOffset + 30f) {
                    hoveredNodeType = nodeType.nodeTypeName
                }
            }
            yOffset += 30f
        }
    }

//    private fun handleScrolling() {
//        val scrollSpeed = 30f
//        val currentTime = System.currentTimeMillis()
//
//        if (Platform.isKeyDown(Runtime.Key.UP)) {
//            val lastPressTime = keyLastPressTime.getOrDefault(Runtime.Key.UP, 0L)
//            if (currentTime - lastPressTime > inputDelay) {
//                scrollPosition -= scrollSpeed * ImGui.getIO().deltaTime
//                keyLastPressTime[Runtime.Key.UP] = currentTime
//            }
//        } else if (Platform.isKeyDown(Runtime.Key.DOWN)) {
//            val lastPressTime = keyLastPressTime.getOrDefault(Runtime.Key.DOWN, 0L)
//            if (currentTime - lastPressTime > inputDelay) {
//                scrollPosition += scrollSpeed * ImGui.getIO().deltaTime
//                keyLastPressTime[Runtime.Key.DOWN] = currentTime
//            }
//        }
//
//        scrollPosition = scrollPosition.coerceIn(0f, (calculateContentHeight() - (menuHeight - 60f)).coerceAtLeast(0f))
//    }

    private fun handleScrolling() {
        val io = ImGui.getIO()
        val visibleHeight = menuHeight - 60f  // Subtract search bar height

        if (lastContentHeight > visibleHeight) {
            if (io.mouseWheel != 0f && isHovered()) {
                targetScrollPosition -= io.mouseWheel * 30f
                targetScrollPosition = targetScrollPosition.coerceIn(0f, maxOf(0f, lastContentHeight - visibleHeight))
            }

            // Smooth scrolling
            scrollPosition += (targetScrollPosition - scrollPosition) * 0.3f
        } else {
            // Gradually reset scroll position if content fits within the menu
            targetScrollPosition = 0f
            scrollPosition += (targetScrollPosition - scrollPosition) * 0.3f
        }
    }


    private fun handleKeyboardNavigation() {
        val currentTime = System.currentTimeMillis()

        fun handleNavigationKey(key: ClientRuntime.Key, action: () -> Unit) {
            if (Platform.isKeyDown(key)) {
                val lastPressTime = keyLastPressTime.getOrDefault(key, 0L)
                if (currentTime - lastPressTime > inputDelay) {
                    action()
                    keyLastPressTime[key] = currentTime
                }
            }
        }

        handleNavigationKey(ClientRuntime.Key.TAB) {
            val nodeTypes = nodeLibrary.map { it.nodeTypeName }
            val currentIndex = nodeTypes.indexOf(hoveredNodeType)
            hoveredNodeType = if (currentIndex == -1) {
                nodeTypes.firstOrNull() ?: ""
            } else {
                nodeTypes[(currentIndex + 1) % nodeTypes.size]
            }
        }

        handleNavigationKey(ClientRuntime.Key.UP) {
            val nodeTypes = nodeLibrary.map { it.nodeTypeName }
            val currentIndex = nodeTypes.indexOf(hoveredNodeType)
            hoveredNodeType = if (currentIndex == -1) {
                nodeTypes.lastOrNull() ?: ""
            } else {
                nodeTypes[(currentIndex - 1 + nodeTypes.size) % nodeTypes.size]
            }
        }

        handleNavigationKey(ClientRuntime.Key.DOWN) {
            val nodeTypes = nodeLibrary.map { it.nodeTypeName }
            val currentIndex = nodeTypes.indexOf(hoveredNodeType)
            hoveredNodeType = if (currentIndex == -1) {
                nodeTypes.firstOrNull() ?: ""
            } else {
                nodeTypes[(currentIndex + 1) % nodeTypes.size]
            }
        }

        handleNavigationKey(ClientRuntime.Key.ENTER) {
            createSelectedNode()
        }

        handleNavigationKey(ClientRuntime.Key.ESCAPE) {
            close()
        }
    }

    private fun createSelectedNode() {
        val mousePos = ImGui.getMousePos()
        createNode(Vector2f(mousePos.x, mousePos.y), hoveredNodeType)
        close()
    }

    private fun createNode(position: Vector2f, nodeType: String) {
        canvasCtx.createNodeAndLink(position, nodeType)
    }

    private fun calculateCursorPosition(xOffset: Float): Int {
        var position = 0
        var currentWidth = 0f
        bodyFont.use {
            while (position < searchText.length && currentWidth < xOffset) {
                currentWidth += ImGui.calcTextSize(searchText[position].toString()).x
                position++
            }
        }
        return position
    }

    private fun updateAnimation() {
        val deltaTime = ImGui.getIO().deltaTime
        if (isOpen && animationProgress < 1f) {
            animationProgress = (animationProgress + deltaTime / animationDuration).coerceAtMost(1f)
        } else if (!isOpen && animationProgress > 0f) {
            animationProgress = (animationProgress - deltaTime / animationDuration).coerceAtLeast(0f)
        }

        // Update scrolling here to ensure smooth movement
        handleScrolling()

        cursorBlinkTime += deltaTime
        if (cursorBlinkTime > cursorBlinkDuration) {
            cursorBlinkTime -= cursorBlinkDuration
        }
    }

    private fun shouldShowCursor(): Boolean {
        return cursorBlinkTime < cursorBlinkDuration / 2
    }

    private fun calculateContentSize(): ImVec2 {
        val contentSize = ImVec2()
        contentSize.x = minMenuWidth  // Start with minimum width

        val currentContentHeight = if (isNodeMenu) {
            calculateNodeMenuHeight()
        } else {
            50f + calculateContentHeight() // 50f for search bar
        }

        // Smooth out content height changes
        lastContentHeight = lastContentHeight * (1 - contentHeightSmoothingFactor) + currentContentHeight * contentHeightSmoothingFactor

        contentSize.y = lastContentHeight

        // Add padding
        contentSize.x += 20f  // 10f padding on each side
        contentSize.y += 20f  // 10f padding on top and bottom

        return contentSize
    }

    private fun calculateNodeMenuHeight(): Float {
        var height = 40f  // Initial padding
        height += 30f  // Selection info
        height += (40f * 3)  // Three action buttons (Delete, Cut, Copy)
        if (hasClipboardContent()) {
            height += 40f  // Paste button
        }
        height += 10f  // Separator
        if (selectedLinks.isNotEmpty()) {
            height += 40f  // Delete Links button
        }
        return height
    }

    var isInitialOpen = true

    fun open(
        position: ImVec2,
        selectedNodes: Set<Node> = emptySet(),
        selectedLinks: Set<Link> = emptySet(),
        rebuild: Boolean = true
    ) {
        if (rebuild) buildFolderStructure()
        if (isInitialOpen) {
            isInitialOpen = false
            return
        }
        isOpen = true

        // Calculate content size
        val contentSize = calculateContentSize()

        // Set menu size, respecting min and max limits
        menuWidth = contentSize.x.coerceIn(minMenuWidth, maxMenuWidth)
        menuHeight = contentSize.y.coerceIn(minMenuHeight, maxMenuHeight)

        // Adjust menu position to fit within window
        val windowSize = ImVec2()
        ImGui.getWindowSize(windowSize)

        // Check if menu would go off the right side of the screen
        if (position.x + menuWidth > windowSize.x) {
            menuPosition.x = windowSize.x - menuWidth
        } else {
            menuPosition.x = position.x
        }

        // Check if menu would go off the bottom of the screen
        if (position.y + menuHeight > windowSize.y) {
            menuPosition.y = windowSize.y - menuHeight
        } else {
            menuPosition.y = position.y
        }

        searchText = ""
        hoveredNodeType = ""
        scrollPosition = 0f
        isSearchBarFocused = false
        cursorPosition = 0
        selectionStart = 0
        selectionEnd = 0
        this.selectedNodes = selectedNodes
        this.selectedLinks = selectedLinks
        isNodeMenu = selectedNodes.isNotEmpty() || selectedLinks.isNotEmpty()
    }

    fun close() {
        isOpen = false
        searchInputState = TextInputState()
    }

    fun isVisible(): Boolean = animationProgress > 0f && isOpen
    fun isHovered(): Boolean = ImGui.isMouseHoveringRect(
        menuPosition.x, menuPosition.y, menuPosition.x + menuWidth, menuPosition.y + menuHeight
    )

    private fun buildFolderStructure() {
        folderStructure = mutableListOf()
        val rootFolder = FolderItem("Root")
        nodeLibrary.forEach { nodeType ->
            val folder = rootFolder.items.filterIsInstance<FolderItem>().find { it.name == nodeType.group }
            if (nodeType.group != "Base") {
                if (folder != null) {
                    val item = NodeItem(nodeType.name, nodeType.group)
                    val icon = nodeLibrary.get(item.nodeType)?.get()?.get("theme")?.cast<PropertyMap>()?.get("icon")
                        ?.castOr { Property.Int(0x1F4D6) }?.get() ?: 0x1F4D6
                    item.nodeIcon = icon.toChar().toString()
                    folder.items.add(item)
                } else {
                    rootFolder.items.add(
                        FolderItem(
                            nodeType.group, mutableListOf(NodeItem(nodeType.name, nodeType.group))
                        )
                    )
                }
            }
        }
        folderStructure = rootFolder.items
    }

    fun openWithFilteredNodes(position: Vector2f, compatibleNodes: List<String>) {
        buildFilteredFolderStructure(compatibleNodes)
        open(ImVec2(position.x, position.y), rebuild = false)
        searchText = ""
        hoveredNodeType = ""
        scrollPosition = 0f
        isSearchBarFocused = false
        cursorPosition = 0
        selectionStart = 0
        selectionEnd = 0
        isNodeMenu = false
    }

    private fun buildFilteredFolderStructure(compatibleNodes: List<String>) {
        folderStructure = mutableListOf()
        val rootFolder = FolderItem("Compatible Nodes")
        nodeLibrary.forEach { nodeType ->
            if (compatibleNodes.contains(nodeType.nodeTypeName)) {
                val folder = rootFolder.items.filterIsInstance<FolderItem>().find { it.name == nodeType.group }
                if (folder != null) {
                    val item = NodeItem(nodeType.name, nodeType.group)
                    val icon = nodeLibrary.get(item.nodeType)?.get()?.get("theme")?.cast<PropertyMap>()?.get("icon")
                        ?.castOr { Property.Int(0x1F4D6) }?.get() ?: 0x1F4D6
                    item.nodeIcon = icon.toChar().toString()
                    folder.items.add(item)
                } else {
                    rootFolder.items.add(
                        FolderItem(
                            nodeType.group, mutableListOf(NodeItem(nodeType.name, nodeType.group))
                        )
                    )
                }
            }
        }
        folderStructure = rootFolder.items
    }


    companion object {

        private fun minOf(a: Int, b: Int): Int = if (a < b) a else b
        private fun maxOf(a: Int, b: Int): Int = if (a > b) a else b


        private fun ImRect.contains(pos: ImVec2): Boolean {
            return pos.x >= min.x && pos.x <= max.x && pos.y >= min.y && pos.y <= max.y
        }
    }
}