#version 300 es

// OpenGL ES 3.x High-Performance Basic Fragment Shader
// @author andresoviedo
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
in vec4 v_Tangent;

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
    // 1. Initial Color Setup
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

    // 2. Alpha handling
    if (u_AlphaMode == 1 && finalColor.a < u_AlphaCutoff) {
        discard;
    }

    // 3. Lighting Calculation
    float diffuse = 0.25;
    float specular = 0.0;
    vec3 N = normalize(v_Normal);

    if (u_Lighted) {
        // Normal mapping logic
        if (u_NormalTextured){
            // Sample normal map [0, 1] and convert to [-1, 1]
            vec3 normalSample = texture(u_NormalTexture, v_TexCoordinate).rgb * 2.0 - 1.0;

            // Re-orthogonalize tangent (Gram-Schmidt process)
            vec3 T = normalize(v_Tangent.xyz - dot(v_Tangent.xyz, N) * N);
            // Construct bitangent respecting handedness (w)
            vec3 B = cross(N, T) * v_Tangent.w;

            // Construct TBN matrix and transform sample to world space
            mat3 TBN = mat3(T, B, N);
            N = normalize(TBN * normalSample);
        }

        // Blinn-Phong lighting
        vec3 lightVec = u_LightPos - v_Position;
        float dist = length(lightVec);
        lightVec = normalize(lightVec);

        // Diffuse (Lambert)
        float diff = max(dot(lightVec, N), 0.0);

        // Attenuation
        float attenuation = 1.0 / (1.0 + 0.025 * dist);
        diffuse = diff * attenuation;

        // Specular (Blinn-Phong)
        vec3 viewDir = normalize(u_cameraPos - v_Position);
        vec3 halfDir = normalize(lightVec + viewDir);
        specular = pow(max(dot(N, halfDir), 0.0), 32.0) * attenuation;
    }

    // Ambient light (Hemisphere Lighting)
    float skyIntensity = 0.50;   // Light from above
    float groundIntensity = 0.25; // Light from below
    float ambient = mix(groundIntensity, skyIntensity, N.y * 0.5 + 0.5);
    float totalLight = min((diffuse + specular + ambient), 1.0);

    // Combine lighting with color
    finalColor.rgb = finalColor.rgb * totalLight;

    // 4. Emissive Handling
    if (u_EmissiveTextured){
        vec4 emissiveTex = texture(u_EmissiveTexture, v_TexCoordinate);
        finalColor.rgb += emissiveTex.rgb * u_EmissiveFactor;
    }

    // Set output color
    fragColor = finalColor;

    // Force opaque if mode is 0 (OPAQUE)
    if (u_AlphaMode == 0) {
        fragColor.a = 1.0;
    }
}