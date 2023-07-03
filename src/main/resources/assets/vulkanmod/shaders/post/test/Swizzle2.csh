#version 460
#pragma shader_stage(compute)

layout (local_size_x = 32, local_size_y = 32) in;


layout(binding = 0, rgba8) uniform restrict image2D Src;


void main()
{
	const uint xx = (gl_GlobalInvocationID.x);
	const uint yy = (gl_GlobalInvocationID.y);
	vec4 diffuseColor = imageLoad(Src, ivec2(xx, yy));
	vec4 invertColor = 1.0-diffuseColor;
	vec4 outColor = mix(diffuseColor, invertColor, 0.5);
	
	imageStore(Src, ivec2(xx, yy), invertColor);
}
