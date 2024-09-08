#version 150

uniform float iTime;
uniform float circleRadius;
uniform vec2 iResolution;
uniform vec3 iColor;
uniform float pulse;
out vec4 fragColor;

#define PI 3.14159265359
#define TWO_PI 6.28318530718

// Smooth noise function
float hash(vec2 p) {
    float h = dot(p, vec2(127.1, 311.7));
    return fract(sin(h) * 43758.5453123);
}

float noise(in vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

// Fractal Brownian Motion
float fbm(vec2 uv) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 0.0;
    for (int i = 0; i < 6; ++i) {
        value += amplitude * noise(uv);
        uv *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

// Star field function
float starField(vec2 uv, float threshold) {
    float n = hash(uv);
    return smoothstep(threshold, threshold + 0.02, n);
}

void main() {
    vec2 uv = gl_FragCoord.xy / iResolution.xy;
    uv = uv * 2.0 - 1.0;
    uv.x *= iResolution.x / iResolution.y;

    float time = iTime * 0.05;

    // Create base flow pattern
    float flowPattern = fbm(uv * 2.0 + time);

    // Use circleRadius for a void-like effect
    float radial = length(uv);
    float voidFactor = smoothstep(0.0, circleRadius * 1.5, radial);

    // Create smooth pulse effect
    float pulseFactor = 0.5 + 0.5 * sin(time * TWO_PI);
    pulseFactor = mix(0.8, 1.0, pulseFactor * pulse);

    // Combine flow pattern with void and pulse effects
    float pattern = flowPattern * voidFactor * pulseFactor;

    // Create a dark color palette
    vec3 darkColor = vec3(0.02, 0.02, 0.05);
    vec3 brightColor = mix(vec3(0.1, 0.1, 0.3), iColor, 0.5);

    // Generate smooth color transition
    vec3 flowColor = mix(darkColor, brightColor, pattern * 0.5);

    // Add star field
    float starBrightness = starField(uv * 10.0, 0.98) * 0.5 + starField(uv * 20.0, 0.995) * 0.5;
    flowColor += starBrightness * brightColor;

    // Apply vignette effect
    float vignette = smoothstep(0.5, 1.8, radial);
    flowColor *= 1.0 - vignette * 0.8;

    // Gentle pulse brightness
    flowColor *= 1.0 + pulse * 0.1 * sin(time * TWO_PI);

    // Add a subtle glow in the center
    float centralGlow = (1.0 - smoothstep(0.0, circleRadius, radial)) * 0.2;
    flowColor += centralGlow * brightColor;

    fragColor = vec4(flowColor, 1.0);
}