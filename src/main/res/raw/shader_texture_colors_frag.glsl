precision highp float;

// colors
varying vec4 v_Color;
uniform vec4 vColorMask;

// texture
uniform bool u_Textured;
uniform sampler2D u_Texture;
varying vec2 v_TexCoordinate;

void main(){
    vec4 color = v_Color;
    if (u_Textured && length(vec3(color)) > 0.0){
        gl_FragColor = color * texture2D(u_Texture, v_TexCoordinate) * vColorMask;
    } else if (u_Textured){
        gl_FragColor = texture2D(u_Texture, v_TexCoordinate) * vColorMask;
    } else {
        gl_FragColor = color * vColorMask;
    }
    gl_FragColor[3] = color[3] * vColorMask[3];
}