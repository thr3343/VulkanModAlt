#version 460


layout(location = 0) out vec4 outColor;

layout(binding = 0) uniform sampler2D inputColor;
	const vec2 InSize = vec2(1920, 1080);
	const vec2 oneTexel = 1/InSize;
    vec2 texCoord = gl_FragCoord.xy/InSize;
	const float Radius = 3;
void main() {
    vec4 c  = texture(inputColor, texCoord);
    vec4 maxVal = c;
    for(float u = 0; u <= Radius; u += 1) {
        for(float v = 0; v <= Radius; v += 1) {
            float weight = (((sqrt(u * u + v * v) / (Radius)) > 1) ? 0 : 1);

            vec4 s0 = texture(inputColor, texCoord + vec2(-u * oneTexel.x, -v * oneTexel.y));
            vec4 s1 = texture(inputColor, texCoord + vec2( u * oneTexel.x,  v * oneTexel.y));
            vec4 s2 = texture(inputColor, texCoord + vec2(-u * oneTexel.x,  v * oneTexel.y));
            vec4 s3 = texture(inputColor, texCoord + vec2( u * oneTexel.x, -v * oneTexel.y));

            vec4 o0 = max(s0, s1);
            vec4 o1 = max(s2, s3);
            vec4 tempMax = max(o0, o1);
            maxVal = mix(maxVal, max(maxVal, tempMax), weight);
        }
    }
	
    outColor = maxVal;

}