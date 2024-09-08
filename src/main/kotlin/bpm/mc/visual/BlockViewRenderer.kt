package bpm.mc.visual

import bpm.client.runtime.ClientRuntime
import bpm.client.runtime.Platform
import bpm.common.network.Client
import bpm.mc.visual.ProxyScreen.customBackgroundRenderer
import bpm.pipe.proxy.PacketProxyUpdate
import bpm.pipe.proxy.ProxiedType
import bpm.pipe.proxy.ProxyState
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import com.mojang.math.Axis
import imgui.ImGui
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.debug.DebugRenderer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.glfw.GLFW
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.toVector3f

class BlockViewRenderer(private val minecraft: Minecraft) {
    private data class SortedAABB(val blockPos: BlockPos, val aabb: AABB, val zDepth: Double, val face: Direction?)


    // Add these properties to BlockViewRenderer class
    internal val faceStates = mutableMapOf<Pair<BlockPos, Direction>, ProxiedType>()
    internal var hoveredFace: Pair<BlockPos, Direction>? = null

    private val sortedScreenSpaceAABBs = mutableListOf<SortedAABB>()
    internal val centers = mutableMapOf<BlockPos, Vector2f>()
    private var rotationX = 30f
    private var rotationY = 45f
    private var zoom = 5f
    private val bufferSource = MultiBufferSource.immediate(ByteBufferBuilder(1536))
    var minScreenX = Float.MAX_VALUE
        private set
    var minScreenY = Float.MAX_VALUE
        private set
    var maxScreenX = Float.MIN_VALUE
        private set
    var maxScreenY = Float.MIN_VALUE
        private set

    private val dirtTexture: ResourceLocation = TextureAtlas.LOCATION_BLOCKS
    private val backgroundRenderType: RenderType = RenderType.create(
        "background_dirt",
        DefaultVertexFormat.POSITION_TEX,
        VertexFormat.Mode.QUADS,
        256,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.POSITION_TEX_SHADER)
            .setTextureState(RenderStateShard.TextureStateShard(dirtTexture, false, false))
            .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
            .setLightmapState(RenderStateShard.NO_LIGHTMAP)
            .createCompositeState(false)
    )

    private val customRenderType: RenderType = RenderType.create(
        "custom_face_highlight",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        256,
        false,
        true,
        RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
            .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .setCullState(RenderStateShard.NO_CULL)
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
            .createCompositeState(false)
    )

    fun setupCamera(pose: PoseStack, partialTicks: Float) {
        val matrixStack = RenderSystem.getModelViewStack()
        pose.pushPose()
        matrixStack.pushMatrix()

        val width = minecraft.window.guiScaledWidth
        val height = minecraft.window.guiScaledHeight

        pose.setIdentity()
        val center = getCenterOfBlocks()
        pose.translate(width / 2f, height / 2f, 0f)
        pose.scale(24 * zoom, -24 * zoom, 24 * zoom)
        pose.mulPose(Axis.XP.rotationDegrees(rotationX))
        pose.mulPose(Axis.YP.rotationDegrees(rotationY))
        pose.translate(-center.x, -center.y, -center.z)
        pose.translate(-0.5, -0.5, -0.5)
        RenderSystem.applyModelViewMatrix()
    }


    fun renderBackground(graphics: GuiGraphics) {
        val pose = graphics.pose()

        val width = minecraft.window.guiScaledWidth
        val height = minecraft.window.guiScaledHeight

        pose.setIdentity()
        val centeredPosX = minScreenX + (maxScreenX - minScreenX) / 2
        val centeredPosY = minScreenY + (maxScreenY - minScreenY) / 2
        pose.translate(centeredPosX, centeredPosY, -1000f)
        RenderSystem.setShader(GameRenderer::getPositionTexShader)
        RenderSystem.setShaderTexture(0, dirtTexture)
        RenderSystem.enableDepthTest()
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()

        val buffer = bufferSource.getBuffer(backgroundRenderType)

        val blockRenderer = minecraft.blockRenderer
        val model = blockRenderer.getBlockModel(Blocks.DIRT.defaultBlockState())
        val dirtSprite = model.getParticleIcon()

        // Expand the background slightly
        val padding = 10f
        buffer.addVertex(pose.last().pose(), minScreenX - padding, minScreenY - padding, 0f)
            .setUv(dirtSprite.u0, dirtSprite.v0)
        buffer.addVertex(pose.last().pose(), minScreenX - padding, maxScreenY + padding, 0f)
            .setUv(dirtSprite.u0, dirtSprite.v1)
        buffer.addVertex(pose.last().pose(), maxScreenX + padding, maxScreenY + padding, 0f)
            .setUv(dirtSprite.u1, dirtSprite.v1)
        buffer.addVertex(pose.last().pose(), maxScreenX + padding, minScreenY - padding, 0f)
            .setUv(dirtSprite.u1, dirtSprite.v0)

        bufferSource.endBatch(backgroundRenderType)

        RenderSystem.disableDepthTest()
        RenderSystem.disableBlend()
        pose.popPose()
    }

    fun updateScreenSpaceBounds(graphics: GuiGraphics) {
        val pose = graphics.pose()
        val modelViewMatrix = Matrix4f(pose.last().pose())
        val projectionMatrix = RenderSystem.getProjectionMatrix()
        val mvpMatrix = Matrix4f(projectionMatrix).mul(modelViewMatrix)

        minScreenX = Float.MAX_VALUE
        minScreenY = Float.MAX_VALUE
        maxScreenX = Float.MIN_VALUE
        maxScreenY = Float.MIN_VALUE

        ProxyScreen.trackedBlocks.keys.forEach { pos ->
            val aabb = AABB(pos)
            val screenSpaceAABB = transformAABBToScreenSpace(aabb, mvpMatrix)

            minScreenX = minOf(minScreenX, screenSpaceAABB.minX.toFloat())
            minScreenY = minOf(minScreenY, screenSpaceAABB.minY.toFloat())
            maxScreenX = maxOf(maxScreenX, screenSpaceAABB.maxX.toFloat())
            maxScreenY = maxOf(maxScreenY, screenSpaceAABB.maxY.toFloat())
        }

        // Convert to screen coordinates
        val window = minecraft.window
        val scaledWidth = window.guiScaledWidth.toFloat()
        val scaledHeight = window.guiScaledHeight.toFloat()

        minScreenX = (minScreenX + 1f) / 2f * scaledWidth
        minScreenY = (1f - maxScreenY) / 2f * scaledHeight
        maxScreenX = (maxScreenX + 1f) / 2f * scaledWidth
        maxScreenY = (1f - minScreenY) / 2f * scaledHeight
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

    fun computeBounds(padding: Double = 0.0): AABB {
        if (sortedScreenSpaceAABBs.isEmpty()) {
            return AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY

        for (aabb in sortedScreenSpaceAABBs) {
            minX = minOf(minX, aabb.aabb.minX)
            minY = minOf(minY, aabb.aabb.minY)
            maxX = maxOf(maxX, aabb.aabb.maxX)
            maxY = maxOf(maxY, aabb.aabb.maxY)
        }

        // Add padding
        minX -= padding
        minY -= padding
        maxX += padding
        maxY += padding

        return AABB(minX, minY, 0.0, maxX, maxY, 0.0)
    }

    fun renderBlockOutline(
        graphics: GuiGraphics,
        pos: BlockPos,
        face: Direction?,
        partialTicks: Float,
        hovered: Boolean = false
    ) {
        if (face == null) return

        val pose = graphics.pose()
        pose.pushPose()
        pose.translate(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())

        RenderSystem.setShader(GameRenderer::getPositionColorShader)
        RenderSystem.disableDepthTest()
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()

        val buffer = bufferSource.getBuffer(customRenderType)

        val matrix4f = pose.last().pose()
        val state = faceStates.getOrDefault(Pair(pos, face), ProxiedType.NONE)
        val isHovered = (hoveredFace == Pair(pos, face) || hovered) && !ProxyScreen.isDragging


//        if(isHovered) {
//            val center = centers[pos] ?: return
//            val font = minecraft.font
//            var text = "Face: $face\nState: $state"
//            val state = faceStates.getOrDefault(Pair(pos, face), ProxiedType.NONE)
//             text += "\nPos: $pos\nFace: $face\nState: $state"
//
//            // Calculate the position for the text (centered on the face)
//            val textPos = when (face) {
//                Direction.DOWN -> Vec3(0.5, 0.0, 0.5)
//                Direction.UP -> Vec3(0.5, 1.0, 0.5)
//                Direction.NORTH -> Vec3(0.5, 0.5, 0.0)
//                Direction.SOUTH -> Vec3(0.5, 0.5, 1.0)
//                Direction.WEST -> Vec3(0.0, 0.5, 0.5)
//                Direction.EAST -> Vec3(1.0, 0.5, 0.5)
//            }
//

//        }


        val color = when {
            isHovered -> Vector4f(1f, 1f, 1f, 0.7f) // White with high alpha for hover
            state == ProxiedType.NONE -> Vector4f(0f, 0f, 0f, 0f) // Transparent for NONE state
            state == ProxiedType.INPUT -> Vector4f(0f, 1f, 0f, 0.4f) // Green for INPUT
            state == ProxiedType.OUTPUT -> Vector4f(1f, 0f, 0f, 0.4f) // Red for OUTPUT
            state == ProxiedType.BOTH -> Vector4f(1f, 1f, 0f, 0.4f) // Yellow for BOTH
            else -> Vector4f(0f, 0f, 0f, 0f) // Fallback transparent
        }

        // Slight expansion to avoid z-fighting
        val eps = 0.02f
        val min = 0f - eps
        val max = 1f + eps

        renderFaceOutline(buffer, matrix4f, face, min, max, color.x(), color.y(), color.z(), color.w())

        bufferSource.endBatch(customRenderType)

        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
        pose.popPose()

        hoveredFace = if (isHovered) Pair(pos, face) else null
    }

    private fun renderFaceOutline(
        buffer: VertexConsumer,
        matrix: Matrix4f,
        face: Direction,
        min: Float,
        max: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        val normal = face.normal.toVector3f()
        when (face) {
            Direction.DOWN -> renderFaceQuad(buffer, matrix, min, min, min, max, min, max, normal, r, g, b, a)
            Direction.UP -> renderFaceQuad(buffer, matrix, min, max, min, max, max, max, normal, r, g, b, a)
            Direction.NORTH -> renderFaceQuad(buffer, matrix, min, min, min, max, max, min, normal, r, g, b, a)
            Direction.SOUTH -> renderFaceQuad(buffer, matrix, min, min, max, max, max, max, normal, r, g, b, a)
            Direction.WEST -> renderFaceQuad(buffer, matrix, min, min, min, min, max, max, normal, r, g, b, a)
            Direction.EAST -> renderFaceQuad(buffer, matrix, max, min, min, max, max, max, normal, r, g, b, a)
        }
    }

    private fun renderBillboardText(
        poseStack: PoseStack,
        text: String,
        x: Float,
        y: Float,
        z: Float,
        partialTicks: Float,
        font: Font,
        bufferSource: MultiBufferSource,
        color: Int = 0xFFFFFF
    ) {
        DebugRenderer.renderFloatingText(
            poseStack, bufferSource, text,
            x.toDouble(), y.toDouble(), z.toDouble(), color, 0.02f, true, 0f, false
        )
    }

    // Add this function to BlockViewRenderer class
    fun handleMouseClick(mouseX: Int, mouseY: Int) {
        val hoveredBlockFace = finalizeSortingAndDetermineHoveredBlock(mouseX, mouseY) ?: return

        var controlClicked = false
        val (pos, face) = hoveredBlockFace
        if (Platform.isKeyDown(ClientRuntime.Key.LEFT_CONTROL) || Platform.isKeyDown(ClientRuntime.Key.RIGHT_CONTROL) || Platform.isKeyDown(
                ClientRuntime.Key.LEFT_SUPER
            )
        ) {

            if (faceStates[hoveredBlockFace] != ProxiedType.NONE) {
                customBackgroundRenderer.onFaceClick()
            }
            for (face in Direction.values()) {
                val key = Pair(pos, face)
                faceStates[key] = when (faceStates[key]) {
                    null, ProxiedType.NONE -> ProxiedType.INPUT
                    ProxiedType.INPUT -> ProxiedType.OUTPUT
                    ProxiedType.OUTPUT -> ProxiedType.BOTH
                    ProxiedType.BOTH -> ProxiedType.NONE
                }
                if (faceStates[key] != ProxiedType.NONE) {
                    customBackgroundRenderer.onFaceClick()
                }
            }
            controlClicked = true
        }
        if (!controlClicked) {
            val key = Pair(pos, face)
            faceStates[key] = when (faceStates[key]) {
                null, ProxiedType.NONE -> ProxiedType.INPUT
                ProxiedType.INPUT -> ProxiedType.OUTPUT
                ProxiedType.OUTPUT -> ProxiedType.BOTH
                ProxiedType.BOTH -> ProxiedType.NONE
            }

            if (faceStates[key] != ProxiedType.NONE) {
                customBackgroundRenderer.onFaceClick()
            }
        }
        //If shifts down, set all to none, if all are none, cycle to input, if input, cycle to output, if output, cycle to both
        if (ImGui.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || ImGui.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            //If all the faces are the same, cycle to the next ordinal value
            val allSame = faceStates.filter { it.key.first == hoveredBlockFace?.first }
                .all { it.value == faceStates[hoveredBlockFace] }
            if (allSame) {
                faceStates.filter { it.key.first == hoveredBlockFace?.first }.forEach { (key, _) ->
                    faceStates[key] = when (faceStates[key]) {
                        ProxiedType.NONE -> ProxiedType.INPUT
                        ProxiedType.INPUT -> ProxiedType.OUTPUT
                        ProxiedType.OUTPUT -> ProxiedType.BOTH
                        ProxiedType.BOTH -> ProxiedType.NONE
                        null -> ProxiedType.NONE
                    }
                }
            } else {
                //Set all to none
                faceStates.filter { it.key.first == hoveredBlockFace?.first }.forEach { (key, _) ->
                    faceStates[key] = ProxiedType.NONE
                }
            }
            if (faceStates[hoveredBlockFace] != ProxiedType.NONE) {
                customBackgroundRenderer.onFaceClick()
            }
        }

        //Update the server proxy state
        Client.send(stateToPacket(pos))
    }


    private fun stateToPacket(blockPos: BlockPos): PacketProxyUpdate {
        val origin = ProxyScreen.origin
        val states = faceStates.filter { it.key.first == blockPos }.map { it.key.second to it.value }.toMap()
        val packet = PacketProxyUpdate(origin)
        packet.proxiedState.relativePos = blockPos
        for ((face, state) in states) {
            packet.proxiedState.setProxiedType(face, state)
        }
        return packet
    }

    private fun renderFaceQuad(
        buffer: VertexConsumer,
        matrix: Matrix4f,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        normal: Vector3f,
        r: Float, g: Float, b: Float, a: Float
    ) {
        when {
            normal.y != 0f -> {
                // UP or DOWN face
                buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a)
                    .setNormal(normal.x(), normal.y(), normal.z())
                buffer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, a)
                    .setNormal(normal.x(), normal.y(), normal.z())
                buffer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, a)
                    .setNormal(normal.x(), normal.y(), normal.z())
                buffer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, a)
                    .setNormal(normal.x(), normal.y(), normal.z())
            }

            normal.x != 0f -> {
                // EAST or WEST face
                buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a)
                    .setNormal(normal.x(), normal.y(), normal.z())
                buffer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a)
                    .setNormal(normal.x(), normal.y(), normal.z())
                buffer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, a)
                    .setNormal(normal.x(), normal.y(), normal.z())
                buffer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, a)
                    .setNormal(normal.x(), normal.y(), normal.z())
            }

            else -> {
                // NORTH or SOUTH face
                buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a)
                    .setNormal(normal.x(), normal.y(), normal.z())
                buffer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a)
                    .setNormal(normal.x(), normal.y(), normal.z())
                buffer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, a)
                    .setNormal(normal.x(), normal.y(), normal.z())
                buffer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, a)
                    .setNormal(normal.x(), normal.y(), normal.z())
            }
        }
    }

    private fun getFaceAABB(face: Direction): AABB {
        return when (face) {
            Direction.DOWN -> AABB(0.0, 0.0, 0.0, 1.0, 0.0, 1.0)
            Direction.UP -> AABB(0.0, 1.0, 0.0, 1.0, 1.0, 1.0)
            Direction.NORTH -> AABB(0.0, 0.0, 0.0, 1.0, 1.0, 0.0)
            Direction.SOUTH -> AABB(0.0, 0.0, 1.0, 1.0, 1.0, 1.0)
            Direction.WEST -> AABB(0.0, 0.0, 0.0, 0.0, 1.0, 1.0)
            Direction.EAST -> AABB(1.0, 0.0, 0.0, 1.0, 1.0, 1.0)
        }
    }

    private fun getFaceCenter(pos: BlockPos, face: Direction): Vec3 {
        val x = pos.x + 0.5 + face.stepX * 0.5
        val y = pos.y + 0.5 + face.stepY * 0.5
        val z = pos.z + 0.5 + face.stepZ * 0.5
        return Vec3(x, y, z)
    }


    private fun transformAABBToScreenSpace(aabb: AABB, mvpMatrix: Matrix4f): AABB {
        val corners = listOf(
            Vector4f(aabb.minX.toFloat(), aabb.minY.toFloat(), aabb.minZ.toFloat(), 1f),
            Vector4f(aabb.maxX.toFloat(), aabb.minY.toFloat(), aabb.minZ.toFloat(), 1f),
            Vector4f(aabb.minX.toFloat(), aabb.maxY.toFloat(), aabb.minZ.toFloat(), 1f),
            Vector4f(aabb.maxX.toFloat(), aabb.maxY.toFloat(), aabb.minZ.toFloat(), 1f),
            Vector4f(aabb.minX.toFloat(), aabb.minY.toFloat(), aabb.maxZ.toFloat(), 1f),
            Vector4f(aabb.maxX.toFloat(), aabb.minY.toFloat(), aabb.maxZ.toFloat(), 1f),
            Vector4f(aabb.minX.toFloat(), aabb.maxY.toFloat(), aabb.maxZ.toFloat(), 1f),
            Vector4f(aabb.maxX.toFloat(), aabb.maxY.toFloat(), aabb.maxZ.toFloat(), 1f)
        )

        val transformedCorners = corners.map { corner ->
            corner.mul(mvpMatrix)
            corner.div(corner.w)
            Vector3f(corner.x, corner.y, corner.z)
        }

        val minX = transformedCorners.minOf { it.x }
        val minY = transformedCorners.minOf { it.y }
        val maxX = transformedCorners.maxOf { it.x }
        val maxY = transformedCorners.maxOf { it.y }

        return AABB(minX.toDouble(), minY.toDouble(), 0.0, maxX.toDouble(), maxY.toDouble(), 1.0)
    }

    private fun isPointInsideAABB(x: Int, y: Int, aabb: AABB): Boolean {
        return x >= aabb.minX && x <= aabb.maxX && y >= aabb.minY && y <= aabb.maxY
    }

    private fun getViewMatrix(): Matrix4f {
        val viewMatrix = Matrix4f()
        val center = getCenterOfBlocks()

        viewMatrix.translate(0f, 0f, -zoom)
        viewMatrix.rotate(org.joml.Quaternionf().rotationX(Math.toRadians(rotationX.toDouble()).toFloat()))
        viewMatrix.rotate(org.joml.Quaternionf().rotationY(Math.toRadians(rotationY.toDouble()).toFloat()))
        viewMatrix.translate(-center.x.toFloat(), -center.y.toFloat(), -center.z.toFloat())


        return viewMatrix
    }


    private fun getCenterOfBlocks(): Vec3 {
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var minZ = Double.MAX_VALUE
        var maxX = Double.MIN_VALUE
        var maxY = Double.MIN_VALUE
        var maxZ = Double.MIN_VALUE

        ProxyScreen.trackedBlocks.keys.forEach { pos ->
            minX = minOf(minX, pos.x.toDouble())
            minY = minOf(minY, pos.y.toDouble())
            minZ = minOf(minZ, pos.z.toDouble())
            maxX = maxOf(maxX, pos.x.toDouble())
            maxY = maxOf(maxY, pos.y.toDouble())
            maxZ = maxOf(maxZ, pos.z.toDouble())
        }

        return Vec3(
            (minX + maxX) / 2,
            (minY + maxY) / 2,
            (minZ + maxZ) / 2
        )
    }


    fun finalizeSortingAndDetermineHoveredBlock(mouseX: Int, mouseY: Int): Pair<BlockPos, Direction>? {
        // Sort the AABBs based on Z-depth (front to back)
        sortedScreenSpaceAABBs.sortBy { it.zDepth }

        // Find the first (frontmost) AABB that contains the mouse position
        val hoveredAABB = sortedScreenSpaceAABBs.firstOrNull { isPointInsideAABB(mouseX, mouseY, it.aabb) }

        // Clear the list for the next frame
        val result = hoveredAABB?.let { Pair(it.blockPos, it.face ?: Direction.NORTH) }
        sortedScreenSpaceAABBs.clear()

        return result
    }


    fun renderBlockEntity(
        graphics: GuiGraphics,
        blockEntity: BlockEntity,
        relativePos: BlockPos,
        partialTicks: Float,
        mouseX: Int,
        mouseY: Int
    ) {
        val pose = graphics.pose()
        pose.pushPose()
        pose.translate(relativePos.x.toFloat(), relativePos.y.toFloat(), relativePos.z.toFloat())

        val modelViewMatrix = Matrix4f(pose.last().pose())
        val projectionMatrix = RenderSystem.getProjectionMatrix()
        val mvpMatrix = Matrix4f(projectionMatrix).mul(modelViewMatrix)

        // Render each face separately
        Direction.values().forEach { face ->
            val aabb = getFaceAABB(face)
            val screenSpaceAABB = transformAABBToScreenSpace(aabb, mvpMatrix)

            // Get the actual screen dimensions and scale factor
            val window = minecraft.window
            val scaledWidth = window.guiScaledWidth.toDouble()
            val scaledHeight = window.guiScaledHeight.toDouble()
            val scaleFactor = window.guiScale.toDouble()

            // Convert NDC coordinates to screen coordinates, considering the scale factor
            val ndcMinecraft = AABB(
                (screenSpaceAABB.minX + 1.0) / 2.0 * scaledWidth,
                (1.0 - screenSpaceAABB.maxY) / 2.0 * scaledHeight,
                screenSpaceAABB.minZ,
                (screenSpaceAABB.maxX + 1.0) / 2.0 * scaledWidth,
                (1.0 - screenSpaceAABB.minY) / 2.0 * scaledHeight,
                screenSpaceAABB.maxZ
            )

            val realNdc = AABB(
                (screenSpaceAABB.minX + 1.0) / 2.0 * window.screenWidth,
                (1.0 - screenSpaceAABB.maxY) / 2.0 * window.screenHeight,
                screenSpaceAABB.minZ,
                (screenSpaceAABB.maxX + 1.0) / 2.0 * window.screenWidth,
                (1.0 - screenSpaceAABB.minY) / 2.0 * window.screenHeight,
                screenSpaceAABB.maxZ
            )

            // Calculate Z-depth (use the center of the face for consistent ordering)
            val centerPos = getFaceCenter(relativePos, face)
            val transformedCenter = mvpMatrix.transform(
                Vector4f(
                    centerPos.x.toFloat(),
                    centerPos.y.toFloat(),
                    centerPos.z.toFloat(),
                    1f
                )
            )
            val zDepth = transformedCenter.z / transformedCenter.w

            sortedScreenSpaceAABBs.add(SortedAABB(relativePos, ndcMinecraft, zDepth.toDouble(), face))


            centers[relativePos] = Vector2f(
                ndcMinecraft.minX.toFloat() + (ndcMinecraft.maxX.toFloat() - ndcMinecraft.minX.toFloat()) / 2f,
                ndcMinecraft.minY.toFloat() + (ndcMinecraft.maxY.toFloat() - ndcMinecraft.minY.toFloat()) / 2f
            )
        }

        // Render the block entity
        val blockEntityRenderer = minecraft.blockEntityRenderDispatcher.getRenderer(blockEntity)
        blockEntityRenderer?.render(
            blockEntity,
            partialTicks,
            pose,
            bufferSource,
            0xF000F0,
            OverlayTexture.NO_OVERLAY
        )

        pose.popPose()
        bufferSource.endBatch()
    }


    fun updateRotation(dx: Float, dy: Float) {
        rotationY += dx * 0.5f
        rotationX += dy * 0.5f
        rotationX = rotationX.coerceIn(-90f, 90f)
    }

    fun updateZoom(dZoom: Float) {
        zoom += dZoom * 0.5f
        zoom = zoom.coerceIn(2f, 8f)
    }

    fun updateProxiedState(state: ProxyState) {
        faceStates.clear()
        for ((relativePos, proxiedType) in state.proxiedBlocks) {
            for ((face, type) in proxiedType.proxiedFaces) {
                faceStates[Pair(relativePos, face)] = type
            }
        }
    }

    companion object {

        private val savedRotationStates = mutableMapOf<BlockPos, Triple<Float, Float, Float>>()
    }

    private var origin = BlockPos.ZERO

    fun open(origin: BlockPos) {
        this.origin = origin
        rotationX = savedRotationStates[origin]?.first ?: 0f
        rotationY = savedRotationStates[origin]?.second ?: 0f
        zoom = savedRotationStates[origin]?.third ?: 1f
    }

    fun close() {
        savedRotationStates[origin] = Triple(rotationX, rotationY, zoom)
    }
}