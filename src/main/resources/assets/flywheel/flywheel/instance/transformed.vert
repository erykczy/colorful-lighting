#include "colorful_lighting:colored_light.glsl"

// START colorful lighting
#ifdef FLW_EMBEDDED
mat4 modelMatrix;
#endif
// END colorful lighting

void flw_instanceVertex(in FlwInstance i) {
    flw_vertexPos = i.pose * flw_vertexPos;
    flw_vertexNormal = mat3(transpose(inverse(i.pose))) * flw_vertexNormal;
    flw_vertexColor *= i.color;
    flw_vertexOverlay = i.overlay;
    // Some drivers have a bug where uint over float division is invalid, so use an explicit cast.
    flw_vertexLight = max(vec2(i.light) / 256.0, flw_vertexLight);

    // START colorful lighting
    vec4 vertexPos = flw_vertexPos;
    #ifdef FLW_EMBEDDED
    vertexPos = modelMatrix * vertexPos;
    #endif
    v_lightColor.data = vertexLightColor(ivec2(i.light), vertexPos.xyz + flw_renderOrigin.xyz);
    // END colorful lighting
}
