#version 300 es

// OpenGL ES 3.x High-Performance Static Fragment Shader
// @author andresoviedo
// @author Gemini AI

precision highp float;

uniform vec4 u_Color;
uniform vec4 u_ColorMask;
uniform bool u_Coloured;

in vec4 v_Color;
out vec4 fragColor;

void main(){

  // Combine base color (material or vertex) with the global color mask
  vec4 color = u_Coloured ? v_Color : u_Color;
  fragColor = color * u_ColorMask;

}
