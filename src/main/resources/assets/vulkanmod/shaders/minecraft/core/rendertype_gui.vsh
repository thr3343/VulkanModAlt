#version 460

layout(location=0) in vec4 fragColor;
layout(location=1) in vec3 Position;

layout(binding=0) uniform UniformBufferObject
{
	mat4 ModelViewMat;
	//mat4 ProjMat;
};

layout(location=0) out vec4 vertexColor;

void main() {
    gl_Position = ModelViewMat * vec4(Position, 1.0);

    vertexColor = fragColor;
}
