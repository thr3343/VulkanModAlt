#version 460


layout(location = 0) out vec4 outColor;

layout(binding = 0) uniform sampler2D prevColor;
layout(binding = 1) uniform sampler2D inputColor;
	const vec2 InSize = vec2(1920, 1080);
	const vec2 oneTexel = 1/InSize;
    vec2 texCoord = gl_FragCoord.xy/InSize;
	const float Radius = 3;
void main() {
    vec4 CurrTexel = texture(inputColor, texCoord);
    vec4 PrevTexel = texture(prevColor, texCoord);

    outColor = vec4(max(PrevTexel.rgb * .95, CurrTexel.rgb), 1.0);

}