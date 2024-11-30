package bpm.client.render.panels

import bpm.client.runtime.windows.CanvasGraphics
import imgui.ImColor
import imgui.ImDrawList
import org.joml.Vector2f

// Enhanced DockSpace class with layout calculations
class DockSpace(
    val id: Int,
    private val gfx: CanvasGraphics,
    var position: Vector2f,
    var size: Vector2f
) {

    private val dockedPanels = mutableMapOf<DockPosition, MutableList<DockablePanel>>()
    private val layoutCache = mutableMapOf<DockPosition, LayoutRegion>()

    init {
        DockPosition.entries.forEach {
            dockedPanels[it] = mutableListOf()
        }
    }

    // Represents a region in the dock space
    data class LayoutRegion(
        var position: Vector2f,
        var size: Vector2f
    )

    // Configuration for dock space
    data class DockSpaceConfig(
        val sidePanelDefaultWidth: Float = 250f,
        val sidePanelMinWidth: Float = 150f,
        val sidePanelMaxWidth: Float = 400f,

        val topBottomPanelDefaultHeight: Float = 200f,
        val topBottomPanelMinHeight: Float = 100f,
        val topBottomPanelMaxHeight: Float = 300f,

        val centerMinSize: Float = 200f,
        val panelSpacing: Float = 5f
    )

    private val config = DockSpaceConfig()

    fun addPanel(panel: DockablePanel, position: DockPosition) {
        dockedPanels[position]?.add(panel)
        panel.onDock(id, position)
        recalculateLayout()
    }

    fun removePanel(panel: DockablePanel) {
        dockedPanels[panel.dockPosition]?.remove(panel)
        panel.onUndock()
        recalculateLayout()
    }

    fun render(drawList: ImDrawList) {
        // Draw dock space background
        drawList.addRectFilled(
            position.x,
            position.y,
            position.x + size.x,
            position.y + size.y,
            ImColor.rgba(140, 30, 30, 255)
        )

        // Render panels in each region according to layout
        dockedPanels.forEach { (position, panels) ->
            val region = layoutCache[position] ?: return@forEach
            panels.forEach { panel ->
                panel.position = calculatePanelPosition(panel, region, position)
                panel.size = calculatePanelSize(panel, region, position)
                panel.render(drawList)
            }
        }
    }

    fun renderPost(drawList: ImDrawList) {
        // Render panels in each region according to layout
        dockedPanels.forEach { (position, panels) ->
            val region = layoutCache[position] ?: return@forEach
            panels.forEach { panel ->
                panel.renderPost(drawList)
            }
        }
    }

    private fun calculatePanelPosition(
        panel: DockablePanel,
        region: LayoutRegion,
        dockPosition: DockPosition
    ): Vector2f {
        val panelsInRegion = dockedPanels[dockPosition]?.size ?: 0
        if (panelsInRegion == 0) return region.position

        val index = dockedPanels[dockPosition]?.indexOf(panel) ?: 0
        val spacing = config.panelSpacing

        return when (dockPosition) {
            DockPosition.LEFT, DockPosition.RIGHT -> {
                Vector2f(
                    region.position.x,
                    region.position.y + (region.size.y / panelsInRegion) * index + spacing * index
                )
            }

            DockPosition.TOP, DockPosition.BOTTOM -> {
                Vector2f(
                    region.position.x + (region.size.x / panelsInRegion) * index + spacing * index,
                    region.position.y
                )
            }

            DockPosition.CENTER -> region.position
            DockPosition.FLOATING -> panel.position // Maintain current position for floating panels
        }
    }

    private fun calculatePanelSize(
        panel: DockablePanel,
        region: LayoutRegion,
        dockPosition: DockPosition
    ): Vector2f {
        val panelsInRegion = dockedPanels[dockPosition]?.size ?: 0
        if (panelsInRegion == 0) return region.size

        val spacing = config.panelSpacing * (panelsInRegion - 1)

        return when (dockPosition) {
            DockPosition.LEFT, DockPosition.RIGHT -> {
                Vector2f(
                    region.size.x,
                    (region.size.y - spacing) / panelsInRegion
                )
            }

            DockPosition.TOP, DockPosition.BOTTOM -> {
                Vector2f(
                    (region.size.x - spacing) / panelsInRegion,
                    region.size.y
                )
            }

            DockPosition.CENTER -> region.size
            DockPosition.FLOATING -> panel.size // Maintain current size for floating panels
        }
    }

    private fun recalculateLayout() {
        layoutCache.clear()

        // Calculate space needed for each dock position
        val leftWidth = if (dockedPanels[DockPosition.LEFT]?.isNotEmpty() == true)
            config.sidePanelDefaultWidth else 0f
        val rightWidth = if (dockedPanels[DockPosition.RIGHT]?.isNotEmpty() == true)
            config.sidePanelDefaultWidth else 0f
        val topHeight = if (dockedPanels[DockPosition.TOP]?.isNotEmpty() == true)
            config.topBottomPanelDefaultHeight else 0f
        val bottomHeight = if (dockedPanels[DockPosition.BOTTOM]?.isNotEmpty() == true)
            config.topBottomPanelDefaultHeight else 0f

        // Calculate remaining space for center
        val centerWidth = size.x - leftWidth - rightWidth - config.panelSpacing * 2
        val centerHeight = size.y - topHeight - bottomHeight - config.panelSpacing * 2

        // Left region
        if (leftWidth > 0) {
            layoutCache[DockPosition.LEFT] = LayoutRegion(
                Vector2f(position.x, position.y + topHeight + config.panelSpacing),
                Vector2f(leftWidth, centerHeight)
            )
        }

        // Right region
        if (rightWidth > 0) {
            layoutCache[DockPosition.RIGHT] = LayoutRegion(
                Vector2f(position.x + size.x - rightWidth, position.y + topHeight + config.panelSpacing),
                Vector2f(rightWidth, centerHeight)
            )
        }

        // Top region
        if (topHeight > 0) {
            layoutCache[DockPosition.TOP] = LayoutRegion(
                Vector2f(position.x, position.y),
                Vector2f(size.x, topHeight)
            )
        }

        // Bottom region
        if (bottomHeight > 0) {
            layoutCache[DockPosition.BOTTOM] = LayoutRegion(
                Vector2f(position.x, position.y + size.y - bottomHeight),
                Vector2f(size.x, bottomHeight)
            )
        }

        // Center region
        layoutCache[DockPosition.CENTER] = LayoutRegion(
            Vector2f(
                position.x + leftWidth + config.panelSpacing,
                position.y + topHeight + config.panelSpacing
            ),
            Vector2f(centerWidth, centerHeight)
        )
    }

    // Handle resizing of dock regions
    private var activeResizeRegion: DockPosition? = null
    private var resizeStartPosition = Vector2f()

    fun handleResize(mousePosition: Vector2f) {
        if (activeResizeRegion == null) {
            // Check if mouse is over any resize area
            layoutCache.forEach { (position, region) ->
                if (isOverResizeArea(mousePosition, position, region)) {
                    activeResizeRegion = position
                    resizeStartPosition = mousePosition
                    return
                }
            }
        } else {
            // Handle active resize
            val delta = Vector2f(mousePosition).sub(resizeStartPosition)
            resizeRegion(activeResizeRegion!!, delta)
            resizeStartPosition = mousePosition

            // Recalculate layout after resize
            recalculateLayout()
        }
    }

    private fun isOverResizeArea(mousePosition: Vector2f, dockPosition: DockPosition, region: LayoutRegion): Boolean {
        val resizeAreaSize = 5f
        return when (dockPosition) {
            DockPosition.LEFT -> mousePosition.x in (region.position.x + region.size.x - resizeAreaSize)..(region.position.x + region.size.x + resizeAreaSize)
            DockPosition.RIGHT -> mousePosition.x in (region.position.x - resizeAreaSize)..(region.position.x + resizeAreaSize)
            DockPosition.TOP -> mousePosition.y in (region.position.y + region.size.y - resizeAreaSize)..(region.position.y + region.size.y + resizeAreaSize)
            DockPosition.BOTTOM -> mousePosition.y in (region.position.y - resizeAreaSize)..(region.position.y + resizeAreaSize)
            else -> false
        }
    }

    private fun resizeRegion(position: DockPosition, delta: Vector2f) {
        val region = layoutCache[position] ?: return

        when (position) {
            DockPosition.LEFT -> {
                val newWidth = (region.size.x + delta.x).coerceIn(
                    config.sidePanelMinWidth,
                    config.sidePanelMaxWidth
                )
                region.size = Vector2f(newWidth, region.size.y)
            }

            DockPosition.RIGHT -> {
                val newWidth = (region.size.x - delta.x).coerceIn(
                    config.sidePanelMinWidth,
                    config.sidePanelMaxWidth
                )
                region.size = Vector2f(newWidth, region.size.y)
                region.position = Vector2f(region.position.x + delta.x, region.position.y)
            }

            DockPosition.TOP -> {
                val newHeight = (region.size.y + delta.y).coerceIn(
                    config.topBottomPanelMinHeight,
                    config.topBottomPanelMaxHeight
                )
                region.size = Vector2f(region.size.x, newHeight)
            }

            DockPosition.BOTTOM -> {
                val newHeight = (region.size.y - delta.y).coerceIn(
                    config.topBottomPanelMinHeight,
                    config.topBottomPanelMaxHeight
                )
                region.size = Vector2f(region.size.x, newHeight)
                region.position = Vector2f(region.position.x, region.position.y + delta.y)
            }

            else -> {}
        }
    }

    fun stopResize() {
        activeResizeRegion = null
    }
}