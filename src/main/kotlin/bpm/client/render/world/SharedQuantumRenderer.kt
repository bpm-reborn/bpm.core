package bpm.client.render.world

import bpm.mc.registries.ModBlocks
import bpm.mc.registries.ModComponents
import bpm.mc.registries.ModItems
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.Pos
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.*
import kotlin.random.Random

object SharedQuantumRenderer {

    var shader: ShaderInstance? = null
    private val timeController = QuantumTimeController()
    val PARTICLE_COUNT get() = 16
    val BASE_PARTICLE_SIZE get() = 0.075f
    val ORBITAL_RADIUS get() = 0.20f

    private val particles = Array(PARTICLE_COUNT) {
        QuantumParticleVisual().apply {
            rotationSpeedX = (Random.nextFloat() - 0.5f) * 4f
            rotationSpeedY = (Random.nextFloat() - 0.5f) * 4f
            rotationSpeedZ = (Random.nextFloat() - 0.5f) * 4f
            rotationX = Random.nextFloat() * 2f * PI.toFloat()
            rotationY = Random.nextFloat() * 2f * PI.toFloat()
            rotationZ = Random.nextFloat() * 2f * PI.toFloat()
        }
    }

    private val renderType = RenderType.create(
        "quantum_particle",
        DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,
        VertexFormat.Mode.QUADS,
        256,
        false,
        true,
        RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.ShaderStateShard { shader })
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
            .setCullState(RenderStateShard.NO_CULL)
            .setLightmapState(RenderStateShard.NO_LIGHTMAP)
            .createCompositeState(true)
    )

    data class QuantumParticleVisual(
        var position: Vector3f = Vector3f(),
        var basePosition: Vector3f = Vector3f(),
        var waveScale: Float = 1.0f,
        var probability: Float = 1.0f,
        var phase: Float = 0.0f,
        var superpositionEffect: Float = 0.0f,
        var entanglementGlow: Float = 0.0f,
        var uncertaintyDistortion: Float = 0.0f,
        var rotationX: Float = 0f,
        var rotationY: Float = 0f,
        var rotationZ: Float = 0f,
        var rotationSpeedX: Float = 0f,
        var rotationSpeedY: Float = 0f,
        var rotationSpeedZ: Float = 0f
    )

    fun renderQuantumEffect(poseStack: PoseStack, buffer: MultiBufferSource) {
        val currentTime = timeController.updateTime()
        updateShaderEffects(currentTime)

        val vertexBuffer = buffer.getBuffer(renderType)
        val pose = poseStack.last().pose()

        renderQuantumState(currentTime, poseStack, pose, vertexBuffer)

        if (buffer is MultiBufferSource.BufferSource) {
            buffer.endBatch(renderType)
        }
    }

    private fun updateShaderEffects(currentTime: Float) {
        shader?.let { shaderInstance ->
            shaderInstance.safeGetUniform("GameTime")?.set(currentTime)
            shaderInstance.markDirty()
        }
    }

    private fun renderQuantumState(
        currentTime: Float,
        poseStack: PoseStack,
        matrix: Matrix4f,
        buffer: VertexConsumer,
    ) {
        particles.forEachIndexed { index, particle ->
            updateParticleVisuals(particle, currentTime, index.toFloat())
            renderQuantumCube(matrix, buffer, particle)
        }
    }

    private fun updateParticleVisuals(particle: QuantumParticleVisual, time: Float, index: Float) {
        val angle = time * 0.5f + index * (2.0f * PI.toFloat() / PARTICLE_COUNT)
        val baseUncertainty = sin(time * 2.0f + index) * 0.05f

        particle.rotationX += particle.rotationSpeedX * 0.016f
        particle.rotationY += particle.rotationSpeedY * 0.016f
        particle.rotationZ += particle.rotationSpeedZ * 0.016f

        particle.basePosition.set(
            cos(angle) * ORBITAL_RADIUS + baseUncertainty,
            sin(angle) * ORBITAL_RADIUS + baseUncertainty,
            sin(time + index) * 0.1f
        )

        particle.position.set(particle.basePosition)

        particle.phase = (particle.phase + time * 0.1f) % (2.0f * PI.toFloat())
        particle.waveScale = 1.0f + sin(time + index) * 0.2f
        particle.probability = (cos(time * 0.5f + index) * 0.3f + 0.7f).coerceIn(0.0f, 1.0f)
        particle.superpositionEffect = sin(time * 2.0f + index) * 0.5f + 0.5f
        particle.entanglementGlow = cos(time + index) * 0.4f + 0.6f
        particle.uncertaintyDistortion = sin(time * 3.0f + index) * 0.3f
    }

    private fun renderQuantumCube(
        matrix: Matrix4f,
        buffer: VertexConsumer,
        particle: QuantumParticleVisual
    ) {
        val color = Vector4f(
            0.7f + particle.superpositionEffect * 0.4f,
            0.3f + particle.entanglementGlow * 0.125f,
            0.7f + particle.probability * 0.3f,
            0.9f + particle.uncertaintyDistortion * 0.15f
        )


        renderCube(matrix, buffer, particle, color)

        // Render quantum effects
        if (particle.superpositionEffect > 0.3f) {
            val ghostColor = Vector4f(color)
            ghostColor.w *= 0.56f

            val ghostOffset = 0.025f * particle.superpositionEffect
            val ghostPos = Vector3f(particle.position).add(ghostOffset, ghostOffset, ghostOffset)
            renderCube(matrix, buffer, particle.copy().apply { position = ghostPos }, ghostColor)
        }

        if (particle.entanglementGlow > 0.5f) {
            val glowColor = Vector4f(color)
            glowColor.w *= 0.4f
            renderCube(
                matrix,
                buffer,
                particle,
                glowColor,
                BASE_PARTICLE_SIZE * 2f * particle.entanglementGlow
            )
        }
    }

    private fun renderCube(
        matrix: Matrix4f,
        buffer: VertexConsumer,
        particle: QuantumParticleVisual,
        color: Vector4f,
        size: Float = BASE_PARTICLE_SIZE * particle.waveScale
    ) {
        val halfSize = size / 2f
        val pos = particle.position

        // Calculate normal pointing towards center
        val normalToCenter = Vector3f(-pos.x, -pos.y, -pos.z)
        if (normalToCenter.length() > 0.0001f) {
            normalToCenter.normalize()
        } else {
            normalToCenter.set(0f, 1f, 0f) // Default normal if at center
        }


        val rotationMatrix = Matrix4f()
            .rotate(particle.rotationX, 1f, 0f, 0f)
            .rotate(particle.rotationY, 0f, 1f, 0f)
            .rotate(particle.rotationZ, 0f, 0f, 1f)

        // Define vertices with all normals pointing towards center
        val vertices = arrayOf(
            // Front face
            Triple(Vector3f(-halfSize, -halfSize, halfSize), Vector2f(0f, 0f), normalToCenter),
            Triple(Vector3f(halfSize, -halfSize, halfSize), Vector2f(1f, 0f), normalToCenter),
            Triple(Vector3f(halfSize, halfSize, halfSize), Vector2f(1f, 1f), normalToCenter),
            Triple(Vector3f(-halfSize, halfSize, halfSize), Vector2f(0f, 1f), normalToCenter),

            // Back face
            Triple(Vector3f(-halfSize, -halfSize, -halfSize), Vector2f(0f, 0f), normalToCenter),
            Triple(Vector3f(-halfSize, halfSize, -halfSize), Vector2f(0f, 1f), normalToCenter),
            Triple(Vector3f(halfSize, halfSize, -halfSize), Vector2f(1f, 1f), normalToCenter),
            Triple(Vector3f(halfSize, -halfSize, -halfSize), Vector2f(1f, 0f), normalToCenter),

            // Top face
            Triple(Vector3f(-halfSize, halfSize, -halfSize), Vector2f(0f, 0f), normalToCenter),
            Triple(Vector3f(-halfSize, halfSize, halfSize), Vector2f(0f, 1f), normalToCenter),
            Triple(Vector3f(halfSize, halfSize, halfSize), Vector2f(1f, 1f), normalToCenter),
            Triple(Vector3f(halfSize, halfSize, -halfSize), Vector2f(1f, 0f), normalToCenter),

            // Bottom face
            Triple(Vector3f(-halfSize, -halfSize, -halfSize), Vector2f(0f, 0f), normalToCenter),
            Triple(Vector3f(halfSize, -halfSize, -halfSize), Vector2f(1f, 0f), normalToCenter),
            Triple(Vector3f(halfSize, -halfSize, halfSize), Vector2f(1f, 1f), normalToCenter),
            Triple(Vector3f(-halfSize, -halfSize, halfSize), Vector2f(0f, 1f), normalToCenter),

            // Right face
            Triple(Vector3f(halfSize, -halfSize, -halfSize), Vector2f(0f, 0f), normalToCenter),
            Triple(Vector3f(halfSize, halfSize, -halfSize), Vector2f(0f, 1f), normalToCenter),
            Triple(Vector3f(halfSize, halfSize, halfSize), Vector2f(1f, 1f), normalToCenter),
            Triple(Vector3f(halfSize, -halfSize, halfSize), Vector2f(1f, 0f), normalToCenter),

            // Left face
            Triple(Vector3f(-halfSize, -halfSize, -halfSize), Vector2f(0f, 0f), normalToCenter),
            Triple(Vector3f(-halfSize, -halfSize, halfSize), Vector2f(1f, 0f), normalToCenter),
            Triple(Vector3f(-halfSize, halfSize, halfSize), Vector2f(1f, 1f), normalToCenter),
            Triple(Vector3f(-halfSize, halfSize, -halfSize), Vector2f(0f, 1f), normalToCenter)
        )

        // Render each face (6 faces, 4 vertices each)
        for (i in vertices.indices step 4) {
            for (j in 0..3) {
                val (vertex, uv, normal) = vertices[i + j]
                // Apply rotation to vertex
                val rotatedVertex = Vector3f(vertex)
                rotationMatrix.transformPosition(rotatedVertex)

                // Apply rotation to normal
                val rotatedNormal = Vector3f(normal)
                rotationMatrix.transformDirection(rotatedNormal)

                buffer.addVertex(
                    matrix,
                    pos.x + vertex.x,
                    pos.y + vertex.y,
                    pos.z + vertex.z
                ).setUv(uv.x, uv.y)
                    .setColor(color.x, color.y, color.z, color.w)
                    .setNormal(normal.x, normal.y, normal.z)
            }
        }
    }
}