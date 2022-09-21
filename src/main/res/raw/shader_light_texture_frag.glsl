precision highp float;

// colors
uniform vec4 vColor;
uniform vec4 vColorMask;

// texture
uniform bool u_Textured;
uniform sampler2D u_Texture;
varying vec2 v_TexCoordinate;

// lights
uniform vec3 u_LightPos;
uniform vec3 u_cameraPos;

varying vec3 v_Model;
varying vec3 v_Normal;
varying vec3 v_Light;

// normalMap
uniform bool u_NormalTextured;
uniform sampler2D u_NormalTexture;

// emissiveMap
uniform bool u_EmissiveTextured;
uniform sampler2D u_EmissiveTexture;

void main(){

    vec3 normal = v_Normal;
    if (u_NormalTextured){

        // obtain normal from normal map in range [0,1]
        vec3 nmap = texture2D(u_NormalTexture, v_TexCoordinate).rgb;

        // transform normal vector to range [-1,1]
        normal = normalize(v_Normal + normalize(nmap * 2.0 - 1.0));

        // normal = normalize(nmap * 2.0 - 1.0);
    }

    // Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
    // pointing in the same direction then it will get max illumination.
    // float diffuse = max(dot(v_Light, normal),0.0); // --> lights only on camera in front of face
    float diffuse = max(dot(v_Light, normal),0.0);

    // Attenuate the light based on distance.
    float distance = distance(u_LightPos,v_Model);
    distance = 1.0 / (1.0 + distance * 0.05);
    diffuse = diffuse * distance;

    // specular light
    vec3 viewDir = normalize(u_cameraPos - v_Model);
    vec3 reflectDir = reflect(-v_Light, normal);
    float specular = pow(max(dot(reflectDir, viewDir),0.0),32.0);

    // ambient light
    float ambient = 0.5;

    // Multiply the color by the illumination level. It will be interpolated across the triangle.
    vec4 color = vColor * min((diffuse + specular + ambient),1.0);

    if (u_Textured && length(vec3(color)) > 0.0){
        color = color * texture2D(u_Texture, v_TexCoordinate);
    } else if (u_Textured){
        color = texture2D(u_Texture, v_TexCoordinate);
    }

    if (u_EmissiveTextured){
        color = color + texture2D(u_EmissiveTexture, v_TexCoordinate);
    }

    gl_FragColor = color * vColorMask;
    gl_FragColor[3] = vColor[3] * vColorMask[3];
}