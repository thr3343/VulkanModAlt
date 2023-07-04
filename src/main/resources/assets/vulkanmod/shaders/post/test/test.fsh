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
	vec4 center = c;
    vec4 up     = texture(inputColor, texCoord + vec2(        0.0, -oneTexel.y));
    vec4 up2    = texture(inputColor, texCoord + vec2(        0.0, -oneTexel.y) * 2.0);
    vec4 down   = texture(inputColor, texCoord + vec2( oneTexel.x,         0.0));
    vec4 down2  = texture(inputColor, texCoord + vec2( oneTexel.x,         0.0) * 2.0);
    vec4 left   = texture(inputColor, texCoord + vec2(-oneTexel.x,         0.0));
    vec4 left2  = texture(inputColor, texCoord + vec2(-oneTexel.x,         0.0) * 2.0);
    vec4 right  = texture(inputColor, texCoord + vec2(        0.0,  oneTexel.y));
    vec4 right2 = texture(inputColor, texCoord + vec2(        0.0,  oneTexel.y) * 2.0);
    vec4 ul     = texture(inputColor, texCoord + vec2(-oneTexel.x, -oneTexel.y));
    vec4 ur     = texture(inputColor, texCoord + vec2( oneTexel.x, -oneTexel.y));
    vec4 bl     = texture(inputColor, texCoord + vec2(-oneTexel.x,  oneTexel.y));
    vec4 br     = texture(inputColor, texCoord + vec2( oneTexel.x,  oneTexel.y));
    vec4 gray = vec4(0.3, 0.59, 0.11, 0.0);
    float uDiff = dot(abs(center - up), gray);
    float dDiff = dot(abs(center - down), gray);
    float lDiff = dot(abs(center - left), gray);
    float rDiff = dot(abs(center - right), gray);
    float u2Diff = dot(abs(center - up2), gray);
    float d2Diff = dot(abs(center - down2), gray);
    float l2Diff = dot(abs(center - left2), gray);
    float r2Diff = dot(abs(center - right2), gray);
    float ulDiff = dot(abs(center - ul), gray);
    float urDiff = dot(abs(center - ur), gray);
    float blDiff = dot(abs(center - bl), gray);
    float brDiff = dot(abs(center - br), gray);
    float sum = uDiff + dDiff + lDiff + rDiff + u2Diff + d2Diff + l2Diff + r2Diff + ulDiff + urDiff + blDiff + brDiff;
    float sumLuma = clamp(sum, 0.0, 1.0);

    outColor = maxVal+vec4(sumLuma);

}