package bpm.client.render.panel

import bpm.client.font.Fonts
import bpm.client.render.Gui
import bpm.client.runtime.windows.CanvasGraphics
import bpm.client.utils.toVec2f
import bpm.client.utils.use
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexSorting
import imgui.ImColor
import imgui.ImDrawList
import imgui.ImGui
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3i

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


    private val guigraphics: GuiGraphics = GuiGraphics(
        Minecraft.getInstance(),
        Minecraft.getInstance().renderBuffers().bufferSource()
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
        val windowSize = ImGui.getWindowSize()
        // Background

        val viewportSize = ImGui.getMainViewport().size
        val screenpos = ImGui.getCursorScreenPos()


        drawList.addRectFilled(
            screenpos.x,
            screenpos.y,
            screenpos.x + size.x,
            screenpos.y + size.y,
            backgroundColor,
            10f
        )

        // Left space for item rendering
        val itemSize = 32f
        val leftSpaceWidth = itemSize + 10f
        val buffer = Minecraft.getInstance().renderBuffers().bufferSource()
        // Render item
        val windowPos = ImGui.getWindowViewport().pos

        //Normalize the screen cursor position using the current windows size
        val pos = Vector2f(screenpos.x - windowPos.x, screenpos.y - windowPos.y)

        //Don't render if it's not visible
//        if (pos.y + size.y > 0 && pos.y < bodySize.y) {
//            renderBlockItem(buffer, proxy.blockName, Vector2f(pos.x, pos.y), windowSize.toVec2f, 0f)
        //TODO: figure out clipping when it's intersecting with our imgui clip rect
        Gui.drawBlockItem(proxy.blockName, pos.x, pos.y)
//        }


        // Separator line
        drawList.addLine(
            screenpos.x + leftSpaceWidth,
            screenpos.y,
            screenpos.x + leftSpaceWidth,
            screenpos.y + size.y,
            ImColor.rgba(100, 100, 100, 255),
            2f
        )

        // Block name
        titleFont.title.use {
            drawList.addText(
                titleFont.title,
                18f,
                screenpos.x + leftSpaceWidth + 10f,
                screenpos.y + 10f,
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
                screenpos.x + leftSpaceWidth + 10f,
                screenpos.y + 35f,
                ImColor.rgba(200, 200, 200, 255),
                positionText
            )
        }
    }

    private fun renderBlockItem(
        bufferSource: MultiBufferSource.BufferSource,
        blockId: String,
        position: Vector2f,
        containerSize: Vector2f,
        partialTicks: Float
    ) {
        val item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(blockId)) ?: Items.AIR
        val itemStack = ItemStack(item)
        val minecraft = Minecraft.getInstance()
//        RenderSystem.disableDepthTest()
//        RenderSystem.enableBlend()
//        RenderSystem.defaultBlendFunc()
//        val pose = guigraphics.pose()
//        pose.setIdentity()
//        pose.pushPose()
//
//
//        guigraphics.pose().translate(0f, 0f, -1000.0f)
//        guigraphics.renderItem(
//            itemStack,
//            position.x.toInt(), position.y.toInt()
//        )
//        guigraphics.pose().popPose()
//
//        RenderSystem.disableBlend()
//        RenderSystem.enableDepthTest()
//

        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.setShader { GameRenderer.getPositionColorShader() }

        val matrix4f = Matrix4f().setOrtho(
            0.0f,
            minecraft.window.screenWidth.toFloat(),
            minecraft.window.screenHeight.toFloat(),
            0.0f,
            0.1f,
            3000.0f
        )
        val pose = PoseStack()
        val screenScale = minecraft.window.guiScale

        // Scale the item
        var scale = 42.0f / screenScale.toFloat()
        val isMacos = System.getProperty("os.name").contains("mac", ignoreCase = true)
        val isRetina = isMacos && minecraft.window.screenWidth.toFloat() * screenScale > 1500.0f
        //if retina, we need to modify the position to double
        if (isRetina) {
            position.x *= 2
            position.y *= 2
            scale *= 2
        }
        pose.pushPose()
        pose.translate(position.x.toDouble() / screenScale, position.y.toDouble() / screenScale, 100.0)
        pose.scale(scale, scale, scale)
        pose.translate(0.5, 0.75, 100.0)
//        pose.mulPose(matrix4f)

        // Render the item
        Minecraft.getInstance().itemRenderer.renderStatic(
            itemStack,
            ItemDisplayContext.GUI,
            15728880, // Fullbright
            OverlayTexture.NO_OVERLAY,
            pose,
            bufferSource,
            null,
            0
        )

        pose.popPose()

    }

    private data class ProxyData(val blockName: String, val position: Vector3i)
}