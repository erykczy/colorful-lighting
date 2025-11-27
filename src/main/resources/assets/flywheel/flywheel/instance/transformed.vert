// START colorful lighting
vec2 transformLight(ivec2 light) {
    return vec2(
    intBitsToFloat(light[0]),
    intBitsToFloat(light[1])
    );
}
// END colorful lighting

void flw_instanceVertex(in FlwInstance i) {
    flw_vertexPos = i.pose * flw_vertexPos;
    flw_vertexNormal = mat3(transpose(inverse(i.pose))) * flw_vertexNormal;
    flw_vertexColor *= i.color;
    flw_vertexOverlay = i.overlay;

    // START colorful lighting
    //int green8 = (floatBitsToInt(i.light[0]) >> 8) & 0xFF;
    //flw_vertexLight = vec2(i.light[0], 0);//max(vec2(i.light) / 256.0, flw_vertexLight);
    flw_vertexLight = transformLight(ivec2(i.light));
    //flw_vertexLight = max(vec2(i.light) / 256.0, flw_vertexLight);
    // END colorful lighting
}
