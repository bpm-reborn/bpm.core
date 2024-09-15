package bpm.client.render.panel

import bpm.client.font.Fonts
import bpm.client.runtime.windows.CanvasGraphics
import bpm.client.utils.PropertyInput
import bpm.client.utils.use
import bpm.common.property.Property
import com.mojang.blaze3d.systems.RenderSystem
import imgui.ImColor
import imgui.ImDrawList
import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiStyleVar
import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import org.joml.Vector2f
import org.joml.Vector3i
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.item.ItemDisplayContext

class ProxiesPanel(manager: PanelManager) : Panel("Proxies", "\uf1ec", manager) {

    private val titleFont = Fonts.getFamily("Inter")["Bold"]
    private val bodyFont = Fonts.getFamily("Inter")["Light"]
    private val faFont = Fonts.getFamily("Fa")["Regular"][18]

    private val accentColor = ImColor.rgba(0, 122, 255, 255)
    private val buttonColor = ImColor.rgba(58, 58, 60, 255)
    private val buttonHoverColor = ImColor.rgba(68, 68, 70, 255)
    private val backgroundColor = ImColor.rgba(40, 40, 40, 255)
    private val proxyList = mutableListOf(
        ProxyData("minecraft:dirt", Vector3i(10, 64, -30)),
        ProxyData("minecraft:stone", Vector3i(-5, 70, 15)),
        ProxyData("minecraft:oak_log", Vector3i(100, 68, -50))
    )

    override fun renderBody(graphics: CanvasGraphics, drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        proxyList.forEachIndexed { index, proxy ->
            val height = 60f
//            val pos = Vector2f(position.x + 10f, position.y + index * (height + 10f) + 10f)
            renderProxy(drawList, proxy, position, Vector2f(size.x - 25f, height), size)
            ImGui.dummy(0f, height + 10f)
        }
    }

    private fun renderProxy(
        drawList: ImDrawList,
        proxy: ProxyData,
        position: Vector2f,
        size: Vector2f,
        bodySize: Vector2f
    ) {
        // Background
        val pos = ImGui.getCursorScreenPos()
        drawList.addRectFilled(
            pos.x,
            pos.y,
            pos.x + size.x,
            pos.y + size.y,
            backgroundColor,
            10f
        )

        // Left space for item rendering
        val itemSize = 32f
        val leftSpaceWidth = itemSize + 10f
        val buffer = Minecraft.getInstance().renderBuffers().bufferSource()
        val bufferBuilder = buffer.getBuffer(RenderType.solid())
        // Render item
        //Don't render if it's not visible
        if (pos.y + size.y > 0 && pos.y < bodySize.y) {
            renderBlockItem(buffer, proxy.blockName, Vector2f(pos.x + 10f, pos.y + 10f), 0f)
        }


        // Separator line
        drawList.addLine(
            pos.x + leftSpaceWidth,
            pos.y,
            pos.x + leftSpaceWidth,
            pos.y + size.y,
            ImColor.rgba(100, 100, 100, 255),
            2f
        )

        // Block name
        titleFont.title.use {
            drawList.addText(
                titleFont.title,
                18f,
                pos.x + leftSpaceWidth + 10f,
                pos.y + 10f,
                ImColor.rgba(255, 255, 255, 255),
                proxy.blockName.split(":").last().replace("_", " ").capitalize()
            )
        }

        // Block position
        val positionText = "Position: ${proxy.position.x}, ${proxy.position.y}, ${proxy.position.z}"
        bodyFont.body.use {
            drawList.addText(
                bodyFont.body,
                14f,
                pos.x + leftSpaceWidth + 10f,
                pos.y + 35f,
                ImColor.rgba(200, 200, 200, 255),
                positionText
            )
        }
    }

    private val poseStack = PoseStack()
    private fun renderBlockItem(
        bufferSource: MultiBufferSource,
        blockId: String,
        position: Vector2f,
        partialTicks: Float
    ) {
        val item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(blockId)) ?: Items.AIR
        val itemStack = ItemStack(item)
        poseStack.setIdentity()
        poseStack.pushPose()
        val resolution = Minecraft.getInstance().window.guiScale.toFloat()
        //Normalizies the position with the resolution
        poseStack.scale(1f / resolution, 1f / resolution, 1f)
        poseStack.translate(position.x, position.y, 1f)
        poseStack.translate(11.0, 20.0, 0.0)
        poseStack.scale(32f, 32f, 32f)
        Minecraft.getInstance().itemRenderer.renderStatic(
            itemStack,
            ItemDisplayContext.GUI,
            15728880, // Fullbright
            OverlayTexture.NO_OVERLAY,
            poseStack,
            bufferSource,
            null,
            0
        )
        poseStack.popPose()
    }


    private data class ProxyData(val blockName: String, val position: Vector3i)
}