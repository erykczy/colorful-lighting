#include "flywheel:util/quaternion.glsl"

// START colorful lighting
vec2 transformLight(ivec2 light) {
    return vec2(
        intBitsToFloat(light[0]),
        intBitsToFloat(light[1])
    );
}
// END colorful lighting

void flw_instanceVertex(in FlwInstance i) {
    flw_vertexPos = vec4(rotateByQuaternion(flw_vertexPos.xyz - i.pivot, i.rotation) + i.pivot + i.position, 1.0);
    flw_vertexNormal = rotateByQuaternion(flw_vertexNormal, i.rotation);
    flw_vertexColor *= i.color;
    flw_vertexOverlay = i.overlay;
    // START colorful lighting
    flw_vertexLight = transformLight(ivec2(i.light));
    // END colorful lighting
}
