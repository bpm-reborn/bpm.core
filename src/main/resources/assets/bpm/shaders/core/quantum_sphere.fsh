#version 150

in vec2 texCoord;
in vec4 vertexColor;
in vec3 worldPos;
in vec4 quantumState;
in vec3 vertexPosition;

uniform float GameTime;

out vec4 fragColor;

float rand(vec2 p) {
    return fract(sin(dot(p.xy, vec2(12.9898,78.233))) * 43758.5453);
}

// Improved noise function for more digital patterns
float digitalNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f); // Smoother step

    float a = rand(i);
    float b = rand(i + vec2(1.0, 0.0));
    float c = rand(i + vec2(0.0, 1.0));
    float d = rand(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// Circuit pattern generation
float circuitPattern(vec2 uv, float time) {
    float pattern = 0.0;

    // Create grid lines
    vec2 grid = abs(fract(uv * 8.0 + time * 0.1) - 0.5);
    pattern += step(0.48, max(grid.x, grid.y)) * 0.5;

    // Add pulsing nodes
    vec2 nodes = fract(uv * 4.0);
    float nodePulse = sin(time * 2.0) * 0.5 + 0.5;
    pattern += smoothstep(0.4 + nodePulse * 0.1, 0.5 + nodePulse * 0.1,
                         length(nodes - 0.5)) * 0.5;

    return pattern;
}

// Enhanced FBM for more electronic-looking patterns
float fbm(vec2 x) {
    float v = 0.0;
    float a = 0.5;
    vec2 shift = vec2(100);
    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));

    for (int i = 0; i < 4; ++i) {
        v += a * digitalNoise(x);
        x = rot * x * 2.0 + shift + GameTime * 0.05;
        a *= 0.5;
    }
    return v;
}

void main() {
    // Base quantum state visualization
    vec3 qColor = normalize(quantumState.xyz) * 0.5 + 0.5;

    // Circuit-like pattern
    float circuit = circuitPattern(texCoord, GameTime);

    // Dynamic energy flow effect
    float energyFlow = fbm(texCoord * 3.0 + GameTime * 0.2);

    // Create sharp color bands based on quantum state
    vec3 baseColor = mix(
        vec3(0.0, 0.2, 0.4),  // Deep blue base
        vec3(0.1, 0.6, 1.0),  // Bright cyan
        step(0.5, energyFlow)
    );

    // Add electric highlights
    float highlight = pow(energyFlow, 3.0) * sin(GameTime * 4.0) * 0.5 + 0.5;
    baseColor += vec3(0.2, 0.8, 1.0) * highlight;

    // Mix in circuit pattern
    baseColor = mix(baseColor, vec3(0.0, 1.0, 0.8), circuit * 0.5);

    // Quantum state influence
    vec3 finalColor = mix(baseColor, qColor, quantumState.y);

    // Add pulsing glow based on quantum state
    float glowPulse = sin(GameTime * 3.0 + length(vertexPosition) * 5.0) * 0.5 + 0.5;
    finalColor += vec3(0.0, 0.5, 1.0) * glowPulse * quantumState.x;

    // Sharp edge highlights
    float edge = 1.0 - smoothstep(0.45, 0.5, length(texCoord - 0.5));
    finalColor += vec3(0.2, 0.8, 1.0) * edge * quantumState.w;

    // Dynamic alpha based on quantum state and patterns
    float alpha = mix(
        vertexColor.a,
        vertexColor.a * (0.8 + circuit * 0.2),
        quantumState.z
    );

    // Add subtle scanline effect
    float scanline = sin(vertexPosition.y * 50.0 + GameTime * 2.0) * 0.5 + 0.5;
    finalColor *= 0.9 + scanline * 0.1;

    fragColor = vec4(finalColor, alpha);
}