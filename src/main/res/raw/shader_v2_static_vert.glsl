precision highp float;

// OpenGL ES 2.x Animated Shader
// @author andresoviedo

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
void main(){

    // calculate MVP matrix
    mat4 u_MVMatrix = u_VMatrix * u_MMatrix;
    mat4 u_MVPMatrix = u_PMatrix * u_MVMatrix;

    // calculate rendered position
    gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);;

    // pass color to fragment shader
    if (u_Coloured){
        v_Color = a_Color;
    }

    // texture
    if (u_Textured) {
        v_TexCoordinate = a_TexCoordinate;
    }

    // normal and tangent to world space
    if (u_Lighted){
        v_Normal = mat3(u_NormalMatrix) * a_Normal;
    }
}