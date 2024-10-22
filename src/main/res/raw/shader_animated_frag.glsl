precision highp float;

// data
uniform mat4 u_MMatrix;
uniform vec3 u_cameraPos;
varying vec3 v_Position;

// color
uniform vec4 vColor;
uniform vec4 vColorMask;

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
uniform mat4 uNormalMatrix;
uniform vec3 u_LightPos;
varying vec3 v_Normal;

// normalMap
uniform bool u_NormalTextured;
uniform sampler2D u_NormalTexture;
varying vec3 v_Tangent;

// emissiveMap
uniform bool u_EmissiveTextured;
uniform sampler2D u_EmissiveTexture;
uniform vec3 u_EmissiveFactor;

// volume
uniform bool u_TransmissionTextured;
uniform sampler2D u_TransmissionTexture;
uniform float u_TransmissionFactor;
//uniform vec3 u_EmissiveFactor;

void main(){

    // colours
    vec4 color;
    if (u_Coloured){
        color = v_Color;
    } else {
        color = vColor;
    }

    // textures
    vec4 tex = vec4(1.0,1.0,1.0,1.0);
    if (u_Textured){
        if (u_TextureTransformed){
            mat3 translation = mat3(1, 0, 0, 0, 1, 0, u_TextureOffset.x, u_TextureOffset.y, 1);
            mat3 rotation = mat3(
                cos(u_TextureRotation), -sin(u_TextureRotation), 0,
                sin(u_TextureRotation), cos(u_TextureRotation), 0,
                0, 0, 1
            );
            mat3 scale = mat3(u_TextureScale.x, 0, 0, 0, u_TextureScale.y, 0, 0, 0, 1);
            mat3 matrix = translation * rotation * scale;
            vec2 uvTransformed = (matrix * vec3(v_TexCoordinate.xy, 1)).xy;
            tex = texture2D(u_Texture, uvTransformed);
        } else {
            tex = texture2D(u_Texture, v_TexCoordinate);
        }
    }

    // emissive texture
    vec4 emissive = vec4(0.0,0.0,0.0,0.0);
    if (u_EmissiveTextured){
        vec4 emtex = texture2D(u_EmissiveTexture, v_TexCoordinate);
        emissive = emtex * vec4(u_EmissiveFactor,1.0);
    }

    // light
    float diffuse = 1.0;
    float specular = 0.0;
    vec3 refractedColor = vec3(0.0,0.0,0.0);
    if (u_Lighted) {

        // Transform the vertex into eye space.
        vec3 modelVertex = vec3(u_MMatrix * vec4(v_Position,1.0));

        // Transform the normal's orientation into eye space.
        // Note that we need to remove the translation part by setting w=0
        vec3 modelNormal = normalize(vec3(u_MMatrix * vec4(v_Normal,0.0)));

        // normal map
        if (u_NormalTextured){

            /// Sample the normal map
            vec3 normalMap = texture2D(u_NormalTexture, v_TexCoordinate).rgb;

            // Convert the normal map from [0, 1] to [-1, 1]
            normalMap = normalize(normalMap * 2.0 - 1.0);

            // Calculate the bitangent using the cross product of the normal and tangent
            vec3 bitangent = cross(modelNormal, v_Tangent);

            // Construct the TBN matrix
            mat3 TBN = mat3(
                normalize(v_Tangent),
                normalize(bitangent),
                normalize(modelNormal)
            );

            // Transform the normal map to world space
            modelNormal = TBN * normalMap;
        }

        // refraction
        if (u_TransmissionTextured){

            // Calculate view direction
            vec3 viewDir = normalize(u_cameraPos - v_Position);

            // Calculate refracted direction
            vec3 refractedDir = refract(viewDir, modelNormal, u_TransmissionFactor);

            // Sample the thickness map
            float thickness = texture2D(u_TransmissionTexture, v_TexCoordinate).r;

            // Calculate refracted color based on thickness and thickness factor
            //vec3 refractedColor = texture2D(albedoMap, refractedDir.xy).rgb * (1.0 - thicknessFactor * thickness);

            //color = refractedColor;

            // Output the refracted color

        }

        // Get a lighting direction vector from the light to the vertex.
        vec3 lightVector = normalize(u_LightPos - modelVertex);

        // Calculate the dot product of the light vector and vertex normal.
        // If the normal and light vector are pointing in the same direction then it will get max illumination.
        // float diffuse = max(dot(lightVector, modelNormal),0.0); // debug: lights only on camera in front of face
        float diff = max(dot(lightVector, modelNormal), 0.0);

        // Attenuate the light based on distance.
        float dist = distance(u_LightPos, modelVertex);
        dist = 1.0 / (1.0 + dist * 0.005);
        diffuse = diff * dist;

        // specular light
        vec3 viewDir = normalize(u_cameraPos - modelVertex);
        vec3 reflectDir = reflect(-lightVector, modelNormal);
        specular = pow(max(dot(reflectDir, viewDir),0.0),32.0);
    }




    // ambient light
    float ambient = 0.5;

    // light
    float light = min((diffuse + specular + ambient),1.0);

    // calculate final color
    gl_FragColor = color * tex * vColorMask * light + emissive;


    // alpha mode
    if(u_Textured){
        if (u_AlphaMode == 0) { // opaque
            gl_FragColor[3] = 1.0;
        } else if (u_AlphaMode == 1) { // mask

            gl_FragColor[3] = tex[3];
            if (tex.a < u_AlphaCutoff){
                discard;
            }
        } else { // blend
            gl_FragColor[3] = tex[3];
        }
        if (gl_FragColor[3] < 0.5){
            //gl_FragColor[1] = 1.0;
        }
    } else {
        gl_FragColor[3] = color[3] * vColorMask[3];
    }

    /*if (u_EmissiveTextured){
        gl_FragColor[0] = emissive[0];
        gl_FragColor[1] = emissive[1];
        gl_FragColor[2] = emissive[2];
    }*/


    /*if (gl_FragColor[3] >= 0.0){
        gl_FragColor[0] = color[0];
        gl_FragColor[1] = color[1];
        gl_FragColor[2] = color[2];
        gl_FragColor[3] = 1.0;
    }*/
}
