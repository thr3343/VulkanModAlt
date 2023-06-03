#ifndef COMMON_H
#define COMMON_H
#define MAX_OFFSET_COUNT 512

vec4 minecraft_sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texelFetch(lightMap, (uv & 255) >> 4, 0);
};
#endif