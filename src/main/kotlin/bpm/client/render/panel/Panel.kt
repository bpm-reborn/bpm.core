package bpm.client.render.panel

import imgui.ImDrawList
import imgui.ImGui
import org.joml.Vector2f
import bpm.client.font.Fonts
import bpm.client.render.IRender
import bpm.client.runtime.ClientRuntime
import bpm.client.runtime.windows.CanvasContext
import bpm.client.runtime.windows.CanvasGraphics
import bpm.client.utils.use
import bpm.common.network.Endpoint
import imgui.ImColor
import imgui.type.ImBoolean
import net.minecraft.client.gui.GuiGraphics


abstract class Panel(val title: String, val icon: String) : IRender {

    protected val graphics: CanvasGraphics get() = ClientRuntime.canvasWindow!!.graphics
    protected val iconFam = Fonts.getFamily("Fa")["Regular"]
    protected val boldFam = Fonts.getFamily("Inter")["Bold"]
    protected val bodyFam = Fonts.getFamily("Inter")["Regular"]
    protected val context = Endpoint.installed<CanvasContext>()
    protected val buttonColor = ImColor.rgba(58, 58, 60, 255)
    protected val buttonHoverColor = ImColor.rgba(68, 68, 70, 255)
    protected var isDragging = false
        set(value) {
            //This prevents actions from being performed while dragging
            graphics.context.isLinking = value
            field = value
        }
    private var x: Int = 0
    private var y: Int = 0
    private var width: Int = 0
    private var height: Int = 0
    private var guiGfx: GuiGraphics? = null

    override fun render(gfx: CanvasGraphics, guiGfx: GuiGraphics) {
        if (this.guiGfx == null) this.guiGfx = guiGfx // This bad, but don't care for now

        val drawList = ImGui.getWindowDrawList()
        ImGui.setCursorScreenPos(ImGui.getCursorScreenPosX(), ImGui.getCursorScreenPosY() + 10)
        renderHeader(drawList)
        val contentStart = ImGui.getCursorScreenPos()
        // Main content area
        renderBody(
            drawList,
            Vector2f(contentStart.x, contentStart.y + 10),
            Vector2f(
                ImGui.getContentRegionAvail().x,
                ImGui.getContentRegionAvail().y - 70f
            )  // Reserve space for footer
        )
        // Footer area with separator
        val footerStart = Vector2f(ImGui.getWindowPosX(), ImGui.getWindowPosY() + ImGui.getWindowHeight() - 60f)

        drawList.addLine(
            footerStart.x,
            footerStart.y,
            footerStart.x + ImGui.getContentRegionAvail().x,
            footerStart.y,
            ImColor.rgba(60, 60, 60, 255),
            1f
        )

        renderFooterContent(
            drawList,
            Vector2f(footerStart.x, footerStart.y + 5f),
            Vector2f(ImGui.getContentRegionAvail().x, 50f)
        )


        val width = ImGui.getWindowWidth()
        val height = ImGui.getWindowHeight()
        val pos = ImGui.getWindowPos()
        val transformedPos = graphics.toScreenSpaceVector(pos.x, pos.y)
        val transformedSize = graphics.toScreenSpaceVector(width, height - 30f) //Accounts for footer

        //Store the minecraft transformed position and size
        this.x = transformedPos.x.toInt()
        this.y = transformedPos.y.toInt()
        this.width = transformedSize.x.toInt()
        this.height = transformedSize.y.toInt()
    }


    protected fun recordClipped(recording: CanvasGraphics.() -> Unit) {
        val gfx = this.graphics
        val guiGfx = this.guiGfx!!
        gfx.recordDrawCall {
            guiGfx.enableScissor(
                x,
                y,
                width + x,
                height + y
            )
            recording(gfx)
            guiGfx.disableScissor()
        }
    }

    protected fun recordUnclipped(recording: CanvasGraphics.() -> Unit) {
        val gfx = this.graphics
        gfx.recordDrawCall {
            recording(gfx)
        }
    }


    private fun renderHeader(drawList: ImDrawList) {

        //Draw the header rect
        drawList.addRectFilled(
            ImGui.getCursorScreenPosX(),
            ImGui.getCursorScreenPosY(),
            ImGui.getCursorScreenPosX() + ImGui.getContentRegionAvail().x,
            ImGui.getCursorScreenPosY() + 35f,
            ImColor.rgba(60, 60, 60, 255),
            0f
        )
        iconFam[32].use {
            drawList.addText(
                it, 32f,
                ImGui.getCursorScreenPosX() + 15f,
                ImGui.getCursorScreenPosY(),
                ImColor.rgba(255, 255, 255, 255),
                icon
            )
        }

        boldFam[22].use {
            drawList.addText(
                it, 22f,
                ImGui.getCursorScreenPosX() + 40f,
                ImGui.getCursorScreenPosY() + 7f,
                ImColor.rgba(255, 255, 255, 255),
                title
            )
        }

        ImGui.dummy(0f, 25f) // Space after header
    }


    protected abstract fun renderBody(drawList: ImDrawList, position: Vector2f, size: Vector2f)
    protected open fun renderFooterContent(drawList: ImDrawList, position: Vector2f, size: Vector2f) = Unit

    protected fun isMouseOver(pos: Vector2f, width: Float, height: Float): Boolean {
        val mousePos = ImGui.getMousePos()
        return mousePos.x >= pos.x && mousePos.x <= pos.x + width &&
                mousePos.y >= pos.y && mousePos.y <= pos.y + height
    }

}