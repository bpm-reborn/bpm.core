package bpm.mc.visual

import bpm.common.network.Listener
import bpm.common.packets.Packet
import bpm.mc.links.EnderControllerState
import bpm.mc.links.EnderNet
import bpm.mc.links.EnderNetState
import bpm.mc.selection.SelectionManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.resources.model.Material
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import org.joml.Matrix4f
import org.joml.Vector3f
import java.awt.Color
import java.util.*
import kotlin.math.abs
import kotlin.math.sin

object Overlay3D : Listener {

    private val state: EnderNetState by lazy { EnderNet.client.state }
    private var time = 0f

    private val COLOR_1 = Color(64, 224, 208)  // Turquoise
    private val COLOR_2 = Color(147, 112, 219) // Purple
    private val SELECTION_COLOR = Color(255, 60, 0) // Bright orange-red for selections
    private val CORNER_COLOR = Color(255, 215, 0, 200) // Gold color with transparency for corners
    private val CONCRETE_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "block/amethyst_block")
    private val concreteMaterial by lazy {
        Material(TextureAtlas.LOCATION_BLOCKS, CONCRETE_TEXTURE)
    }

    private fun lerpColor(time: Float): Color {
        val factor = (sin(time * 2f) + 1f) / 2f
        return Color(
            (COLOR_1.red + (COLOR_2.red - COLOR_1.red) * factor).toInt().coerceIn(0, 255),
            (COLOR_1.green + (COLOR_2.green - COLOR_1.green) * factor).toInt().coerceIn(0, 255),
            (COLOR_1.blue + (COLOR_2.blue - COLOR_1.blue) * factor).toInt().coerceIn(0, 255)
        )
    }

    fun render(
        renderer: LevelRenderer,
        stack: PoseStack,
        projectionMatrix: Matrix4f,
        viewMatrix: Matrix4f,
        camera: Camera,
        frustum: Frustum,
        bufferSource: MultiBufferSource
    ) {
        time += 0.016f

        stack.pushPose()
        val cameraPos = camera.position
        stack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z)

        // Render EnderNet controllers and links
        state.controllers.values.forEach { controller ->
            renderController(controller, stack, bufferSource)
            renderLinks(controller, stack, bufferSource)
        }

        // Render selections
        renderSelections(stack, bufferSource)

        stack.popPose()
    }

    private fun renderSelections(stack: PoseStack, bufferSource: MultiBufferSource) {
        val matrix = stack.last().pose()
        val selectionColor = SELECTION_COLOR
        val pulsingFactor = (sin(time * 3f) + 1f) / 2f
        val alpha = (128 + (127 * pulsingFactor).toInt())
        val colorWithAlpha = Color(255,255,255,255)

        SelectionManager.client.getAllSelections().forEach { selection ->
            when (selection.selectionType) {
                SelectionManager.SelectionType.BLOCK -> {
                    selection.blockPos?.let { pos ->
                        // Use the fixed renderer with 2-pixel thick lines
                        renderCubeOutline(
                            bufferSource,
                            matrix,
                            pos.x + 0.5f,
                            pos.y + 0.5f,
                            pos.z + 0.5f,
                            0.51f, // slightly larger than block
                            colorWithAlpha,
                            1.5f // 2 pixels thick
                        )

                        // Render corner cubes at the selection position
                        renderCornerCubes(
                            bufferSource,
                            matrix,
                            pos.x + 0.5f,
                            pos.y + 0.5f,
                            pos.z + 0.5f,
                            0.51f, // corner cube size
                            0.05f, // corner cube size
                            colorWithAlpha
                        )
                    }
                }

                SelectionManager.SelectionType.ENTITY -> {
                    // Entity rendering would go here
                }

                SelectionManager.SelectionType.AREA -> {
                    selection.start?.let { start ->
                        selection.end?.let { end ->
                            renderAreaSelection(
                                bufferSource.getBuffer(RenderType.lines()),
                                matrix,
                                start,
                                end,
                                colorWithAlpha
                            )

                            // Add corner cubes to area selection too
                            val minX = minOf(start.x, end.x).toFloat()
                            val minY = minOf(start.y, end.y).toFloat()
                            val minZ = minOf(start.z, end.z).toFloat()
                            val maxX = maxOf(start.x, end.x).toFloat() + 1f
                            val maxY = maxOf(start.y, end.y).toFloat() + 1f
                            val maxZ = maxOf(start.z, end.z).toFloat() + 1f

                            renderAreaCornerCubes(
                                bufferSource,
                                matrix,
                                minX, minY, minZ,
                                maxX, maxY, maxZ,
                                0.15f,
                                CORNER_COLOR
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Renders small cubes at each corner of an area selection
     */
    private fun renderAreaCornerCubes(
        bufferSource: MultiBufferSource,
        matrix: Matrix4f,
        minX: Float, minY: Float, minZ: Float,
        maxX: Float, maxY: Float, maxZ: Float,
        size: Float,
        color: Color
    ) {
        val buffer = concreteMaterial.buffer(bufferSource, RenderType::entityTranslucent)

        // Bottom corners
        renderSolidCube(buffer, matrix, minX, minY, minZ, size, color) // Bottom near left
        renderSolidCube(buffer, matrix, maxX, minY, minZ, size, color) // Bottom near right
        renderSolidCube(buffer, matrix, maxX, minY, maxZ, size, color) // Bottom far right
        renderSolidCube(buffer, matrix, minX, minY, maxZ, size, color) // Bottom far left

        // Top corners
        renderSolidCube(buffer, matrix, minX, maxY, minZ, size, color) // Top near left
        renderSolidCube(buffer, matrix, maxX, maxY, minZ, size, color) // Top near right
        renderSolidCube(buffer, matrix, maxX, maxY, maxZ, size, color) // Top far right
        renderSolidCube(buffer, matrix, minX, maxY, maxZ, size, color) // Top far left
    }

    /**
     * Renders small cubes at each corner of the selection cube
     */
    private fun renderCornerCubes(
        bufferSource: MultiBufferSource,
        matrix: Matrix4f,
        x: Float,
        y: Float,
        z: Float,
        cubeSize: Float,
        cornerSize: Float,
        color: Color
    ) {
        // Use translucent render type for proper alpha blending
        val buffer = concreteMaterial.buffer(bufferSource, RenderType::entityTranslucent)

        // Bottom corners
        renderSolidCube(buffer, matrix, x - cubeSize, y - cubeSize, z - cubeSize, cornerSize, color) // Bottom near left
        renderSolidCube(
            buffer,
            matrix,
            x + cubeSize,
            y - cubeSize,
            z - cubeSize,
            cornerSize,
            color
        ) // Bottom near right
        renderSolidCube(buffer, matrix, x + cubeSize, y - cubeSize, z + cubeSize, cornerSize, color) // Bottom far right
        renderSolidCube(buffer, matrix, x - cubeSize, y - cubeSize, z + cubeSize, cornerSize, color) // Bottom far left

        // Top corners
        renderSolidCube(buffer, matrix, x - cubeSize, y + cubeSize, z - cubeSize, cornerSize, color) // Top near left
        renderSolidCube(buffer, matrix, x + cubeSize, y + cubeSize, z - cubeSize, cornerSize, color) // Top near right
        renderSolidCube(buffer, matrix, x + cubeSize, y + cubeSize, z + cubeSize, cornerSize, color) // Top far right
        renderSolidCube(buffer, matrix, x - cubeSize, y + cubeSize, z + cubeSize, cornerSize, color) // Top far left
    }

    /**
     * Renders a solid cube at specified position with given size
     */
    private fun renderSolidCube(
        buffer: VertexConsumer,
        matrix: Matrix4f,
        x: Float,
        y: Float,
        z: Float,
        size: Float,
        color: Color
    ) {
        // Enable depth testing and disable backface culling for the solid cubes
        RenderSystem.enableDepthTest()
        RenderSystem.disableCull()

        // Define the 8 vertices of the cube
        val vertices = arrayOf(
            Vector3f(x - size, y - size, z - size), // 0: bottom, near, left
            Vector3f(x + size, y - size, z - size), // 1: bottom, near, right
            Vector3f(x - size, y + size, z - size), // 2: top, near, left
            Vector3f(x + size, y + size, z - size), // 3: top, near, right
            Vector3f(x - size, y - size, z + size), // 4: bottom, far, left
            Vector3f(x + size, y - size, z + size), // 5: bottom, far, right
            Vector3f(x - size, y + size, z + size), // 6: top, far, left
            Vector3f(x + size, y + size, z + size)  // 7: top, far, right
        )

        // Front face (-Z) - Correct winding order: clockwise when looking at front face
        renderFaceQuad(buffer, matrix, vertices[0], vertices[1], vertices[3], vertices[2], color, 0f, 0f, 1f)

        // Back face (+Z) - Correct winding order: clockwise when looking at back face
        renderFaceQuad(buffer, matrix, vertices[5], vertices[4], vertices[6], vertices[7], color, 0f, 0f, -1f)

        // Right face (+X) - Correct winding order: clockwise when looking at right face
        renderFaceQuad(buffer, matrix, vertices[1], vertices[5], vertices[7], vertices[3], color, -1f, 0f, 0f)

        // Left face (-X) - Correct winding order: clockwise when looking at left face
        renderFaceQuad(buffer, matrix, vertices[4], vertices[0], vertices[2], vertices[6], color, 1f, 0f, 0f)

        // Top face (+Y) - Correct winding order: clockwise when looking at top face
        renderFaceQuad(buffer, matrix, vertices[2], vertices[3], vertices[7], vertices[6], color, 0f, -1f, 0f)

        // Bottom face (-Y) - Correct winding order: clockwise when looking at bottom face
        renderFaceQuad(buffer, matrix, vertices[1], vertices[0], vertices[4], vertices[5], color, 0f, 1f, 0f)

        // Re-enable culling after rendering
        RenderSystem.enableCull()
    }


    /**
     * Renders a quad face with explicit normal for a cube face
     */
    private fun renderFaceQuad(
        buffer: VertexConsumer,
        matrix: Matrix4f,
        v1: Vector3f, v2: Vector3f, v3: Vector3f, v4: Vector3f,
        color: Color,
        normalX: Float, normalY: Float, normalZ: Float
    ) {
        // Add all four vertices of the quad in counter-clockwise order when viewed from outside
        // This is the opposite of what we might expect, but it's how Minecraft expects vertices
        addFullVertex(buffer, matrix, v1.x, v1.y, v1.z, 0f, 0f, normalX, normalY, normalZ, color)
        addFullVertex(buffer, matrix, v4.x, v4.y, v4.z, 0f, 1f, normalX, normalY, normalZ, color)
        addFullVertex(buffer, matrix, v3.x, v3.y, v3.z, 1f, 1f, normalX, normalY, normalZ, color)
        addFullVertex(buffer, matrix, v2.x, v2.y, v2.z, 1f, 0f, normalX, normalY, normalZ, color)
    }


    private fun renderAreaSelection(
        buffer: VertexConsumer,
        matrix: Matrix4f,
        start: BlockPos,
        end: BlockPos,
        color: Color
    ) {
        val minX = minOf(start.x, end.x).toFloat()
        val minY = minOf(start.y, end.y).toFloat()
        val minZ = minOf(start.z, end.z).toFloat()
        val maxX = maxOf(start.x, end.x).toFloat() + 1f
        val maxY = maxOf(start.y, end.y).toFloat() + 1f
        val maxZ = maxOf(start.z, end.z).toFloat() + 1f

        // Bottom face
        addColoredVertex(buffer, matrix, minX, minY, minZ, color)
        addColoredVertex(buffer, matrix, maxX, minY, minZ, color)

        addColoredVertex(buffer, matrix, maxX, minY, minZ, color)
        addColoredVertex(buffer, matrix, maxX, minY, maxZ, color)

        addColoredVertex(buffer, matrix, maxX, minY, maxZ, color)
        addColoredVertex(buffer, matrix, minX, minY, maxZ, color)

        addColoredVertex(buffer, matrix, minX, minY, maxZ, color)
        addColoredVertex(buffer, matrix, minX, minY, minZ, color)

        // Top face
        addColoredVertex(buffer, matrix, minX, maxY, minZ, color)
        addColoredVertex(buffer, matrix, maxX, maxY, minZ, color)

        addColoredVertex(buffer, matrix, maxX, maxY, minZ, color)
        addColoredVertex(buffer, matrix, maxX, maxY, maxZ, color)

        addColoredVertex(buffer, matrix, maxX, maxY, maxZ, color)
        addColoredVertex(buffer, matrix, minX, maxY, maxZ, color)

        addColoredVertex(buffer, matrix, minX, maxY, maxZ, color)
        addColoredVertex(buffer, matrix, minX, maxY, minZ, color)

        // Vertical edges
        addColoredVertex(buffer, matrix, minX, minY, minZ, color)
        addColoredVertex(buffer, matrix, minX, maxY, minZ, color)

        addColoredVertex(buffer, matrix, maxX, minY, minZ, color)
        addColoredVertex(buffer, matrix, maxX, maxY, minZ, color)

        addColoredVertex(buffer, matrix, maxX, minY, maxZ, color)
        addColoredVertex(buffer, matrix, maxX, maxY, maxZ, color)

        addColoredVertex(buffer, matrix, minX, minY, maxZ, color)
        addColoredVertex(buffer, matrix, minX, maxY, maxZ, color)
    }

    private fun addColoredVertex(
        buffer: VertexConsumer,
        matrix: Matrix4f,
        x: Float,
        y: Float,
        z: Float,
        color: Color
    ) {
        buffer.addVertex(matrix, x, y, z)
            .setColor(color.red, color.green, color.blue, color.alpha)
            .setNormal(0f, 1f, 0f)
    }

    private fun renderCube(
        buffer: VertexConsumer,
        matrix: Matrix4f,
        x: Float,
        y: Float,
        z: Float,
        color: Color
    ) {
        renderSelectionCube(buffer, matrix, x, y, z, color, 0.5f)
    }

    private fun renderSelectionCube(
        buffer: VertexConsumer,
        matrix: Matrix4f,
        x: Float,
        y: Float,
        z: Float,
        color: Color,
        size: Float
    ) {
        // Front face
        addColoredVertex(buffer, matrix, x - size, y - size, z - size, color)
        addColoredVertex(buffer, matrix, x + size, y - size, z - size, color)

        addColoredVertex(buffer, matrix, x - size, y - size, z - size, color)
        addColoredVertex(buffer, matrix, x - size, y + size, z - size, color)

        addColoredVertex(buffer, matrix, x + size, y - size, z - size, color)
        addColoredVertex(buffer, matrix, x + size, y + size, z - size, color)

        addColoredVertex(buffer, matrix, x - size, y + size, z - size, color)
        addColoredVertex(buffer, matrix, x + size, y + size, z - size, color)

        // Back face
        addColoredVertex(buffer, matrix, x - size, y - size, z + size, color)
        addColoredVertex(buffer, matrix, x + size, y - size, z + size, color)

        addColoredVertex(buffer, matrix, x - size, y - size, z + size, color)
        addColoredVertex(buffer, matrix, x - size, y + size, z + size, color)

        addColoredVertex(buffer, matrix, x + size, y - size, z + size, color)
        addColoredVertex(buffer, matrix, x + size, y + size, z + size, color)

        addColoredVertex(buffer, matrix, x - size, y + size, z + size, color)
        addColoredVertex(buffer, matrix, x + size, y + size, z + size, color)

        // Connecting edges
        addColoredVertex(buffer, matrix, x - size, y - size, z - size, color)
        addColoredVertex(buffer, matrix, x - size, y - size, z + size, color)

        addColoredVertex(buffer, matrix, x + size, y - size, z - size, color)
        addColoredVertex(buffer, matrix, x + size, y - size, z + size, color)

        addColoredVertex(buffer, matrix, x - size, y + size, z - size, color)
        addColoredVertex(buffer, matrix, x - size, y + size, z + size, color)

        addColoredVertex(buffer, matrix, x + size, y + size, z - size, color)
        addColoredVertex(buffer, matrix, x + size, y + size, z + size, color)
    }

    private fun renderController(
        controller: EnderControllerState,
        stack: PoseStack,
        bufferSource: MultiBufferSource
    ) {
        val pos = controller.worldPos.pos
        val buffer = bufferSource.getBuffer(RenderType.lines())
        val color = lerpColor(time)
        val matrix = stack.last().pose()

        renderCube(
            buffer,
            matrix,
            pos.x + 0.5f,
            pos.y + 0.5f,
            pos.z + 0.5f,
            color
        )
    }

    private fun renderLinks(
        controller: EnderControllerState,
        stack: PoseStack,
        bufferSource: MultiBufferSource
    ) {
        val controllerPos = controller.worldPos.pos
        val buffer = bufferSource.getBuffer(RenderType.lines())
        val color = lerpColor(time)
        val matrix = stack.last().pose()

        controller.links.forEach { link ->
            val linkPos = link.pos

            // Draw line from controller to link
            addColoredVertex(
                buffer, matrix,
                controllerPos.x + 0.5f,
                controllerPos.y + 0.5f,
                controllerPos.z + 0.5f,
                color
            )
            addColoredVertex(
                buffer, matrix,
                linkPos.x + 0.5f,
                linkPos.y + 0.5f,
                linkPos.z + 0.5f,
                color
            )

            // Render cube outline for the link
            renderCube(
                buffer,
                matrix,
                linkPos.x + 0.5f,
                linkPos.y + 0.5f,
                linkPos.z + 0.5f,
                color
            )
        }
    }

    private val renderType by lazy {
        val TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/shulker/spark.png")
        RenderType.entityTranslucent(TEXTURE_LOCATION)
    }

    /**
     * Renders a cube outline using solid elongated cuboids for edges with configurable thickness
     */
    fun renderCubeOutline(
        bufferSource: MultiBufferSource,
        matrix: Matrix4f,
        x: Float,
        y: Float,
        z: Float,
        size: Float,
        color: Color,
        thickness: Float = 1.0f
    ) {
        // Use translucent rendering type for proper alpha blending
        val buffer = concreteMaterial.buffer(bufferSource, RenderType::entityTranslucent)

        // Disable culling for edge rendering
        RenderSystem.disableCull()

        renderSolidCubeLines(buffer, matrix, x, y, z, color, size, thickness)

        // Re-enable culling after rendering
        RenderSystem.enableCull()
    }

    /**
     * Renders a cube using solid elongated cuboids for edges
     */
    /**
     * Renders a cube using solid elongated cuboids for edges
     */
    private fun renderSolidCubeLines(
        buffer: VertexConsumer,
        matrix: Matrix4f,
        x: Float,
        y: Float,
        z: Float,
        color: Color,
        size: Float,
        thickness: Float
    ) {
        // Calculate line thickness (scaled based on the size and thickness parameter)
        val lineThickness = thickness * 0.015f

        // Define the 8 vertices of the cube
        val vertices = arrayOf(
            Vector3f(x - size, y - size, z - size), // 0: bottom, near, left
            Vector3f(x + size, y - size, z - size), // 1: bottom, near, right
            Vector3f(x - size, y + size, z - size), // 2: top, near, left
            Vector3f(x + size, y + size, z - size), // 3: top, near, right
            Vector3f(x - size, y - size, z + size), // 4: bottom, far, left
            Vector3f(x + size, y - size, z + size), // 5: bottom, far, right
            Vector3f(x - size, y + size, z + size), // 6: top, far, left
            Vector3f(x + size, y + size, z + size)  // 7: top, far, right
        )

        // Define the 12 edges of the cube (vertex pairs)
        val edges = arrayOf(
            Pair(0, 1), // Bottom near edge
            Pair(1, 5), // Bottom right edge
            Pair(5, 4), // Bottom far edge
            Pair(4, 0), // Bottom left edge
            Pair(2, 3), // Top near edge
            Pair(3, 7), // Top right edge
            Pair(7, 6), // Top far edge
            Pair(6, 2), // Top left edge
            Pair(0, 2), // Near left edge
            Pair(1, 3), // Near right edge
            Pair(5, 7), // Far right edge
            Pair(4, 6)  // Far left edge
        )

        // Render each edge as a solid elongated cuboid
        for ((startIdx, endIdx) in edges) {
            val startVertex = vertices[startIdx]
            val endVertex = vertices[endIdx]

            // Create a copy of the vertices to work with
            val start = Vector3f(startVertex)
            val end = Vector3f(endVertex)

            // Calculate edge direction vector
            val dir = Vector3f()
            end.sub(start, dir)
            val length = dir.length()
            dir.normalize()

            // Find perpendicular axes
            val up = Vector3f()
            if (abs(dir.x) < 0.8f && abs(dir.z) < 0.8f) {
                // Edge is mostly vertical, use X axis as reference
                up.set(1.0f, 0.0f, 0.0f)
            } else {
                // Edge is mostly horizontal, use Y axis as reference
                up.set(0.0f, 1.0f, 0.0f)
            }

            // Create the basis vectors for the edge
            val side = Vector3f()
            up.cross(dir, side)
            side.normalize()

            up.set(0f, 0f, 0f)
            dir.cross(side, up)
            up.normalize()

            // Apply thickness
            side.mul(lineThickness)
            up.mul(lineThickness)

            // Create cuboid corners
            val corners = Array(8) { Vector3f() }

            // Start face
            corners[0] = Vector3f(start).add(Vector3f(side).negate()).add(Vector3f(up).negate())
            corners[1] = Vector3f(start).add(side).add(Vector3f(up).negate())
            corners[2] = Vector3f(start).add(side).add(up)
            corners[3] = Vector3f(start).add(Vector3f(side).negate()).add(up)

            // End face
            corners[4] = Vector3f(end).add(Vector3f(side).negate()).add(Vector3f(up).negate())
            corners[5] = Vector3f(end).add(side).add(Vector3f(up).negate())
            corners[6] = Vector3f(end).add(side).add(up)
            corners[7] = Vector3f(end).add(Vector3f(side).negate()).add(up)

            // Render all 6 faces of the cuboid with correct winding order
            // Front face (start)
            renderFaceQuad(
                buffer,
                matrix,
                corners[0],
                corners[1],
                corners[2],
                corners[3],
                color,
                -dir.x,
                -dir.y,
                -dir.z
            )

            // Back face (end)
            renderFaceQuad(buffer, matrix, corners[5], corners[4], corners[7], corners[6], color, dir.x, dir.y, dir.z)

            // Top face
            renderFaceQuad(buffer, matrix, corners[3], corners[2], corners[6], corners[7], color, up.x, up.y, up.z)

            // Bottom face
            renderFaceQuad(buffer, matrix, corners[1], corners[0], corners[4], corners[5], color, -up.x, -up.y, -up.z)

            // Right face
            renderFaceQuad(
                buffer,
                matrix,
                corners[1],
                corners[5],
                corners[6],
                corners[2],
                color,
                side.x,
                side.y,
                side.z
            )

            // Left face
            renderFaceQuad(
                buffer,
                matrix,
                corners[4],
                corners[0],
                corners[3],
                corners[7],
                color,
                -side.x,
                -side.y,
                -side.z
            )
        }
    }

    /**
     * Renders all 6 faces of a cuboid
     */
    private fun renderCuboidFaces(
        buffer: VertexConsumer,
        matrix: Matrix4f,
        vertices: Array<Vector3f>,
        color: Color
    ) {
        // Front face (0,1,2,3)
        renderQuad(buffer, matrix, vertices[0], vertices[1], vertices[2], vertices[3], color)

        // Back face (4,5,6,7)
        renderQuad(buffer, matrix, vertices[4], vertices[5], vertices[6], vertices[7], color)

        // Right face (1,5,6,2)
        renderQuad(buffer, matrix, vertices[1], vertices[5], vertices[6], vertices[2], color)

        // Left face (0,4,7,3)
        renderQuad(buffer, matrix, vertices[0], vertices[4], vertices[7], vertices[3], color)

        // Top face (3,7,6,2)
        renderQuad(buffer, matrix, vertices[3], vertices[7], vertices[6], vertices[2], color)

        // Bottom face (0,4,5,1)
        renderQuad(buffer, matrix, vertices[0], vertices[4], vertices[5], vertices[1], color)
    }

    /**
     * Helper method to render a quad from 4 vertices with UV coordinates
     */
    private fun renderQuad(
        buffer: VertexConsumer,
        matrix: Matrix4f,
        v1: Vector3f,  // Bottom-left
        v2: Vector3f,  // Bottom-right
        v3: Vector3f,  // Top-right
        v4: Vector3f,  // Top-left
        color: Color
    ) {
        // Calculate normal for the quad (using counter-clockwise vertices)
        val edge1 = Vector3f(v2).sub(v1)
        val edge2 = Vector3f(v4).sub(v1)
        val normal = Vector3f()
        edge1.cross(edge2, normal)
        normal.normalize()

        // In Minecraft rendering system, quads should typically be supplied in counter-clockwise order
        // UV coordinates assigned to each corner:
        // v1 (bottom-left): (0,0)
        // v2 (bottom-right): (1,0)
        // v3 (top-right): (1,1)
        // v4 (top-left): (0,1)

        // If using VertexFormat.Mode.QUADS directly (for certain RenderTypes)
        addFullVertex(buffer, matrix, v1.x, v1.y, v1.z, 0f, 0f, normal.x, normal.y, normal.z, color)
        addFullVertex(buffer, matrix, v2.x, v2.y, v2.z, 1f, 0f, normal.x, normal.y, normal.z, color)
        addFullVertex(buffer, matrix, v3.x, v3.y, v3.z, 1f, 1f, normal.x, normal.y, normal.z, color)
        addFullVertex(buffer, matrix, v4.x, v4.y, v4.z, 0f, 1f, normal.x, normal.y, normal.z, color)
    }

    /**
     * Helper method to add a complete vertex with all required elements
     */
    private fun addFullVertex(
        buffer: VertexConsumer,
        matrix: Matrix4f,
        x: Float,
        y: Float,
        z: Float,
        u: Float,
        v: Float,
        normalX: Float,
        normalY: Float,
        normalZ: Float,
        color: Color
    ) {
        val lightmapU = 15 * 16    // Block light (15)
        val lightmapV = 15 * 16    // Sky light (15)
        buffer.addVertex(matrix, x, y, z)
            .setColor(color.red, color.green, color.blue, color.alpha)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setUv2(lightmapU, lightmapV)
            .setNormal(normalX, normalY, normalZ)
    }

    override fun onPacket(packet: Packet, from: UUID) {
        // Handle packet updates if needed
    }
}