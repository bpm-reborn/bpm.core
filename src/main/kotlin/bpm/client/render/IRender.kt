package bpm.client.render

import bpm.client.runtime.windows.CanvasGraphics
import net.minecraft.client.gui.GuiGraphics

fun interface IRender {

    fun render(gfx: CanvasGraphics, guiGfx: GuiGraphics)
}
