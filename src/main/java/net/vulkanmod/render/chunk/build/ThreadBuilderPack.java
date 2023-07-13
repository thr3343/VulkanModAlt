package net.vulkanmod.render.chunk.build;

import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import net.vulkanmod.render.vertex.TerrainRenderType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ThreadBuilderPack {
    private final Map<TerrainRenderType, TerrainBufferBuilder> builders = new EnumMap<>(TerrainRenderType.class);

    {

        for (TerrainRenderType renderType : TerrainRenderType.values()) {
            if (builders.put(renderType, new TerrainBufferBuilder(renderType.maxSize)) != null) {
                throw new IllegalStateException("Duplicate key");
            }
        }

    }

    public TerrainBufferBuilder builder(TerrainRenderType renderType) {
        return this.builders.get(renderType);
    }

    public void clearAll() {
        this.builders.values().forEach(TerrainBufferBuilder::clear);
    }

    public void discardAll() {
        this.builders.values().forEach(TerrainBufferBuilder::discard);
    }

}
