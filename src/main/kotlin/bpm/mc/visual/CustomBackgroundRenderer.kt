package bpm.mc.visual

import bpm.Bpm
import bpm.pipe.proxy.ProxiedType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import imgui.ImGui
import imgui.flag.ImGuiMouseCursor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.*
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.joml.Vector3f
import kotlin.math.max
import kotlin.random.Random


class CustomBackgroundRenderer(private val minecraft: Minecraft) {
    companion object {

        var backgroundShader: ShaderInstance? = null
    }

    private val colorTarget = Vector3f(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())
    private val colorCurrent = Vector3f(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())

    private var time = 0f
    private var zoom = 1f
    private var rotation = 0f
    private val bufferSource = MultiBufferSource.immediate(ByteBufferBuilder(1536))
    // New variables for the added uniforms
    private var blurRadius = 2.0f
    private var numStars = 200
    private var starSize = 0.002f
    var pulseValue = 0f
    var lastClickTime = 0L
    // Add methods to update zoom and rotation
    fun updateZoom(dZoom: Float) {
        zoom += dZoom * 0.12f
        zoom = zoom.coerceIn(1f, 2f)  // Limit zoom range
    }
    fun onFaceClick() {
        pulseValue = 1f
        lastClickTime = System.currentTimeMillis()
    }
    fun updateRotation(newRotation: Float) {
        rotation = newRotation
    }

    fun updatePulse() = Companion.backgroundShader?.let {
        val currentTime = System.currentTimeMillis()
        val timeSinceClick = currentTime - lastClickTime

        // Lerp the pulse value back to zero over 1 second
        pulseValue = max(0f, 1f - timeSinceClick / 1000f)

        // Update the uniform in your shader
        it.safeGetUniform("pulse")?.set(pulseValue)
    }

    private val backgroundRenderType: RenderType = RenderType.create(
        "${Bpm.ID}_custom_background",
        DefaultVertexFormat.POSITION,
        VertexFormat.Mode.QUADS,
        256,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.ShaderStateShard { backgroundShader })
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .createCompositeState(false)
    )

    // Add methods to update new uniforms
    fun updateBlurRadius(newBlurRadius: Float) {
        blurRadius = newBlurRadius.coerceIn(0f, 10f)  // Limit blur radius range
    }

    fun updateNumParticles(newNumParticles: Int) {
        numStars = newNumParticles.coerceIn(0, 100)  // Limit number of particles
    }

    fun updateParticleSize(newParticleSize: Float) {
        starSize = newParticleSize.coerceIn(0.01f, 0.1f)  // Limit particle size range
    }

    fun renderBackground(
        graphics: GuiGraphics,
        renderer: BlockViewRenderer,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
        face: Direction?,
        hovered: BlockPos?
    ) {
        val pose = graphics.pose()
        val buffer = bufferSource.getBuffer(backgroundRenderType)
        val bounds = renderer.computeBounds(25.0)

        pose.setIdentity()
        pose.translate(0f, 0f, -1000f)
        //lerp the color

        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.setShader { backgroundShader }

        backgroundShader?.let { shader ->
            colorCurrent.lerp(colorTarget, 0.01f)
            time += 0.5f
            shader.safeGetUniform("iTime")?.set(time)


            val width = ImGui.getWindowViewport().size.x
            val height = ImGui.getWindowViewport().size.y
            val boundsWidth = bounds.maxX - bounds.minX
            val boundsHeight = bounds.maxY - bounds.minY
            val aspect = height / width
            //If hovered isn't null, use it's center screen position for the mouse
            val realMouse = ImGui.getMousePos()

//            shader.safeGetUniform("iMouse")?.set(
//                (realMouse.x - bounds.minX) / boundsWidth,
//                (realMouse.y - bounds.minY) / boundsHeight,
//            )
            //The comupted size

            if (hovered != null && face != null) {
                val state = renderer.faceStates.getOrDefault(Pair(hovered, face), ProxiedType.NONE)
                when (state) {
                    ProxiedType.INPUT -> shader.safeGetUniform("iColor")?.set(0f, 1f, 0f)
                    ProxiedType.OUTPUT -> shader.safeGetUniform("iColor")?.set(1f, 0f, 0f)
                    ProxiedType.BOTH -> shader.safeGetUniform("iColor")?.set(1f, 1f, 0f)
                    ProxiedType.NONE -> shader.safeGetUniform("iColor")?.set(1f, 1f, 1f) // White for hover on NONE state
                }
                shader.safeGetUniform("circleRadius")?.set(2.5f) // Larger radius for hover effect
            } else {
                shader.safeGetUniform("iColor")?.set(0f, 0f, 0f) // Black for non-hovered background
            }

            shader.safeGetUniform("circleRadius")?.set(1.5f) // Smaller radius for non-hover effect


            shader.safeGetUniform("iResolution")?.set(width.toFloat(), height.toFloat())

            // Render background with padding
            buffer.addVertex(pose.last().pose(), 0f, 0f, 0f)
            buffer.addVertex(pose.last().pose(), 0f, height.toFloat(), 0f)
            buffer.addVertex(pose.last().pose(), width, height.toFloat(), 0f)
            buffer.addVertex(pose.last().pose(), width, 0f, 0f)
            bufferSource.endBatch(backgroundRenderType)
        }

        RenderSystem.disableBlend()
    }
}