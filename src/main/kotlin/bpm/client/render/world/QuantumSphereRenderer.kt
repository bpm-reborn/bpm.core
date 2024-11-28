//package bpm.client.render.world
//
//import bpm.Bpm
//import com.mojang.blaze3d.vertex.*
//import net.minecraft.client.renderer.MultiBufferSource
//import net.minecraft.client.renderer.RenderStateShard
//import net.minecraft.client.renderer.RenderType
//import net.minecraft.client.renderer.ShaderInstance
//import net.minecraft.world.item.ItemDisplayContext
//import net.minecraft.world.item.ItemStack
//import org.joml.Matrix4f
//import org.joml.Vector3f
//import kotlin.math.*
//import net.minecraft.util.Mth
//import kotlin.random.Random
//
///**
// * QuantumTimeController governs the flow of interdimensional time and void manifestation.
// * This system creates an unsettling display of quantum mechanics gone wrong, where reality
// * itself seems to bend and warp under the influence of an otherworldly presence.
// */
//class QuantumTimeController {
//    private var lastUpdateTime: Long = System.nanoTime()
//    private var quantumTime: Float = 0f
//    private var deltaSeconds: Float = 0f
//
//    // Track the corruption of spacetime
//    private var currentState: AnimationState = AnimationState.SINGULARITY_FORMING
//    private var stateProgress: Float = 0f
//    private var realityDistortion: Float = 0f
//
//    // Timing parameters for void manifestation
//    private val singularityDuration: Float = 4.0f    // Time for reality to begin breaking down
//    private val voidDuration: Float = 3.5f           // Duration of pure void manifestation
//    private val assimilationDuration: Float = 3.0f   // Time for complete reality consumption
//
//    // Parameters controlling reality destabilization
//    private var currentDistortion: Float = 1f
//    private var targetDistortion: Float = 1f
//    private var entanglementIntensity: Float = 0f
//    private var voidResonance: Float = 0f
//
//    // Tracks how deeply reality has been corrupted
//    private var realityCorruption: Float = 0f
//    private val corruptionRate: Float = 0.05f
//
//    /**
//     * Calculates the intensity of chaos field distortions.
//     * Creates an unsettling pattern of reality fluctuations using layered wave functions.
//     */
//    private fun calculateChaosField(time: Float): Float {
//        // Primary reality distortion wave
//        val primaryWave = sin(time * 1.5f + cos(time * 0.7f) * 0.3f)
//
//        // Secondary void resonance
//        val secondaryWave = cos(time * 2.3f + sin(time * 0.9f) * 0.5f)
//
//        // Deep chaos undertones
//        val chaosWave = sin(time * 3.7f + cos(time * 1.2f) * 0.2f)
//
//        // Combine waves with void influence
//        return (primaryWave * 0.5f + secondaryWave * 0.3f + chaosWave * 0.2f)
//            .coerceIn(-1f, 1f)
//    }
//
//    /**
//     * States representing different phases of reality corruption
//     */
//    enum class AnimationState {
//        SINGULARITY_FORMING,    // Reality begins to tear
//        VOID_MANIFESTATION,     // The void seeps through
//        ASSIMILATION           // Reality is consumed
//    }
//
//    /**
//     * Updates the progression of void corruption through reality
//     */
//    fun updateTime(): Float {
//        val currentTime = System.nanoTime()
//        deltaSeconds = (currentTime - lastUpdateTime).toFloat() / 1_000_000_000f
//
//        // Update core time values
//        quantumTime += deltaSeconds
//        stateProgress += deltaSeconds
//
//        // Progress reality corruption
//        realityCorruption = (realityCorruption + deltaSeconds * corruptionRate)
//            .coerceIn(0f, 1f)
//
//        // Update void resonance based on current state
//        when (currentState) {
//            AnimationState.SINGULARITY_FORMING -> {
//                val progress = (stateProgress / singularityDuration).coerceIn(0f, 1f)
//                if (progress >= 1f) {
//                    currentState = AnimationState.VOID_MANIFESTATION
//                    stateProgress = 0f
//                }
//                // Reality begins to distort
//                targetDistortion = 1f + sin(progress * PI.toFloat()) * 0.5f
//            }
//
//            AnimationState.VOID_MANIFESTATION -> {
//                val progress = (stateProgress / voidDuration).coerceIn(0f, 1f)
//                if (progress >= 1f) {
//                    currentState = AnimationState.ASSIMILATION
//                    stateProgress = 0f
//                }
//                // Void energy peaks
//                targetDistortion = 1.5f + cos(progress * PI.toFloat() * 2f) * 0.3f
//            }
//
//            AnimationState.ASSIMILATION -> {
//                val progress = (stateProgress / assimilationDuration).coerceIn(0f, 1f)
//                if (progress >= 1f) {
//                    currentState = AnimationState.SINGULARITY_FORMING
//                    stateProgress = 0f
//                }
//                // Reality is consumed
//                targetDistortion = 2f - progress * 0.5f
//            }
//        }
//
//        // Smooth transitions in distortion
//        currentDistortion = lerp(currentDistortion, targetDistortion, deltaSeconds * 2f)
//
//        // Calculate void resonance
//        voidResonance = calculateChaosField(quantumTime) * realityCorruption
//
//        // Update entanglement intensity
//        entanglementIntensity = (sin(quantumTime * 0.8f) * 0.5f + 0.5f) *
//                (1f + realityCorruption * 0.5f)
//
//        lastUpdateTime = currentTime
//        return quantumTime
//    }
//
//    /**
//     * Calculates the current intensity of void collapse based on the animation state.
//     * This creates a pulsing, unsettling effect that grows stronger as reality breaks down.
//     */
//    /**
//     * Calculates the collapse intensity of the void manifestation, creating an unsettling
//     * pulsing effect that suggests an otherworldly intelligence at work. The intensity
//     * varies based on both the current animation state and the accumulated corruption
//     * of surrounding reality.
//     */
//    fun getCollapseIntensity(): Float {
//        // Track previous intensity for smooth transitions
//        var baseCollapseIntensity = when (currentState) {
//            AnimationState.SINGULARITY_FORMING -> {
//                // Initial reality tear - creates a slow, deliberate build-up
//                val progress = (stateProgress / singularityDuration).coerceIn(0f, 1f)
//                // Deep resonance wave suggests gathering power
//                val resonanceWave = sin(progress * PI.toFloat() * 1.5f) * 0.4f
//                // Corruption seepage indicates reality breaking down
//                val corruptionSeep = progress * 0.6f
//                // Combine for an unsettling build-up
//                (resonanceWave + corruptionSeep) * (1f + realityCorruption * 0.4f)
//            }
//
//            AnimationState.VOID_MANIFESTATION -> {
//                // Peak void presence - creates intense, alien pulsations
//                val progress = (stateProgress / voidDuration).coerceIn(0f, 1f)
//                // Rapid, artificial-feeling pulses
//                val voidPulse = sin(progress * PI.toFloat() * 3f) * 0.3f
//                // Slower, deeper undertone suggests massive power
//                val depthPulse = cos(progress * PI.toFloat() * 1.2f) * 0.2f
//                // Combine with heightened base intensity
//                0.7f + voidPulse + depthPulse + (realityCorruption * 0.3f)
//            }
//
//            AnimationState.ASSIMILATION -> {
//                // Reality consumption - creates a gradually intensifying presence
//                val progress = (stateProgress / assimilationDuration).coerceIn(0f, 1f)
//                // Dominating presence overtakes normal space
//                val dominancePulse = sin(progress * PI.toFloat() * 2.5f) * 0.25f
//                // Growing void strength as reality fails
//                val assimilationForce = 0.9f - (progress * 0.3f)
//                // Maximum corruption influence during assimilation
//                assimilationForce + dominancePulse + (realityCorruption * 0.5f)
//            }
//        }
//
//        // Add void interference effects
//        val voidInterference = calculateVoidInterference(quantumTime)
//
//        // Combine base intensity with void effects
//        return (baseCollapseIntensity + voidInterference)
//            .coerceIn(0.1f, 1f)  // Never fully collapse to maintain presence
//    }
//
//    /**
//     * Calculates additional void interference patterns that make the collapse
//     * feel more alien and intelligent.
//     */
//    private fun calculateVoidInterference(time: Float): Float {
//        // High-frequency technological pulse
//        val techPulse = sin(time * 4f) * 0.1f
//
//        // Medium-frequency void resonance
//        val voidWave = cos(time * 2.3f) * 0.15f
//
//        // Low-frequency power fluctuation
//        val powerFlux = sin(time * 1.1f) * 0.2f
//
//        // Combine waves with corruption influence
//        return (techPulse + voidWave + powerFlux) *
//                (0.3f + realityCorruption * 0.4f)
//    }
//
//    /**
//     * Updates the quantum state based on the current time and void influence.
//     * Creates an increasingly unstable and corrupted quantum field that grows
//     * more ominous as time progresses.
//     */
//     fun updateQuantumState(currentTime: Float): QuantumStateInfo {
//        // Calculate base field instability
//        val fieldInstability = sin(currentTime * 1.2f) * 0.3f +
//                cos(currentTime * 0.7f) * 0.2f +
//                sin(currentTime * 2.1f) * 0.1f
//
//        // Calculate void corruption factor
//        val voidCorruption = when (currentState) {
//            AnimationState.SINGULARITY_FORMING -> {
//                // Growing instability as singularity forms
//                val progress = (stateProgress / singularityDuration).coerceIn(0f, 1f)
//                progress * 0.6f + sin(progress * PI.toFloat() * 2f) * 0.2f
//            }
//
//            AnimationState.VOID_MANIFESTATION -> {
//                // Peak instability during void manifestation
//                val progress = (stateProgress / voidDuration).coerceIn(0f, 1f)
//                0.7f + sin(progress * PI.toFloat() * 3f) * 0.3f
//            }
//
//            AnimationState.ASSIMILATION -> {
//                // Decreasing but deeply resonating instability during assimilation
//                val progress = (stateProgress / assimilationDuration).coerceIn(0f, 1f)
//                0.8f - progress * 0.4f + cos(progress * PI.toFloat() * 4f) * 0.2f
//            }
//        }
//
//        // Calculate quantum uncertainty factor
//        val uncertaintyFactor = (fieldInstability + voidCorruption * 0.5f)
//            .coerceIn(0.2f, 0.8f)
//
//        // Calculate superposition strength with void influence
//        val superpositionStrength = when (currentState) {
//            AnimationState.SINGULARITY_FORMING -> {
//                // Growing superposition as reality tears
//                (1f + sin(currentTime * 1.5f)) * 0.3f + voidCorruption * 0.4f
//            }
//
//            AnimationState.VOID_MANIFESTATION -> {
//                // Intense superposition during void phase
//                0.6f + sin(currentTime * 2f) * 0.2f + voidCorruption * 0.3f
//            }
//
//            AnimationState.ASSIMILATION -> {
//                // Decaying but unstable superposition
//                0.5f + cos(currentTime * 1.8f) * 0.3f * (1f - voidCorruption * 0.4f)
//            }
//        }
//
//        // Calculate final entanglement with void influence
//        val voidEntanglement = sin(currentTime * 0.8f) * 0.4f +
//                cos(currentTime * 1.2f) * 0.3f +
//                voidCorruption * 0.5f
//
//        return QuantumStateInfo(
//            isCollapsing = currentState == AnimationState.SINGULARITY_FORMING,
//            amplitude = currentDistortion * (1f + fieldInstability * 0.2f),
//            uncertainty = uncertaintyFactor,
//            superposition = superpositionStrength.coerceIn(0f, 1f),
//            entanglement = voidEntanglement.coerceIn(0f, 1f)
//        )
//    }
//
//    fun getQuantumState(): QuantumStateInfo {
//        // Calculate uncertainty amplified by void presence
//        val uncertaintyFactor = calculateChaosField(quantumTime) * 0.5f + 0.5f +
//                realityCorruption * 0.3f
//
//        return QuantumStateInfo(
//            isCollapsing = currentState == AnimationState.SINGULARITY_FORMING,
//            amplitude = currentDistortion,
//            uncertainty = uncertaintyFactor,
//            superposition = voidResonance,
//            entanglement = entanglementIntensity
//        )
//    }
//
////    /**
////     * Calculates the current intensity of void collapse based on the animation state.
////     * This creates a pulsing, unsettling effect that grows stronger as reality breaks down.
////     */
////    fun getCollapseIntensity(): Float {
////        val baseIntensity = when (currentState) {
////            AnimationState.SINGULARITY_FORMING -> {
////                // During singularity formation, create a rising wave of void energy
////                val progress = (stateProgress / singularityDuration).coerceIn(0f, 1f)
////                val waveComponent = sin(progress * PI.toFloat() * 2f) * 0.3f
////                val baseComponent = progress * 0.7f
////                (waveComponent + baseComponent) * (1f + realityCorruption * 0.5f)
////            }
////
////            AnimationState.VOID_MANIFESTATION -> {
////                // During void manifestation, create intense, chaotic pulses
////                val progress = (stateProgress / voidDuration).coerceIn(0f, 1f)
////                val rapidPulse = sin(progress * PI.toFloat() * 4f) * 0.25f
////                val slowPulse = cos(progress * PI.toFloat() * 1.5f) * 0.15f
////                0.6f + rapidPulse + slowPulse + (realityCorruption * 0.3f)
////            }
////
////            AnimationState.ASSIMILATION -> {
////                // During assimilation, create a deep, resonating pulse
////                val progress = (stateProgress / assimilationDuration).coerceIn(0f, 1f)
////                val depthPulse = sin(progress * PI.toFloat() * 3f) * 0.2f
////                val assimilationFactor = 1f - (progress * 0.4f)
////                0.8f + depthPulse * assimilationFactor + (realityCorruption * 0.4f)
////            }
////        }
////
////        // Add chaos field influence for more unsettling effect
////        val chaosInfluence = calculateChaosField(quantumTime) * 0.15f
////
////        // Combine with void resonance for final intensity
////        return (baseIntensity + chaosInfluence + voidResonance * 0.2f)
////            .coerceIn(0f, 1f)
////    }
//
//
//    private fun lerp(start: Float, end: Float, factor: Float): Float {
//        return start + (end - start) * factor.coerceIn(0f, 1f)
//    }
//}
//
///**
// * Encapsulates the current state of quantum corruption
// */
//data class QuantumStateInfo(
//    val isCollapsing: Boolean,
//    val amplitude: Float,
//    val uncertainty: Float,
//    val superposition: Float,
//    val entanglement: Float
//)
//
///**
// * Renders the manifestation of quantum corruption in visual form
// */
//class QuantumEntanglementRenderer {
//    companion object {
//        var shader: ShaderInstance? = null
//        private val timeController = QuantumTimeController()
//
//        // Configuration for void manifestation
//        private const val ORBITAL_LAYERS = 5           // Layers of reality corruption
//        private const val CUBES_PER_LAYER = 16        // Density of void presence
//        private const val BASE_CUBE_SIZE = 0.03f      // Size of void fragments
//        private const val ORBITAL_BASE_RADIUS = 0.2f   // Reach of void influence
//
//        // Movement parameters for unsettling effect
//        private const val POSITION_LERP_FACTOR = 0.05f  // Slow, creeping movement
//        private const val SCALE_LERP_FACTOR = 0.08f    // Gradual size changes
//        private const val COLOR_LERP_FACTOR = 0.06f    // Slow color corruption
//    }
//
//    /**
//     * Represents a single quantum-corrupted cube in space
//     */
//    private data class QuantumCubeState(
//        var currentPosition: Vector3f = Vector3f(0f, 0f, 0f),
//        var targetPosition: Vector3f = Vector3f(0f, 0f, 0f),
//        var currentScale: Float = 1f,
//        var targetScale: Float = 1f,
//        var currentVoidEnergy: Float = 0.5f,
//        var targetVoidEnergy: Float = 0.5f,
//        // Add velocity tracking for smoother movements
//        var scaleVelocity: Float = 0f,
//        var energyVelocity: Float = 0f,
//        var positionVelocity: Vector3f = Vector3f(0f, 0f, 0f)
//    ) {
//        fun updateState(transitionSpeed: Float, chaosField: Float) {
//            // Constants for smooth damping
//            val dampingFactor = 0.85f         // Reduces oscillation
//            val maxVelocity = 0.1f           // Prevents too rapid changes
//            val minTransitionSpeed = 0.02f    // Ensures some movement
//
//            // Calculate reality distortion with reduced magnitude
//            val distortionField = Vector3f(
//                sin(chaosField * 1.3f) * 0.015f,  // Reduced from 0.02f
//                cos(chaosField * 1.7f) * 0.015f,
//                sin(chaosField * 2.1f) * 0.015f
//            )
//
//            // Smooth position updates using velocity
//            val targetWithDistortion = targetPosition.add(distortionField)
//            val positionDelta = Vector3f(
//                targetWithDistortion.x - currentPosition.x,
//                targetWithDistortion.y - currentPosition.y,
//                targetWithDistortion.z - currentPosition.z
//            )
//
//            // Update position velocity with damping
//            positionVelocity.x = (positionVelocity.x * dampingFactor +
//                    positionDelta.x * transitionSpeed).coerceIn(-maxVelocity, maxVelocity)
//            positionVelocity.y = (positionVelocity.y * dampingFactor +
//                    positionDelta.y * transitionSpeed).coerceIn(-maxVelocity, maxVelocity)
//            positionVelocity.z = (positionVelocity.z * dampingFactor +
//                    positionDelta.z * transitionSpeed).coerceIn(-maxVelocity, maxVelocity)
//
//            // Apply velocity to position
//            currentPosition = Vector3f(
//                currentPosition.x + positionVelocity.x,
//                currentPosition.y + positionVelocity.y,
//                currentPosition.z + positionVelocity.z
//            )
//
//            // Smooth scale transitions
//            val scaleDelta = (targetScale * (1f + sin(chaosField) * 0.1f)) - currentScale // Reduced chaos influence
//            scaleVelocity = (scaleVelocity * dampingFactor +
//                    scaleDelta * transitionSpeed).coerceIn(-maxVelocity, maxVelocity)
//            currentScale += scaleVelocity
//
//            // Smooth void energy transitions with additional dampening
//            val targetEnergyWithNoise = targetVoidEnergy *
//                    (1f + cos(chaosField * 0.7f) * 0.15f) // Reduced from 0.2f
//            val energyDelta = targetEnergyWithNoise - currentVoidEnergy
//            energyVelocity = (energyVelocity * dampingFactor +
//                    energyDelta * transitionSpeed).coerceIn(-maxVelocity * 0.5f, maxVelocity * 0.5f)
//            currentVoidEnergy = (currentVoidEnergy + energyVelocity)
//                .coerceIn(0.2f, 1f)
//        }
//
//        private fun lerp(start: Float, end: Float, factor: Float): Float {
//            return start + (end - start) * factor
//        }
//
//        private fun lerpVector(start: Vector3f, end: Vector3f, factor: Float): Vector3f {
//            return Vector3f(
//                lerp(start.x, end.x, factor),
//                lerp(start.y, end.y, factor),
//                lerp(start.z, end.z, factor)
//            )
//        }
//    }
//
//    // Track void manifestation states
//    private val orbitalCubes = Array(ORBITAL_LAYERS) { layer ->
//        Array(CUBES_PER_LAYER) {
//            QuantumCubeState()
//        }
//    }
//
//    /**
//     * Creates specialized render type for void manifestation
//     */
//    private val renderType = RenderType.create(
//        "${Bpm.ID}_quantum_cube",
//        DefaultVertexFormat.POSITION_TEX_COLOR,
//        VertexFormat.Mode.QUADS,
//        512,
//        false,
//        true,
//        RenderType.CompositeState.builder()
//            .setShaderState(RenderStateShard.ShaderStateShard { shader })
//            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
//            .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
//            .setCullState(RenderStateShard.NO_CULL)
//            .setLightmapState(RenderStateShard.NO_LIGHTMAP)
//            .createCompositeState(true)
//    )
//
//    /**
//     * Renders a single void-corrupted cube
//     */
//    private fun renderQuantumCube(
//        matrix: Matrix4f,
//        buffer: VertexConsumer,
//        cubeState: QuantumCubeState,
//        voidResonance: Float
//    ) {
//        // Calculate void-influenced colors with gentler transitions
//        val energyFactor = cubeState.currentVoidEnergy * (1f - voidResonance * 0.2f) // Reduced from 0.3f
//
//        // More subtle color variations
//        val baseColor = arrayOf(
//            0.15f + energyFactor * 0.15f,     // Gentler red variation
//            0.35f + energyFactor * 0.25f,     // Smoother green transition
//            0.75f + energyFactor * 0.25f,     // More stable blue base
//            // Smoother alpha transitions with tighter bounds
//            (0.55f + energyFactor * 0.35f).coerceIn(0.45f, 0.85f)
//        )
//
//        // Define void-touched faces
//        val normals = arrayOf(
//            Vector3f(1f, 0f, 0f),   // Right face
//            Vector3f(-1f, 0f, 0f),  // Left face
//            Vector3f(0f, 1f, 0f),   // Top face
//            Vector3f(0f, -1f, 0f),  // Bottom face
//            Vector3f(0f, 0f, 1f),   // Front face
//            Vector3f(0f, 0f, -1f)   // Back face
//        )
//
//        // Render each corrupted face
//        for (normal in normals) {
//            renderVoidFace(
//                matrix,
//                buffer,
//                cubeState.currentPosition,
//                cubeState.currentScale,
//                normal,
//                baseColor,
//                15728880  // Maximum light level for void glow
//            )
//        }
//    }
//
//    /**
//     * Renders a single face of a void-corrupted cube
//     */
//    private fun renderVoidFace(
//        matrix: Matrix4f,
//        buffer: VertexConsumer,
//        pos: Vector3f,
//        size: Float,
//        normal: Vector3f,
//        color: Array<Float>,
//        light: Int
//    ) {
//        // Calculate vertices based on void influence
//        val (v1, v2, v3, v4) = calculateVoidVertices(normal, size)
//
//        // Render the corrupted face
//        for (vertex in arrayOf(v1, v2, v3, v4)) {
//            buffer.addVertex(
//                matrix,
//                pos.x + vertex.x,
//                pos.y + vertex.y,
//                pos.z + vertex.z
//            )
//                .setColor(color[0], color[1], color[2], color[3])
//                .setUv(0.5f + normal.x * 0.5f, 0.5f + normal.y * 0.5f)
//                .setNormal(normal.x, normal.y, normal.z)
//                .setLight(light)
//        }
//    }
//
//    /**
//     * Calculates void-influenced vertex positions
//     */
//    private fun calculateVoidVertices(normal: Vector3f, size: Float): Array<Vector3f> {
//        return when {
//            normal.x != 0f -> arrayOf(
//                Vector3f(normal.x * size, -size, -size),
//                Vector3f(normal.x * size, size, -size),
//                Vector3f(normal.x * size, size, size),
//                Vector3f(normal.x * size, -size, size)
//            )
//            normal.y != 0f -> arrayOf(
//                Vector3f(-size, normal.y * size, -size),
//                Vector3f(-size, normal.y * size, size),
//                Vector3f(size, normal.y * size, size),
//                Vector3f(size, normal.y * size, -size)
//            )
//            else -> arrayOf(
//                Vector3f(-size, -size, normal.z * size),
//                Vector3f(size, -size, normal.z * size),
//                Vector3f(size, size, normal.z * size),
//                Vector3f(-size, size, normal.z * size)
//            )
//        }
//    }
//
//    /**
//     * Main render function for void manifestation
//     */
//    fun render(
//        stack: ItemStack,
//        type: ItemDisplayContext,
//        poseStack: PoseStack,
//        buffer: MultiBufferSource,
//        light: Int,
//        overlay: Int
//    ) {
//        // Update the flow of corrupted time
//        val currentTime = timeController.updateTime()
//
//        poseStack.pushPose()
//
//        // Adjust visualization based on display context
//        when (type) {
//            ItemDisplayContext.GUI -> {
//                // Center the void manifestation in GUI
//                poseStack.translate(0.5f, 0.5f, 0.0f)
//                // Increase scale for more imposing presence
//                poseStack.scale(1.3f, 1.3f, 1.3f)
//                // Rotate to show depth of void
//                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(45f))
//            }
//
//            else -> {
//                // Position void manifestation in world space
//                poseStack.translate(0.5f, 0.5f, 0.5f)
//                // Slightly reduced scale for ominous hovering effect
//                poseStack.scale(0.7f, 0.7f, 0.7f)
//            }
//        }
//
//        // Update shader with void corruption parameters
//        shader?.let { shaderInstance ->
//            // Transmit current time to shader for void effects
//            shaderInstance.safeGetUniform("GameTime")?.set(currentTime)
//            // Update collapse intensity for reality distortion
//            shaderInstance.safeGetUniform("CollapseIntensity")?.set(timeController.getCollapseIntensity())
//            shaderInstance.markDirty()
//        }
//
//        // Get current quantum state for void manifestation
//        val state = timeController.updateQuantumState(currentTime)
//
//        // Prepare vertex buffer for void rendering
//        val vertexBuffer = buffer.getBuffer(renderType)
//        val pose = poseStack.last()
//
//        // Update and render each orbital cube layer
//        updateOrbitalCubes(currentTime, state)
//        renderVoidManifestation(pose.pose(), vertexBuffer, state)
//
//        if (buffer is MultiBufferSource.BufferSource) {
//            buffer.endBatch(renderType)
//        }
//        poseStack.popPose()
//    }
//
//    /**
//     * Updates the state of quantum-corrupted cubes based on void influence
//     */
//    private fun updateOrbitalCubes(currentTime: Float, quantumState: QuantumStateInfo) {
//        for (layer in 0 until ORBITAL_LAYERS) {
//            // Calculate void radius for this layer
//            val layerRadius = ORBITAL_BASE_RADIUS * (1f + layer * 0.25f)
//            // Each layer has a unique phase in the void
//            val layerPhase = layer * PI.toFloat() / ORBITAL_LAYERS +
//                    sin(currentTime * 0.3f) * 0.2f
//
//            for (i in 0 until CUBES_PER_LAYER) {
//                val cube = orbitalCubes[layer][i]
//
//                // Calculate base angle with void distortion
//                val baseAngle = (i * 2f * PI.toFloat() / CUBES_PER_LAYER) +
//                        currentTime * (0.2f + layer * 0.15f) +
//                        layerPhase +
//                        sin(currentTime * 1.5f + layer * 0.5f) * 0.1f
//
//                // Calculate vertical displacement with void influence
//                val verticalOffset = sin(baseAngle + currentTime) *
//                        (0.08f + quantumState.uncertainty * 0.15f) +
//                        cos(currentTime * 0.7f + layer * 0.3f) * 0.05f
//
//                // Calculate target position with void distortion
//                val targetPos = Vector3f(
//                    cos(baseAngle) * layerRadius * (1f + quantumState.uncertainty * 0.15f),
//                    sin(baseAngle) * layerRadius * (1f + quantumState.uncertainty * 0.15f),
//                    verticalOffset * layer * 0.2f
//                )
//
//                // Calculate scale with void resonance
//                val baseScale = BASE_CUBE_SIZE *
//                        (0.8f + quantumState.superposition * 0.4f) *
//                        (1f + sin(currentTime * 2.5f + i * 0.7f) * 0.1f)
//
//                // Update cube properties
//                cube.targetPosition = targetPos
//                cube.targetScale = baseScale
//                cube.targetVoidEnergy = 0.3f + quantumState.amplitude * 0.6f
//
//                // Calculate transition speed based on void state
//                val transitionSpeed = when {
//                    quantumState.isCollapsing -> 0.04f + quantumState.uncertainty * 0.02f
//                    else -> 0.06f + quantumState.uncertainty * 0.05f
//                }
//
//                // Update cube state with void influence
//                cube.updateState(
//                    transitionSpeed,
//                    sin(currentTime * 1.2f + layer * 0.4f + i * 0.2f)
//                )
//            }
//        }
//    }
//
//    /**
//     * Renders the complete void manifestation
//     */
//    private fun renderVoidManifestation(
//        matrix: Matrix4f,
//        buffer: VertexConsumer,
//        state: QuantumStateInfo
//    ) {
//        // Sort cubes by distance for proper transparency
//        val sortedCubes = orbitalCubes.flatten().sortedByDescending { cube ->
//            cube.currentPosition.z
//        }
//
//        // Render each void-corrupted cube
//        for (cube in sortedCubes) {
//            renderQuantumCube(
//                matrix,
//                buffer,
//                cube,
//                state.superposition
//            )
//        }
//    }
//
//    /**
//     * Calculates void collapse intensity for shader effects
//     */
//    fun getCollapseIntensity(): Float {
//        return timeController.getCollapseIntensity()
//    }
//
//    /**
//     * Helper function for linear interpolation with void influence
//     */
//    private fun lerp(start: Float, end: Float, factor: Float): Float {
//        return start + (end - start) * factor.coerceIn(0f, 1f)
//    }
//
//    /**
//     * Helper function for vector interpolation in void-corrupted space
//     */
//    private fun lerpVector(start: Vector3f, end: Vector3f, factor: Float): Vector3f {
//        return Vector3f(
//            lerp(start.x, end.x, factor),
//            lerp(start.y, end.y, factor),
//            lerp(start.z, end.z, factor)
//        )
//    }
//}


package bpm.client.render.world

import com.mojang.blaze3d.vertex.*
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.*

/**
 * Renders quantum particles and their wave functions with visually distinctive effects
 * that represent quantum mechanical properties like superposition and entanglement.
 */
class QuantumRenderer {

    companion object {

        // Shader for quantum effects
        var shader: ShaderInstance? = null
        private val timeController = QuantumTimeController()

        // Visualization parameters
        private const val WAVE_FUNCTION_LAYERS = 32    // Resolution of probability field
        private const val PARTICLE_COUNT = 16         // Number of quantum particles
        private const val BASE_PARTICLE_SIZE = 0.12f  // Core particle size
        private const val ORBITAL_RADIUS = 0.23f      // Quantum orbital radius
    }

    // Track quantum particles and their visual states
    private val particles = Array(PARTICLE_COUNT) { QuantumParticleVisual() }

    // Create specialized render type for quantum effects
    private val renderType = RenderType.create(
        "quantum_particle",
        DefaultVertexFormat.POSITION_TEX_COLOR,
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

    /**
     * Represents the visual state of a quantum particle
     */
    private class QuantumParticleVisual {

        var position = Vector3f()
        var waveScale = 1.0f
        var probability = 1.0f
        var phase = 0.0f

        // Visual effects for quantum properties
        var superpositionEffect = 0.0f
        var entanglementGlow = 0.0f
        var uncertaintyDistortion = 0.0f
    }

    /**
     * Main render function that visualizes the quantum state
     */
    fun render(
        stack: ItemStack,
        type: ItemDisplayContext,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        // Update quantum time evolution
        val currentTime = timeController.updateTime()

        poseStack.pushPose()
        setupTransforms(type, poseStack)

        // Update shader parameters
        updateShaderEffects(currentTime)

        // Get vertex buffer and pose matrix
        val vertexBuffer = buffer.getBuffer(renderType)
        val pose = poseStack.last().pose()

        // Render quantum visualization
        renderQuantumState(currentTime, pose, vertexBuffer)

        if (buffer is MultiBufferSource.BufferSource) {
            buffer.endBatch(renderType)
        }
        poseStack.popPose()
    }

    /**
     * Sets up transformation matrices based on display context
     */
    private fun setupTransforms(type: ItemDisplayContext, poseStack: PoseStack) {
        when (type) {
            ItemDisplayContext.GUI -> {
                poseStack.translate(.33f, 0.75f, 0.0f)
//                poseStack.scale(1, 1f)
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(0f))
            }

            else -> {
                poseStack.translate(0.5f, 0.5f, 0.5f)
                poseStack.scale(0.5f, 0.5f, 0.5f)
            }
        }
    }

    /**
     * Updates shader parameters for quantum effects
     */
    private fun updateShaderEffects(currentTime: Float) {
        shader?.let { shaderInstance ->
            shaderInstance.safeGetUniform("GameTime")?.set(currentTime)
            shaderInstance.safeGetUniform("QuantumEffects")?.set(
                calculateQuantumEffectIntensity(currentTime)
            )
            shaderInstance.markDirty()
        }
    }

    /**
     * Calculates overall quantum effect intensity for visual effects
     */
    private fun calculateQuantumEffectIntensity(time: Float): Float {
        val baseIntensity = (sin(time * 0.5f) * 0.3f + 0.7f)
        val quantumFluctuation = cos(time * 2.0f) * 0.15f
        return (baseIntensity + quantumFluctuation).coerceIn(0.0f, 1.0f)
    }

    /**
     * Renders the complete quantum state
     */
    private fun renderQuantumState(
        currentTime: Float,
        matrix: Matrix4f,
        buffer: VertexConsumer
    ) {
        // Update and render each quantum particle
        particles.forEachIndexed { index, particle ->
            updateParticleVisuals(particle, currentTime, index.toFloat())
            renderQuantumParticle(matrix, buffer, particle)
        }

        //Scale the wave function visualization
        matrix.scale(0.5f)
        renderWaveFunction(matrix, buffer, currentTime)
    }

    /**
     * Updates visual properties of quantum particles
     */
    private fun updateParticleVisuals(
        particle: QuantumParticleVisual,
        time: Float,
        index: Float
    ) {
        // Calculate orbital motion
        val angle = time * 0.5f + index * (2.0f * PI.toFloat() / PARTICLE_COUNT)

        // Add quantum uncertainty to position
        val uncertainty = sin(time * 2.0f + index) * 0.05f
        particle.position = Vector3f(
            cos(angle) * ORBITAL_RADIUS + uncertainty,
            sin(angle) * ORBITAL_RADIUS + uncertainty,
            sin(time + index) * 0.1f
        )

        // Update quantum visual effects
        particle.phase = (particle.phase + time * 0.1f) % (2.0f * PI.toFloat())
        particle.waveScale = 1.0f + sin(time + index) * 0.2f
        particle.probability = (cos(time * 0.5f + index) * 0.3f + 0.7f)
            .coerceIn(0.0f, 1.0f)

        // Calculate quantum effect intensities
        particle.superpositionEffect = sin(time * 2.0f + index) * 0.5f + 0.5f
        particle.entanglementGlow = cos(time + index) * 0.4f + 0.6f
        particle.uncertaintyDistortion = sin(time * 3.0f + index) * 0.3f
    }

    /**
     * Renders a single quantum particle with its quantum effects
     */
    private fun renderQuantumParticle(
        matrix: Matrix4f,
        buffer: VertexConsumer,
        particle: QuantumParticleVisual
    ) {
        // Calculate particle color based on quantum properties
        var color = Vector4f(
            0.3f + particle.superpositionEffect * 0.4f,
            0.5f + particle.entanglementGlow * 0.3f,
            0.7f + particle.probability * 0.3f,
            0.6f + particle.uncertaintyDistortion * 0.2f
        )

        // Render particle core
        renderParticleCore(matrix, buffer, particle, color)

        // Render quantum effects
        renderQuantumEffects(matrix, buffer, particle, color)
    }

    /**
     * Renders the core of a quantum particle
     */
    private fun renderParticleCore(
        matrix: Matrix4f,
        buffer: VertexConsumer,
        particle: QuantumParticleVisual,
        color: Vector4f
    ) {
        val size = BASE_PARTICLE_SIZE * particle.waveScale

        // Render particle as a billboarded quad
        renderQuad(
            matrix,
            buffer,
            particle.position,
            size,
            color
        )
    }

    /**
     * Renders quantum mechanical effects around the particle
     */
    private fun renderQuantumEffects(
        matrix: Matrix4f,
        buffer: VertexConsumer,
        particle: QuantumParticleVisual,
        color: Vector4f
    ) {
        // Render superposition effect
        if (particle.superpositionEffect > 0.3f) {
            val ghostColor = Vector4f(color)
            ghostColor.w *= 0.3f

            // Render ghost particles showing superposition
            val ghostOffset = 0.05f * particle.superpositionEffect
            renderQuad(
                matrix,
                buffer,
                particle.position.add(Vector3f(ghostOffset, ghostOffset, 0f)),
                BASE_PARTICLE_SIZE * 1.2f,
                ghostColor
            )
        }

        // Render entanglement glow
        if (particle.entanglementGlow > 0.5f) {
            val glowColor = Vector4f(color)
            glowColor.w *= 0.4f
            renderQuad(
                matrix,
                buffer,
                particle.position,
                BASE_PARTICLE_SIZE * 2f * particle.entanglementGlow,
                glowColor
            )
        }
    }

    /**
     * Renders a spinning cube in the center
     */
    private fun renderWaveFunction(
        matrix: Matrix4f,
        buffer: VertexConsumer,
        time: Float
    )   {
        val size = 0.5f
        val rotation = time * 0.1f

        // Define vertices for spinning cube
        val vertices = arrayOf(
            Vector3f(-size, -size, -size),
            Vector3f(size, -size, -size),
            Vector3f(size, size, -size),
            Vector3f(-size, size, -size),
            Vector3f(-size, -size, size),
            Vector3f(size, -size, size),
            Vector3f(size, size, size),
            Vector3f(-size, size, size)
        )

        // Render cube faces
        val colors = arrayOf(
            Vector4f(0.8f, 0.2f, 0.2f, 0.6f),
            Vector4f(0.2f, 0.8f, 0.2f, 0.6f),
            Vector4f(0.2f, 0.2f, 0.8f, 0.6f),
            Vector4f(0.8f, 0.8f, 0.2f, 0.6f),
            Vector4f(0.8f, 0.2f, 0.8f, 0.6f),
            Vector4f(0.2f, 0.8f, 0.8f, 0.6f)
        )

        for (i in 0 until 6) {
            val color = colors[i]
            val face = arrayOf(
                vertices[i
                ],
                vertices[(i + 1) % 4],
                vertices[(i + 5) % 8],
                vertices[(i + 4) % 8]
            )

            renderQuad(matrix, buffer, Vector3f(0f, 0f, 0f), size, color)
        }
    }

    /**
     * Calculates wave function probability at a point
     */
    private fun calculateWaveProbability(x: Float, y: Float, time: Float): Float {
        var probability = 0f

        // Combine probability contributions from all particles
        particles.forEach { particle ->
            val dx = x - particle.position.x
            val dy = y - particle.position.y
            val distance = sqrt(dx * dx + dy * dy)

            // Gaussian wave packet
            probability += particle.probability * exp(-distance * distance * 2f) *
                    (1f + cos(particle.phase + distance * 5f)) * 0.5f
        }

        return probability.coerceIn(0f, 1f)
    }

    /**
     * Renders a quad at the specified position
     */
    private fun renderQuad(
        matrix: Matrix4f,
        buffer: VertexConsumer,
        position: Vector3f,
        size: Float,
        color: Vector4f
    ) {
        val halfSize = size / 2f

        // Define vertices
        val vertices = arrayOf(
            Vector3f(-halfSize, -halfSize, 0f),
            Vector3f(halfSize, -halfSize, 0f),
            Vector3f(halfSize, halfSize, 0f),
            Vector3f(-halfSize, halfSize, 0f)
        )

        // Render quad
        vertices.forEach { vertex ->
            buffer.addVertex(
                matrix,
                position.x + vertex.x,
                position.y + vertex.y,
                position.z + vertex.z
            )
                .setColor(color.x, color.y, color.z, color.w)
                .setUv(0.5f, 0.5f)
        }
    }
}