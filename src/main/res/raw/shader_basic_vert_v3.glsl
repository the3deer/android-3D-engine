#version 300 es

// OpenGL ES 3.x High-Performance Static Vertex Shader
// @author Gemini AI

layout(location = 0) in vec3 a_Position;
layout(location = 3) in vec4 a_Color;

// MVP matrices
uniform mat4 u_MMatrix;
uniform mat4 u_VMatrix;
uniform mat4 u_PMatrix;

// colors
uniform bool u_Coloured;

out vec4 v_Color;

void main(){

    // calculate rendered position
    gl_Position = u_PMatrix * u_VMatrix * u_MMatrix * vec4(a_Position, 1.0);
    gl_PointSize = 15.0;

    // pass color to fragment shader
    if (u_Coloured){
        v_Color = a_Color;
    }

}
