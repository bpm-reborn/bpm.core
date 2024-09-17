package bpm.client.render.panel

import bpm.client.runtime.windows.CanvasGraphics
import bpm.client.utils.use
import imgui.ImColor
import imgui.ImColor.rgba
import imgui.ImDrawList
import imgui.ImGui
import imgui.flag.ImGuiMouseCursor
import net.minecraft.client.gui.GuiGraphics
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

class PanelManager(private val graphics: CanvasGraphics) {

    private val panels = ConcurrentHashMap<String, Panel>()
    private val minimizedPanels = ConcurrentHashMap.newKeySet<Panel>()
    private val animations = ConcurrentHashMap<String, AnimationState>()
    private var lastFrameTime = System.nanoTime()
    private var draggedPanel: Panel? = null
    private var resizingPanel: Panel? = null
    private var resizeEdge: ResizeEdge? = null
    private var dragOffset = Vector2f()
    private var previewRect: Vector4f? = null
    private var lastScreenSize = Vector2f()

    private enum class DockPosition { LEFT, RIGHT, TOP, BOTTOM, CENTER }

    private enum class ResizeEdge { LEFT, RIGHT, TOP, BOTTOM }


    private data class DockSpace(
        var position: Vector2f, var size: Vector2f, val panels: MutableList<Panel> = mutableListOf()
    )

    private data class AnimationState(
        var progress: Float = 0f,
        var startPosition: Vector2f = Vector2f(),
        var endPosition: Vector2f = Vector2f()
    )

    private val dockSpaces = mutableMapOf<DockPosition, DockSpace>()

    init {
        resetDockSpaces()
    }

    private fun resetDockSpaces() {
        val displaySize = ImGui.getIO().displaySize
        if (displaySize.x.toInt() == lastScreenSize.x.toInt() && displaySize.y.toInt() == lastScreenSize.y.toInt()) return
        lastScreenSize.set(displaySize.x, displaySize.y)
        val thirdWidth = displaySize.x / 3
        val thirdHeight = displaySize.y / 3

        dockSpaces.clear()
        dockSpaces[DockPosition.LEFT] = DockSpace(Vector2f(0f, 0f), Vector2f(thirdWidth, displaySize.y))
        dockSpaces[DockPosition.RIGHT] = DockSpace(Vector2f(thirdWidth * 2, 0f), Vector2f(thirdWidth, displaySize.y))
        dockSpaces[DockPosition.TOP] = DockSpace(Vector2f(0f, 0f), Vector2f(displaySize.x, thirdHeight))
        dockSpaces[DockPosition.BOTTOM] = DockSpace(Vector2f(0f, thirdHeight * 2), Vector2f(displaySize.x, thirdHeight))
        dockSpaces[DockPosition.CENTER] = DockSpace(Vector2f(thirdWidth, thirdHeight), Vector2f(thirdWidth, thirdHeight))
    }


    fun addPanel(panel: Panel) {
        panels[panel.title] = panel
        val initialPosition = calculateInitialPosition(panel)
        panel.position = initialPosition
        panel.panelWidth = panel.panelWidth.takeIf { it > 0f } ?: 300f // Default width if not set
        panel.panelHeight = panel.panelHeight.takeIf { it > 0f } ?: 200f // Default height if not set
        animations[panel.title] = AnimationState(progress = 1f, startPosition = initialPosition, endPosition = initialPosition)
        panel.setupPanel(graphics, this)
        dockPanel(panel, findDockPosition(initialPosition))
    }


    private fun findDockPosition(position: Vector2f): DockPosition {
        val displaySize = ImGui.getIO().displaySize
        val centerThreshold = 100f

        return when {
            position.x < centerThreshold -> DockPosition.LEFT
            position.x > displaySize.x - centerThreshold -> DockPosition.RIGHT
            position.y < centerThreshold -> DockPosition.TOP
            position.y > displaySize.y - centerThreshold -> DockPosition.BOTTOM
            else -> DockPosition.CENTER
        }
    }



    private fun dockPanel(panel: Panel, dockPosition: DockPosition) {
        val dockSpace = dockSpaces[dockPosition] ?: return
        dockSpace.panels.add(panel)
        updateDockSpaces()
    }

    private fun updateDockSpaces() {
        resetDockSpaces() // Reset dock spaces before updating

        dockSpaces.forEach { (position, dockSpace) ->
            if (dockSpace.panels.isNotEmpty()) {
                val panelCount = dockSpace.panels.size
                val heightPerPanel = dockSpace.size.y / panelCount

                dockSpace.panels.forEachIndexed { index, panel ->
                    panel.position.x = dockSpace.position.x
                    panel.position.y = dockSpace.position.y + index * heightPerPanel
                    panel.panelWidth = dockSpace.size.x
                    panel.panelHeight = heightPerPanel
                    animations[panel.title]?.endPosition?.set(panel.position)
                    panel.onResize()
                }
            }
        }
    }


    private fun updatePanelsInDockSpace(dockSpace: DockSpace, x: Float, y: Float, width: Float, height: Float) {
        dockSpace.position.set(x, y)
        dockSpace.size.set(width, height)
        val panelCount = dockSpace.panels.size
        if (panelCount > 0) {
            val heightPerPanel = height / panelCount
            dockSpace.panels.forEachIndexed { index, panel ->
                panel.position.x = x
                panel.position.y = y + index * heightPerPanel
                panel.panelWidth = width
                panel.panelHeight = heightPerPanel
                animations[panel.title]?.endPosition?.set(panel.position)
                panel.onResize()
            }
        }
    }


    fun renderPanels(drawList: ImDrawList) {
        checkScreenResize()
        updateAnimations()
        panels.values.forEach { panel ->
            val animState = animations[panel.title] ?: return@forEach
            val position = lerp(animState.startPosition, animState.endPosition, animState.progress)

            if (!minimizedPanels.contains(panel)) {
                drawList.pushClipRect(
                    position.x, position.y, position.x + panel.panelWidth, position.y + panel.panelHeight, true
                )
                panel.render(drawList, position, 1f)
                drawList.popClipRect()
                handlePanelDragging(panel, position)
                handlePanelResizing(panel, position)
            }
        }

        renderPreview(drawList)
        renderMinimizedTabs(drawList)
    }


    private fun handlePanelResizing(panel: Panel, position: Vector2f) {
        val resizeHandleSize = 5f
        val mousePos = ImGui.getMousePos()

        fun isOverResizeHandle(edge: ResizeEdge): Boolean {
            return when (edge) {
                ResizeEdge.LEFT -> mousePos.x in (position.x - resizeHandleSize)..(position.x + resizeHandleSize)
                ResizeEdge.RIGHT -> mousePos.x in (position.x + panel.panelWidth - resizeHandleSize)..(position.x + panel.panelWidth + resizeHandleSize)
                ResizeEdge.TOP -> mousePos.y in (position.y - resizeHandleSize)..(position.y + resizeHandleSize)
                ResizeEdge.BOTTOM -> mousePos.y in (position.y + panel.panelHeight - resizeHandleSize)..(position.y + panel.panelHeight + resizeHandleSize)
            } && mousePos.y in position.y..(position.y + panel.panelHeight) && mousePos.x in position.x..(position.x + panel.panelWidth)
        }

        if (resizingPanel == null) {
            val edge = ResizeEdge.values().find { isOverResizeHandle(it) }
            if (edge != null) {
                when (edge) {
                    ResizeEdge.LEFT, ResizeEdge.RIGHT -> ImGui.setMouseCursor(ImGuiMouseCursor.ResizeEW)
                    ResizeEdge.TOP, ResizeEdge.BOTTOM -> ImGui.setMouseCursor(ImGuiMouseCursor.ResizeNS)
                }
                if (ImGui.isMouseClicked(0)) {
                    resizingPanel = panel
                    resizeEdge = edge
                }
            }
        } else if (resizingPanel == panel) {
            if (ImGui.isMouseDown(0)) {
                val delta = when (resizeEdge) {
                    ResizeEdge.LEFT -> mousePos.x - position.x
                    ResizeEdge.RIGHT -> mousePos.x - (position.x + panel.panelWidth)
                    ResizeEdge.TOP -> mousePos.y - position.y
                    ResizeEdge.BOTTOM -> mousePos.y - (position.y + panel.panelHeight)
                    null -> 0f
                }
                resizePanel(panel, resizeEdge!!, delta)
            } else {
                resizingPanel = null
                resizeEdge = null
                updateDockSpaces()
            }
        }
    }

    private fun resizePanel(panel: Panel, edge: ResizeEdge, delta: Float) {
        val dockPosition = findDockPosition(panel.position)
        val dockSpace = dockSpaces[dockPosition]!!

        when (edge) {
            ResizeEdge.LEFT -> {
                val newWidth = panel.panelWidth - delta
                if (newWidth > 100) {
                    panel.panelWidth = newWidth
                    panel.position.x += delta
                    updatePanelsInDockSpace(dockSpace, dockSpace.position.x + delta, dockSpace.position.y, dockSpace.size.x - delta, dockSpace.size.y)
                }
            }
            ResizeEdge.RIGHT -> {
                val newWidth = panel.panelWidth + delta
                if (newWidth > 100) {
                    panel.panelWidth = newWidth
                    updatePanelsInDockSpace(dockSpace, dockSpace.position.x, dockSpace.position.y, dockSpace.size.x + delta, dockSpace.size.y)
                }
            }
            ResizeEdge.TOP -> {
                val newHeight = panel.panelHeight - delta
                if (newHeight > 100) {
                    panel.panelHeight = newHeight
                    panel.position.y += delta
                    updatePanelsInDockSpace(dockSpace, dockSpace.position.x, dockSpace.position.y + delta, dockSpace.size.x, dockSpace.size.y - delta)
                }
            }
            ResizeEdge.BOTTOM -> {
                val newHeight = panel.panelHeight + delta
                if (newHeight > 100) {
                    panel.panelHeight = newHeight
                    updatePanelsInDockSpace(dockSpace, dockSpace.position.x, dockSpace.position.y, dockSpace.size.x, dockSpace.size.y + delta)
                }
            }
        }

        animations[panel.title]?.endPosition?.set(panel.position)
        panel.onResize()
    }

    private fun handlePanelDragging(panel: Panel, position: Vector2f) {
        val headerHeight = 30f

        if (ImGui.isMouseHoveringRect(
                position.x, position.y, position.x + panel.panelWidth, position.y + headerHeight
            )
        ) {
            if (ImGui.isMouseClicked(0)) {
                draggedPanel = panel
                dragOffset.x = ImGui.getMousePos().x - position.x
                dragOffset.y = ImGui.getMousePos().y - position.y
            }
        }

        if (draggedPanel == panel && ImGui.isMouseDragging(0)) {
            ImGui.setMouseCursor(ImGuiMouseCursor.ResizeAll)
            val mousePos = ImGui.getMousePos()
            val newPosition = Vector2f(mousePos.x - dragOffset.x, mousePos.y - dragOffset.y)
            panel.position.set(newPosition)
            updatePreview(panel, newPosition)
        }

        if (ImGui.isMouseReleased(0) && draggedPanel == panel) {
            snapPanel(panel)
            draggedPanel = null
            previewRect = null
        }
    }



    private fun updatePreview(panel: Panel, position: Vector2f) {
        val dockPosition = findDockPosition(position)
        val previewRect = when (dockPosition) {
            DockPosition.LEFT -> Vector4f(0f, 0f, ImGui.getIO().displaySize.x / 2, ImGui.getIO().displaySize.y)
            DockPosition.RIGHT -> Vector4f(ImGui.getIO().displaySize.x / 2, 0f, ImGui.getIO().displaySize.x, ImGui.getIO().displaySize.y)
            DockPosition.TOP -> Vector4f(0f, 0f, ImGui.getIO().displaySize.x, ImGui.getIO().displaySize.y / 2)
            DockPosition.BOTTOM -> Vector4f(0f, ImGui.getIO().displaySize.y / 2, ImGui.getIO().displaySize.x, ImGui.getIO().displaySize.y)
            DockPosition.CENTER -> Vector4f(
                ImGui.getIO().displaySize.x / 4,
                ImGui.getIO().displaySize.y / 4,
                ImGui.getIO().displaySize.x * 3 / 4,
                ImGui.getIO().displaySize.y * 3 / 4
            )
        }

        this.previewRect = previewRect
    }



    private fun renderPreview(drawList: ImDrawList) {
        val preview = previewRect ?: return
        drawList.addRect(
            preview.x, preview.y, preview.z, preview.w, ImColor.rgba(0, 122, 255, 255), 5f, 0, 3f
        )
    }

    private fun snapPanel(panel: Panel) {
        val preview = previewRect ?: return
        val newPosition = Vector2f(preview.x, preview.y)
        panel.position.set(newPosition)
        panel.panelWidth = preview.z - preview.x
        panel.panelHeight = preview.w - preview.y

        animations[panel.title]?.apply {
            startPosition.set(panel.position)
            endPosition.set(panel.position)
            progress = 1f
        }

        panel.onResize()
    }




    private fun findBestSnapPosition(panel: Panel): Vector2f {
        val displaySize = ImGui.getIO().displaySize
        var bestPosition = panel.position
        var bestOverlap = 0f

        // Check panel-to-panel snapping
        panels.values.filter { it != panel && !minimizedPanels.contains(it) }.forEach { otherPanel ->
            val overlap = calculateOverlap(panel, otherPanel)
            if (overlap > 0 && overlap > bestOverlap) {
                val alignedPosition = alignPanels(panel, otherPanel)
                bestPosition = alignedPosition
                bestOverlap = overlap
            }
        }

        // If no overlap with other panels, check screen edges
        if (bestOverlap == 0f) {
            val edgeSnapThreshold = 20f
            val edgeSnapPositions = listOf(
                Vector2f(0f, panel.position.y),  // Left edge
                Vector2f(displaySize.x - panel.panelWidth, panel.position.y),  // Right edge
                Vector2f(panel.position.x, 0f),  // Top edge
                Vector2f(panel.position.x, displaySize.y - panel.panelHeight)  // Bottom edge
            )

            for (edgePosition in edgeSnapPositions) {
                val distance = panel.position.distance(edgePosition)
                if (distance < edgeSnapThreshold && distance < bestOverlap) {
                    bestPosition = edgePosition
                    bestOverlap = distance
                }
            }
        }

        return bestPosition
    }


    private fun calculateOverlap(panel1: Panel, panel2: Panel): Float {
        val left = max(panel1.position.x, panel2.position.x)
        val right = min(panel1.position.x + panel1.panelWidth, panel2.position.x + panel2.panelWidth)
        val top = max(panel1.position.y, panel2.position.y)
        val bottom = min(panel1.position.y + panel1.panelHeight, panel2.position.y + panel2.panelHeight)

        val overlapWidth = max(0f, right - left)
        val overlapHeight = max(0f, bottom - top)

        return overlapWidth * overlapHeight
    }


    private fun alignPanels(panel: Panel, otherPanel: Panel): Vector2f {
        val panelCorners = listOf(
            panel.position,
            Vector2f(panel.position.x + panel.panelWidth, panel.position.y),
            Vector2f(panel.position.x, panel.position.y + panel.panelHeight),
            Vector2f(panel.position.x + panel.panelWidth, panel.position.y + panel.panelHeight)
        )

        val otherPanelCorners = listOf(
            otherPanel.position,
            Vector2f(otherPanel.position.x + otherPanel.panelWidth, otherPanel.position.y),
            Vector2f(otherPanel.position.x, otherPanel.position.y + otherPanel.panelHeight),
            Vector2f(otherPanel.position.x + otherPanel.panelWidth, otherPanel.position.y + otherPanel.panelHeight)
        )

        var closestDistance = Float.MAX_VALUE
        var bestAlignment = panel.position

        for ((i, panelCorner) in panelCorners.withIndex()) {
            for ((j, otherCorner) in otherPanelCorners.withIndex()) {
                val distance = panelCorner.distance(otherCorner)
                if (distance < closestDistance) {
                    closestDistance = distance
                    bestAlignment = if (i == j) {
                        // If corners are the same, align to the opposite side
                        when (i) {
                            0 -> Vector2f(
                                otherPanel.position.x + otherPanel.panelWidth,
                                otherPanel.position.y + otherPanel.panelHeight
                            ) // Top-left to bottom-right
                            1 -> Vector2f(
                                otherPanel.position.x - panel.panelWidth, otherPanel.position.y + otherPanel.panelHeight
                            ) // Top-right to bottom-left
                            2 -> Vector2f(
                                otherPanel.position.x + otherPanel.panelWidth, otherPanel.position.y - panel.panelHeight
                            ) // Bottom-left to top-right
                            3 -> Vector2f(
                                otherPanel.position.x - panel.panelWidth, otherPanel.position.y - panel.panelHeight
                            ) // Bottom-right to top-left
                            else -> Vector2f(
                                otherCorner.x - (panelCorner.x - panel.position.x),
                                otherCorner.y - (panelCorner.y - panel.position.y)
                            )
                        }
                    } else {
                        Vector2f(
                            otherCorner.x - (panelCorner.x - panel.position.x),
                            otherCorner.y - (panelCorner.y - panel.position.y)
                        )
                    }
                }
            }
        }

        return bestAlignment
    }

    private fun keepPanelWithinScreen(panel: Panel, position: Vector2f): Vector2f {
        val displaySize = ImGui.getIO().displaySize
        return Vector2f(
            position.x.coerceIn(10f, (displaySize.x - 10) - panel.panelWidth),
            position.y.coerceIn(10f, (displaySize.y - 10) - panel.panelHeight)
        )
    }

    private fun startSnapAnimation(panel: Panel, endPosition: Vector2f) {
        val animState = animations[panel.title] ?: return
        animState.progress = 0f
        animState.startPosition = Vector2f(panel.position)
        animState.endPosition = endPosition
        panel.lastMaximizedPosition = endPosition
    }

    private fun updateAnimations() {
        val currentTime = System.nanoTime()
        val deltaTime = (currentTime - lastFrameTime) / 1_000_000_000f
        lastFrameTime = currentTime

        animations.forEach { (_, animState) ->
            animState.progress = min(1f, animState.progress + deltaTime / ANIMATION_DURATION)
        }
    }

    private fun renderMinimizedTabs(drawList: ImDrawList) {
        val displaySize = ImGui.getIO().displaySize
        minimizedPanels.forEachIndexed { index, panel ->
            val tabPosition = Vector2f(
                displaySize.x - (TAB_WIDTH + TAB_SPACING) * (index + 1), displaySize.y - TAB_HEIGHT
            )
            renderTab(drawList, panel, tabPosition)
        }
    }

    private fun checkScreenResize() {
        val currentSize = ImGui.getIO().displaySize
        if (currentSize.x != lastScreenSize.x || currentSize.y != lastScreenSize.y) {
            updateDockSpaces()
            resetDockSpaces()
            lastScreenSize.set(currentSize.x, currentSize.y)
        }
    }

    private fun renderTab(drawList: ImDrawList, panel: Panel, position: Vector2f) {
        drawList.addRectFilled(
            position.x, position.y, position.x + TAB_WIDTH, position.y + TAB_HEIGHT, rgba(0.3f, 0.3f, 0.3f, 1f), 5f
        )

        panel.iconFam[20].use {
            drawList.addText(
                it, 20f, position.x + 10f, position.y + 5f, rgba(1f, 1f, 1f, 1f), panel.icon
            )
        }

        panel.boldFam[16].use {
            drawList.addText(
                it, 16f, position.x + 40f, position.y + 7f, rgba(1f, 1f, 1f, 1f), panel.title
            )
        }

        if (isMouseOver(position, TAB_WIDTH, TAB_HEIGHT) && ImGui.isMouseClicked(0)) {
            maximizePanel(panel)
        }
    }

    private fun maximizePanel(panel: Panel) {
        if (minimizedPanels.contains(panel)) {
            minimizedPanels.remove(panel)
            startMaximizeAnimation(panel)
        }
    }


    private fun findNearestCorner(panel: Panel): String {
        val displaySize = ImGui.getIO().displaySize
        val panelCenter = Vector2f(
            panel.position.x + panel.panelWidth / 2, panel.position.y + panel.panelHeight / 2
        )

        val corners = listOf(
            "Top Left" to Vector2f(0f, 0f),
            "Top Right" to Vector2f(displaySize.x, 0f),
            "Bottom Left" to Vector2f(0f, displaySize.y),
            "Bottom Right" to Vector2f(displaySize.x, displaySize.y)
        )

        return corners.minByOrNull { (_, position) -> position.distance(panelCenter) }?.first ?: "Top Left"
    }

    private fun isMouseOver(pos: Vector2f, width: Float, height: Float): Boolean {
        val mousePos = ImGui.getMousePos()
        return mousePos.x >= pos.x && mousePos.x <= pos.x + width && mousePos.y >= pos.y && mousePos.y <= pos.y + height
    }

    private fun calculateInitialPosition(panel: Panel): Vector2f {
        val displaySize = ImGui.getIO().displaySize
        return Vector2f(displaySize.x / 2 - panel.panelWidth / 2, displaySize.y / 2 - panel.panelHeight / 2)
    }

    private fun lerp(start: Vector2f, end: Vector2f, t: Float): Vector2f {
        return Vector2f(
            start.x + (end.x - start.x) * t, start.y + (end.y - start.y) * t
        )
    }

    fun minimizePanel(panel: Panel) {
        if (!minimizedPanels.contains(panel)) {
            minimizedPanels.add(panel)
            startMinimizeAnimation(panel)
        }
    }

    private fun startMinimizeAnimation(panel: Panel) {
        val animState = animations[panel.title] ?: return
        animState.progress = 0f
        animState.startPosition = Vector2f(panel.position)
        animState.endPosition = calculateTabPosition(panel)
    }

    private fun startMaximizeAnimation(panel: Panel) {
        val animState = animations[panel.title] ?: return
        animState.progress = 0f
        animState.startPosition = calculateTabPosition(panel)
        animState.endPosition = panel.lastMaximizedPosition
    }

    private fun calculateTabPosition(panel: Panel): Vector2f {
        val displaySize = ImGui.getIO().displaySize
        val index = minimizedPanels.indexOf(panel)
        return Vector2f(
            displaySize.x - (TAB_WIDTH + TAB_SPACING) * (index + 1), displaySize.y - TAB_HEIGHT
        )
    }

    fun renderPanelsPost(gfx: GuiGraphics, bounds: Vector4f) {
        panels.values.forEach { panel ->
            val animState = animations[panel.title] ?: return@forEach
            val position = lerp(animState.startPosition, animState.endPosition, animState.progress)

            if (!minimizedPanels.contains(panel)) {
                panel.renderPost(
                    gfx, Vector3f(position.x, position.y, 100f), Vector3f(panel.panelWidth, panel.panelHeight, 100f)
                )
            }
        }

    }

    fun isAnyHovered(): Boolean {
        return panels.values.any { panel ->
            val animState = animations[panel.title] ?: return@any false
            val position = lerp(animState.startPosition, animState.endPosition, animState.progress)
            ImGui.isMouseHoveringRect(
                position.x, position.y, position.x + panel.panelWidth, position.y + panel.panelHeight
            )
        }
    }

    fun isAnyDraggedOrResized(): Boolean {
        return draggedPanel != null || panels.values.any { panel -> panel.isResizing } || panels.values.any { panel -> panel.isDragging }
    }


    companion object {

        private const val ANIMATION_DURATION = 0.2f
        private const val TAB_HEIGHT = 30f
        private const val TAB_WIDTH = 200f
        private const val TAB_SPACING = 10f
    }
}