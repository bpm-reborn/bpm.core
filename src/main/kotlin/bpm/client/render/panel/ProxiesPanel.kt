package bpm.client.render.panel

import bpm.client.font.Fonts
import bpm.client.runtime.ClientRuntime
import bpm.client.utils.use
import bpm.common.network.Client
import bpm.common.utils.FontAwesome
import bpm.common.workspace.packets.ProxyNodeCreateRequest
import bpm.mc.links.EnderControllerState
import bpm.mc.links.EnderNet
import bpm.mc.links.WorldPos
import bpm.pipe.PipeNetwork
import bpm.pipe.proxy.ProxiedState
import bpm.pipe.proxy.ProxiedType
import bpm.pipe.proxy.ProxyState
import imgui.ImColor
import imgui.ImDrawList
import imgui.ImGui
import imgui.flag.ImGuiMouseButton
import imgui.flag.ImGuiMouseCursor
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector3i

object ProxiesPanel : Panel("Proxies", FontAwesome.Reply) {

    private val titleFont = Fonts.getFamily("Inter")["Bold"]
    private val bodyFont = Fonts.getFamily("Inter")["Light"]
    private val recordedDrawCall = mutableListOf<(gfx: GuiGraphics) -> Unit>()
    private val unClippedRecordedDrawCalls = mutableListOf<(gfx: GuiGraphics) -> Unit>()
    private var draggedProxy: WorldPos? = null
    // Colors for gradients and accents
    private val gradientTopColor = ImColor.rgba(60, 60, 65, 255)      // Dark gray
    private val gradientBottomColor = ImColor.rgba(45, 45, 50, 255)   // Slightly darker gray
    private val accentColor = ImColor.rgba(130, 150, 255, 255)        // Soft blue, slightly brighter for dark theme
    private val separatorColor = ImColor.rgba(80, 80, 90, 180)        // Mid-dark gray
    private val textPrimaryColor = ImColor.rgba(220, 220, 230, 255)   // Light gray for primary text
    private val textSecondaryColor = ImColor.rgba(170, 170, 190, 255) // Medium gray for secondary text

    override fun renderBody(
        drawList: ImDrawList, position: Vector2f, size: Vector2f
    ) {

        EnderNet.getLinks(ClientRuntime.workspaceUUID).forEach {
            ImGui.dummy(0f, 10f) // Add some space between proxy states
            renderProxyState(drawList, it, position, size)
        }
        handleProxyDragging(drawList)
    }

    private fun renderProxyState(
        drawList: ImDrawList,
        proxyState: WorldPos,
        position: Vector2f,
        size: Vector2f
    ) {
        val screenpos = ImGui.getCursorScreenPos()
        val windowPos = ImGui.getWindowViewport().pos
        val pos = Vector2f(screenpos.x - windowPos.x, screenpos.y - windowPos.y + 10f)

        if (ImGui.isMouseClicked(ImGuiMouseButton.Left) && isMouseOver(pos, size.x, 60f)) {
            draggedProxy = proxyState
            isDragging = true
        }

        renderProxiedBlock(
            drawList,
            proxyState.pos,
            Vector2f(pos.x + 10f, ImGui.getCursorScreenPosY() + 10f),
            Vector2f(size.x - 20f, 60f)
        )
        ImGui.dummy(0f, 70f)
    }

    override fun renderPost(gfx: GuiGraphics, scaledPos: Vector3f, scaledSize: Vector3f) {
        val transformedPos = graphics.toScreenSpaceVector(scaledPos.x, scaledPos.y + 30f)
        val transformedSize = graphics.toScreenSpaceVector(scaledSize.x, scaledSize.y)
        gfx.enableScissor(
            transformedPos.x.toInt(),
            transformedPos.y.toInt(),
            transformedSize.x.toInt() + transformedPos.x.toInt(),
            transformedSize.y.toInt() + transformedPos.y.toInt()
        )
        recordedDrawCall.forEach { it(gfx) }
        recordedDrawCall.clear()
        gfx.disableScissor()

        unClippedRecordedDrawCalls.forEach { it(gfx) }
        unClippedRecordedDrawCalls.clear()
    }

    private fun renderProxy(
        drawList: ImDrawList,
        origin: BlockPos,
        position: Vector2f,
        size: Vector2f,
        clipped: Boolean = true
    ) {
        val level = ClientRuntime.level ?: return
        val blockState = level.getBlockState(origin)
        val itemStack = ItemStack(blockState.block)

        ImGui.pushClipRect(position.x, position.y, position.x + size.x, position.y + size.y, true)

        // Gradient background
        drawList.addRectFilledMultiColor(
            position.x,
            position.y,
            position.x + size.x,
            position.y + size.y,
            gradientTopColor.toLong(),
            gradientTopColor.toLong(),
            gradientBottomColor.toLong(),
            gradientBottomColor.toLong()
        )

        // Subtle border
        drawList.addRect(
            position.x,
            position.y,
            position.x + size.x,
            position.y + size.y,
            separatorColor,
            8f,
            0,
            2f
        )

        // Item rendering
        val itemSize = 32f
        val leftSpaceWidth = itemSize + 20f
        (if (clipped) recordedDrawCall else unClippedRecordedDrawCalls).add {
            graphics.renderBlockItem(
                itemStack,
                position.x + 5f,  // Added padding
                position.y,
            )
        }

        // Vertical separator with gradient
        val separatorGradientTop = ImColor.rgba(120, 120, 140, 200)
        val separatorGradientBottom = ImColor.rgba(80, 80, 100, 150)
        drawList.addRectFilledMultiColor(
            position.x + leftSpaceWidth - 1f,
            position.y + 5f,
            position.x + leftSpaceWidth + 1f,
            position.y + size.y - 5f,
            separatorGradientTop.toLong(),
            separatorGradientTop.toLong(),
            separatorGradientBottom.toLong(),
            separatorGradientBottom.toLong()
        )

        // Block name with subtle text shadow
        titleFont.title.use {
            // Shadow
            drawList.addText(
                titleFont.title,
                18f,
                position.x + leftSpaceWidth + 11f,
                position.y + 11f,
                ImColor.rgba(0, 0, 0, 100),
                itemStack.displayName.string
            )
            // Main text
            drawList.addText(
                titleFont.title,
                18f,
                position.x + leftSpaceWidth + 10f,
                position.y + 10f,
                textPrimaryColor,
                itemStack.displayName.string
            )
        }

        // Block position with accent color
        val positionText = "Position: ${origin.x}, ${origin.y}, ${origin.z}"
        bodyFont.body.use {
            drawList.addText(
                bodyFont.body,
                14f,
                position.x + leftSpaceWidth + 10f,
                position.y + 35f,
                textSecondaryColor,
                positionText
            )
        }

        ImGui.popClipRect()
    }


    private fun handleProxyDragging(drawList: ImDrawList) {
        val draggedProxyState = draggedProxy
        if (draggedProxyState != null && ImGui.isMouseDragging(ImGuiMouseButton.Left)) {
            val mousePos = ImGui.getMousePos()

            // Render a preview of the dragged proxy
            renderProxy(
                ImGui.getForegroundDrawList(),
                draggedProxyState.pos,
                Vector2f(mousePos.x + 20f, mousePos.y),
                Vector2f(200f, 60f),
                false
            )

            ImGui.setMouseCursor(ImGuiMouseCursor.None)
        } else if (draggedProxyState != null && ImGui.isMouseReleased(ImGuiMouseButton.Left)) {
            // Handle the drop action here (e.g., create a node in the canvas)
            val mousePos = ImGui.getMousePos()
            Client.send(
                ProxyNodeCreateRequest(
                    graphics.toWorldSpaceVector(mousePos.x, mousePos.y),
                    draggedProxyState.pos,
                    draggedProxyState.level
                )
            )
            draggedProxy = null
            isDragging = false
        }
    }

    private fun renderProxiedBlock(
        drawList: ImDrawList,
        relativePos: BlockPos,
        position: Vector2f,
        size: Vector2f
    ) {
        // Gradient background for proxied block
        drawList.addRectFilledMultiColor(
            position.x,
            position.y,
            position.x + size.x,
            position.y + size.y,
            gradientTopColor.toLong(),
            gradientTopColor.toLong(),
            gradientBottomColor.toLong(),
            gradientBottomColor.toLong()
        )

        // Subtle glow effect on hover
        if (isMouseOver(position, size.x, size.y)) {
            drawList.addRect(
                position.x,
                position.y,
                position.x + size.x,
                position.y + size.y,
                accentColor,
                8f,
                0,
                2f
            )
        } else {
            // Normal border
            drawList.addRect(
                position.x,
                position.y,
                position.x + size.x,
                position.y + size.y,
                separatorColor,
                8f,
                0,
                1.5f
            )
        }

        // Relative position info with improved styling
        val infoText = "Relative Pos: ${relativePos.x}, ${relativePos.y}, ${relativePos.z}"
        bodyFont.body.use {
            // Text shadow
            drawList.addText(
                bodyFont.body,
                14f,
                position.x + 11f,
                position.y + 11f,
                ImColor.rgba(0, 0, 0, 100),
                infoText
            )
            // Main text
            drawList.addText(
                bodyFont.body,
                14f,
                position.x + 10f,
                position.y + 10f,
                textSecondaryColor,
                infoText
            )
        }

        renderProxy(
            drawList,
            relativePos,
            Vector2f(position.x, position.y),
            Vector2f(size.x, size.y)
        )
    }
}

