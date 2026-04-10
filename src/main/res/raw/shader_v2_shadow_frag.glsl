#version 100
precision highp float;

// data
uniform mat4 u_MMatrix;
uniform vec3 u_cameraPos;
varying vec3 v_Position;

// color
uniform vec4 u_Color;
uniform vec4 u_ColorMask;

// colors
uniform bool u_Coloured;
varying vec4 v_Color;

// texture
uniform bool u_Textured;
uniform sampler2D u_Texture;
varying vec2 v_TexCoordinate;

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
varying vec3 v_Normal;

// normalMap
uniform bool u_NormalTextured;
uniform sampler2D u_NormalTexture;
varying vec4 v_Tangent;

// emissiveMap
uniform bool u_EmissiveTextured;
uniform sampler2D u_EmissiveTexture;
uniform vec3 u_EmissiveFactor;

// volume
uniform bool u_TransmissionTextured;
uniform sampler2D u_TransmissionTexture;
uniform float u_TransmissionFactor;

// shadow
uniform sampler2D uShadowTexture;
uniform float u_ShadowTexelSizeX; // Dynamic texel width
uniform float u_ShadowTexelSizeY; // Dynamic texel height
varying vec4 vShadowCoord;

// unpack colour to depth value
float unpack (vec4 colour)
{
    const vec4 bitShifts = vec4(1.0 / (256.0 * 256.0 * 256.0),
    1.0 / (256.0 * 256.0),
    1.0 / 256.0,
    1);
    return dot(colour, bitShifts);
}

// Simple shadow mapping with PCF (Percentage Closer Filtering)
float shadowPCF()
{
    vec4 shadowMapPosition = vShadowCoord / vShadowCoord.w;
    shadowMapPosition = (shadowMapPosition + 1.0) / 2.0;

    float bias = 0.0005;
    float shadow = 0.0;

    // 3x3 PCF Kernel using dynamic texel size
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            vec2 offset = vec2(float(x) * u_ShadowTexelSizeX, float(y) * u_ShadowTexelSizeY);
            vec4 packedZValue = texture2D(uShadowTexture, shadowMapPosition.st + offset);
            float distanceFromLight = unpack(packedZValue);
            if (distanceFromLight > (shadowMapPosition.z - bias)) {
                shadow += 1.0;
            }
        }
    }

    return shadow / 9.0;
}

void main(){

    // colors initialization
    vec4 baseColor = u_Coloured ? v_Color : u_Color;
    vec4 texColor = u_Textured ? texture2D(u_Texture, v_TexCoordinate) : vec4(1.0);

    // Texture transformation (if enabled)
    if (u_Textured && u_TextureTransformed){
        mat3 translation = mat3(1, 0, 0, 0, 1, 0, u_TextureOffset.x, u_TextureOffset.y, 1);
        mat3 rotation = mat3(
            cos(u_TextureRotation), -sin(u_TextureRotation), 0,
            sin(u_TextureRotation), cos(u_TextureRotation), 0,
            0, 0, 1
        );
        mat3 scale = mat3(u_TextureScale.x, 0, 0, 0, u_TextureScale.y, 0, 0, 0, 1);
        mat3 matrix = translation * rotation * scale;
        vec2 uvTransformed = (matrix * vec3(v_TexCoordinate.xy, 1)).xy;
        texColor = texture2D(u_Texture, uvTransformed);
    }

    // Combine base, texture, and mask
    vec4 finalColor = baseColor * texColor * u_ColorMask;

    // Alpha mode handling
    if (u_AlphaMode == 1 && finalColor.a < u_AlphaCutoff) {
        discard;
    }

    // Light initialization
    float diffuse = 0.25;
    float specular = 0.0;
    vec3 N = normalize(v_Normal);
    float shadowFactor = 1.0;

    if (u_Lighted) {
        // Normal mapping logic
        if (u_NormalTextured){
            // Sample normal map [0, 1] and convert to [-1, 1]
            vec3 normalSample = texture2D(u_NormalTexture, v_TexCoordinate).rgb * 2.0 - 1.0;

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

        float diff = max(dot(lightVec, N), 0.0);

        // Attenuation
        float attenuation = 1.0 / (1.0 + 0.025 * dist);
        diffuse = diff * attenuation;

        // Specular (Blinn-Phong)
        vec3 viewDir = normalize(u_cameraPos - v_Position);
        vec3 halfDir = normalize(lightVec + viewDir);
        specular = pow(max(dot(N, halfDir), 0.0), 32.0) * attenuation;

        // shadow mapping
        if (vShadowCoord.w > 0.0) {
            shadowFactor = shadowPCF();
            shadowFactor = (shadowFactor * 0.5) + 0.5;
        }
    }

    // Ambient light
    float ambient = 0.40;
    float totalLight = min((diffuse + specular + ambient), 1.0);

    // Combine lighting with color
    finalColor.rgb = finalColor.rgb * totalLight *  shadowFactor;

    // Apply Emissive texture (if enabled)
    if (u_EmissiveTextured){
        vec4 emissiveTex = texture2D(u_EmissiveTexture, v_TexCoordinate);
        finalColor.rgb += emissiveTex.rgb * u_EmissiveFactor;
    }

    // Set output color
    gl_FragColor = finalColor;

    // Force opaque if mode is 0 (OPAQUE)
    if (u_AlphaMode == 0) {
        gl_FragColor.a = 1.0;
    }
}