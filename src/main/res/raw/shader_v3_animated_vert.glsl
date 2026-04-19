#version 300 es

// OpenGL ES 3.x High-Performance Animated Shader
// @author Gemini AI

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec3 a_Normal;
layout(location = 2) in vec2 a_TexCoordinate;
layout(location = 3) in vec4 a_Color;
layout(location = 4) in vec4 in_jointIndices;
layout(location = 5) in vec4 in_weights;
layout(location = 6) in vec4 a_Tangent;

uniform mat4 u_MMatrix;
uniform mat4 u_VMatrix;
uniform mat4 u_PMatrix;
uniform mat4 u_NormalMatrix;
uniform mat4 u_BindShapeMatrix;

uniform sampler2D u_JointTexture;

uniform bool u_Animated;
uniform bool u_Coloured;
uniform bool u_Textured;
uniform bool u_Lighted;
uniform bool u_NormalTextured;

out vec3 v_Position;
out vec3 v_Normal;
out vec2 v_TexCoordinate;
out vec4 v_Color;
out vec4 v_Tangent;

mat4 getJointMatrix(int jointIndex) {
    return mat4(
        texelFetch(u_JointTexture, ivec2(0, jointIndex), 0),
        texelFetch(u_JointTexture, ivec2(1, jointIndex), 0),
        texelFetch(u_JointTexture, ivec2(2, jointIndex), 0),
        texelFetch(u_JointTexture, ivec2(3, jointIndex), 0)
    );
}

void main() {
    vec4 localPos = vec4(a_Position, 1.0);
    vec3 localNormal = a_Normal;
    vec3 localTangent = vec3(0.0);
    float bitangentSign = 1.0;

    if (u_NormalTextured) {
        localTangent = a_Tangent.xyz;
        bitangentSign = a_Tangent.w;
    }

    if (u_Animated) {
        vec4 bindPos = u_BindShapeMatrix * localPos;

        mat4 skinMat = getJointMatrix(int(in_jointIndices.x)) * in_weights.x;
        skinMat += getJointMatrix(int(in_jointIndices.y)) * in_weights.y;
        skinMat += getJointMatrix(int(in_jointIndices.z)) * in_weights.z;
        skinMat += getJointMatrix(int(in_jointIndices.w)) * in_weights.w;

        localPos = skinMat * bindPos;
        localNormal = mat3(skinMat) * a_Normal;

        if (u_NormalTextured) {
            localTangent = mat3(skinMat) * a_Tangent.xyz;
        }
    }

    // Standard transformations
    v_Position = vec3(u_MMatrix * localPos);
    v_Normal = mat3(u_NormalMatrix) * localNormal;

    if (u_NormalTextured) {
        v_Tangent = vec4(mat3(u_NormalMatrix) * localTangent, bitangentSign);
    } else {
        v_Tangent = vec4(0.0, 0.0, 0.0, 1.0);
    }

    v_TexCoordinate = a_TexCoordinate;
    v_Color = a_Color;

    gl_Position = u_PMatrix * u_VMatrix * u_MMatrix * localPos;
}
