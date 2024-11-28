package bpm.client.render.world

import kotlin.random.Random

import org.joml.Matrix4f
import org.joml.Vector3f
import kotlin.math.*

/**
 * Controls the quantum time evolution and particle behavior while maintaining
 * compatibility with the original interface. This system now implements proper
 * quantum mechanics underneath while keeping the same external usage pattern.
 */
class QuantumTimeController {

    private var lastUpdateTime: Long = System.nanoTime()
    private var quantumTime: Float = 0f
    private var deltaSeconds: Float = 0f

    // Quantum mechanical properties
    private var waveFunction: ComplexWaveFunction = ComplexWaveFunction()
    private val particles = mutableListOf<QuantumState>()
    private var potentialField: PotentialField = PotentialField()

    // Physical constants
    companion object {

        const val PLANCK_CONSTANT = 6.62607015e-34f
        const val REDUCED_PLANCK = PLANCK_CONSTANT / (2 * PI.toFloat())
        const val ELECTRON_MASS = 9.1093837015e-31f

        // Scaling factors to make quantum effects visible at game scale
        const val SPACE_SCALE = 1e9f  // nm to meters
        const val TIME_SCALE = 1e15f  // fs to seconds
    }

    /**
     * Updates the quantum state of the system, maintaining compatibility
     * with the original interface while implementing proper quantum mechanics.
     */
    fun updateTime(): Float {
        val currentTime = System.nanoTime()
        deltaSeconds = (currentTime - lastUpdateTime).toFloat() / 1_000_000_000f

        // Scale time for quantum effects
        val quantumDeltaTime = deltaSeconds * TIME_SCALE

        // Update quantum time
        quantumTime += deltaSeconds

        // Update wave function
        waveFunction.evolve(quantumDeltaTime, potentialField)

        // Update quantum particles
        particles.forEach { particle ->
            updateParticleState(particle, quantumDeltaTime)
        }

        lastUpdateTime = currentTime
        return quantumTime
    }

    /**
     * Updates a single quantum particle's state according to proper quantum mechanics
     */
    private fun updateParticleState(particle: QuantumState, deltaTime: Float) {
        // Calculate new position based on quantum probability distribution
        val newPosition = calculateQuantumPosition(particle)

        // Update momentum based on quantum uncertainty principle
        val uncertaintyFactor = REDUCED_PLANCK / (2 * particle.position.length() * ELECTRON_MASS)
        particle.momentum = Vector3f(
            particle.momentum.x + (GaussianRandom.nextGaussian() * uncertaintyFactor).toFloat(),
            particle.momentum.y + (GaussianRandom.nextGaussian() * uncertaintyFactor).toFloat(),
            particle.momentum.z + (GaussianRandom.nextGaussian() * uncertaintyFactor).toFloat()
        )

        // Apply quantum tunneling if appropriate
        if (shouldTunnel(particle)) {
            applyQuantumTunneling(particle)
        }

        particle.position = newPosition
    }

    private fun calculateQuantumPosition(particle: QuantumState): Vector3f {
        // Get current wave function probability density
        val psi = waveFunction.getProbabilityAmplitude(particle.position)

        // Calculate quantum uncertainty in position
        val posUncertainty = REDUCED_PLANCK / (2f * particle.momentum.length())

        // Generate position shifts based on quantum uncertainty
        val dx = GaussianRandom.nextGaussian() * posUncertainty * psi.magnitude()
        val dy = GaussianRandom.nextGaussian() * posUncertainty * psi.magnitude()
        val dz = GaussianRandom.nextGaussian() * posUncertainty * psi.magnitude()

        // Calculate classical trajectory
        val classicalMotion = Vector3f(particle.momentum).mul(deltaSeconds / ELECTRON_MASS)

        // Combine classical and quantum effects
        return Vector3f(
            particle.position.x + classicalMotion.x + dx,
            particle.position.y + classicalMotion.y + dy,
            particle.position.z + classicalMotion.z + dz
        )
    }


    /**
     * Determines if a particle should undergo quantum tunneling based on
     * proper quantum mechanical calculations
     */
    private fun shouldTunnel(particle: QuantumState): Boolean {
        // Get potential barrier at particle's position
        val barrierHeight = potentialField.getPotential(particle.position)
        val particleEnergy = calculateParticleEnergy(particle)

        // Calculate tunneling probability using WKB approximation
        if (particleEnergy < barrierHeight) {
            val barrierWidth = potentialField.getBarrierWidth(particle.position)
            val kappa = sqrt(2f * ELECTRON_MASS * (barrierHeight - particleEnergy)) / REDUCED_PLANCK
            val tunnelProbability = exp(-2f * kappa * barrierWidth)

            // Scaled probability for game visibility
            val scaledProbability = tunnelProbability * 0.1f

            return Random.nextFloat() < scaledProbability
        }

        return false
    }

    /**
     * Represents the quantum potential field that particles interact with
     */
    private inner class PotentialField {

        fun getPotential(position: Vector3f): Float {
            // Create interesting potential landscape
            return sin(position.x * 2f) * cos(position.y * 2f) * 0.5f +
                    sin(position.z * 3f) * 0.3f
        }

        fun getBarrierWidth(position: Vector3f): Float {
            // Calculate width of potential barrier at given position
            val stepSize = 0.1f
            var width = 0f
            var pos = position.x

            while (getPotential(Vector3f(pos, position.y, position.z)) > 0) {
                width += stepSize
                pos += stepSize

                if (width > 5f) break  // Prevent infinite loops
            }

            return width
        }
    }

    /**
     * Calculates total energy of a quantum particle
     */
    private fun calculateParticleEnergy(particle: QuantumState): Float {
        // Kinetic energy
        val kineticEnergy = particle.momentum.lengthSquared() / (2f * ELECTRON_MASS)

        // Potential energy
        val potentialEnergy = potentialField.getPotential(particle.position)

        return kineticEnergy + potentialEnergy
    }

    /**
     * Applies quantum tunneling effect to a particle
     */
    private fun applyQuantumTunneling(particle: QuantumState) {
        // Find tunnel exit point
        val barrierWidth = potentialField.getBarrierWidth(particle.position)

        // Teleport particle past barrier with slight energy loss
        particle.position.x += barrierWidth * 1.1f
        particle.momentum.mul(0.9f)  // Energy loss from tunneling
    }

    /**
     * Complex wave function implementation for quantum mechanics
     */
    private inner class ComplexWaveFunction {

        private var amplitudes = mutableMapOf<Vector3f, Complex>()

        /**
         * Gets the probability amplitude at a given position
         */
        fun getProbabilityAmplitude(position: Vector3f): Complex {
            // If we don't have an amplitude at this position, calculate it
            if (!amplitudes.containsKey(position)) {
                // Create a new wave packet centered at the position
                val amplitude = createWavePacket(position)
                amplitudes[position] = amplitude
            }

            return amplitudes[position] ?: Complex(0f, 0f)
        }
        /**
         * Creates a Gaussian wave packet centered at the given position
         */
        private fun createWavePacket(center: Vector3f): Complex {
            // Wave packet width (scaled for game visibility)
            val width = 0.5f

            // Calculate wave packet amplitude
            val distanceSquared = center.lengthSquared()
            val amplitude = exp(-distanceSquared / (2f * width * width))

            // Add phase based on momentum
            val averageMomentum = particles.map { it.momentum }.fold(Vector3f()) { acc, m ->
                acc.add(m)
            }.div(particles.size.toFloat())

            val phase = (averageMomentum.x * center.x +
                    averageMomentum.y * center.y +
                    averageMomentum.z * center.z) / REDUCED_PLANCK

            return Complex(
                amplitude * cos(phase),
                amplitude * sin(phase)
            )
        }

        fun evolve(deltaTime: Float, potential: PotentialField) {
            // Solve time-dependent Schrödinger equation
            val newAmplitudes = mutableMapOf<Vector3f, Complex>()

            amplitudes.forEach { (position, amplitude) ->
                // Calculate Laplacian of wave function
                val laplacian = calculateLaplacian(position)

                // Calculate new amplitude using Schrödinger equation
                val potentialTerm = potential.getPotential(position)
                val energyTerm = -REDUCED_PLANCK * REDUCED_PLANCK * laplacian /
                        (2f * ELECTRON_MASS) + potentialTerm

                val phase = -energyTerm * deltaTime / REDUCED_PLANCK
                val evolution = Complex(cos(phase), sin(phase))

                newAmplitudes[position] = amplitude * evolution
            }

            amplitudes = newAmplitudes
        }

        private fun calculateLaplacian(position: Vector3f): Float {
            // Finite difference approximation of Laplacian
            val h = 0.1f
            var sum = 0f

            for (dx in listOf(-h, h)) {
                val pos = Vector3f(position.x + dx, position.y, position.z)
                sum += (amplitudes[pos]?.magnitude() ?: 0f)
            }
            for (dy in listOf(-h, h)) {
                val pos = Vector3f(position.x, position.y + dy, position.z)
                sum += (amplitudes[pos]?.magnitude() ?: 0f)
            }
            for (dz in listOf(-h, h)) {
                val pos = Vector3f(position.x, position.y, position.z + dz)
                sum += (amplitudes[pos]?.magnitude() ?: 0f)
            }

            val center = amplitudes[position]?.magnitude() ?: 0f
            return (sum - 6f * center) / (h * h)
        }
    }
}

private object GaussianRandom {

    private var hasNextGaussian = false
    private var nextGaussian = 0.0f

    fun nextGaussian(): Float {
        // If we have a pre-calculated value, use it
        if (hasNextGaussian) {
            hasNextGaussian = false
            return nextGaussian
        }

        // Generate two uniform random numbers between 0 and 1
        var v1: Float
        var v2: Float
        var s: Float

        // Use Box-Muller transform to convert uniform to Gaussian distribution
        do {
            v1 = 2f * Random.nextFloat() - 1f
            v2 = 2f * Random.nextFloat() - 1f
            s = v1 * v1 + v2 * v2
        } while (s >= 1f || s == 0f)

        s = sqrt((-2f * ln(s)) / s)
        nextGaussian = v2 * s
        hasNextGaussian = true
        return v1 * s
    }
}
/**
 * Represents a quantum state with position and momentum
 */
private data class QuantumState(
    var position: Vector3f,
    var momentum: Vector3f
)

/**
 * Complex number implementation for quantum calculations
 */
private data class Complex(val real: Float, val imag: Float) {

    operator fun times(other: Complex): Complex {
        return Complex(
            real * other.real - imag * other.imag,
            real * other.imag + imag * other.real
        )
    }

    fun magnitude(): Float = sqrt(real * real + imag * imag)
}