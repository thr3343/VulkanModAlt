#version 460


layout(location = 0) out vec4 outColor;

layout(binding = 0) uniform sampler2D inputColor;
	const vec2 InSize = vec2(1920, 1080);
	const vec2 oneTexel = 1/InSize;
    vec2 texCoord = gl_FragCoord.xy/InSize;
void main() {

	
	    vec4 u  = texture(inputColor, texCoord + vec2(        0.0, -oneTexel.y));
    vec4 d  = texture(inputColor, texCoord + vec2(        0.0,  oneTexel.y));
    vec4 l  = texture(inputColor, texCoord + vec2(-oneTexel.x,         0.0));
    vec4 r  = texture(inputColor, texCoord + vec2( oneTexel.x,         0.0));

    vec4 v1 = min(l, r);
    vec4 v2 = min(u, d);
    vec4 v3 = min(v1, v2);

    vec4 ul = texture(inputColor, texCoord + vec2(-oneTexel.x, -oneTexel.y));
    vec4 dr = texture(inputColor, texCoord + vec2( oneTexel.x,  oneTexel.y));
    vec4 dl = texture(inputColor, texCoord + vec2(-oneTexel.x,  oneTexel.y));
    vec4 ur = texture(inputColor, texCoord + vec2( oneTexel.x, -oneTexel.y));

    vec4 v4 = min(ul, dr);
    vec4 v5 = min(ur, dl);
    vec4 v6 = min(v4, v5);

    vec4 v7 = min(v3, v6);

    vec4 uu = texture(inputColor, texCoord + vec2(              0.0, -oneTexel.y * 2.0));
    vec4 dd = texture(inputColor, texCoord + vec2(              0.0,  oneTexel.y * 2.0));
    vec4 ll = texture(inputColor, texCoord + vec2(-oneTexel.x * 2.0,               0.0));
    vec4 rr = texture(inputColor, texCoord + vec2( oneTexel.x * 2.0,               0.0));

    vec4 v8 = min(uu, dd);
    vec4 v9 = min(ll, rr);
    vec4 v10 = min(v8, v9);

    vec4 v11 = min(v7, v10);

    vec4 c  = texelFetch(inputColor, ivec2(gl_FragCoord.xy), 0);
    vec4 color = min(c, v11);
    outColor = color;
}