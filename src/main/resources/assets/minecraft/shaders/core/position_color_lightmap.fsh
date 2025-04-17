#version 150

#moj_import <minecraft:light.glsl>

uniform sampler2D Sampler2;

uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord2;

out vec4 fragColor;

void main() {
    vec4 color = minecraft_sample_lightmap(Sampler2, texCoord2) * vertexColor;
    fragColor = color * ColorModulator;
}
