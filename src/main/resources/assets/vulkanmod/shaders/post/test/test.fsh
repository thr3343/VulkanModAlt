#version 460

layout(origin_upper_left) in vec4 gl_FragCoord;
layout(location = 0) out vec4 fragColor;


layout(binding = 0) uniform sampler2D DiffuseSampler;

	 vec2 InSize = textureSize(DiffuseSampler, 0);
	 vec2 oneTexel = 1/InSize;
    vec2 texCoord = gl_FragCoord.xy/InSize;
	
layout(push_constant) uniform PushConstant
{
	float Radius;
};

void main() {
    vec4 maxVal = texture(DiffuseSampler, texCoord);;
    for(float u = 0; u <= Radius; u ++) {
        for(float v = 0; v <= Radius; v ++ ) {
            float weight = (((sqrt(u * u + v * v) / (Radius)) > 1) ? 0 : 1);

            vec4 s0 = texture(DiffuseSampler, texCoord + vec2(-u * oneTexel.x, -v * oneTexel.y));
            vec4 s1 = texture(DiffuseSampler, texCoord + vec2( u * oneTexel.x,  v * oneTexel.y));
            vec4 s2 = texture(DiffuseSampler, texCoord + vec2(-u * oneTexel.x,  v * oneTexel.y));
            vec4 s3 = texture(DiffuseSampler, texCoord + vec2( u * oneTexel.x, -v * oneTexel.y));

            vec4 o0 = max(s0, s1);
            vec4 o1 = max(s2, s3);
            vec4 tempMax = max(o0, o1);
            maxVal = mix(maxVal, max(maxVal, tempMax), weight);
        }
    }

    fragColor = vec4(maxVal.rgb, 1.0);
}