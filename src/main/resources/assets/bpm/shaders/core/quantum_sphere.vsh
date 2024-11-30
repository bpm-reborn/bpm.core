#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float GameTime;

out vec2 texCoord;
out vec4 vertexColor;
out vec3 worldPos;
out vec4 quantumState;
out vec3 vertexPosition;
out vec3 normal;

vec4 calculateQuantumState(vec3 position, float time) {
    vec4 state;
    float distFromCenter = length(position);

    // Enhanced quantum state calculations with position influence
    state.x = sin(time * 0.5 + distFromCenter) * 0.3 + 0.7;        // Amplitude
    state.y = cos(time * 2.0 + position.x) * 0.15 + 0.5;          // Phase
    state.z = abs(sin(time * 1.5 + position.y));                  // Superposition

    // Entanglement effect influenced by vertex color
    float entanglement = sin(time * 0.7) * sin(time * 1.3 + position.z * 2.0);
    state.w = entanglement * 0.5 + 0.5;

    return state;
}

void main() {
    vec3 modifiedPosition = Position;

    // Quantum displacement influenced by vertex color
    float displacement = sin(GameTime * 2.0 + length(Position) * 3.0) * 0.02;
    displacement *= Color.a; // Scale displacement by vertex alpha
    modifiedPosition += normalize(Position) * displacement;

    // Enhanced quantum state calculation with color influence
    quantumState = calculateQuantumState(Position, GameTime);
    quantumState *= Color.a; // Scale quantum effects by vertex alpha

    vertexPosition = Position;
    worldPos = modifiedPosition;

    gl_Position = ProjMat * ModelViewMat * vec4(modifiedPosition, 1.0);
    texCoord = UV0;
    vertexColor = Color;
    normal = Normal;
}