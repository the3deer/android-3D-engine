#version 100
precision highp float;

const int MAX_JOINTS = 60;

// MVP matrices
uniform mat4 u_MMatrix;
uniform mat4 u_VMatrix;
uniform mat4 u_PMatrix;

// mesh
attribute vec3 a_Position;
varying vec3 v_Position;

// colors
uniform bool u_Coloured;
attribute vec4 a_Color;
varying vec4 v_Color;

// texture
uniform bool u_Textured;
attribute vec2 a_TexCoordinate;
varying vec2 v_TexCoordinate;

// light
uniform bool u_Lighted;
attribute vec3 a_Normal;
varying vec3 v_Normal;
uniform mat4 u_NormalMatrix;

// normalMap (GLTF Tangents are vec4)
uniform bool u_NormalTextured;
attribute vec4 a_Tangent;
varying vec4 v_Tangent;

// animation
uniform bool u_Animated;
attribute vec4 in_jointIndices;
attribute vec4 in_weights;
uniform mat4 u_BindShapeMatrix;
uniform mat4 jointTransforms[MAX_JOINTS];

void main(){
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

        // Dynamic skinning
        vec4 animatedPos = (jointTransforms[int(in_jointIndices.x)] * bindPos) * in_weights.x;
        animatedPos += (jointTransforms[int(in_jointIndices.y)] * bindPos) * in_weights.y;
        animatedPos += (jointTransforms[int(in_jointIndices.z)] * bindPos) * in_weights.z;
        animatedPos += (jointTransforms[int(in_jointIndices.w)] * bindPos) * in_weights.w;
        localPos = animatedPos;

        // Normal skinning
        mat3 skinMat0 = mat3(jointTransforms[int(in_jointIndices.x)]);
        mat3 skinMat1 = mat3(jointTransforms[int(in_jointIndices.y)]);
        mat3 skinMat2 = mat3(jointTransforms[int(in_jointIndices.z)]);
        mat3 skinMat3 = mat3(jointTransforms[int(in_jointIndices.w)]);

        localNormal = (skinMat0 * a_Normal) * in_weights.x;
        localNormal += (skinMat1 * a_Normal) * in_weights.y;
        localNormal += (skinMat2 * a_Normal) * in_weights.z;
        localNormal += (skinMat3 * a_Normal) * in_weights.w;

        // Tangent skinning - Only if normal mapping is enabled
        if (u_NormalTextured) {
            localTangent = (skinMat0 * a_Tangent.xyz) * in_weights.x;
            localTangent += (skinMat1 * a_Tangent.xyz) * in_weights.y;
            localTangent += (skinMat2 * a_Tangent.xyz) * in_weights.z;
            localTangent += (skinMat3 * a_Tangent.xyz) * in_weights.w;
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
