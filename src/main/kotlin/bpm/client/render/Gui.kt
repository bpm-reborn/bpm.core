package bpm.client.render

import bpm.client.font.Fonts
import bpm.client.utils.use
import com.mojang.blaze3d.vertex.PoseStack
import imgui.ImColor
import imgui.ImDrawList
import imgui.ImFont
import imgui.ImGui
import imgui.flag.ImGuiMouseButton
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.joml.Vector2f

object Gui {

    val font = Fonts.getFamily("Inter")["Regular"]
    private val isMacos = System.getProperty("os.name").contains("mac", ignoreCase = true)
    private val minecraft = Minecraft.getInstance()
    private val guiScale get() = minecraft.window.guiScale
    private val retina get() = isMacos && minecraft.window.screenWidth.toFloat() * guiScale > 1500.0f
    private val poseStack = PoseStack()
    private val renderBufferSource: MultiBufferSource.BufferSource get() = minecraft.renderBuffers().bufferSource()

    fun drawBlockItem(blockId: String, x: Float, y: Float, scale: Float = 42.0f) {
        val item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(blockId)) ?: Items.AIR
        val itemStack = ItemStack(item)
        drawBlockItem(itemStack, x, y, scale)
    }


    fun drawBlockItem(
        itemStack: ItemStack,
        x: Float,
        y: Float,
        scale: Float = 42.0f
    ) {
        var scaledScale = scale / guiScale.toFloat()
        var scaledX = x / guiScale.toFloat()
        var scaledY = y / guiScale.toFloat()
        if (retina) {
            scaledX *= 2
            scaledY *= 2
            scaledScale *= 2
        }
        poseStack.pushPose()
        poseStack.translate(scaledX, scaledY, 100.0f)
        poseStack.scale(scaledScale, scaledScale, scaledScale)
        poseStack.translate(0.5, 0.75, 100.0)
        Minecraft.getInstance().itemRenderer.renderStatic(
            itemStack,
            ItemDisplayContext.GUI,
            15728880, // Fullbright
            OverlayTexture.NO_OVERLAY,
            poseStack,
            renderBufferSource,
            null,
            0
        )
        poseStack.popPose()
    }

    fun toolTip(text: String) {
        //Draws a tooltip of the given text at the mouse position
        val drawList = ImGui.getForegroundDrawList()
        val bodyFont = font.header
        bodyFont.use {
            val size = ImGui.calcTextSize(text)
            val padding = 4f
            val pos = ImGui.getMousePos()
            val windowPos = ImGui.getWindowPos()
            val windowSize = ImGui.getWindowSize()
            //Draws the background of the tooltip
            val pos1 = Vector2f(pos.x, pos.y + 20)
            val pos2 = Vector2f(pos1.x + size.x + padding * 2, pos1.y + size.y + padding * 2)
            drawList.addRectFilled(pos1.x, pos1.y, pos2.x, pos2.y, ImColor.rgba(0.1f, 0.1f, 0.1f, 0.9f))
            //Draws the text of the tooltip
            drawList.addText(pos1.x + padding, pos1.y + padding, ImColor.rgba(1f, 1f, 1f, 1f), text)
        }
    }

    fun isMouseOver(position: Vector2f, size: Vector2f): Boolean {
        val mousePos = ImGui.getMousePos()
        return mousePos.x >= position.x && mousePos.x <= position.x + size.x &&
                mousePos.y >= position.y && mousePos.y <= position.y + size.y
    }


}

