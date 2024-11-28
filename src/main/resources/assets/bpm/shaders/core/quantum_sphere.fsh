#version 150

in vec2 texCoord;
in vec4 vertexColor;
in vec3 worldPos;
in float quantumEffect;
in vec4 quantumState;

uniform float GameTime;

out vec4 fragColor;

// Schrödinger equation inspired wave function coloring
vec3 quantumColor(vec3 position, float time) {
    // Base quantum state colors
    vec3 groundState = vec3(0.1, 0.4, 0.8);  // Blue for ground state
    vec3 excitedState = vec3(0.8, 0.2, 0.9); // Purple for excited state
    vec3 entangledState = vec3(0.9, 0.1, 0.3); // Red for entanglement

    // Wave function phase
    float phase = dot(position, vec3(1.0, 1.0, 1.0)) * 2.0 + time;
    float psi = sin(phase) * 0.5 + 0.5;

    // Probability density
    float probability = exp(-dot(position, position));

    // Combine states based on quantum parameters
    vec3 color = mix(
        groundState,
        excitedState,
        quantumState.x  // Amplitude determines excitation
    );

    // Add entanglement effects
    color = mix(
        color,
        entangledState,
        quantumState.w * probability  // Entanglement with spatial probability
    );

    return color * (0.8 + psi * 0.4);
}

// Creates quantum interference patterns
float interferencePattern(vec3 pos, float time) {
    float k1 = dot(pos, vec3(1.0, 0.0, 0.0));
    float k2 = dot(pos, vec3(-0.5, 0.866, 0.0));
    float psi1 = sin(k1 * 10.0 + time);
    float psi2 = sin(k2 * 10.0 + time);
    return (psi1 + psi2) * 0.5 + 0.5;
}

void main() {
    // Calculate base quantum color
    vec3 qColor = quantumColor(worldPos, GameTime);

    // Add interference effects
    float interference = interferencePattern(worldPos, GameTime);
    qColor += vec3(0.2, 0.0, 0.4) * interference * quantumState.z;

    // Add uncertainty effects
    float uncertainty = sin(GameTime * 2.0 + length(worldPos) * 5.0) *
                       quantumState.y * 0.3;
    qColor = mix(qColor, vec3(0.5, 0.2, 0.8), uncertainty);

    // Calculate alpha based on quantum state
    float alpha = vertexColor.a * (0.7 + quantumEffect * 0.3);

    // Add quantum glow
    float glow = exp(-length(texCoord - vec2(0.5)) * 3.0);
    qColor += vec3(0.4, 0.1, 0.6) * glow * quantumState.x;

    fragColor = vec4(qColor, alpha);
}