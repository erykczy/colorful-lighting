#version 150

#moj_import <light.glsl>

vec4 calculateLightColor(sampler2D lightMap, ivec2 uv, vec3 blockLightColor) {
    vec3 sky = minecraft_sample_lightmap(lightMap, ivec2(0, uv.y)).xyz;
    vec3 block = pow(blockLightColor, vec3(1.3));
    return vec4(sky + block, 1.0);
}