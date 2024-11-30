package bpm.client.dockspace

import imgui.ImDrawList
import imgui.ImGui
import imgui.flag.ImGuiMouseCursor
import org.joml.Vector2f

enum class DockPosition {
    LEFT, RIGHT, TOP, BOTTOM, CENTER, FLOATING
}

abstract class Panel(
    val id: Int = nextId(),
    val title: String,
    var layout: DockPosition = DockPosition.FLOATING,
    val isMoveable: Boolean = true
) {

    var parent: Panel? = null
    var position: Vector2f = Vector2f(0f, 0f)
    var size: Vector2f = Vector2f(300f, 200f)
    var isVisible: Boolean = true

    // Store relative sizes for split ratios
    private var relativeWidth: Float = 0.5f  // Default split ratio
    private var relativeHeight: Float = 0.5f
    protected val children = mutableListOf<Panel>()

    // Dragging state
    private var isDragging = false
    private var dragStartPos = Vector2f()
    private var dragOffset = Vector2f()

    // Resizing state
    private var isResizing = false
    private var resizeEdge: DockPosition? = null
    private var resizeStartSize = Vector2f()
    private var resizeStartPos = Vector2f()


    companion object {

        private var idCounter = 1
        private fun nextId() = idCounter++
        private const val HEADER_HEIGHT = 25f
        private const val RESIZE_AREA = 5f
        //Preview target
        private var activePreviewTarget: Panel? = null
        private var activePreviewPosition: DockPosition? = null
        private var currentDraggingPanel: Panel? = null
    }

    fun render(drawList: ImDrawList) {
        if (!isVisible) return
        val childrenToRender = ArrayList(children)
        if (isMoveable) {

            // Draw panel background
            drawList.addRectFilled(
                position.x, position.y,
                position.x + size.x, position.y + size.y,
                ImGui.colorConvertFloat4ToU32(0.2f, 0.2f, 0.2f, 1.0f)
            )

            // Draw header
            drawList.addRectFilled(
                position.x, position.y,
                position.x + size.x, position.y + HEADER_HEIGHT,
                ImGui.colorConvertFloat4ToU32(0.3f, 0.3f, 0.3f, 1.0f)
            )
            //draw border
            drawList.addRect(
                position.x, position.y,
                position.x + size.x, position.y + size.y,
                ImGui.colorConvertFloat4ToU32(0.1f, 0.1f, 0.1f, 1.0f)
            )
            // Draw title
            ImGui.setCursorPos(position.x + 5f, position.y + 5f)
            ImGui.text(title)

        }

        // Handle mouse interactions
        handleMouseInteraction()

        // Render panel content
        val contentY = position.y + HEADER_HEIGHT
        ImGui.setCursorPos(position.x, contentY)
        renderContent(drawList)

        // Render children
        childrenToRender.forEach { child ->
            child.render(drawList)
        }

        // Draw dock preview if this is the active preview target
        if (this == activePreviewTarget && activePreviewPosition != null) {
            drawDockPreview(drawList, activePreviewPosition!!)
        }
    }

    fun isHovered(): Boolean {
        val mousePos = Vector2f(ImGui.getMousePosX(), ImGui.getMousePosY())
        return isInPanel(mousePos)
    }

    private fun drawDockPreview(drawList: ImDrawList, dockPosition: DockPosition) {
        val previewColor = ImGui.colorConvertFloat4ToU32(0.2f, 0.4f, 0.8f, 0.3f)
        val previewBorderColor = ImGui.colorConvertFloat4ToU32(0.2f, 0.4f, 0.8f, 0.8f)

        val (previewX, previewY, previewWidth, previewHeight) = when (dockPosition) {
            DockPosition.LEFT -> Quad(
                position.x,
                position.y,
                size.x * 0.5f,
                size.y
            )

            DockPosition.RIGHT -> Quad(
                position.x + size.x * 0.5f,
                position.y,
                size.x * 0.5f,
                size.y
            )

            DockPosition.TOP -> Quad(
                position.x,
                position.y,
                size.x,
                size.y * 0.5f
            )

            DockPosition.BOTTOM -> Quad(
                position.x,
                position.y + size.y * 0.5f,
                size.x,
                size.y * 0.5f
            )

            DockPosition.CENTER -> Quad(
                position.x,
                position.y,
                size.x,
                size.y
            )

            DockPosition.FLOATING -> return
        }

        // Draw filled rectangle with semi-transparency
        drawList.addRectFilled(
            previewX, previewY,
            previewX + previewWidth, previewY + previewHeight,
            previewColor
        )

        // Draw border
        drawList.addRect(
            previewX, previewY,
            previewX + previewWidth, previewY + previewHeight,
            previewBorderColor
        )
    }

    private fun handleMouseInteraction() {
        if (!isMoveable) return  // Early return if panel isn't moveable

        val mousePos = Vector2f(ImGui.getMousePosX(), ImGui.getMousePosY())

        when {
            isDragging -> handleDragging(mousePos)
            isResizing -> handleResizing(mousePos)
            else -> {
                // Check for resize edges and update cursor
                when (getResizeEdge(mousePos)) {
                    DockPosition.RIGHT -> ImGui.setMouseCursor(ImGuiMouseCursor.ResizeEW)
                    DockPosition.BOTTOM -> ImGui.setMouseCursor(ImGuiMouseCursor.ResizeNS)
                    DockPosition.LEFT -> ImGui.setMouseCursor(ImGuiMouseCursor.ResizeEW)
                    DockPosition.TOP -> ImGui.setMouseCursor(ImGuiMouseCursor.ResizeNS)

                    null -> {
                        // Only reset cursor if we're hovering over this panel
                        if (isInPanel(mousePos)) {
                            ImGui.setMouseCursor(ImGuiMouseCursor.Arrow)
                        }
                    }

                    else -> {}
                }
                checkForNewInteraction(mousePos)
            }
        }

        // Keep resize cursor during resize operation
        if (isResizing) {
            when (resizeEdge) {
                DockPosition.RIGHT -> ImGui.setMouseCursor(ImGuiMouseCursor.ResizeEW)
                DockPosition.BOTTOM -> ImGui.setMouseCursor(ImGuiMouseCursor.ResizeNS)
                DockPosition.LEFT -> ImGui.setMouseCursor(ImGuiMouseCursor.ResizeEW)
                DockPosition.TOP -> ImGui.setMouseCursor(ImGuiMouseCursor.ResizeNS)
                else -> {}
            }
        }
    }

    private fun handleDragging(mousePos: Vector2f) {
        if (currentDraggingPanel != this) return
        if (ImGui.isMouseDragging(0)) {
            position.set(
                mousePos.x - dragOffset.x,
                mousePos.y - dragOffset.y
            )
            layoutChildren()

            // Update preview globally
            findRoot().findPanelAt(mousePos, this)?.let { targetPanel ->
                activePreviewTarget = targetPanel
                activePreviewPosition = calculateDockPosition(targetPanel, mousePos)
            } ?: run {
                activePreviewTarget = null
                activePreviewPosition = null
            }

        } else if (ImGui.isMouseReleased(0)) {
            isDragging = false
            currentDraggingPanel = null
            // Try to dock with another panel
            activePreviewTarget?.let { targetPanel ->
                activePreviewPosition?.let { dockPos ->
                    parent?.removeChild(this)
                    targetPanel.addChild(this, dockPos)
                }
            }
            // Clear preview
            activePreviewTarget = null
            activePreviewPosition = null
        }
    }

    private fun handleResizing(mousePos: Vector2f) {
        if (ImGui.isMouseDragging(0)) {
            val parentSize = parent?.size ?: Vector2f(ImGui.getIO().displaySize.x, ImGui.getIO().displaySize.y)

            when (resizeEdge) {
                DockPosition.RIGHT -> {
                    val newWidth = mousePos.x - position.x
                    relativeWidth = newWidth / parentSize.x
                    size.x = newWidth
                    ImGui.setMouseCursor(ImGuiMouseCursor.ResizeEW)
                }

                DockPosition.BOTTOM -> {
                    val newHeight = mousePos.y - position.y
                    relativeHeight = newHeight / parentSize.y
                    size.y = newHeight
                    ImGui.setMouseCursor(ImGuiMouseCursor.ResizeNS)
                }

                DockPosition.LEFT -> {
                    val deltaX = mousePos.x - resizeStartPos.x
                    val newWidth = resizeStartSize.x - deltaX
                    relativeWidth = newWidth / parentSize.x
                    position.x = resizeStartPos.x + deltaX
                    size.x = newWidth
                    ImGui.setMouseCursor(ImGuiMouseCursor.ResizeEW)
                }

                DockPosition.TOP -> {
                    val deltaY = mousePos.y - resizeStartPos.y
                    val newHeight = resizeStartSize.y - deltaY
                    relativeHeight = newHeight / parentSize.y
                    position.y = resizeStartPos.y + deltaY
                    size.y = newHeight
                    ImGui.setMouseCursor(ImGuiMouseCursor.ResizeNS)
                }

                else -> {}
            }
            layoutChildren()
        } else if (ImGui.isMouseReleased(0)) {
            isResizing = false
            resizeEdge = null
            ImGui.setMouseCursor(ImGuiMouseCursor.Arrow)
            layoutChildren()
        }
    }

    private fun checkForNewInteraction(mousePos: Vector2f) {
        if (ImGui.isMouseClicked(0) && isMoveable) {  // Check isMoveable before starting interaction
            when {
                isInHeaderArea(mousePos) && currentDraggingPanel == null -> startDragging(mousePos)
                getResizeEdge(mousePos) != null -> startResizing(mousePos)
            }
        }
    }

    private fun startDragging(mousePos: Vector2f) {
        currentDraggingPanel = this
        isDragging = true
        dragStartPos.set(mousePos)
        dragOffset.set(mousePos.x - position.x, mousePos.y - position.y)
    }

    private fun startResizing(mousePos: Vector2f) {
        isResizing = true
        resizeEdge = getResizeEdge(mousePos)
        resizeStartSize.set(size)
        resizeStartPos.set(position)
    }

    private fun isInHeaderArea(pos: Vector2f): Boolean {
        // If we're in the top resize area, don't consider it header area
        if (pos.y <= position.y + RESIZE_AREA) {
            return false
        }
        //If we have children that have an overlapping header return false
        if (children.any { it.isInHeaderArea(pos) }) return false

        return pos.y >= position.y &&
                pos.y <= position.y + HEADER_HEIGHT &&
                pos.x >= position.x &&
                pos.x <= position.x + size.x
    }

    private fun getResizeEdge(pos: Vector2f): DockPosition? {
        if (!isInPanel(pos)) return null

        val rightEdge = pos.x >= position.x + size.x - RESIZE_AREA
        val bottomEdge = pos.y >= position.y + size.y - RESIZE_AREA
        val leftEdge = pos.x <= position.x + RESIZE_AREA
        val topEdge = pos.y <= position.y + RESIZE_AREA

        return when {
            rightEdge -> DockPosition.RIGHT
            bottomEdge -> DockPosition.BOTTOM
            leftEdge -> DockPosition.LEFT
            topEdge -> DockPosition.TOP
            else -> null
        }
    }

    private fun calculateDockPosition(targetPanel: Panel, pos: Vector2f): DockPosition {
        val relX = (pos.x - targetPanel.position.x) / targetPanel.size.x
        val relY = (pos.y - targetPanel.position.y) / targetPanel.size.y

        return when {
            relX < 0.25f -> DockPosition.LEFT
            relX > 0.75f -> DockPosition.RIGHT
            relY < 0.25f -> DockPosition.TOP
            relY > 0.75f -> DockPosition.BOTTOM
            else -> DockPosition.CENTER
        }
    }

    fun addChild(panel: Panel, position: DockPosition): Panel {
        children.add(panel)
        panel.parent = this
        panel.layout = position

        // Set initial relative sizes based on position
        when (position) {
            DockPosition.LEFT, DockPosition.RIGHT -> {
                panel.relativeWidth = 0.5f
                panel.relativeHeight = 1.0f
            }

            DockPosition.TOP, DockPosition.BOTTOM -> {
                panel.relativeWidth = 1.0f
                panel.relativeHeight = 0.5f
            }

            DockPosition.CENTER -> {
                panel.relativeWidth = 1.0f
                panel.relativeHeight = 1.0f
            }

            DockPosition.FLOATING -> {
                // Keep default relative sizes
            }
        }

        layoutChildren()
        return panel
    }


    fun removeChild(panel: Panel) {
        children.remove(panel)
        panel.parent = null
        panel.layout = DockPosition.FLOATING
        layoutChildren()
    }

    private fun layoutChildren() {
        val childrenToLayout = ArrayList(children)

        childrenToLayout.forEach { child ->
            when (child.layout) {
                DockPosition.LEFT -> {
                    child.position.set(position)
                    child.size.set(size.x * child.relativeWidth, size.y)
                }

                DockPosition.RIGHT -> {
                    val leftWidth = size.x * (1 - child.relativeWidth)
                    child.position.set(position.x + leftWidth, position.y)
                    child.size.set(size.x * child.relativeWidth, size.y)
                }

                DockPosition.TOP -> {
                    child.position.set(position)
                    child.size.set(size.x, size.y * child.relativeHeight)
                }

                DockPosition.BOTTOM -> {
                    val topHeight = size.y * (1 - child.relativeHeight)
                    child.position.set(position.x, position.y + topHeight)
                    child.size.set(size.x, size.y * child.relativeHeight)
                }

                DockPosition.CENTER -> {
                    child.position.set(position)
                    child.size.set(size)
                }

                DockPosition.FLOATING -> {} // Keep current position and size
            }
            child.layoutChildren()
        }
    }


    fun findPanelAt(pos: Vector2f, exclude: Panel? = null, checkingHeader: Boolean = false): Panel? {
        // When checking header area, if this panel's header contains the point, return immediately
        if (checkingHeader && isInHeaderArea(pos) && this != exclude) {
            return this
        }

        // First check children in reverse order (top-most first)
        for (i in children.size - 1 downTo 0) {
            val child = children[i]
            val found = child.findPanelAt(pos, exclude, checkingHeader)
            if (found != null) {
                return found
            }
        }

        // If not checking header area, then check if point is within this panel
        if (!checkingHeader && isInPanel(pos) && this != exclude) {
            return this
        }

        return null
    }

    private fun isInPanel(pos: Vector2f): Boolean {
        return pos.x >= position.x && pos.x <= position.x + size.x &&
                pos.y >= position.y && pos.y <= position.y + size.y
    }

    private fun findRoot(): Panel {
        var current = this
        while (current.parent != null) {
            current = current.parent!!
        }
        return current
    }

    abstract fun renderContent(drawList: ImDrawList)
    fun onResize(width: Float, height: Float) {
        size.set(width, height)
        layoutChildren()
    }
}


private data class Quad(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)


class RootPanel : Panel(0, "Root", DockPosition.CENTER, false) {
    init {
        position = Vector2f(0f, 0f)
        size = Vector2f(ImGui.getIO().displaySize.x, ImGui.getIO().displaySize.y)
    }

    fun childrenHovered(): Boolean {
        return children.any { it.isHovered() }
    }

    override fun renderContent(drawList: ImDrawList) {
        // Root panel doesn't need to render any content
    }
}