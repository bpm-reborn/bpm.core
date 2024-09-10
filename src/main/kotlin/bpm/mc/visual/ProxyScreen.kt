package bpm.mc.visual

import bpm.client.runtime.ClientRuntime
import bpm.common.logging.KotlinLogging
import bpm.common.network.Client
import bpm.common.network.Listener
import bpm.common.packets.Packet
import bpm.pipe.proxy.PacketProxyRequest
import bpm.pipe.proxy.PacketProxyResponse
import bpm.pipe.proxy.ProxiedType
import com.mojang.blaze3d.systems.RenderSystem
import imgui.ImGui
import imgui.flag.ImGuiMouseCursor
import imgui.flag.ImGuiWindowFlags
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import java.util.*

object ProxyScreen : Screen(Component.literal("Block Preview")), Listener {

    val trackedBlocks = mutableMapOf<BlockPos, BlockState>()
    private val trackedBlockEntities = mutableMapOf<BlockPos, BlockEntity>()
    private var renderer: BlockViewRenderer = BlockViewRenderer(Minecraft.getInstance())
    internal var origin: BlockPos = BlockPos.ZERO
    private var hoveredBlock: Pair<BlockPos, Direction>? = null
    internal lateinit var customBackgroundRenderer: CustomBackgroundRenderer
    private val logger = KotlinLogging.logger { }
    override fun init() {
        super.init()
        renderer = BlockViewRenderer(Minecraft.getInstance())
        customBackgroundRenderer = CustomBackgroundRenderer(minecraft!!)
        renderer.open(origin)
    }

    override fun onPacket(packet: Packet, from: UUID) {
        if (packet is PacketProxyResponse) {
            val state = packet.proxyState

            //Convert all absolute positions to relative positions
//            state.proxiedBlocks.forEach { (pos, proxiedState) ->
//                val newPos = pos.subtract(state.origin)
//                proxiedState.relativePos = newPos
//                state[pos] = proxiedState
//            }

            //Update the renderers with the new proxied state
            renderer.updateProxiedState(state)
            logger.debug { "Received proxied state: $state" }
        }
    }

    fun open(origin: BlockPos, positions: List<BlockPos>) {
        this.origin = origin
        Client.send(PacketProxyRequest(origin))
        trackedBlocks.clear()
        trackedBlockEntities.clear()

        val level = Minecraft.getInstance().level ?: return
        positions.forEach { pos ->
            val state = level.getBlockState(pos)
            trackedBlocks[pos.subtract(origin)] = state
            if (state.block is EntityBlock) {
                val blockEntity = level.getBlockEntity(pos)
                if (blockEntity != null) {
                    trackedBlockEntities[pos.subtract(origin)] = blockEntity
                }
            }
        }

        if (Minecraft.getInstance().screen != this) {
            Minecraft.getInstance().setScreen(this)
        }

    }

    override fun tick() {
        customBackgroundRenderer.updatePulse()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {

        ClientRuntime.newFrame()

        if (isDragging) ImGui.setMouseCursor(ImGuiMouseCursor.TextInput)
        RenderSystem.enableDepthTest()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        renderer.updateScreenSpaceBounds(graphics)

        // After rendering all blocks and block entities, determine which one is hovered
        customBackgroundRenderer.renderBackground(
            graphics,
            renderer,
            mouseX,
            mouseY,
            partialTick,
            hoveredBlock?.second,
            hoveredBlock?.first
        )
        hoveredBlock = renderer.finalizeSortingAndDetermineHoveredBlock(mouseX, mouseY)

        renderer.setupCamera(graphics.pose(), partialTick)
        // Render blocks first
        trackedBlocks.forEach { (relativePos, blockState) ->
            renderer.renderBlock(graphics, blockState, relativePos, partialTick)
        }

        // Then render block entities
        trackedBlockEntities.forEach { (pos, blockEntity) ->
            renderer.renderBlockEntity(graphics, blockEntity, pos, partialTick, mouseX, mouseY)
        }
        ProxyScreen.trackedBlocks.forEach { (pos, _) ->
            Direction.values().forEach { face ->
                val state = renderer.faceStates.getOrDefault(Pair(pos, face), ProxiedType.NONE)
                if (state != ProxiedType.NONE) {
                    renderer.renderBlockOutline(graphics, pos, face, partialTick)
                }
            }
        }

        // Render the outline for the hovered block face
        hoveredBlock?.let { (pos, face) ->
            renderer.renderBlockOutline(graphics, pos, face, partialTick, true)

        }
        renderHoveredFaceInfo(graphics, mouseX, mouseY)

        RenderSystem.disableDepthTest()
        customBackgroundRenderer.renderBackground(
            graphics,
            renderer,
            mouseX,
            mouseY,
            partialTick,
            hoveredBlock?.second,
            hoveredBlock?.first
        )

        val matrixStack = RenderSystem.getModelViewStack()
        matrixStack.popMatrix()
        ClientRuntime.endFrame()
    }

    private fun renderHoveredFaceInfo(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val hoveredFace = renderer.hoveredFace
        if (hoveredFace != null) {
            val (pos, face) = hoveredFace
            val state = renderer.faceStates.getOrDefault(hoveredFace, ProxiedType.NONE)

            ImGui.setNextWindowPos(20f, 20f)
            ImGui.setNextWindowBgAlpha(0.7f)
            ImGui.begin(
                "Hovered Face Info",
                ImGuiWindowFlags.NoMove or ImGuiWindowFlags.NoResize or ImGuiWindowFlags.AlwaysAutoResize or ImGuiWindowFlags.NoTitleBar
            )
            ImGui.text("Position: $pos")
            ImGui.text("Face: $face")
            ImGui.text("State: $state")
            ImGui.end()
        }
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        if (button == 0) {
            renderer.updateRotation(dragX.toFloat(), dragY.toFloat())
            //Takes the cross product of the drag vector to determine the rotation
            val dot = Math.abs(dragX) / (Math.sqrt(dragX * dragX + dragY * dragY) * Math.sqrt(dragX * dragX + dragY * dragY))
            customBackgroundRenderer.updateRotation(dot.toFloat())
            hoveredBlock = null
            isDragging = true
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
    }

    override fun mouseReleased(p_94722_: Double, p_94723_: Double, p_94724_: Int): Boolean {
        isDragging = false
        return super.mouseReleased(p_94722_, p_94723_, p_94724_)
    }

    override fun mouseScrolled(p_94686_: Double, p_94687_: Double, p_94688_: Double, p_294830_: Double): Boolean {
        renderer.updateZoom(p_294830_.toFloat())

        customBackgroundRenderer.updateZoom(p_294830_.toFloat())
        return true
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) { // Left click
            renderer.handleMouseClick(mouseX.toInt(), mouseY.toInt())
            return true
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun onClose() {
        super.onClose()
        renderer.close()
        trackedBlocks.clear()
        trackedBlockEntities.clear()
    }


}