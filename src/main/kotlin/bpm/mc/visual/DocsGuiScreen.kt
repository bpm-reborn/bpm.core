package bpm.mc.visual

import bpm.client.runtime.ClientRuntime
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class DocsGuiScreen : Screen(Component.literal("Docs Editor")) {

    override fun init() {
    }


    override fun render(p_281549_: GuiGraphics, p_281550_: Int, p_282878_: Int, p_282465_: Float) {
        super.renderBackground(p_281549_, p_281550_, p_282878_, p_282465_)

        ClientRuntime.newFrame()
        ClientRuntime.markdownBrowser.render()
        ClientRuntime.endFrame()
    }

    override fun onClose() {
        //No op, we use this simply for event handling
        super.onClose()
    }

}