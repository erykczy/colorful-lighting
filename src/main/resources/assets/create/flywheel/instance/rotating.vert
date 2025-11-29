#include "flywheel:util/quaternion.glsl"
#include "colorful_lighting:colored_light.glsl"

void flw_instanceVertex(in FlwInstance instance) {
    float degrees = instance.offset + flw_renderSeconds * instance.speed;

    vec4 kineticRot = quaternionDegrees(instance.axis, degrees);
    vec3 rotated = rotateByQuaternion(flw_vertexPos.xyz - .5, instance.rotation);

    flw_vertexPos.xyz = rotateByQuaternion(rotated, kineticRot) + instance.pos + .5;
    flw_vertexNormal = rotateByQuaternion(rotateByQuaternion(flw_vertexNormal, instance.rotation), kineticRot);
    flw_vertexColor *= instance.color;
    flw_vertexLight = max(vec2(instance.light) / 256., flw_vertexLight);
    flw_vertexOverlay = instance.overlay;

    // START colorful lighting
    // rotating instance stores instance.light components in short so conversion is needed
    v_lightColor.data = vertexLightColor(instance.light, flw_vertexPos.xyz + flw_renderOrigin.xyz);
    // END colorful lighting
}
