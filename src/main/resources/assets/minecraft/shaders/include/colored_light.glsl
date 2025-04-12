#version 150

#moj_import <light.glsl>

vec4 calculateLightColor(sampler2D lightMap, ivec2 uv, vec3 blockLightColor) {
    vec3 sky = minecraft_sample_lightmap(lightMap, ivec2(0, uv.y)).xyz;
    vec3 block = pow(blockLightColor, vec3(1.3));

    // workaround for incorrect rendering of items in inventory
    float useVanilla = step(blockLightColor.r + blockLightColor.g + blockLightColor.b, 0.0);
    vec3 vanillaSkyBlock = minecraft_sample_lightmap(lightMap, uv).xyz;

    return vec4((1.0 - useVanilla) * (sky + block) + useVanilla * vanillaSkyBlock, 1.0);
}