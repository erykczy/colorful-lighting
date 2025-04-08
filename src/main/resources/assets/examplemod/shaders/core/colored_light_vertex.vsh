#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;
in vec3 BlockLightColor;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ChunkOffset;
uniform int FogShape;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    vec3 pos = Position + ChunkOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    vertexDistance = fog_distance(pos, FogShape);
    texCoord0 = UV0;

    float blockLight = UV2.x/256.0;
    vec3 skyLightColor = minecraft_sample_lightmap(Sampler2, ivec2(0, UV2.y)).xyz;
    //if(BlockLightColor.g < 0.1)
    vec3 blockLightColor = pow(BlockLightColor, vec3(2));//vec3(0, BlockLightColor.g < 0.3 ? BlockLightColor.g / 2.0 : BlockLightColor.g, 0);// * max(BlockLightColor.r, max(BlockLightColor.g, BlockLightColor.b));
    vec4 lightColor = vec4(skyLightColor + blockLightColor, 1.0);
    vertexColor = lightColor * Color;
}
