package bpm.client.render.panel

import bpm.client.font.Fonts
import bpm.client.runtime.ClientRuntime
import bpm.client.utils.use
import bpm.common.utils.FontAwesome
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
    private var draggedProxy: ProxyState? = null

    override fun renderBody(
        drawList: ImDrawList, position: Vector2f, size: Vector2f
    ) {
        //Iterate the current workspace proxies
        PipeNetwork.ProxyManagerClient.getProxies(ClientRuntime.workspaceUUID).forEach { proxyState ->
            renderProxyState(drawList, proxyState, position, size)
            ImGui.dummy(0f, 10f) // Add some space between proxy states
        }
        handleProxyDragging(drawList)
    }

    private fun renderProxyState(
        drawList: ImDrawList,
        proxyState: ProxyState,
        position: Vector2f,
        size: Vector2f
    ) {
        val screenpos = ImGui.getCursorScreenPos()
        val windowPos = ImGui.getWindowViewport().pos
        val pos = Vector2f(screenpos.x - windowPos.x + 15f, screenpos.y - windowPos.y + 10f)

        // Render the proxy state
        renderProxy(drawList, proxyState.origin, pos, Vector2f(size.x - 35, 60f))
        ImGui.dummy(0f, 70f)
        // Check for dragging
        if (ImGui.isMouseClicked(ImGuiMouseButton.Left) && isMouseOver(pos, size.x - 20f, 60f)) {
            draggedProxy = proxyState
        }

        // Render proxied blocks
        ImGui.indent(20f)
        var index = 0
        proxyState.proxiedBlocks.filter { it.value.proxiedFaces.values.any { it != ProxiedType.NONE } }
            .forEach { (blockPos, proxiedState) ->

                //Draw connection lines
                val pos1 = Vector2f(pos.x + 40, ImGui.getCursorScreenPosY() + 10f)
                val originPos = Vector2f(pos1.x + 10f, pos1.y + 30f)

                val thickness = 3f
                drawList.addLine(
                    pos1.x - 20f, pos1.y + 30f, originPos.x, originPos.y, ImColor.rgba(100, 100, 100, 255), thickness
                )
                if (index != 0) {
                    //Add veritcal connnection line
                    drawList.addLine(
                        pos1.x - 20f, pos1.y, pos1.x - 20f, pos1.y - 30f, ImColor.rgba(100, 100, 100, 255), thickness
                    )
                    drawList.addLine(
                        pos1.x - 20f,
                        pos1.y - 15f,
                        pos1.x - 20f,
                        pos1.y + 30f,
                        ImColor.rgba(100, 100, 100, 255),
                        thickness
                    )

                } else {
//                    drawList.addCircleFilled(pos1.x - 20f, pos1.y - 12f, 5f, ImColor.rgba(100, 100, 100, 255))

                    drawList.addLine(
                        pos1.x - 20f,
                        pos1.y - 15f,
                        pos1.x - 20f,
                        pos1.y + 30f,
                        ImColor.rgba(100, 100, 100, 255),
                        thickness
                    )
                }
                drawList.addCircleFilled(pos1.x - 20f, pos1.y + 30f, 5f, ImColor.rgba(12, 12, 12, 255))
                drawList.addCircle(pos1.x - 20f, pos1.y + 30f, 5f, ImColor.rgba(66, 66, 66, 255), 12, 2f)

                index++

//                drawList.addCircleFilled(originPos.x, originPos.y, 5f, ImColor.rgba(100, 100, 100, 255))

                renderProxiedBlock(
                    drawList,
                    blockPos,
                    proxiedState,
                    Vector2f(pos.x + 35f, ImGui.getCursorScreenPosY() + 10f),
                    Vector2f(size.x - 70f, 60f)
                )
                ImGui.dummy(0f, 60f)
            }
        ImGui.unindent(20f)
    }

    override fun renderPost(gfx: GuiGraphics, scaledPos: Vector3f, scaledSize: Vector3f) {
        val transformedPos = graphics.toScreenSpaceVector(scaledPos.x, scaledPos.y + 30f)
        val transformedSize = graphics.toScreenSpaceVector(scaledSize.x, scaledSize.y - 100f)
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
        drawList: ImDrawList, origin: BlockPos, position: Vector2f, size: Vector2f, clipped: Boolean = true
    ) {
        val level = ClientRuntime.level ?: return
        val blockState = level.getBlockState(origin)
        val itemStack = ItemStack(blockState.block)

        // Background
        drawList.addRectFilled(
            position.x, position.y, position.x + size.x, position.y + size.y, ImColor.rgba(60, 60, 60, 255), 5f
        )

        // Item rendering
        val itemSize = 32f
        val leftSpaceWidth = itemSize + 10f
        //Record the draw call
        (if (clipped) recordedDrawCall else unClippedRecordedDrawCalls).add {
            graphics.renderBlockItem(
                itemStack,
                position.x,
                position.y,
            )
        }

        // Separator line
        drawList.addLine(
            position.x + leftSpaceWidth,
            position.y,
            position.x + leftSpaceWidth,
            position.y + size.y,
            ImColor.rgba(100, 100, 100, 255),
            2f
        )

        // Block name
        titleFont.title.use {
            drawList.addText(
                titleFont.title,
                18f,
                position.x + leftSpaceWidth + 10f,
                position.y + 10f,
                ImColor.rgba(255, 255, 255, 255),
                itemStack.displayName.string
            )
        }

        // Block position
        val positionText = "Origin: ${origin.x}, ${origin.y}, ${origin.z}"
        bodyFont.body.use {
            drawList.addText(
                bodyFont.body,
                14f,
                position.x + leftSpaceWidth + 10f,
                position.y + 35f,
                ImColor.rgba(200, 200, 200, 255),
                positionText
            )
        }
    }

    private fun handleProxyDragging(drawList: ImDrawList) {
        val draggedProxyState = draggedProxy
        if (draggedProxyState != null && ImGui.isMouseDragging(ImGuiMouseButton.Left)) {
            val mousePos = ImGui.getMousePos()

            // Render a preview of the dragged proxy
            renderProxy(
                ImGui.getForegroundDrawList(),
                draggedProxyState.origin,
                Vector2f(mousePos.x + 20f, mousePos.y),
                Vector2f(200f, 60f),
                false
            )

            ImGui.setMouseCursor(ImGuiMouseCursor.None)
        } else if (draggedProxyState != null && ImGui.isMouseReleased(ImGuiMouseButton.Left)) {
            // Handle the drop action here (e.g., create a node in the canvas)
            val mousePos = ImGui.getMousePos()
            // TODO: Implement the creation of a proxy node in the canvas
            draggedProxy = null
        }
    }

    private fun renderProxiedBlock(
        drawList: ImDrawList,
        relativePos: BlockPos,
        proxiedState: ProxiedState,
        position: Vector2f,
        size: Vector2f
    ) {
        // Background
        drawList.addRectFilled(
            position.x, position.y, position.x + size.x, position.y + size.y, ImColor.rgba(50, 50, 50, 255), 8f
        )

        // Proxied block info
        val infoText = "Relative Pos: ${relativePos.x}, ${relativePos.y}, ${relativePos.z}"
        bodyFont.body.use {
            drawList.addText(
                bodyFont.body,
                14f,
                position.x + 10f,
                position.y + 10f,
                ImColor.rgba(200, 200, 200, 255),
                infoText
            )
        }

        renderProxy(drawList, relativePos, Vector2f(position.x, position.y), Vector2f(size.x, size.y))
    }

    private data class ProxyData(val blockName: String, val position: Vector3i)
}