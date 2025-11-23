#include "flywheel:util/quaternion.glsl"
#include "flywheel:util/matrix.glsl"

void flw_instanceVertex(in FlwInstance instance) {
    flw_vertexPos = vec4(rotateByQuaternion(flw_vertexPos.xyz - .5, instance.rotation) + instance.pos + .5, 1.);

    flw_vertexNormal = rotateByQuaternion(flw_vertexNormal, instance.rotation);

    vec2 scroll = fract(instance.speed * flw_renderTicks + instance.offset) * instance.scale;

    flw_vertexTexCoord = flw_vertexTexCoord + instance.diff + scroll;
    flw_vertexOverlay = instance.overlay;
    // START colorful lighting
    // rotating instance stores instance.light components in short so conversion is needed
    flw_vertexLight[0] = intBitsToFloat(instance.light[0]);
    flw_vertexLight[1] = intBitsToFloat(instance.light[1]);
    // END colorful lighting
}
