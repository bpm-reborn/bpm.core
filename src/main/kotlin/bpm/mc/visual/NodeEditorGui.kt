package bpm.mc.visual

import bpm.client.runtime.ClientRuntime
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.util.*

class NodeEditorGui : Screen(Component.literal("Node Editor")) {

    private var workspaceUuid: UUID? = null
    private var openTime: Long = 0

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
        ClientRuntime.endFrame()

        ClientGui.renderPost(pGuiGraphics)

        //No op, we use this simply for event handling
//        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
    }

    override fun onClose() {
        super.onClose()
        //Close the workspace, updating the overlay 2d
        ClientGui.close()
    }

    override fun isPauseScreen(): Boolean = false

}
