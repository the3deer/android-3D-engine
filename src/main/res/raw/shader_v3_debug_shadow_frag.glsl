#version 300 es
precision highp float;

uniform sampler2D u_Texture;
in vec2 v_TexCoordinate;
out vec4 fragColor;

/**
 * Unpack a 32-bit RGBA color into a float depth value.
 * This must match the packing logic used in the depth map shader.
 */
float unpack(vec4 colour)
{
    const vec4 bitShifts = vec4(1.0 / (256.0 * 256.0 * 256.0),
                                1.0 / (256.0 * 256.0),
                                1.0 / 256.0,
                                1.0);
    return dot(colour, bitShifts);
}

void main() {
    vec4 packedColor = texture(u_Texture, v_TexCoordinate);

    // Unpack the depth value
    float depth = unpack(packedColor);

    // Display as grayscale
    fragColor = vec4(vec3(depth), 1.0);
}
