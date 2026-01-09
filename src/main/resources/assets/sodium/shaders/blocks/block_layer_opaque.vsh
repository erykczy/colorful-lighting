#version 330 core

#import <sodium:include/fog.glsl>
#import <sodium:include/chunk_vertex.glsl>
#import <sodium:include/chunk_matrices.glsl>
#import <sodium:include/chunk_material.glsl>

out vec4 v_Color;
out vec2 v_TexCoord;

out float v_MaterialMipBias;
#ifdef USE_FRAGMENT_DISCARD
out float v_MaterialAlphaCutoff;
#endif

#ifdef USE_FOG
out float v_FragDistance;
#endif

uniform int u_FogShape;
uniform vec3 u_RegionOffset;

uniform sampler2D u_LightTex; // The light map texture sampler

// --- COLORFUL LIGHTING START ---
vec4 _sample_lightmap_vanilla(sampler2D lightMap, ivec2 uv) {
    return texture(lightMap, clamp(uv / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}

vec4 _sample_lightmap(sampler2D lightMap, ivec2 uv) {
    int leastSignificantShort = uv.x;
    int mostSignificantShort = uv.y;

    int red8 = (leastSignificantShort >> 0) & 0xFF;
    int green8 = (leastSignificantShort >> 8) & 0xFF;

    int skyLight4 = (mostSignificantShort >> 0) & 0xF;
    int blue8 = (mostSignificantShort >> 4) & 0xFF;
    int alpha4 = (mostSignificantShort >> 12) & 0xF;

    if ((mostSignificantShort & 0xF000) != 0xF000) {
        return _sample_lightmap_vanilla(lightMap, uv);
    }

    const float divideBy255 = 0.00392156862;
    vec3 blockLightColor = vec3(float(red8)*divideBy255, float(green8)*divideBy255, float(blue8)*divideBy255);

    vec3 sky = _sample_lightmap_vanilla(lightMap, ivec2(0, skyLight4 << 4)).xyz;

    // Tweak vibrancy: lower gamma to 1.0 (linear) or 0.6 for more punch
    vec3 block = pow(blockLightColor, vec3(1.0));

    // Boost saturation/brightness if needed
    // block *= 1.2;

    return vec4(sky + block * max(0.3, 1.0 - sky.r), 1.0);
}
// --- COLORFUL LIGHTING END ---

uvec3 _get_relative_chunk_coord(uint pos) {
    return uvec3(pos) >> uvec3(5u, 0u, 2u) & uvec3(7u, 3u, 7u);
}

vec3 _get_draw_translation(uint pos) {
    return _get_relative_chunk_coord(pos) * vec3(16.0);
}

void main() {
    _vert_init();

    vec3 translation = u_RegionOffset + _get_draw_translation(_draw_id);
    vec3 position = _vert_position + translation;

#ifdef USE_FOG
    v_FragDistance = getFragDistance(u_FogShape, position);
#endif

    gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(position, 1.0);

    v_Color = _vert_color * _sample_lightmap(u_LightTex, _vert_tex_light_coord);
    v_TexCoord = _vert_tex_diffuse_coord;

    v_MaterialMipBias = _material_mip_bias(_material_params);
#ifdef USE_FRAGMENT_DISCARD
    v_MaterialAlphaCutoff = _material_alpha_cutoff(_material_params);
#endif
}
