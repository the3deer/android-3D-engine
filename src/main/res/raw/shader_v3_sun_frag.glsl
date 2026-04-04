#version 300 es
precision highp float;

uniform vec4 u_Color;
uniform vec4 u_ColorMask;

out vec4 fragColor;

void main() {
    // gl_PointCoord goes from (0,0) to (1,1)
    vec2 uv = gl_PointCoord - vec2(0.5);
    float dist = length(uv);

    // 1. Core Sun Disk
    float core = smoothstep(0.15, 0.1, dist);

    // 2. Soft Halo
    float halo = exp(-dist * 8.0) * 0.4;

    // 3. Glare Spikes (Starburst)
    // Calculate angle from center to create rays
    float angle = atan(uv.y, uv.x);

    // Create 8 main rays using a high-power cosine function
    float rays1 = pow(max(0.0, cos(angle * 8.0)), 10.0) * exp(-dist * 4.0) * 0.5;

    // Create 12 smaller rays for extra detail
    float rays2 = pow(max(0.0, cos(angle * 12.0 + 0.5)), 20.0) * exp(-dist * 6.0) * 0.3;

    // 4. Anamorphic horizontal flare (cinematic look)
    float flare = exp(-abs(uv.y) * 40.0) * exp(-abs(uv.x) * 3.0) * 0.2;

    // Combine components
    float intensity = core + halo + rays1 + rays2 + flare;

    // Golden-White Color Palette
    vec3 coreColor = vec3(1.0, 1.0, 1.0);
    vec3 edgeColor = vec3(1.0, 0.9, 0.5);
    vec3 finalColor = mix(edgeColor, coreColor, core);

    // Final output with smooth edge alpha
    float alpha = intensity * smoothstep(0.5, 0.4, dist);
    fragColor = vec4(finalColor, alpha) * u_Color * u_ColorMask;
}