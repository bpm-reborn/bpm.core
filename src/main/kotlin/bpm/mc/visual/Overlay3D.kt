package bpm.mc.visual

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.culling.Frustum
import bpm.common.network.Listener
import bpm.common.packets.Packet
import bpm.mc.links.EnderControllerState
import bpm.mc.links.EnderNet
import bpm.mc.links.EnderNetState
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import org.joml.Matrix4f
import org.joml.Vector3f
import java.awt.Color
import java.util.*
import kotlin.math.sin

object Overlay3D : Listener {

    private val state: EnderNetState = EnderNet.getState()
    private var time = 0f

    private val COLOR_1 = Color(64, 224, 208)  // Turquoise
    private val COLOR_2 = Color(147, 112, 219) // Purple

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

        state.controllers.values.forEach { controller ->
            renderController(controller, stack, bufferSource)
            renderLinks(controller, stack, bufferSource)
        }

        stack.popPose()
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
            .setColor(color.red, color.green, color.blue, 255)
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
        // Front face
        addColoredVertex(buffer, matrix, x - 0.5f, y - 0.5f, z - 0.5f, color)
        addColoredVertex(buffer, matrix, x + 0.5f, y - 0.5f, z - 0.5f, color)

        addColoredVertex(buffer, matrix, x - 0.5f, y - 0.5f, z - 0.5f, color)
        addColoredVertex(buffer, matrix, x - 0.5f, y + 0.5f, z - 0.5f, color)

        addColoredVertex(buffer, matrix, x + 0.5f, y - 0.5f, z - 0.5f, color)
        addColoredVertex(buffer, matrix, x + 0.5f, y + 0.5f, z - 0.5f, color)

        addColoredVertex(buffer, matrix, x - 0.5f, y + 0.5f, z - 0.5f, color)
        addColoredVertex(buffer, matrix, x + 0.5f, y + 0.5f, z - 0.5f, color)

        // Back face
        addColoredVertex(buffer, matrix, x - 0.5f, y - 0.5f, z + 0.5f, color)
        addColoredVertex(buffer, matrix, x + 0.5f, y - 0.5f, z + 0.5f, color)

        addColoredVertex(buffer, matrix, x - 0.5f, y - 0.5f, z + 0.5f, color)
        addColoredVertex(buffer, matrix, x - 0.5f, y + 0.5f, z + 0.5f, color)

        addColoredVertex(buffer, matrix, x + 0.5f, y - 0.5f, z + 0.5f, color)
        addColoredVertex(buffer, matrix, x + 0.5f, y + 0.5f, z + 0.5f, color)

        addColoredVertex(buffer, matrix, x - 0.5f, y + 0.5f, z + 0.5f, color)
        addColoredVertex(buffer, matrix, x + 0.5f, y + 0.5f, z + 0.5f, color)

        // Connecting edges
        addColoredVertex(buffer, matrix, x - 0.5f, y - 0.5f, z - 0.5f, color)
        addColoredVertex(buffer, matrix, x - 0.5f, y - 0.5f, z + 0.5f, color)

        addColoredVertex(buffer, matrix, x + 0.5f, y - 0.5f, z - 0.5f, color)
        addColoredVertex(buffer, matrix, x + 0.5f, y - 0.5f, z + 0.5f, color)

        addColoredVertex(buffer, matrix, x - 0.5f, y + 0.5f, z - 0.5f, color)
        addColoredVertex(buffer, matrix, x - 0.5f, y + 0.5f, z + 0.5f, color)

        addColoredVertex(buffer, matrix, x + 0.5f, y + 0.5f, z - 0.5f, color)
        addColoredVertex(buffer, matrix, x + 0.5f, y + 0.5f, z + 0.5f, color)
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

    override fun onPacket(packet: Packet, from: UUID) {
        // Handle packet updates if needed
    }
}