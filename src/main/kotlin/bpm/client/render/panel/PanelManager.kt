package bpm.client.render.panel

import bpm.client.runtime.windows.CanvasGraphics
import bpm.client.utils.use
import imgui.ImColor
import imgui.ImColor.*
import imgui.ImDrawList
import imgui.ImGui
import imgui.flag.ImGuiMouseCursor
import org.joml.Vector2f
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

class PanelManager {

    private val panels = ConcurrentHashMap<String, Panel>()
    private val minimizedPanels = ConcurrentHashMap.newKeySet<Panel>()
    private val animations = ConcurrentHashMap<String, AnimationState>()
    private var lastFrameTime = System.nanoTime()
    private var draggedPanel: Panel? = null
    private var dragOffset = Vector2f()
    private val cornerStates = mapOf(
        "Top Left" to CornerState(),
        "Top Right" to CornerState(),
        "Bottom Left" to CornerState(),
        "Bottom Right" to CornerState()
    )

    private data class AnimationState(
        var progress: Float = 0f,
        var startPosition: Vector2f = Vector2f(),
        var endPosition: Vector2f = Vector2f()
    )

    private data class CornerState(
        val panels: MutableList<Panel> = mutableListOf()
    )

    fun addPanel(panel: Panel) {
        panels[panel.title] = panel
        val initialPosition = calculateInitialPosition(panel)
        animations[panel.title] = AnimationState(progress = 1f, endPosition = initialPosition)
        panel.position = initialPosition
        addPanelToCorner(panel, getCornerName(initialPosition))
    }

    fun isAnyHovered(): Boolean {
        return panels.values.any { panel ->
            val position = lerp(
                animations[panel.title]?.startPosition ?: panel.position,
                animations[panel.title]?.endPosition ?: panel.position,
                animations[panel.title]?.progress ?: 1f
            )
            ImGui.isMouseHoveringRect(
                position.x, position.y, position.x + panel.panelWidth, position.y + 30f
            )
        }
    }

    operator fun get(title: String): Panel? = panels[title]
    private val lastSize = Vector2f()
    fun renderPanels(graphics: CanvasGraphics, drawList: ImDrawList) {
        updateAnimations()
        //Poor man's resolution change detection
        if (lastSize != Vector2f(ImGui.getIO().displaySize.x, ImGui.getIO().displaySize.y)) {
            panels.values.forEach { panel ->
                val corner = getCornerName(panel.position)
                val newPosition = calculatePositionInCorner(panel, corner)
                startSnapAnimation(panel, newPosition)
            }
            for ((corner, state) in cornerStates) {
                rearrangePanelsInCorner(corner)
            }
        }
        panels.values.forEach { panel ->
            val animState = animations[panel.title] ?: return@forEach
            val position = lerp(animState.startPosition, animState.endPosition, animState.progress)

            if (!minimizedPanels.contains(panel)) {
                drawList.pushClipRect(
                    position.x, position.y, position.x + panel.panelWidth, position.y + panel.panelHeight, true
                )
                panel.render(graphics, drawList, position, 1f)
                drawList.popClipRect()

                handlePanelDragging(panel, position)
            }
        }

        renderMinimizedTabs(drawList)
        lastSize.set(ImGui.getIO().displaySize.x, ImGui.getIO().displaySize.y)
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
                removePanelFromCorner(panel)
            }
        }

        if (draggedPanel == panel && ImGui.isMouseDragging(0)) {
            ImGui.setMouseCursor(ImGuiMouseCursor.None)
            val mousePos = ImGui.getMousePos()
            val newPosition = Vector2f(mousePos.x - dragOffset.x, mousePos.y - dragOffset.y)
            panel.updatePosition(newPosition)
            animations[panel.title]?.endPosition = newPosition
        }

        if (ImGui.isMouseReleased(0) && draggedPanel == panel) {
            snapToNearestCorner(panel)
            draggedPanel = null
        }
    }

    private fun calculateInitialPosition(panel: Panel): Vector2f {
        val displaySize = ImGui.getIO().displaySize
        val corner = if (panels.size % 2 == 0) "Top Left" else "Top Right"
        return calculatePositionInCorner(panel, corner)
    }

    private fun calculatePositionInCorner(panel: Panel, corner: String): Vector2f {
        val displaySize = ImGui.getIO().displaySize
        val cornerPanels = cornerStates[corner]!!.panels

        return when (corner) {
            "Top Left" -> Vector2f(cornerPanels.filter { it != panel }.sumOf { it.panelWidth.toDouble() }.toFloat(), 0f)
            "Top Right" -> Vector2f(displaySize.x - panel.panelWidth - cornerPanels.filter { it != panel }
                .sumOf { it.panelWidth.toDouble() }.toFloat(), 0f)

            "Bottom Left" -> Vector2f(
                cornerPanels
                    .filter { it != panel }
                    .sumOf { it.panelWidth.toDouble() }.toFloat(),
                displaySize.y - panel.panelHeight
            )

            "Bottom Right" -> Vector2f(displaySize.x - panel.panelWidth - cornerPanels.filter {
                it != panel
            }.sumOf { it.panelWidth.toDouble() }
                .toFloat(), displaySize.y - panel.panelHeight)

            else -> Vector2f(0f, 0f)
        }
    }

    fun snapToNearestCorner(panel: Panel) {
        val nearestCorner = findNearestCorner(panel)
        addPanelToCorner(panel, nearestCorner)

        val snappedPosition = calculatePositionInCorner(panel, nearestCorner)
        startSnapAnimation(panel, snappedPosition)
    }

    private fun addPanelToCorner(panel: Panel, corner: String) {
        cornerStates[corner]?.panels?.add(panel)
        rearrangePanelsInCorner(corner)
    }

    private fun removePanelFromCorner(panel: Panel) {
        cornerStates.forEach { (corner, state) ->
            if (state.panels.remove(panel)) {
                rearrangePanelsInCorner(corner)
            }
        }
    }

    private fun rearrangePanelsInCorner(corner: String) {
        val panels = cornerStates[corner]?.panels ?: return
        var xOffset = 0f
        panels.forEach { panel ->
            val newPosition = when (corner) {
                "Top Left" -> Vector2f(xOffset, 0f)
                "Top Right" -> Vector2f(ImGui.getIO().displaySize.x - xOffset - panel.panelWidth, 0f)
                "Bottom Left" -> Vector2f(xOffset, ImGui.getIO().displaySize.y - panel.panelHeight)
                "Bottom Right" -> Vector2f(
                    ImGui.getIO().displaySize.x - xOffset - panel.panelWidth,
                    ImGui.getIO().displaySize.y - panel.panelHeight
                )

                else -> Vector2f(0f, 0f)
            }
            startSnapAnimation(panel, newPosition)
            xOffset += panel.panelWidth
        }
    }

    private fun getCornerName(position: Vector2f): String {
        val displaySize = ImGui.getIO().displaySize
        return when {
            position.x <= displaySize.x / 2 -> if (position.y <= displaySize.y / 2) "Top Left" else "Bottom Left"
            else -> if (position.y <= displaySize.y / 2) "Top Right" else "Bottom Right"
        }
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

    private fun renderTab(drawList: ImDrawList, panel: Panel, position: Vector2f) {
        drawList.addRectFilled(
            position.x, position.y, position.x + TAB_WIDTH, position.y + TAB_HEIGHT, rgba(0.3f, 0.3f, 0.3f, 1f), 5f
        )

        panel.iconFont.use {
            drawList.addText(
                panel.iconFont, 20f, position.x + 10f, position.y + 5f, rgba(1f, 1f, 1f, 1f), panel.icon
            )
        }

        panel.textFont.use {
            drawList.addText(
                panel.textFont, 16f, position.x + 40f, position.y + 7f, rgba(1f, 1f, 1f, 1f), panel.title
            )
        }

        if (isMouseOver(position, TAB_WIDTH, TAB_HEIGHT) && ImGui.isMouseClicked(0)) {
            maximizePanel(panel)
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

    fun minimizePanel(panel: Panel) {
        if (!minimizedPanels.contains(panel)) {
            minimizedPanels.add(panel)
            removePanelFromCorner(panel)
            startMinimizeAnimation(panel)
        }
    }

    private fun maximizePanel(panel: Panel) {
        if (minimizedPanels.contains(panel)) {
            minimizedPanels.remove(panel)
            snapToNearestCorner(panel)
            startMaximizeAnimation(panel)
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

    private fun lerp(start: Vector2f, end: Vector2f, t: Float): Vector2f {
        return Vector2f(
            start.x + (end.x - start.x) * t, start.y + (end.y - start.y) * t
        )
    }

    companion object {

        private const val ANIMATION_DURATION = 0.2f
        private const val TAB_HEIGHT = 30f
        private const val TAB_WIDTH = 200f
        private const val TAB_SPACING = 10f
    }
}