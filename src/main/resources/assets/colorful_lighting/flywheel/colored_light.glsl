#include "flywheel:internal/light_lut.glsl"
#include "colorful_lighting:colored_light_types.glsl"

layout(std430, binding = 8) restrict readonly buffer ColoredLightSections {
    int coloredLightSections[];
};

vec4 minecraft_sample_vanilla_lightmap(sampler2D lightMap, ivec2 uv) {
    return texture(lightMap, clamp(uv / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}

int ivec2ToInt(ivec2 data) {
    return data.y << 16 | (data.x & 0xFFFF);
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

bool isPackedDataColored(int packedData) {
    return ((packedData >> 28) & 0xF) == 0xF;
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

// get packed colored light from uniform buffer object
int getColoredLightFromBuffer(ivec3 blockPos) {
    uint lightSectionIndex;
    if (_flw_chunkCoordToSectionIndex(blockPos >> 4, lightSectionIndex)) {
        return -1;
    }
    ivec3 blockPosRelative = ivec3((blockPos & 0xF) + 1);
    int index = (blockPosRelative.x + blockPosRelative.z * 18 + blockPosRelative.y * 18 * 18);
    return coloredLightSections[lightSectionIndex * 18 * 18 * 18 + index];
}

// sample ColoredLightFloatData at a given blockPos
ColoredLightFloatData sampleLightColor(ivec3 blockPos, ivec2 instanceLight) {
    int fetchedLight = getColoredLightFromBuffer(blockPos);
    if(fetchedLight == -1) { // no light in buffer for this section
        return coloredLightData_integerToFloat(unpackColoredLightData(ivec2ToInt(instanceLight)));
    }
    else {
        ColoredLightFloatData data = coloredLightData_integerToFloat(unpackColoredLightData(fetchedLight));
        vec2 light;
        flw_lightFetch(blockPos, light);
        data.skyLight = light.y;
        return data;
    }
}

// linear ColoredLightFloatData interpolation
ColoredLightFloatData mixLight(ColoredLightFloatData a, ColoredLightFloatData b, float t) {
    return ColoredLightFloatData(
        mix(a.lightColor, b.lightColor, t),
        mix(a.skyLight, b.skyLight, t),
        a.alpha
    );
}

// trilinear ColoredLightFloatData interpolation
/*ColoredLightFloatData sampleTrilinearLightColor(vec3 pos, ivec2 instanceLight) {
    ivec3 corner = ivec3(round(pos))-ivec3(1);
    ColoredLightFloatData c000 = sampleLightColor(ivec3(corner.x + 0, corner.y + 0, corner.z + 0), instanceLight);
    ColoredLightFloatData c100 = sampleLightColor(ivec3(corner.x + 1, corner.y + 0, corner.z + 0), instanceLight);
    ColoredLightFloatData c101 = sampleLightColor(ivec3(corner.x + 1, corner.y + 0, corner.z + 1), instanceLight);
    ColoredLightFloatData c001 = sampleLightColor(ivec3(corner.x + 0, corner.y + 0, corner.z + 1), instanceLight);
    ColoredLightFloatData c010 = sampleLightColor(ivec3(corner.x + 0, corner.y + 1, corner.z + 0), instanceLight);
    ColoredLightFloatData c110 = sampleLightColor(ivec3(corner.x + 1, corner.y + 1, corner.z + 0), instanceLight);
    ColoredLightFloatData c111 = sampleLightColor(ivec3(corner.x + 1, corner.y + 1, corner.z + 1), instanceLight);
    ColoredLightFloatData c011 = sampleLightColor(ivec3(corner.x + 0, corner.y + 1, corner.z + 1), instanceLight);

    vec3 xyz = pos - (corner + vec3(0.5));
    xyz = mix(xyz, pos - (floor(pos) + vec3(0.5)), vec3(1.0));

    ColoredLightFloatData c00 = mixLight(c000, c100, xyz.x);
    ColoredLightFloatData c10 = mixLight(c010, c110, xyz.x);
    ColoredLightFloatData c01 = mixLight(c001, c101, xyz.x);
    ColoredLightFloatData c11 = mixLight(c011, c111, xyz.x);

    ColoredLightFloatData c0 = mixLight(c00, c10, xyz.y);
    ColoredLightFloatData c1 = mixLight(c01, c11, xyz.y);

    return mixLight(c0, c1, xyz.z);
}*/

ColoredLightFloatData vertexLightColor(ivec2 instanceLight, vec3 vertexPos) {
    ivec3 blockPos = ivec3(floor(vertexPos));
    return sampleLightColor(blockPos, instanceLight);
    /*if (getColoredLightFromBuffer(blockPos) == -1) {
        // light provided by instance
        return sampleLightColor(blockPos, instanceLight);
    }
    else {
        // light provided by uniform buffer object (so we can sample multiple positions and do trilinear interpolation)
        return sampleTrilinearLightColor(vertexPos, instanceLight);
    }*/
}