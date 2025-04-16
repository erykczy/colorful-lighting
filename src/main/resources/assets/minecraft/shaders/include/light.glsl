#version 150

#define MINECRAFT_LIGHT_POWER   (0.6)
#define MINECRAFT_AMBIENT_LIGHT (0.4)

vec4 minecraft_mix_light(vec3 lightDir0, vec3 lightDir1, vec3 normal, vec4 color) {
    float light0 = max(0.0, dot(lightDir0, normal));
    float light1 = max(0.0, dot(lightDir1, normal));
    float lightAccum = min(1.0, (light0 + light1) * MINECRAFT_LIGHT_POWER + MINECRAFT_AMBIENT_LIGHT);
    return vec4(color.rgb * lightAccum, color.a);
}

vec4 minecraft_sample_vanilla_lightmap(sampler2D lightMap, ivec2 uv) {
    return texture(lightMap, clamp(uv / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}

vec4 minecraft_sample_lightmap(sampler2D lightMap, ivec2 uv) {
    int leastSignificantShort = uv.x;
    int mostSignificantShort = uv.y;
    int red8 = (leastSignificantShort >> 0) & 0xFF;
    int green8 = (leastSignificantShort >> 8) & 0xFF;
    int skyLight4 = (mostSignificantShort >> 0) & 0xF;
    int blue8 = (mostSignificantShort >> 4) & 0xFF;
    int alpha4 = (mostSignificantShort >> 12) & 0xF;
    if(alpha4 != 0xF) {
        return minecraft_sample_vanilla_lightmap(lightMap, uv);
    }
    const float divideBy255 = 0.003921;
    vec3 blockLightColor = vec3(red8*divideBy255, green8*divideBy255, blue8*divideBy255);

    vec3 sky = minecraft_sample_vanilla_lightmap(lightMap, ivec2(0, skyLight4)).xyz;
    vec3 block = pow(blockLightColor, vec3(1.3));
    return vec4(sky + block, 1.0);
}
