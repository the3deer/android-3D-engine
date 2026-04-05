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

// Simple hash for procedural stars
float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

/**
 * Calculates a realistic solar position based on day progress.
 * @param progress 0.0 (Midnight) to 1.0 (Next Midnight). 0.5 is Noon.
 * @return Normalized sun direction vector.
 */
vec3 getSunDirection(float progress) {
    // Convert 0.0-1.0 progress to an angle where 0.5 (Noon) is the peak.
    float angle = (progress * 2.0 * PI) - PI;

    // Spain Latitude (~40N)
    float lat = 0.7;

    float x = sin(angle);
    float y = cos(angle) * cos(lat);
    float z = cos(angle) * sin(lat);

    return normalize(vec3(-x, y, z));
}

vec3 getProceduralSky(vec3 dir) {
    float height = dir.y;
    float dayProgress = u_Time;

    // 1. Solar Position
    vec3 sunDir = getSunDirection(dayProgress);

    // 2. Cycle Factor (1.0 at Noon, 0.0 at Midnight)
    float cycle = clamp(sunDir.y * 2.0, 0.0, 1.0);

    // 3. Atmosphere Colors
    vec3 skyColor = mix(vec3(0.005, 0.005, 0.015), vec3(0.3, 0.5, 0.9), pow(cycle, 0.5));
    float horizonGlowFactor = clamp(1.0 - abs(sunDir.y * 5.0), 0.0, 1.0);
    vec3 horizonColor = mix(vec3(0.005, 0.005, 0.01), vec3(0.8, 0.4, 0.2), horizonGlowFactor * cycle);
    horizonColor = mix(horizonColor, skyColor, 0.5);

    vec3 finalSky = mix(horizonColor, skyColor, max(height, 0.0));

    // 4. Sun Disk
    float sunPoint = max(dot(dir, sunDir), 0.0);
    float sunDisk = pow(sunPoint, 1024.0) * 3.0;
    float sunGlow = pow(sunPoint, 16.0) * 0.8 * cycle;
    vec3 sunColor = vec3(1.0, 0.9, 0.7) * (sunDisk + sunGlow);
    if (sunDir.y < -0.05) sunColor *= 0.0;

    // 5. Stars (Visible at Night)
    vec3 stars = vec3(0.0);
    if (cycle < 0.3 && height > 0.0) {
        // Simulating Earth's rotation: The celestial sphere rotates around the polar axis.
        float starRotationAngle = dayProgress * 2.0 * PI;
        float s = sin(starRotationAngle);
        float c = cos(starRotationAngle);

        // Rotate the sampling direction around the Y axis
        vec3 rotatedDir = vec3(
            dir.x * c - dir.z * s,
            dir.y,
            dir.x * s + dir.z * c
        );

        // Stable stars using grid quantization to prevent flickering during rotation
        const float gridScale = 250.0;
        vec3 gridDir = floor(rotatedDir * gridScale);
        float starHash = hash(gridDir);

        if (starHash > 0.996) {
            // Randomize blink speed and phase per star using starHash
            float speed = 1000000.0 + starHash * 2000000.0;
            float phase = starHash * 62.8;
            float twinkle = sin(dayProgress * speed + phase) * 0.5 + 0.5;

            // Distance from center of the grid cell to make stars circular
            float dist = length(fract(rotatedDir * gridScale) - 0.5);
            float starSize = 0.45 * (starHash - 0.996) / (1.0 - 0.996) + 0.05;

            // Fade out stars as daylight increases
            float starAlpha = 1.0 - clamp(cycle * 3.33, 0.0, 1.0);

            // Reduced twinkle intensity (0.9 to 1.0 brightness range) for a more subtle look
            float blinkFactor = 0.9 + 0.1 * twinkle;
            stars = vec3(1.0) * smoothstep(starSize, starSize * 0.5, dist) * blinkFactor * starAlpha;
        }
    }

    // 6. Ground
    if (height < 0.0) {
        finalSky = mix(horizonColor, vec3(0.01), min(-height * 10.0, 1.0));
    }

    return finalSky + sunColor + stars;
}

void main(){
    if (u_Textured) {
        fragColor = texture(u_TextureCube, v_TexCoordinate.xyz);
    } else {
        fragColor = vec4(getProceduralSky(normalize(v_TexCoordinate.xyz)), 1.0);
    }
    fragColor = fragColor * u_Color * u_ColorMask;
}