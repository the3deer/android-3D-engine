#version 300 es
precision highp float;

// MVP matrices
uniform mat4 u_MMatrix;
uniform mat4 u_VMatrix;
uniform mat4 u_PMatrix;

// mesh
layout(location = 0) in vec3 a_Position;

// skybox
out vec4 v_TexCoordinate;

void main(){

    // calculate MVP matrix
    mat4 u_MVMatrix = u_VMatrix * u_MMatrix;
    mat4 u_MVPMatrix = u_PMatrix * u_MVMatrix;

    // calculate rendered position
    gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);

    // colors
    v_TexCoordinate = vec4(a_Position, 1.0);
}
