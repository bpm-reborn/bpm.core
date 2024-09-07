package bpm.mc.visual

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL11
import java.nio.ByteBuffer

object BlockPreviewScreen : Screen(Component.literal("Block Preview")) {
    val trackedBlocks = mutableMapOf<BlockPos, BlockState>()
    private val trackedBlockEntities = mutableMapOf<BlockPos, BlockEntity>()
    private var orbitalCamera: OrbitalCamera = OrbitalCamera(Minecraft.getInstance())
    private var origin: BlockPos = BlockPos.ZERO
    private lateinit var pickingRenderTarget: RenderTarget
    private val blockColors = mutableMapOf<BlockPos, Int>()


    override fun init() {
        super.init()
        orbitalCamera = OrbitalCamera(Minecraft.getInstance())
        pickingRenderTarget = TextureTarget(minecraft!!.window.width, minecraft!!.window.height, true, Minecraft.ON_OSX)
        assignColorsToBlocks()
    }
    private fun assignColorsToBlocks() {
        var colorIndex = 1
        trackedBlocks.keys.forEach { pos ->
            blockColors[pos] = colorIndex++
        }
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

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val matrixStack = RenderSystem.getModelViewStack()

        // Render the normal view
        renderNormalView(graphics, partialTick)
        matrixStack.popMatrix()
        // Render the picking view
        renderPickingView(partialTick)

        // Check for hovered block
        val hoveredBlock = getHoveredBlock(mouseX, mouseY)
        hoveredBlock?.let { pos ->
            orbitalCamera.renderBlockOutline(graphics, pos, partialTick)
        }

        // Blit the picking texture to the screen for debug
        blitPickingTextureDebug(graphics)

        RenderSystem.disableDepthTest()
        matrixStack.popMatrix()
    }


    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        if (button == 0) {
            orbitalCamera.updateRotation(dragX.toFloat(), dragY.toFloat())
            return true
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
    }

    override fun mouseScrolled(p_94686_: Double, p_94687_: Double, p_94688_: Double, p_294830_: Double): Boolean {
        orbitalCamera.updateZoom(p_294830_.toFloat())
        return true
    }

    override fun onClose() {
        super.onClose()
        trackedBlocks.clear()
        trackedBlockEntities.clear()
    }
    private fun renderNormalView(graphics: GuiGraphics, partialTick: Float) {
        RenderSystem.enableDepthTest()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        orbitalCamera.setupCamera(graphics.pose(), partialTick)

        trackedBlocks.forEach { (relativePos, blockState) ->
            orbitalCamera.renderBlock(graphics, blockState, relativePos, partialTick)
        }

        trackedBlockEntities.forEach { (relativePos, blockEntity) ->
            orbitalCamera.renderBlockEntity(graphics, blockEntity, relativePos, partialTick)
        }
    }

    private fun renderPickingView(partialTick: Float) {
        pickingRenderTarget.bindWrite(true)
        RenderSystem.clear(16640, Minecraft.ON_OSX)

        val pickingGraphics = GuiGraphics(minecraft, minecraft!!.renderBuffers().bufferSource())
        orbitalCamera.setupCamera(pickingGraphics.pose(), partialTick)

        trackedBlocks.forEach { (relativePos, blockState) ->
            val color = blockColors[relativePos] ?: 0
            orbitalCamera.renderBlockForPicking(pickingGraphics, blockState, relativePos, color)
        }

        pickingRenderTarget.unbindWrite()
        minecraft!!.mainRenderTarget.bindWrite(true)
    }
    private fun blitPickingTextureDebug(graphics: GuiGraphics) {
        RenderSystem.disableDepthTest()
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        RenderSystem.setShaderTexture(0, pickingRenderTarget.colorTextureId)

        RenderSystem.setShader { GameRenderer.getPositionTexColorShader() }
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()

        val matrix4f: Matrix4f = graphics.pose().last().pose()
        val bufferbuilder = Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)

        val width = minecraft!!.window.guiScaledWidth.toFloat()
        val height = minecraft!!.window.guiScaledHeight.toFloat()
        bufferbuilder.addVertex(matrix4f,  0f, height, 0.0f).setUv(0.0f, 0.0f).setColor(255, 255, 255, 255)
        bufferbuilder.addVertex(matrix4f, width, height, 0.0f).setUv(0.0f, 1.0f).setColor(255, 255, 255, 255)
        bufferbuilder.addVertex(matrix4f, width, 0f, 0.0f).setUv(1.0f, 1.0f).setColor(255, 255, 255, 255)
        bufferbuilder.addVertex(matrix4f, 0.0f, 0.0f, 0.0f).setUv(1.0f, 0.0f).setColor(255, 255, 255, 255)
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow())
        RenderSystem.disableBlend()
        RenderSystem.enableDepthTest()
    }
    private fun getHoveredBlock(mouseX: Int, mouseY: Int): BlockPos? {
        val pixelColor = getPixelColor(mouseX, mouseY)

        return blockColors.entries.find { it.value == pixelColor }?.key
    }

    private fun getPixelColor(x: Int, y: Int): Int {
        val buffer = ByteBuffer.allocateDirect(4)
        pickingRenderTarget.bindRead()
        GlStateManager._readPixels(x, pickingRenderTarget.height - y, 1, 1, 6408, 5121, buffer)
        pickingRenderTarget.unbindRead()
        buffer.rewind()
        return buffer.getInt(0) and 0xFFFFFF
    }

}

class OrbitalCamera(private val minecraft: Minecraft) {

    private var rotationX = 30f
    private var rotationY = 45f
    private var zoom = 5f
    private val bufferSource = MultiBufferSource.immediate(ByteBufferBuilder(1536))
    fun renderBlockForPicking(graphics: GuiGraphics, blockState: BlockState, relativePos: BlockPos, color: Int) {
        val pose = graphics.pose()
        pose.pushPose()
        pose.translate(relativePos.x.toFloat(), relativePos.y.toFloat(), relativePos.z.toFloat())

        val blockRenderer = minecraft.blockRenderer
        val model = blockRenderer.getBlockModel(blockState)

        val r = (color shr 16 and 255) / 255f
        val g = (color shr 8 and 255) / 255f
        val b = (color and 255) / 255f

        blockRenderer.modelRenderer.renderModel(
            pose.last(),
            graphics.bufferSource().getBuffer(RenderType.solid()),
            blockState,
            model,
            r, g, b,
            0xF000F0,
            OverlayTexture.NO_OVERLAY,
            net.neoforged.neoforge.client.model.data.ModelData.EMPTY,
            RenderType.solid()
        )

        pose.popPose()
        graphics.bufferSource().endBatch()
    }

    fun setupCamera(pose: PoseStack, partialTicks: Float) {
        val matrixStack = RenderSystem.getModelViewStack()
        pose.pushPose()
        matrixStack.pushMatrix()

        val width = minecraft.window.guiScaledWidth
        val height = minecraft.window.guiScaledHeight

        pose.setIdentity()
        val center = getCenterOfBlocks()
        pose.translate(width / 2f, height / 2f, 1000f - zoom * 100f)
        pose.scale(24 * zoom, -24 * zoom, 24 * zoom)
        pose.mulPose(Axis.XP.rotationDegrees(rotationX))
        pose.mulPose(Axis.YP.rotationDegrees(rotationY))
        pose.translate(-center.x, -center.y, -center.z)
        pose.translate(-0.5, -0.5, -0.5)
        RenderSystem.applyModelViewMatrix()
    }

    private fun getCenterOfBlocks(): Vector3f {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        var maxZ = Float.MIN_VALUE

        BlockPreviewScreen.trackedBlocks.keys.forEach { pos ->
            minX = minOf(minX, pos.x.toFloat())
            minY = minOf(minY, pos.y.toFloat())
            minZ = minOf(minZ, pos.z.toFloat())
            maxX = maxOf(maxX, pos.x.toFloat())
            maxY = maxOf(maxY, pos.y.toFloat())
            maxZ = maxOf(maxZ, pos.z.toFloat())
        }

        return Vector3f(
            (minX + maxX) / 2f,
            (minY + maxY) / 2f,
            (minZ + maxZ) / 2f
        )
    }

    fun renderBlock(graphics: GuiGraphics, blockState: BlockState, relativePos: BlockPos, partialTicks: Float) {
        val pose = graphics.pose()
        pose.pushPose()
        pose.translate(relativePos.x.toFloat(), relativePos.y.toFloat(), relativePos.z.toFloat())

        val blockRenderer = minecraft.blockRenderer
        val model = blockRenderer.getBlockModel(blockState)

        val light = 0xF000F0
        val overlay = OverlayTexture.NO_OVERLAY

        blockRenderer.modelRenderer.renderModel(
            pose.last(),
            bufferSource.getBuffer(RenderType.solid()),
            blockState,
            model,
            1f, 1f, 1f,
            light,
            overlay,
            net.neoforged.neoforge.client.model.data.ModelData.EMPTY,
            RenderType.solid()
        )

        pose.popPose()
        bufferSource.endBatch()
    }

    private fun renderCuboidLines(
        buffer: VertexConsumer,
        matrix: Matrix4f,
        minX: Float,
        minY: Float,
        minZ: Float,
        maxX: Float,
        maxY: Float,
        maxZ: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {       // Bottom face
        buffer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a).setNormal(0f, -1f, 0f)
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a).setNormal(0f, -1f, 0f)
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a).setNormal(0f, -1f, 0f)
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a).setNormal(0f, -1f, 0f)
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a).setNormal(0f, -1f, 0f)
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a).setNormal(0f, -1f, 0f)
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a).setNormal(0f, -1f, 0f)
        buffer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a).setNormal(0f, -1f, 0f)

        // Top face
        buffer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a).setNormal(0f, 1f, 0f)
        buffer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a).setNormal(0f, 1f, 0f)
        buffer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a).setNormal(0f, 1f, 0f)
        buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a).setNormal(0f, 1f, 0f)
        buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a).setNormal(0f, 1f, 0f)
        buffer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a).setNormal(0f, 1f, 0f)
        buffer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a).setNormal(0f, 1f, 0f)
        buffer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a).setNormal(0f, 1f, 0f)

        // Vertical edges
        buffer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a).setNormal(-1f, 0f, -1f)
        buffer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a).setNormal(-1f, 0f, -1f)
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a).setNormal(1f, 0f, -1f)
        buffer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a).setNormal(1f, 0f, -1f)
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a).setNormal(1f, 0f, 1f)
        buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a).setNormal(1f, 0f, 1f)
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a).setNormal(-1f, 0f, 1f)
        buffer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a).setNormal(-1f, 0f, 1f)
    }

    fun renderBlockOutline(graphics: GuiGraphics, pos: BlockPos, partialTicks: Float) {
        val pose = graphics.pose()
        pose.pushPose()
        pose.translate(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())

        RenderSystem.setShader(GameRenderer::getPositionColorShader)
        RenderSystem.disableDepthTest()
        RenderSystem.lineWidth(2.0f)

        val bufferSource = minecraft.renderBuffers().bufferSource()
        val buffer = bufferSource.getBuffer(RenderType.lines())

        val matrix4f = pose.last().pose()
        val red = 1.0f
        val green = 1.0f
        val blue = 1.0f
        val alpha = 1.0f

        renderCuboidLines(buffer, matrix4f, 0f, 0f, 0f, 1f, 1f, 1f, red, green, blue, alpha)

        bufferSource.endBatch(RenderType.lines())
        RenderSystem.enableDepthTest()
        pose.popPose()
    }
    private fun rayIntersectsAABB(start: Vec3, end: Vec3, aabb: AABB): Vec3? {
        val dir = end.subtract(start).normalize()
        val invDir = Vec3(1.0 / dir.x, 1.0 / dir.y, 1.0 / dir.z)

        val t1 = (aabb.minX - start.x) * invDir.x
        val t2 = (aabb.maxX - start.x) * invDir.x
        val t3 = (aabb.minY - start.y) * invDir.y
        val t4 = (aabb.maxY - start.y) * invDir.y
        val t5 = (aabb.minZ - start.z) * invDir.z
        val t6 = (aabb.maxZ - start.z) * invDir.z

        val tmin = maxOf(minOf(t1, t2), minOf(t3, t4), minOf(t5, t6))
        val tmax = minOf(maxOf(t1, t2), maxOf(t3, t4), maxOf(t5, t6))

        if (tmax < 0 || tmin > tmax) {
            return null
        }

        val t = if (tmin < 0) tmax else tmin
        return start.add(dir.scale(t))
    }


    fun getHoveredBlock(mouseX: Int, mouseY: Int): BlockPos? {
        val ray = getRayFromMouse(mouseX, mouseY)
        return ray?.let { (start, end) ->
            var closestBlock: BlockPos? = null
            var closestDistance = Double.MAX_VALUE

            for ((pos, blockState) in BlockPreviewScreen.trackedBlocks) {
                val shapes = blockState.getShape(minecraft.level, pos).toAabbs()
                for (shape in shapes) {
                    val aabb = shape.move(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
                    val hit = rayIntersectsAABB(start, end, aabb)
                    if (hit != null) {
                        val distance = start.distanceToSqr(hit)
                        if (distance < closestDistance) {
                            closestDistance = distance
                            closestBlock = pos
                        }
                    }
                }
            }
            closestBlock
        }
    }

    fun getRayFromMouse(mouseX: Int, mouseY: Int): Pair<Vec3, Vec3>? {
        val viewport = minecraft.window
        val screenPos = Vector3f(
            (mouseX.toFloat() / viewport.guiScaledWidth) * 2f - 1f,
            1f - (mouseY.toFloat() / viewport.guiScaledHeight) * 2f,
            -1f
        )

        val inverseProjection = Matrix4f(RenderSystem.getProjectionMatrix()).invert()
        val inverseView = Matrix4f(RenderSystem.getModelViewMatrix()).invert()

        val toWorld = Vector4f(screenPos.x, screenPos.y, screenPos.z, 1f)
        toWorld.mul(inverseProjection)
        toWorld.mul(inverseView)

        if (toWorld.w == 0f) return null

        toWorld.div(toWorld.w)

        val rayStart = Vec3(0.0, 0.0, 0.0)
        val rayDir = Vec3(toWorld.x.toDouble(), toWorld.y.toDouble(), toWorld.z.toDouble()).normalize()
        val rayEnd = rayStart.add(rayDir.scale(1000.0))

        return Pair(rayStart, rayEnd)
    }
    fun renderBlockEntity(graphics: GuiGraphics, blockEntity: BlockEntity, relativePos: BlockPos, partialTicks: Float) {
        val pose = graphics.pose()
        pose.pushPose()
        pose.translate(relativePos.x.toFloat(), relativePos.y.toFloat(), relativePos.z.toFloat())

        val blockEntityRenderer = minecraft.blockEntityRenderDispatcher.getRenderer(blockEntity)
        blockEntityRenderer?.render(blockEntity, partialTicks, pose, bufferSource, 0xF000F0, OverlayTexture.NO_OVERLAY)

        pose.popPose()
        bufferSource.endBatch()
    }

    fun updateRotation(dx: Float, dy: Float) {
        rotationY += dx * 0.5f
        rotationX += dy * 0.5f
        rotationX = rotationX.coerceIn(-90f, 90f)
    }

    fun updateZoom(dZoom: Float) {
        zoom -= dZoom * 0.5f
        zoom = zoom.coerceIn(2f, 10f)
    }
}