#include "flywheel:internal/material.glsl"
#include "flywheel:internal/api_impl.glsl"
#include "flywheel:internal/uniforms/uniforms.glsl"

out vec4 flw_vertexPos;
out vec4 flw_vertexColor;
out vec2 flw_vertexTexCoord;
flat out ivec2 flw_vertexOverlay;
flat out vec2 flw_vertexLight;
out vec3 flw_vertexNormal;

out float flw_distance;

// START colorful lighting
#include "colorful_lighting:colored_light_types.glsl"

out VertexLightData {
    ColoredLightFloatData data;
} v_lightColor;
// END colorful lighting

FlwMaterial flw_material;

#define flw_vertexId gl_VertexID
