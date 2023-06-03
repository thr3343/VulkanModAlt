#ifndef COMMON_H
#define COMMON_H
//light.glsl
#define MINECRAFT_LIGHT_POWER   0.6
#define MINECRAFT_AMBIENT_LIGHT 0.4

vec4 minecraft_sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texelFetch(lightMap, (uv & 255) >> 4, 0);
}
#endif