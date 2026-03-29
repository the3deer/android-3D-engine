#version 300 es

// OpenGL ES 3.x High-Performance Static Vertex Shader
// @author andresoviedo
// @author Gemini AI

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec3 a_Normal;
layout(location = 2) in vec2 a_TexCoordinate;
layout(location = 3) in vec4 a_Color;

// MVP matrices
uniform mat4 u_MMatrix;
uniform mat4 u_VMatrix;
uniform mat4 u_PMatrix;
uniform mat4 u_NormalMatrix;

uniform bool u_Coloured;
uniform bool u_Textured;
uniform bool u_Lighted;

out vec3 v_Position;
out vec3 v_Normal;
out vec2 v_TexCoordinate;
out vec4 v_Color;
out vec4 v_Tangent;

void main() {
    // 1. Initialize all varyings to avoid undefined values/NaN in fragment shader
    v_Position = vec3(0.0);
    v_Normal = vec3(0.0, 1.0, 0.0); // Default up normal
    v_TexCoordinate = vec2(0.0);
    v_Color = vec4(1.0);
    v_Tangent = vec4(0.0);

    vec4 localPos = vec4(a_Position, 1.0);
    v_Position = vec3(u_MMatrix * localPos);

    if (u_Lighted) {
        v_Normal = mat3(u_NormalMatrix) * a_Normal;
    }

    if (u_Coloured) {
        v_Color = a_Color;
    }

    if (u_Textured) {
        v_TexCoordinate = a_TexCoordinate;
    }

    gl_Position = u_PMatrix * u_VMatrix * u_MMatrix * localPos;
}
