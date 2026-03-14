#version 300 es

// OpenGL ES 3.x High-Performance Fragment Shader
// @author Gemini AI

precision highp float;

uniform mat4 u_MMatrix;
uniform vec3 u_cameraPos;
in vec3 v_Position;

// color
uniform vec4 u_Color;
uniform vec4 u_ColorMask;

// colors
uniform bool u_Coloured;
in vec4 v_Color;

// texture
uniform bool u_Textured;
uniform sampler2D u_Texture;
in vec2 v_TexCoordinate;

// texture transform
uniform bool u_TextureTransformed;
uniform vec2 u_TextureOffset;
uniform vec2 u_TextureScale;
uniform float u_TextureRotation;

// blending
uniform float u_AlphaCutoff;
uniform int u_AlphaMode;

// light
uniform bool u_Lighted;
uniform vec3 u_LightPos;
in vec3 v_Normal;

// normalMap
uniform bool u_NormalTextured;
uniform sampler2D u_NormalTexture;

// emissiveMap
uniform bool u_EmissiveTextured;
uniform sampler2D u_EmissiveTexture;
uniform vec3 u_EmissiveFactor;

// volume
uniform bool u_TransmissionTextured;
uniform sampler2D u_TransmissionTexture;
uniform float u_TransmissionFactor;

out vec4 fragColor;

void main() {
    // colors initialization
    vec4 baseColor = u_Coloured ? v_Color : u_Color;
    vec4 texColor = vec4(1.0);

    if (u_Textured) {
        vec2 uv = v_TexCoordinate;
        if (u_TextureTransformed) {
            mat3 translation = mat3(1, 0, 0, 0, 1, 0, u_TextureOffset.x, u_TextureOffset.y, 1);
            mat3 rotation = mat3(
            cos(u_TextureRotation), -sin(u_TextureRotation), 0,
            sin(u_TextureRotation), cos(u_TextureRotation), 0,
            0, 0, 1
            );
            mat3 scale = mat3(u_TextureScale.x, 0, 0, 0, u_TextureScale.y, 0, 0, 0, 1);
            mat3 matrix = translation * rotation * scale;
            uv = (matrix * vec3(v_TexCoordinate.xy, 1)).xy;
        }
        texColor = texture(u_Texture, uv);
    }

    // Combine base, texture, and mask
    vec4 finalColor = baseColor * texColor * u_ColorMask;

    // Alpha mode handling (Early discard for Mask mode)
    // 1 == MASK
    if (u_AlphaMode == 1 && finalColor.a < u_AlphaCutoff) {
        discard;
    }

    // Light initialization
    float diffuse = 0.25;
    if (u_Lighted) {
        vec3 N = normalize(v_Normal);
        vec3 L = normalize(u_LightPos - v_Position);
        diffuse = max(dot(N, L), 0.0);
    }

    // Ambient light
    float ambient = 0.40;
    float totalLight = min((diffuse + ambient), 1.0);

    // Combine lighting with color
    finalColor.rgb = finalColor.rgb * totalLight;

    // Apply Emissive texture (if enabled)
    if (u_EmissiveTextured){
        vec4 emissiveTex = texture(u_EmissiveTexture, v_TexCoordinate);
        finalColor.rgb += emissiveTex.rgb * u_EmissiveFactor;
    }

    // Set output color
    fragColor = finalColor;
    // fragColor = vec4(1.0,1.0,1.0,1.0);  <-- debug only

    // Force opaque if mode is 0 (OPAQUE)
    if (u_AlphaMode == 0) {
        fragColor.a = 1.0;
    }
}
