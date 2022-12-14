precision highp float;

// colors
uniform vec4 vColor;  // not used
uniform vec4 vColorMask;

// skybox
uniform samplerCube u_TextureCube;
varying vec4 v_TexCoordinate;

void main(){
  gl_FragColor = textureCube(u_TextureCube,v_TexCoordinate.xyz);
  gl_FragColor = gl_FragColor * vColor * vColorMask;
}