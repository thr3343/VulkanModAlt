package net.vulkanmod.render.vertex;

import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.vulkan.VRenderSystem;

import java.util.*;
import java.util.stream.Collectors;

public enum TerrainRenderType {
    SOLID(RenderType.solid(), 0.0f),
    CUTOUT_MIPPED(RenderType.cutoutMipped(), 0.5f),
    CUTOUT(RenderType.cutout(), 0.1f),
    TRANSLUCENT(RenderType.translucent(), 0.0f),
    TRIPWIRE(RenderType.tripwire(), 0.1f);

    private static final Map<RenderType, TerrainRenderType> RENDER_TYPE_MAP = new Hashtable<>(
            Arrays.stream(TerrainRenderType.values()).collect(Collectors.toMap(
                    (terrainRenderType) -> terrainRenderType.renderType, (terrainRenderType) -> terrainRenderType)));

    public static final EnumSet<TerrainRenderType> COMPACT_RENDER_TYPES = EnumSet.of(CUTOUT, CUTOUT_MIPPED, TRANSLUCENT);
    public static final EnumSet<TerrainRenderType> SEMI_COMPACT_RENDER_TYPES = EnumSet.of(CUTOUT_MIPPED, TRANSLUCENT);



    public final RenderType renderType;
    final float alphaCutout;

    TerrainRenderType(RenderType renderType, float alphaCutout) {
        this.renderType = renderType;
        this.alphaCutout = alphaCutout;
    }

    public void setCutoutUniform() {
        VRenderSystem.alphaCutout = this.alphaCutout;
    }

    public static TerrainRenderType get(RenderType renderType) {
        return RENDER_TYPE_MAP.get(renderType);
    }
}
