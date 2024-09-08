package bpm.mc.visual

import bpm.client.runtime.ClientRuntime
import com.mojang.blaze3d.systems.RenderSystem
import imgui.ImGui
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

object BlockPreviewScreen : Screen(Component.literal("Block Preview")) {

    val trackedBlocks = mutableMapOf<BlockPos, BlockState>()
    private val trackedBlockEntities = mutableMapOf<BlockPos, BlockEntity>()
    private var renderer: BlockViewRenderer = BlockViewRenderer(Minecraft.getInstance())
    internal var origin: BlockPos = BlockPos.ZERO
    private var hoveredBlock: Pair<BlockPos, Direction>? = null
    private lateinit var customBackgroundRenderer: CustomBackgroundRenderer
    override fun init() {
        super.init()
        renderer = BlockViewRenderer(Minecraft.getInstance())
        customBackgroundRenderer = CustomBackgroundRenderer(minecraft!!)
    }

    fun open(origin: BlockPos, positions: List<BlockPos>) {
        this.origin = origin
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

    private fun renderBackground() {
        //Render a window around the block preview
        ImGui.begin(
            "Block Preview",
            ImGuiWindowFlags.NoResize or ImGuiWindowFlags.NoMove or ImGuiWindowFlags.NoCollapse
        )
        ImGui.text("Block Preview")
        ImGui.end()
    }


    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        ClientRuntime.newFrame()

        RenderSystem.enableDepthTest()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        renderer.updateScreenSpaceBounds(graphics)
        // After rendering all blocks and block entities, determine which one is hovered
        customBackgroundRenderer.renderBackground(graphics, renderer, mouseX, mouseY, partialTick, hoveredBlock?.second, hoveredBlock?.first)
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


        // Render the outline for the hovered block face
        hoveredBlock?.let { (pos, face) ->
            renderer.renderBlockOutline(graphics, pos, face, partialTick)

        }

        RenderSystem.disableDepthTest()
        customBackgroundRenderer.renderBackground(graphics, renderer, mouseX, mouseY, partialTick, hoveredBlock?.second, hoveredBlock?.first)

        val matrixStack = RenderSystem.getModelViewStack()
        matrixStack.popMatrix()
        ClientRuntime.endFrame()

    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        if (button == 0 && hoveredBlock == null) {
            renderer.updateRotation(dragX.toFloat(), dragY.toFloat())
            //Takes the cross product of the drag vector to determine the rotation
            val dot = Math.abs(dragX) / (Math.sqrt(dragX * dragX + dragY * dragY) * Math.sqrt(dragX * dragX + dragY * dragY))
            customBackgroundRenderer.updateRotation(dot.toFloat())
            return true
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
    }

    override fun mouseScrolled(p_94686_: Double, p_94687_: Double, p_94688_: Double, p_294830_: Double): Boolean {
        renderer.updateZoom(p_294830_.toFloat())

        customBackgroundRenderer.updateZoom(p_294830_.toFloat())
        return true
    }

    override fun onClose() {
        super.onClose()
        trackedBlocks.clear()
        trackedBlockEntities.clear()
    }
}