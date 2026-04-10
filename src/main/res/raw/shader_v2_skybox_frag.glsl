#version 100
precision highp float;

// colors
uniform vec4 u_Color;  // not used
uniform vec4 u_ColorMask;

// skybox
uniform samplerCube u_TextureCube;
varying vec4 v_TexCoordinate;

void main(){
  gl_FragColor = textureCube(u_TextureCube,v_TexCoordinate.xyz);
  gl_FragColor = gl_FragColor * u_Color * u_ColorMask;
}