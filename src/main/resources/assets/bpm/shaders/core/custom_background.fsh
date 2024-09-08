#version 150

uniform float iTime;
uniform float circleRadius;
uniform vec2 iResolution;
uniform vec3 iColor;
uniform float pulse;  // New uniform for the pulse effect
out vec4 fragColor;

#define SPEED 0.025
#define DISTORTION_STRENGTH 0.1
#define BLACK_HOLE_COLOR vec3(0.0, 0.0, 0.0)
#define ACCRETION_DISK_COLOR vec3(0.8, 0.4, 0.1)
#define SPACE_COLOR vec3(0.05, 0.05, 0.15)
#define BACKGROUND_COLOR vec3(0.2, 0.2, 0.2)

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
    for (int i = 0; i < 4; ++i) {
        v += a * noise(p);
        p = rot * p * 2.0;
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 uv = (gl_FragCoord.xy / iResolution.xy) * 2.0 - 1.0;
    uv.x *= iResolution.x / iResolution.y;
    float time = iTime * SPEED;

    // Black hole position
    float angle = time * 0.5;
    vec2 blackHolePos = vec2(
        cos(angle) * 0.5,
        sin(angle) * 0.5
    );

    // Distance from current pixel to black hole center
    float dist = length(uv - blackHolePos);

    // Create distortion effect (affected by pulse)
    vec2 distortedUV = uv - blackHolePos;
    float distortionFactor = 1.0 / (1.0 + exp((dist - circleRadius) * 10.0));
    distortedUV *= mix(1.0, 1.0 - DISTORTION_STRENGTH * (1.0 + pulse), distortionFactor);
    distortedUV += blackHolePos;

    // Create accretion disk (affected by pulse)
    float diskWidth = 0.1 * (1.0 + pulse * 0.5);
    float diskMask = smoothstep(circleRadius, circleRadius + diskWidth, dist) *
                     (1.0 - smoothstep(circleRadius + diskWidth, circleRadius + diskWidth * 2.0, dist));

    // Create black hole mask (affected by pulse)
    float blackHoleMask = 1.0 - smoothstep(circleRadius * (0.8 - pulse * 0.2), circleRadius, dist);

    // Create background effect
    float bgNoise = fbm(distortedUV * 3.0 + time * 0.1);
    vec3 backgroundColor = mix(BACKGROUND_COLOR, BACKGROUND_COLOR * 1.2, bgNoise);

    // Combine colors
    vec3 diskColor = ACCRETION_DISK_COLOR * (1.0 + 0.5 * sin(dist * 50.0 - time * 5.0));
    diskColor = mix(diskColor, vec3(1.0), pulse * 0.5);  // Make disk brighter with pulse
    vec3 finalColor = mix(backgroundColor, diskColor, diskMask);
    finalColor = mix(finalColor, BLACK_HOLE_COLOR, blackHoleMask);

    // Add some glow to the accretion disk (affected by pulse)
    finalColor += diskColor * diskMask * (0.5 + pulse);

    // Add pulse-based color effect to the entire scene
    finalColor = mix(finalColor, iColor, pulse * 0.3);

    fragColor = vec4(finalColor, 1.0);
}