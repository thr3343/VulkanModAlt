package net.vulkanmod.render.vertex;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.vulkan.VRenderSystem;

public enum TerrainRenderType {
    SOLID(RenderType.solid(), 0.0f),
    CUTOUT_MIPPED(RenderType.cutoutMipped(), 0.5f),
    CUTOUT(RenderType.cutout(), 0.1f),
    TRANSLUCENT(RenderType.translucent(), 0.0f),
    TRIPWIRE(RenderType.tripwire(), 0.1f);

    public static final ObjectArrayList<TerrainRenderType> COMPACT_RENDER_TYPES = new ObjectArrayList<>();
    public static final ObjectArrayList<TerrainRenderType> SEMI_COMPACT_RENDER_TYPES = new ObjectArrayList<>();

    static {
        SEMI_COMPACT_RENDER_TYPES.add(CUTOUT);
        COMPACT_RENDER_TYPES.add(CUTOUT_MIPPED);
        SEMI_COMPACT_RENDER_TYPES.add(CUTOUT_MIPPED);
        COMPACT_RENDER_TYPES.add(TRANSLUCENT);
        SEMI_COMPACT_RENDER_TYPES.add(TRANSLUCENT);
    }

//    final RenderType renderType;
    final float alphaCutout;
    public final String name;
    public final int maxSize;

    TerrainRenderType(RenderType renderType, float alphaCutout) {
//        this.renderType = renderType;
        this.alphaCutout = alphaCutout;
        this.name=renderType.name;
        this.maxSize=renderType.bufferSize();
    }

    public void setCutoutUniform() {
        VRenderSystem.alphaCutout = this.alphaCutout;
    }

    public static TerrainRenderType get(String renderType) {
        return switch (renderType)
        {
            case "solid" -> SOLID;
            case "cutout_mipped" -> CUTOUT_MIPPED;
            case "cutout" -> CUTOUT;
            case "translucent" -> TRANSLUCENT;
            case "tripwire" -> TRIPWIRE;
            default -> throw new IllegalStateException("Unexpected value: " + renderType);
        };
    }
}
