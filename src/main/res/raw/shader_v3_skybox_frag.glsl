#version 300 es
precision highp float;

// colors
uniform vec4 u_Color;
uniform vec4 u_ColorMask;

// skybox
uniform samplerCube u_TextureCube;
in vec4 v_TexCoordinate;

out vec4 fragColor;

void main(){
  fragColor = texture(u_TextureCube, v_TexCoordinate.xyz);
  fragColor = fragColor * u_Color * u_ColorMask;
}
