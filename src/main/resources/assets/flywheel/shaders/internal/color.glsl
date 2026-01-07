#include "flywheel:internal/packed_color.glsl"
#include "colorful_lighting:include/colored_light.glsl"

vec4 _flw_sampleLight(sampler2D lightMap, vec2 light) {
    return sample_lightmap_colored(lightMap, light);
}

vec4 flw_sampleColor(sampler2D colorSampler, vec2 texCoords, vec4 color, vec2 light, sampler2D lightMap) {
    return texture(colorSampler, texCoords) * color * _flw_sampleLight(lightMap, light);
}

vec4 flw_sampleColor(sampler2D colorSampler, vec2 texCoords, vec4 color, vec2 light, sampler2D lightMap, vec4 overlay) {
    vec4 texColor = texture(colorSampler, texCoords);
    return vec4(mix(overlay.rgb, texColor.rgb, overlay.a), texColor.a) * color * _flw_sampleLight(lightMap, light);
}
