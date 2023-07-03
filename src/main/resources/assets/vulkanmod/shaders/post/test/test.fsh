#version 460


layout(location = 0) out vec4 outColor;

layout(set =0, binding = 0) uniform image2D inputColor;

void main() {
    outColor = imageLoad(inputColor).bgra;
}