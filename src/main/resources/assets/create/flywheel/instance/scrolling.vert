#include "flywheel:util/quaternion.glsl"
#include "flywheel:util/matrix.glsl"
// START colorful lighting
#include "colorful_lighting:colored_light.glsl"
// END colorful lighting

void flw_instanceVertex(in FlwInstance instance) {
    flw_vertexPos = vec4(rotateByQuaternion(flw_vertexPos.xyz - .5, instance.rotation) + instance.pos + .5, 1.);

    flw_vertexNormal = rotateByQuaternion(flw_vertexNormal, instance.rotation);

    vec2 scroll = fract(instance.speed * flw_renderTicks + instance.offset) * instance.scale;

    flw_vertexTexCoord = flw_vertexTexCoord + instance.diff + scroll;
    flw_vertexOverlay = instance.overlay;
    flw_vertexLight = max(vec2(instance.light) / 256., flw_vertexLight);

    // START colorful lighting
    v_lightColor.data = vertexLightColor(instance.light, flw_vertexPos.xyz + flw_renderOrigin.xyz);
    // END colorful lighting
}
