#version 460

layout (early_fragment_tests) in;

vec4 linear_fog(vec4 inColor, float vertexDistance, float fogStart, float fogEnd, vec4 fogColor) {
    if (vertexDistance <= fogStart) {
        return inColor;
    }

    float fogValue = vertexDistance < fogEnd ? smoothstep(fogStart, fogEnd, vertexDistance) : 1.0;
    return vec4(mix(inColor.rgb, fogColor.rgb, fogValue * fogColor.a), inColor.a);
}

layout(binding = 2) uniform sampler2D Sampler0;




layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 texCoord0;
//layout(location = 3) in vec4 normal;

layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = texture(Sampler0, texCoord0) * vertexColor;
}
