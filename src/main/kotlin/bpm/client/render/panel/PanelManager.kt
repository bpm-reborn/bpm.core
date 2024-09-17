package bpm.client.render.panel

import bpm.client.runtime.windows.CanvasGraphics
import bpm.client.utils.toVec2f
import bpm.client.utils.use
import imgui.ImColor
import imgui.ImColor.rgba
import imgui.ImDrawList
import imgui.ImGui
import imgui.flag.ImGuiMouseButton
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

    fun addPanel(panel: Panel) {
        panels[panel.title] = panel
        panel.setupPanel(graphics, this)
        arrangePanel(panel)
    }

    private fun arrangePanel(panel: Panel) {
        val displaySize = ImGui.getIO().displaySize
        val padding = 5f
        val sideWidth = 0.25f
        val sideHeight = 0.5f
        val bottomHeight = 0.35f
        when (panel.title) {
            "Variables" -> {
                panel.position.set(padding, padding)
                panel.panelWidth = displaySize.x * sideWidth
                panel.panelHeight = displaySize.y * 0.5f
            }
            "Proxies" -> {
                val variablesPanel = panels["Variables"]
                if (variablesPanel != null) {
                    panel.position.set(padding, variablesPanel.position.y + variablesPanel.panelHeight + padding)
                    panel.panelWidth = variablesPanel.panelWidth
                    panel.panelHeight = displaySize.y - panel.position.y - padding
                } else {
                    panel.position.set(padding, displaySize.y * 0.5f + padding)
                    panel.panelWidth = displaySize.x * sideWidth
                    panel.panelHeight = displaySize.y * 0.5f - padding * 2
                }
            }
            "Console" -> {
                val variablesPanel = panels["Variables"]
                val proxiesPanel = panels["Proxies"]
                if (variablesPanel != null && proxiesPanel != null) {

                    panel.panelWidth = (displaySize.x - proxiesPanel.panelWidth) - padding * 3
                    panel.panelHeight = displaySize.y * 0.35f
                    //Set y position so that the console is on the bottom of the screen
                    panel.position.set((displaySize.x * sideWidth + padding * 2 ).toFloat(), displaySize.y - panel.panelHeight - padding)
                } else {
                    panel.position.set(displaySize.x * 0.3f + padding * 2, displaySize.y * 0.5f + padding)
                    panel.panelWidth = displaySize.x * 0.7f - padding * 3
                    panel.panelHeight = displaySize.y * bottomHeight - padding * 2
                }
            }
            else -> {
                panel.position.set(displaySize.x * 0.25f, displaySize.y * 0.25f)
                panel.panelWidth = displaySize.x * 0.5f
                panel.panelHeight = displaySize.y * 0.5f
            }
        }
    }

    fun renderPanels(drawList: ImDrawList) {
        panels.values.forEach { panel ->
            panel.render(drawList, panel.position, 1f)
        }
    }

    fun isAnyHovered(): Boolean {
        return panels.values.any { panel ->
            ImGui.isMouseHoveringRect(
                panel.position.x,
                panel.position.y,
                panel.position.x + panel.panelWidth,
                panel.position.y + panel.panelHeight
            )
        }
    }

    fun renderPanelsPost(gfx: GuiGraphics, bounds: Vector4f) {
        panels.values.forEach { panel ->
            panel.renderPost(
                gfx,
                Vector3f(panel.position.x, panel.position.y, 100f),
                Vector3f(panel.panelWidth, panel.panelHeight, 100f)
            )
        }
    }
}