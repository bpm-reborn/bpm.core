#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float GameTime;
uniform vec4 QuantumState;

out vec2 texCoord;
out vec4 vertexColor;
out vec3 worldPos;
out float quantumEffect;
out vec4 quantumState;

// Quantum wave packet function that creates localized particle probability distribution
float wavePacket(vec3 position, float width, float phase) {
    float distSq = dot(position, position);
    // Gaussian envelope
    float envelope = exp(-distSq / (2.0 * width * width));
    // Wave oscillation
    float oscillation = cos(sqrt(distSq) * 5.0 + phase);
    return envelope * oscillation;
}

// Quantum tunneling effect through potential barriers
float tunnelProbability(vec3 position, float barrier) {
    float energy = QuantumState.x;  // Using amplitude as energy
    if (energy < barrier) {
        float width = 1.0;  // Barrier width
        float kappa = sqrt(2.0 * (barrier - energy));
        return exp(-2.0 * kappa * width);
    }
    return 1.0;
}

// Quantum superposition creates multiple position states
vec3 superpositionStates(vec3 position, float strength) {
    vec3 state1 = position;
    vec3 state2 = position * (1.0 + 0.1 * sin(GameTime * 2.0));
    return mix(state1, state2, strength * QuantumState.z);
}

// Uncertainty principle affects position and momentum
vec3 uncertaintyDistortion(vec3 position) {
    float uncertainty = QuantumState.y;
    vec3 momentum = vec3(
        sin(GameTime * 1.3 + position.x),
        cos(GameTime * 1.7 + position.y),
        sin(GameTime * 2.1 + position.z)
    );
    return position + momentum * uncertainty * 0.1;
}

void main() {
    // Apply quantum mechanical effects to position
    vec3 pos = Position;

    // Apply uncertainty principle
    pos = uncertaintyDistortion(pos);

    // Create superposition states
    pos = superpositionStates(pos, QuantumState.z);

    // Calculate wave packet probability
    float psi = wavePacket(pos, 1.0, GameTime * 2.0);

    // Apply tunneling effects
    float tunnel = tunnelProbability(pos, 0.5);
    pos *= (1.0 + tunnel * 0.1);

    // Calculate quantum effect intensity for fragment shader
    quantumEffect = psi * (1.0 + QuantumState.z * 0.5);

    // Pass quantum state to fragment shader
    quantumState = QuantumState;

    worldPos = pos;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
    texCoord = UV0;
    vertexColor = Color;
}
