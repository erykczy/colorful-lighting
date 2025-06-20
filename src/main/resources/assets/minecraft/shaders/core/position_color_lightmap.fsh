#version 150

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <colorful_lighting:light.glsl>

uniform sampler2D Sampler2;

in vec4 vertexColor;
in vec2 texCoord2;

out vec4 fragColor;

void main() {
    vec4 color = sample_lightmap(Sampler2, texCoord2) * vertexColor;
    fragColor = color * ColorModulator;
}
