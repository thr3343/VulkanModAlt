package net.vulkanmod.render.vertex;

import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.vulkan.VRenderSystem;

import java.util.EnumSet;
import java.util.Set;

public enum TerrainRenderType {
//    SOLID(RenderType.solid(), 0.0f),
    CUTOUT_MIPPED(RenderType.cutoutMipped(), 0.5f);
//    CUTOUT(RenderType.cutout(), 0.1f),
//    TRANSLUCENT(RenderType.translucent(), 0.0f);
//    TRIPWIRE(RenderType.tripwire(), 0.1f);

    public static final Set<TerrainRenderType> COMPACT_RENDER_TYPES = EnumSet.of(CUTOUT_MIPPED);
//    public static final Set<TerrainRenderType> SEMI_COMPACT_RENDER_TYPES = EnumSet.of(CUTOUT, CUTOUT_MIPPED, TRANSLUCENT);


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

    public static RenderType to(TerrainRenderType renderType2) {
        return switch (renderType2)
        {

//            case SOLID -> RenderType.solid();
            case CUTOUT_MIPPED -> RenderType.cutoutMipped();
//            case TRANSLUCENT -> RenderType.translucent();
//            case TRIPWIRE -> RenderType.tripwire();
        };
    }

    public void setCutoutUniform() {
        VRenderSystem.alphaCutout = this.alphaCutout;
    }

    public static TerrainRenderType get(String renderType) {
        return switch (renderType)
        {
            case "solid", "cutout", "cutout_mipped", "translucent","tripwire"  -> CUTOUT_MIPPED;
            default -> throw new IllegalStateException("Unexpected value: " + renderType);
        };
    }
}
