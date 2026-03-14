#version 300 es

// OpenGL ES 3.x Depth Map Fragment Shader
// @author Gemini AI

precision highp float;

in vec4 v_Position;
out vec4 fragColor;

// Helper to pack a float depth value into a 32-bit RGBA color
// This is used for compatibility with older drivers that might not handle
// pure depth textures well, though in GLES 3.x we could use floating point textures.
vec4 pack (float depth)
{
    const vec4 bitSh = vec4(256.0 * 256.0 * 256.0,
    256.0 * 256.0,
    256.0,
    1.0);
    const vec4 bitMsk = vec4(0,
    1.0 / 256.0,
    1.0 / 256.0,
    1.0 / 256.0);
    vec4 comp = fract(depth * bitSh);
    comp -= comp.xxyz * bitMsk;
    return comp;
}

void main() {
    // Standard depth calculation
    float normalizedDistance = v_Position.z / v_Position.w;

    // Scale from [-1, 1] range to [0, 1]
    normalizedDistance = (normalizedDistance + 1.0) / 2.0;

    // Pack value into 32-bit RGBA texture
    fragColor = pack(normalizedDistance);
}
