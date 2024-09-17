package bpm.client.render.panel

import imgui.ImDrawList
import imgui.ImGui
import org.joml.Vector2f
import bpm.client.font.Fonts
import bpm.client.runtime.windows.CanvasContext
import bpm.client.runtime.windows.CanvasGraphics
import bpm.client.utils.toVec2f
import bpm.client.utils.use
import bpm.common.network.Endpoint
import bpm.common.utils.FontAwesome
import imgui.ImColor
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiMouseButton
import imgui.flag.ImGuiMouseCursor
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImString
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import org.joml.Vector2i
import org.joml.Vector3f

abstract class Panel(val title: String, val icon: String) {

    protected lateinit var manager: PanelManager
    protected lateinit var graphics: CanvasGraphics
    var isDragging: Boolean = false
        protected set
    val iconFam = Fonts.getFamily("Fa")["Regular"]
    val boldFam = Fonts.getFamily("Inter")["Bold"]
    val bodyFam = Fonts.getFamily("Inter")["Regular"]
    private val displaySize get() = ImGui.getIO().displaySize
    protected val context = Endpoint.installed<CanvasContext>()
    var position = Vector2f()
    var panelWidth = 300f
        internal set
    var panelHeight = 200f
        internal set
    protected val buttonColor = ImColor.rgba(58, 58, 60, 255)
    protected val buttonHoverColor = ImColor.rgba(68, 68, 70, 255)
    private val lastSize = Vector2i()
    internal fun setupPanel(graphics: CanvasGraphics, manager: PanelManager) {
        this.manager = manager
        this.graphics = graphics
    }

    fun render(drawList: ImDrawList, position: Vector2f, scale: Float) {
        val size = Vector2f(panelWidth * scale, panelHeight * scale)

        renderBackground(drawList, position, size)
        renderTitle(drawList, position, size)
        renderContent(drawList, position, size)
        renderFooter(drawList, position, size)

        checkResizeAndArrange()
    }

    private fun checkResizeAndArrange() {
        val windowSize = ImGui.getWindowViewport().size
        val size = Vector2i(windowSize.x.toInt(), windowSize.y.toInt())
        if (size != lastSize) {
            lastSize.set(size)
            manager.arrangePanel(this)
        }
    }

    private fun renderContent(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        val contentStart = Vector2f(position.x + 10f, position.y + 40f)
        val contentSize = Vector2f(size.x - 20f, size.y - 110f)
        drawList.addRectFilled(
            contentStart.x,
            contentStart.y,
            contentStart.x + contentSize.x,
            contentStart.y + contentSize.y,
            ImGui.colorConvertFloat4ToU32(0.1f, 0.1f, 0.1f, 1f),
            10f
        )
        ImGui.pushClipRect(
            contentStart.x, contentStart.y, contentStart.x + contentSize.x, contentStart.y + contentSize.y, true
        )

        ImGui.setNextWindowPos(contentStart.x, contentStart.y)
        if (ImGui.beginChild("##content_$title", contentSize.x, contentSize.y, false)) {
            renderBody(drawList, contentStart, contentSize)
        }
        ImGui.endChild()
        renderAfter(graphics, drawList, Vector2f(position), size)
        ImGui.popClipRect()
    }

    protected abstract fun renderBody(drawList: ImDrawList, position: Vector2f, size: Vector2f)

    protected open fun renderAfter(graphics: CanvasGraphics, drawList: ImDrawList, position: Vector2f, size: Vector2f) =
        Unit

    protected open fun renderTitle(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        val titleHeight = 30f
        drawList.addRectFilled(
            position.x,
            position.y,
            position.x + size.x,
            position.y + titleHeight,
            ImGui.colorConvertFloat4ToU32(0.3f, 0.3f, 0.3f, 1f)
        )

        iconFam[32].use {
            drawList.addText(
                it, 32f, position.x + 15f, position.y, ImGui.colorConvertFloat4ToU32(1f, 1f, 1f, 1f), icon
            )
        }

        boldFam[22].use {
            drawList.addText(
                it, 22f, position.x + 40f, position.y + 7f, ImGui.colorConvertFloat4ToU32(1f, 1f, 1f, 1f), title
            )
        }
    }

    protected open fun renderBackground(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        drawList.addRectFilled(
            position.x,
            position.y,
            position.x + size.x,
            position.y + size.y,
            ImGui.colorConvertFloat4ToU32(0.2f, 0.2f, 0.2f, 1f)
        )
    }

    protected fun renderFooter(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        val footerHeight = 50f
        val footerY = ImGui.getCursorScreenPos().y + 5f

        drawList.addRectFilled(
            position.x,
            footerY,
            position.x + size.x,
            position.y + size.y + 5f,
            ImColor.rgba(30, 30, 30, 255),
            10f
        )
        //Creats a new window for the footer
        val sizee = Vector2f(size.x, footerHeight)
        ImGui.setNextWindowPos(position.x, footerY)
        ImGui.setNextWindowSize(sizee.x, sizee.y)
        ImGui.beginChild("##footer_$title", sizee.x, sizee.y, false, ImGuiWindowFlags.NoScrollbar)
        renderFooterContent(
            drawList,
            Vector2f(position.x + 15f, footerY + 5f),
            Vector2f(size.x - 30f, footerHeight - 10f)
        )
        ImGui.endChild()
    }

    protected open fun renderFooterContent(drawList: ImDrawList, position: Vector2f, size: Vector2f) = Unit
    private fun isMouseOverTitleBar(): Boolean {
        val titleBarHeight = 30f
        return isMouseOver(position, panelWidth, titleBarHeight)
    }

    protected fun isMouseOver(pos: Vector2f, width: Float, height: Float): Boolean {
        val mousePos = ImGui.getMousePos()
        return mousePos.x >= pos.x && mousePos.x <= pos.x + width &&
                mousePos.y >= pos.y && mousePos.y <= pos.y + height
    }

    open fun renderPost(gfx: GuiGraphics, scaledPos: Vector3f, scaledSize: Vector3f) = Unit

    fun updatePosition(newPosition: Vector2f) {
        position = newPosition
    }
}