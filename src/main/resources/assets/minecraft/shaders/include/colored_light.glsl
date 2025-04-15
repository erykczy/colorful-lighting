#version 150

#moj_import <light.glsl>

vec4 calculateLightColor(sampler2D lightMap, ivec2 uv2) {
    int leastSignificantShort = uv2.x;
    int mostSignificantShort = uv2.y;
    int blockLight = (mostSignificantShort) & 0xF;
    int skyLight = (mostSignificantShort >> 4) & 0xF;
    int red4 = (leastSignificantShort) & 0xF;
    int green4 = (leastSignificantShort >> 4) & 0xF;
    int blue4 = (leastSignificantShort >> 8) & 0xF;
    vec3 blockLightColor = vec3(red4*0.06666, green4*0.06666, blue4*0.06666);

    vec3 sky = minecraft_sample_lightmap(lightMap, ivec2(0, uv2.y)).xyz;
    vec3 block = pow(blockLightColor, vec3(1.3));

    // workaround for incorrect rendering of items in inventory
    //float useVanilla = blockLightColor.a; // special case
    //vec3 vanillaSkyBlock = minecraft_sample_lightmap(lightMap, uv2).xyz;

    return vec4(sky + block, 1.0); //(1.0 - useVanilla) * (sky + block) + useVanilla * vanillaSkyBlock
}