#include "flywheel:internal/light_lut.glsl"

layout(std430, binding = 8) restrict readonly buffer ColoredLightSections {
    int coloredLightSections[];
};

struct ColoredLightIntegerData {
    int red8;
    int green8;
    int blue8;
    int skyLight4;
    int alpha4;
};

struct ColoredLightFloatData {
    vec3 lightColor;
    float skyLight;
    float alpha;
};

vec4 minecraft_sample_vanilla_lightmap(sampler2D lightMap, ivec2 uv) {
    return texture(lightMap, clamp(uv / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}

int ivec2ToInt(ivec2 data) {
    return data.y << 16 | (data.x & 0xFFFF);//int((uint(data.y) << 16) | uint(data.x));
}

ColoredLightIntegerData unpackColoredLightData(int packedData) {
    return ColoredLightIntegerData(
        packedData & 0xFF, // red
        (packedData >> 8) & 0xFF, // green
        (packedData >> 20) & 0xFF, // blue
        (packedData >> 16) & 0xF, // sky 4
        (packedData >> 28) & 0xF // alpha 4
    );
}
ColoredLightIntegerData unpackColoredLightData(ivec2 packedData) {
    return unpackColoredLightData(ivec2ToInt(packedData));
}

bool isPackedDataColored(int packedData) {
    return ((packedData >> 28) & 0xF) == 0xF;
}
bool isPackedDataColored(ivec2 packedData) {
    return isPackedDataColored(ivec2ToInt(packedData));
}

ColoredLightFloatData coloredLightData_integerToFloat(ColoredLightIntegerData data) {
    return ColoredLightFloatData(
        vec3(data.red8 / 255.0,
            data.green8 / 255.0,
            data.blue8 / 255.0
        ),
        data.skyLight4 / 15.0,
        data.alpha4 / 15.0
    );
}

vec4 mixColoredLightWithLightMap(sampler2D lightMap, ColoredLightFloatData data) {
    vec3 sky = minecraft_sample_vanilla_lightmap(lightMap, ivec2(0, int(data.skyLight * 15) << 4)).xyz;
    vec3 block = pow(data.lightColor, vec3(1.3));
    return vec4(sky + block * max(0.3, 1.0 - sky.r), 1.0);
}

int fetchColoredLight(ivec3 blockPos) {
    uint lightSectionIndex;
    if (_flw_chunkCoordToSectionIndex(blockPos >> 4, lightSectionIndex)) {
        return -1;
    }
    ivec3 blockPosRelative = ivec3((blockPos & 0xF) + 1);
    int index = (blockPosRelative.x + blockPosRelative.z * 18 + blockPosRelative.y * 18 * 18);
    return coloredLightSections[lightSectionIndex * 18 * 18 * 18 + index];
}

ColoredLightFloatData vertexLightColor(ivec2 instanceLight, ivec3 blockPos) {
    int fetchedLight = fetchColoredLight(blockPos);
    ColoredLightIntegerData data = unpackColoredLightData(
    fetchedLight == -1 ? ivec2ToInt(instanceLight) : fetchedLight
    );
    return coloredLightData_integerToFloat(data);
}