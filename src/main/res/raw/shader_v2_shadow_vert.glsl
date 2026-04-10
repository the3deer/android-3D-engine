#version 100
precision highp float;

const int MAX_JOINTS = 60;
//const int MAX_WEIGHTS = 3;

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

// shadow
uniform mat4 u_LVMatrix;
varying vec4 vShadowCoord;

void main(){

    vec4 animatedPos = vec4(a_Position, 1.0);
    vec3 animatedNormal = a_Normal;
    vec3 animatedTangent = a_Tangent.xyz;

    if (u_Animated) {
        // Calculate animated position
        vec4 bindPos = u_BindShapeMatrix * vec4(a_Position, 1.0);

        animatedPos = (jointTransforms[int(in_jointIndices[0])] * bindPos) * in_weights[0];
        animatedPos += (jointTransforms[int(in_jointIndices[1])] * bindPos) * in_weights[1];
        animatedPos += (jointTransforms[int(in_jointIndices[2])] * bindPos) * in_weights[2];
        animatedPos += (jointTransforms[int(in_jointIndices[3])] * bindPos) * in_weights[3];

        // Calculate animated normal and tangent (rotation only)
        mat3 skinMat0 = mat3(jointTransforms[int(in_jointIndices[0])]);
        mat3 skinMat1 = mat3(jointTransforms[int(in_jointIndices[1])]);
        mat3 skinMat2 = mat3(jointTransforms[int(in_jointIndices[2])]);
        mat3 skinMat3 = mat3(jointTransforms[int(in_jointIndices[3])]);

        animatedNormal = (skinMat0 * a_Normal) * in_weights[0];
        animatedNormal += (skinMat1 * a_Normal) * in_weights[1];
        animatedNormal += (skinMat2 * a_Normal) * in_weights[2];
        animatedNormal += (skinMat3 * a_Normal) * in_weights[3];

        animatedTangent = (skinMat0 * a_Tangent.xyz) * in_weights[0];
        animatedTangent += (skinMat1 * a_Tangent.xyz) * in_weights[1];
        animatedTangent += (skinMat2 * a_Tangent.xyz) * in_weights[2];
        animatedTangent += (skinMat3 * a_Tangent.xyz) * in_weights[3];
    }

    // calculate MVP matrix
    mat4 u_MVMatrix = u_VMatrix * u_MMatrix;
    mat4 u_MVPMatrix = u_PMatrix * u_MVMatrix;

    // calculate rendered position
    gl_Position = u_MVPMatrix * animatedPos;
    v_Position = vec3(u_MMatrix * animatedPos);

    // colour
    if (u_Coloured){
        v_Color = a_Color;
    }

    // texture
    if (u_Textured) {
        v_TexCoordinate = a_TexCoordinate;
    }

    // normal
    if (u_Lighted){
        // Use the transformed normal. It also needs to be transformed by the model matrix
        // to get it into world space for lighting calculations.
        //mat3 u_NormalMatrix = mat3(transpose(inverse(u_MMatrix)));
        v_Normal = mat3(u_NormalMatrix) * animatedNormal;
        vShadowCoord = u_PMatrix * u_LVMatrix * u_MMatrix * animatedPos;
    }

    if (u_NormalTextured) {
        v_Tangent = vec4(mat3(u_NormalMatrix) * animatedTangent, a_Tangent.w);
    }
}
