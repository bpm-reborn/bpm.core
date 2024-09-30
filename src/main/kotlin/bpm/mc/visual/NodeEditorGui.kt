package bpm.mc.visual

import bpm.client.runtime.ClientRuntime
import imgui.ImGui
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.util.*

class NodeEditorGui : Screen(Component.literal("Node Editor")) {

    private var workspaceUuid: UUID? = null
    private var openTime: Long = 0
    private var helpShown = false

    init {
        openTime = System.currentTimeMillis()
    }


    override fun init() {
        //No op, we use this simply for event handling
//        minecraft?.options?.hideGui = true
    }

    override fun render(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
//        renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
//        this.renderPanorama(pGuiGraphics, pPartialTick)

        renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        ClientRuntime.newFrame()
        ClientRuntime.process(pGuiGraphics)
//        renderHelp(pGuiGraphics)

        ClientRuntime.endFrame()

        ClientGui.renderPost(pGuiGraphics)

        //No op, we use this simply for event handling
//        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
    }
    //Renders a button that when clicked, shows the (markdown browser) help screen
    private fun renderHelp(pGuiGraphics: GuiGraphics) {
        val drawList = ImGui.getWindowDrawList()
        val windowPos = ImGui.getWindowPos()
        val windowSize = ImGui.getWindowSize()
        //Draws a button in the top left
        if (ImGui.button("Help")) {
            helpShown = true

        }
    }

    override fun onClose() {
        if (helpShown) {
            helpShown = false
            return
        }
        super.onClose()
        //Close the workspace, updating the overlay 2d
        ClientGui.close()
    }

    override fun isPauseScreen(): Boolean = false

}
