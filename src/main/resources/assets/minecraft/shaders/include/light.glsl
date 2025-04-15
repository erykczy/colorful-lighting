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
    int blockLight = (mostSignificantShort) & 0xF;
    int skyLight = (mostSignificantShort >> 4) & 0xF;
    int red4 = (leastSignificantShort) & 0xF;
    int green4 = (leastSignificantShort >> 4) & 0xF;
    int blue4 = (leastSignificantShort >> 8) & 0xF;
    vec3 blockLightColor = vec3(red4*0.06666, green4*0.06666, blue4*0.06666);

    vec3 sky = minecraft_sample_vanilla_lightmap(lightMap, ivec2(0, uv.y)).xyz;
    vec3 block = pow(blockLightColor, vec3(1.3));
    return vec4(sky + block, 1.0);
}
