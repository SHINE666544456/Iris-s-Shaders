#version 330

// LightShader uber pass: sharpen -> depth AO -> bloom-lite -> haze -> grade -> vignette.
// Every effect is skipped when its strength is 0, so presets only pay for what they use.

uniform sampler2D MainSampler;
uniform sampler2D MainDepthSampler;

// ORDER MUST MATCH the "uniforms" list in every post_effect/*.json (std140, floats only).
layout(std140) uniform LightShaderConfig {
    float Exposure;
    float Contrast;
    float Saturation;
    float Warmth;
    float Vignette;
    float Sharpen;
    float BloomStrength;
    float BloomThreshold;
    float BloomRadius;
    float AoStrength;
    float AoRadius;
    float HazeStrength;
    float HazeStart;
    float Far;
};

in vec2 texCoord;
out vec4 fragColor;

const float NEAR = 0.05;
const float PI2 = 6.28318530718;

const vec2 RING[8] = vec2[8](
    vec2(1.0, 0.0), vec2(0.7071, 0.7071), vec2(0.0, 1.0), vec2(-0.7071, 0.7071),
    vec2(-1.0, 0.0), vec2(-0.7071, -0.7071), vec2(0.0, -1.0), vec2(0.7071, -0.7071)
);

float linearize(float d) {
    float z = d * 2.0 - 1.0;
    return (2.0 * NEAR * Far) / (Far + NEAR - z * (Far - NEAR));
}

float luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

// Identity below 0.8, smooth roll-off to 1.0 above. Keeps exposure boosts from clipping hard.
vec3 softKnee(vec3 c) {
    vec3 over = max(c - 0.8, 0.0);
    return min(c, vec3(0.8)) + 0.2 * (1.0 - exp(-over / 0.2));
}

void main() {
    vec2 uv = texCoord;
    ivec2 size = textureSize(MainSampler, 0);
    vec2 texel = 1.0 / vec2(size);
    float aspect = float(size.x) / float(size.y);

    vec3 col = texture(MainSampler, uv).rgb;
    float depth = texture(MainDepthSampler, uv).r;
    bool sky = depth >= 0.99999;
    float lin = sky ? Far : linearize(depth);

    float ign = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));

    // 1. Sharpen (4 taps, clamped unsharp mask)
    if (Sharpen > 0.0) {
        vec3 n = texture(MainSampler, uv + vec2(texel.x, 0.0)).rgb
               + texture(MainSampler, uv - vec2(texel.x, 0.0)).rgb
               + texture(MainSampler, uv + vec2(0.0, texel.y)).rgb
               + texture(MainSampler, uv - vec2(0.0, texel.y)).rgb;
        vec3 detail = col - n * 0.25;
        col += clamp(detail * Sharpen * 2.0, vec3(-0.08), vec3(0.08));
    }

    // 2. Depth-based ambient occlusion (8 taps, noise-rotated ring)
    if (AoStrength > 0.0 && !sky) {
        float a = ign * PI2;
        mat2 rot = mat2(cos(a), sin(a), -sin(a), cos(a));
        float r = clamp(AoRadius * 36.0 / max(lin, 1.0), 1.5, 14.0);
        float occ = 0.0;
        for (int i = 0; i < 8; i++) {
            vec2 o = rot * RING[i] * r * texel;
            float diff = lin - linearize(texture(MainDepthSampler, uv + o).r);
            occ += step(0.04 + lin * 0.002, diff) * (1.0 - smoothstep(0.4, 1.6, diff));
        }
        col *= 1.0 - AoStrength * (occ / 8.0);
    }

    // 3. Bloom-lite (16 bilinear taps in two rings, thresholded)
    if (BloomStrength > 0.0) {
        vec3 b = vec3(0.0);
        for (int i = 0; i < 8; i++) {
            b += max(texture(MainSampler, uv + RING[i] * texel * BloomRadius * 2.0).rgb - BloomThreshold, 0.0);
            b += max(texture(MainSampler, uv + RING[i] * texel * BloomRadius * 5.0).rgb - BloomThreshold, 0.0);
        }
        col += b * (BloomStrength * 0.125);
    }

    // 4. Distance haze: desaturate and cool far terrain (sky untouched)
    if (HazeStrength > 0.0 && !sky) {
        float h = smoothstep(HazeStart, 1.0, lin / Far) * HazeStrength;
        vec3 hazeCol = vec3(luma(col)) * vec3(0.94, 1.0, 1.08) + 0.03;
        col = mix(col, hazeCol, h);
    }

    // 5. Grade
    col *= Exposure;
    col = softKnee(col);
    col = (col - 0.5) * Contrast + 0.5;
    col = mix(vec3(luma(col)), col, Saturation);
    col.r *= 1.0 + 0.06 * Warmth;
    col.b *= 1.0 - 0.06 * Warmth;

    // 6. Vignette
    float d = length((uv - 0.5) * vec2(aspect, 1.0));
    col *= 1.0 - Vignette * smoothstep(0.35, 0.95, d);

    // Dither to hide banding
    col += (ign - 0.5) / 255.0;

    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
