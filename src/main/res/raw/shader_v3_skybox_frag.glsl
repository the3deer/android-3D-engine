#version 300 es
precision highp float;

// colors
uniform vec4 u_Color;
uniform vec4 u_ColorMask;

// skybox
uniform samplerCube u_TextureCube;
uniform bool u_Textured;
uniform float u_Time; // Progress of day (0.0 to 1.0)
in vec4 v_TexCoordinate;

out vec4 fragColor;

const float PI = 3.14159265359;

/**
 * Calculates a realistic solar position based on day progress.
 * @param progress 0.0 (Midnight) to 1.0 (Next Midnight). 0.5 is Noon.
 * @return Normalized sun direction vector.
 */
vec3 getSunDirection(float progress) {
    // Convert 0.0-1.0 progress to an angle where 0.5 (Noon) is the peak.
    // Solar angle: 0 at Sunrise, PI at Sunset.
    // We adjust the offset so Noon (0.5) corresponds to the highest point.
    float angle = (progress * 2.0 * PI) - PI;

    // Spain Latitude is approx 40 degrees North (0.7 radians)
    float lat = 0.7;

    // Calculate position:
    // X: East (-) to West (+)
    // Y: Altitude (Altitude is highest when angle is 0, which we shift from progress 0.5)
    // Z: Southward tilt
    float x = sin(angle);
    float y = cos(angle) * cos(lat);
    float z = cos(angle) * sin(lat);

    return normalize(vec3(-x, y, z));
}

vec3 getProceduralSky(vec3 dir) {
    float height = dir.y;
    float dayProgress = u_Time;

    // 1. Calculate Sun Direction
    vec3 sunDir = getSunDirection(dayProgress);

    // 2. Day/Night Cycle Factor
    // peaks at 1.0 when sun is at zenith, 0.0 when below horizon
    float cycle = clamp(sunDir.y * 2.0, 0.0, 1.0);

    // 3. Atmosphere Colors
    // Blue for day, deep dark blue/black for night
    vec3 skyColor = mix(vec3(0.01, 0.01, 0.03), vec3(0.3, 0.5, 0.9), pow(cycle, 0.5));

    // Horizon glow (orange at sunset/sunrise)
    float horizonGlowFactor = clamp(1.0 - abs(sunDir.y * 5.0), 0.0, 1.0);
    vec3 horizonColor = mix(vec3(0.01, 0.01, 0.02), vec3(0.8, 0.4, 0.2), horizonGlowFactor * cycle);
    horizonColor = mix(horizonColor, skyColor, 0.5); // Blend into sky

    vec3 finalSky = mix(horizonColor, skyColor, max(height, 0.0));

    // 4. Sun Disk
    float sunPoint = max(dot(dir, sunDir), 0.0);
    float sunDisk = pow(sunPoint, 1024.0) * 3.0;
    float sunGlow = pow(sunPoint, 16.0) * 0.8 * cycle;

    vec3 sunColor = vec3(1.0, 0.9, 0.7) * (sunDisk + sunGlow);
    if (sunDir.y < -0.05) sunColor *= 0.0; // Hide sun at night

    // 5. Ground
    if (height < 0.0) {
        finalSky = mix(horizonColor, vec3(0.02), min(-height * 10.0, 1.0));
    }

    return finalSky + sunColor;
}

void main(){
    if (u_Textured) {
        fragColor = texture(u_TextureCube, v_TexCoordinate.xyz);
    } else {
        fragColor = vec4(getProceduralSky(normalize(v_TexCoordinate.xyz)), 1.0);
    }
    fragColor = fragColor * u_Color * u_ColorMask;
}