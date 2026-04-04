#version 300 es
precision highp float;

uniform mat4 u_MMatrix;
uniform mat4 u_VMatrix;
uniform mat4 u_PMatrix;

layout(location = 0) in vec3 a_Position;

void main() {
    mat4 u_MVMatrix = u_VMatrix * u_MMatrix;
    mat4 u_MVPMatrix = u_PMatrix * u_MVMatrix;

    gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);

    // Make the sun point large enough to see the glow
    gl_PointSize = 100.0;
}