#version 100
precision highp float;

// color
uniform vec4 u_Color;
uniform vec4 u_ColorMask;

// colors
uniform bool u_Coloured;
varying vec4 v_Color;

void main(){

  // color
  vec4 color;
  if (u_Coloured){
    color = v_Color;
  } else {
    color = u_Color;
  }

  gl_FragColor = color * u_ColorMask;
  gl_FragColor[3] = color[3] * u_ColorMask[3];
}