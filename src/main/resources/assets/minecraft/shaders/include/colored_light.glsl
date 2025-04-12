#version 150

#moj_import <light.glsl>

vec4 calculateLightColor(sampler2D lightMap, ivec2 uv, vec4 blockLightColor) {
    vec3 sky = minecraft_sample_lightmap(lightMap, ivec2(0, uv.y)).xyz;
    vec3 block = pow(blockLightColor.rgb, vec3(1.3));

    // workaround for incorrect rendering of items in inventory
    float useVanilla = blockLightColor.a; // special case
    vec3 vanillaSkyBlock = minecraft_sample_lightmap(lightMap, uv).xyz;

    return vec4((1.0 - useVanilla) * (sky + block) + useVanilla * vanillaSkyBlock, 1.0);
}