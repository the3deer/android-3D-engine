#version 300 es

// OpenGL ES 3.x Depth Map Vertex Shader
// @author Gemini AI

layout(location = 0) in vec3 a_Position;
layout(location = 4) in vec4 in_jointIndices;
layout(location = 5) in vec4 in_weights;

uniform mat4 u_MMatrix;
uniform mat4 u_VMatrix;
uniform mat4 u_PMatrix;
uniform mat4 u_BindShapeMatrix;

uniform sampler2D u_JointTexture;

uniform bool u_Animated;

out vec4 v_Position;

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

    if (u_Animated) {
        vec4 bindPos = u_BindShapeMatrix * localPos;

        mat4 skinMat = getJointMatrix(int(in_jointIndices.x)) * in_weights.x;
        skinMat += getJointMatrix(int(in_jointIndices.y)) * in_weights.y;
        skinMat += getJointMatrix(int(in_jointIndices.z)) * in_weights.z;
        skinMat += getJointMatrix(int(in_jointIndices.w)) * in_weights.w;

        localPos = skinMat * bindPos;
    }

    gl_Position = u_PMatrix * u_VMatrix * u_MMatrix * localPos;
    v_Position = gl_Position;
}
