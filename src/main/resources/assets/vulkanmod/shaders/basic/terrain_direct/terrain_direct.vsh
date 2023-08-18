#version 460

//light.glsl
#define MINECRAFT_LIGHT_POWER   0.6
#define MINECRAFT_AMBIENT_LIGHT 0.4

vec4 minecraft_sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texelFetch(lightMap, (uv & 255) >> 4, 0);
}

layout(binding = 0) uniform UniformBufferObject {
   mat4 MVP;
};



layout(binding = 3) uniform sampler2D Sampler2;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec2 texCoord0;
//layout(location = 3) out vec4 normal;

//Compressed Vertex

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in uvec2 UV0;
layout(location = 3) in ivec2 UV2;
//layout(location = 4) in vec3 Normal;

const float UV_INV = 1.0 / 65536.0;
const float POSITION_INV = 1.0 / 1900.0;

void main() {

	const ivec3 instXY = ivec3(gl_InstanceIndex, 0, gl_InstanceIndex) << ivec3(0, 0, 16);

	
	//gl_Position = MVP * vec4(fma(instXY, UV_INV3, ivec3(1)), 1);
	gl_Position = MVP * vec4(Position+(instXY>>16), 1);
	
    vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0*UV_INV;
//    normal = MVP * vec4(Normal, 0);
}

//Default Vertex

//layout(location = 0) in vec3 Position;
//layout(location = 1) in vec4 Color;
//layout(location = 2) in vec2 UV0;
//layout(location = 3) in ivec2 UV2;
//layout(location = 4) in vec3 Normal;

//void main() {
//    gl_Position = MVP * vec4(Position + ChunkOffset, 1.0);
//
//    vertexDistance = length((ModelViewMat * vec4(Position + ChunkOffset, 1.0)).xyz);
//    vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);
//    texCoord0 = UV0;
//    //    normal = MVP * vec4(Normal, 0.0);
//}