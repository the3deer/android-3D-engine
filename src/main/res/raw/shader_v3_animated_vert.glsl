#version 300 es

// OpenGL ES 3.x High-Performance Animated Shader
// @author Gemini AI

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec3 a_Normal;
layout(location = 2) in vec2 a_TexCoordinate;
layout(location = 3) in vec4 a_Color;
layout(location = 4) in vec4 in_jointIndices;
layout(location = 5) in vec4 in_weights;

const int MAX_JOINTS = 60;

uniform mat4 u_MMatrix;
uniform mat4 u_VMatrix;
uniform mat4 u_PMatrix;
uniform mat4 u_NormalMatrix;
uniform mat4 u_BindShapeMatrix;
uniform mat4 jointTransforms[MAX_JOINTS];

uniform bool u_Animated;
uniform bool u_Coloured;
uniform bool u_Textured;
uniform bool u_Lighted;

out vec3 v_Position;
out vec3 v_Normal;
out vec2 v_TexCoordinate;
out vec4 v_Color;
out vec4 v_Tangent;

void main() {
    vec4 localPos = vec4(a_Position, 1.0);
    vec3 localNormal = a_Normal;

    if (u_Animated) {
        vec4 bindPos = u_BindShapeMatrix * localPos;

        // Dynamic skinning
        vec4 animatedPos = (jointTransforms[int(in_jointIndices.x)] * bindPos) * in_weights.x;
        animatedPos += (jointTransforms[int(in_jointIndices.y)] * bindPos) * in_weights.y;
        animatedPos += (jointTransforms[int(in_jointIndices.z)] * bindPos) * in_weights.z;
        animatedPos += (jointTransforms[int(in_jointIndices.w)] * bindPos) * in_weights.w;
        localPos = animatedPos;

        // Normal skinning (rotation/scale only)
        mat3 skinMat0 = mat3(jointTransforms[int(in_jointIndices.x)]);
        mat3 skinMat1 = mat3(jointTransforms[int(in_jointIndices.y)]);
        mat3 skinMat2 = mat3(jointTransforms[int(in_jointIndices.z)]);
        mat3 skinMat3 = mat3(jointTransforms[int(in_jointIndices.w)]);

        localNormal = (skinMat0 * a_Normal) * in_weights.x;
        localNormal += (skinMat1 * a_Normal) * in_weights.y;
        localNormal += (skinMat2 * a_Normal) * in_weights.z;
        localNormal += (skinMat3 * a_Normal) * in_weights.w;
    }

    // Standard transformations
    v_Position = vec3(u_MMatrix * localPos);
    v_Normal = mat3(u_NormalMatrix) * localNormal;
    v_TexCoordinate = a_TexCoordinate;
    v_Color = a_Color;

    gl_Position = u_PMatrix * u_VMatrix * u_MMatrix * localPos;
}
