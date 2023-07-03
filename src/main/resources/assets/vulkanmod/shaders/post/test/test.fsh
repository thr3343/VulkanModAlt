#version 460


layout(location = 0) out vec4 outColor;

layout(input_attachment_index = 1, set =0, binding = 0) uniform subpassInput inputColor;

void main() {
    outColor = 1-subpassLoad(inputColor);
}