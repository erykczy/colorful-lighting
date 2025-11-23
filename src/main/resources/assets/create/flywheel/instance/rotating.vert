#include "flywheel:util/quaternion.glsl"

void flw_instanceVertex(in FlwInstance instance) {
    float degrees = instance.offset + flw_renderSeconds * instance.speed;

    vec4 kineticRot = quaternionDegrees(instance.axis, degrees);
    vec3 rotated = rotateByQuaternion(flw_vertexPos.xyz - .5, instance.rotation);

    flw_vertexPos.xyz = rotateByQuaternion(rotated, kineticRot) + instance.pos + .5;
    flw_vertexNormal = rotateByQuaternion(rotateByQuaternion(flw_vertexNormal, instance.rotation), kineticRot);
    flw_vertexColor *= instance.color;
    // START colorful lighting
    // rotating instance stores instance.light components in short so conversion is needed
    flw_vertexLight[0] = intBitsToFloat(instance.light[0]);
    flw_vertexLight[1] = intBitsToFloat(instance.light[1]);
    // END colorful lighting

    flw_vertexOverlay = instance.overlay;
}
