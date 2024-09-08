#version 150

uniform float iTime;
uniform float circleRadius;
uniform vec2 iResolution;
uniform vec2 iMouse;
uniform vec3 iColor;
out vec4 fragColor;

#define LAYERS 6
#define SPEED 0.025
#define DISTORTION 0.7
#define VOID_PURPLE vec3(0.1123, 0.0, 0.2)
#define EDGE_SOFTNESS 0.123

#define VOID_COLOR1 vec3(0.1, 0.1, 0.1)
#define VOID_COLOR2 vec3(0.3, 0.3, 0.3)
// Optimized hash function
float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

// Optimized noise function
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

// Optimized fbm function
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

// Optimized starfield function
float starField(vec2 uv) {
    float stars = 0.0;
    float size = 400.0;
    vec2 gv = fract(uv * size) - 0.5;
    vec2 id = floor(uv * size);

    float star = step(0.98, hash(id));
    float glow = smoothstep(0.3, 0.0, length(gv));
    stars += star * glow;

    return stars * 0.5;
}


// New function for the void background effect
vec3 voidEffect(vec2 uv, float time) {
    vec3 color = VOID_COLOR1;

    // Create a warping effect
    vec2 warpUV = uv;
    warpUV += vec2(
        sin(uv.y * 4.0 + time * 0.5) * 0.1,
        cos(uv.x * 4.0 + time * 0.5) * 0.1
    );

    // Add some noise to the void
    float voidNoise = fbm(warpUV * 3.0 + time * 0.1);
    color = mix(VOID_COLOR1, VOID_COLOR2, voidNoise * 0.5);

    // Add subtle movement
    float movement = fbm(uv * 2.0 - time * 0.05);
    color += vec3(movement * 0.05);

    return color;
}

void main() {
    vec2 uv = (gl_FragCoord.xy / iResolution.xy) * 2.0 - 1.0;
    uv.x *= iResolution.x / iResolution.y;
    float time = iTime * SPEED;

    vec2 mouseUV = (iMouse.xy / iResolution.xy) * 2.0 - 1.0;
    mouseUV.x *= iResolution.x / iResolution.y;

    float dist = length(uv - mouseUV);
    float circleMask = smoothstep(circleRadius + EDGE_SOFTNESS, circleRadius - EDGE_SOFTNESS, dist);

    vec2 circleUV = (uv - mouseUV) / circleRadius * 0.5 + 0.5;

    // Vortex effect
    float angle = atan(circleUV.y - 0.5, circleUV.x - 0.5);
    float vortexDist = length(circleUV - 0.5);
    float vortex = sin(vortexDist * 10.0 - time * 2.0) * 0.1;
    circleUV = vec2(cos(angle + vortex), sin(angle + vortex)) * vortexDist + 0.5;

    // Inner portal effect
    vec3 innerColor = vec3(0.0);
    for (int i = 0; i < LAYERS; i++) {
        float depth = float(i) / float(LAYERS);
        float scale = mix(5.0, 0.5, depth);
        vec2 uvOffset = circleUV * scale + vec2(time, -time) * (1.0 - depth);

        float pattern = fbm(uvOffset);
        pattern += fbm(uvOffset * 2.0 + 1000.0) * 0.5;

        vec3 layerColor = mix(iColor, VOID_PURPLE, depth);
        innerColor += layerColor * pattern * (1.0 - depth) * 0.3;
    }

    innerColor += vec3(1.0, 0.8, 0.6) * starField(circleUV + vec2(time * 0.1, 0.0));
    float vignette = smoothstep(1.0, 0.5, vortexDist);
    innerColor *= vignette;
    innerColor = pow(innerColor, vec3(0.8)) * 1.2;

    // Void background effect
    vec3 voidColor = voidEffect(uv, time);

    // Combine inner portal and void background
    vec3 finalColor = mix(voidColor, innerColor, circleMask);

    // Add glow around the circle
    float glow = smoothstep(circleRadius + EDGE_SOFTNESS * 2.0, circleRadius, dist);
    finalColor += mix(VOID_COLOR2, iColor, glow) * glow * 0.3;

    fragColor = vec4(finalColor, 1.0);
}