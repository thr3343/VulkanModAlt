package net.vulkanmod.render.chunk.build;

import net.vulkanmod.render.chunk.util.Util;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import net.vulkanmod.render.vertex.TerrainRenderType;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class UploadBuffer {

    public final int indexCount;
    public final boolean autoIndices;
    public final boolean indexOnly;
    private final ByteBuffer vertexBuffer;
    private final ByteBuffer indexBuffer;
    public final int index;
    private final TerrainRenderType renderType2;

    //debug
    private boolean released = false;

    public UploadBuffer(int index, TerrainRenderType renderType2, TerrainBufferBuilder.RenderedBuffer renderedBuffer) {
        this.index = index;
        this.renderType2 = renderType2;
        TerrainBufferBuilder.DrawState drawState = renderedBuffer.drawState();
        this.indexCount = drawState.indexCount();
        this.autoIndices = drawState.sequentialIndex();
        this.indexOnly = drawState.indexOnly();

        this.vertexBuffer = !this.indexOnly ? Util.createCopy(renderedBuffer.vertexBuffer()) : null;

        this.indexBuffer = !drawState.sequentialIndex() ? Util.createCopy(renderedBuffer.indexBuffer()) : null;
    }

    public int indexCount() { return indexCount; }

    public ByteBuffer getVertexBuffer() { return vertexBuffer; }

    public ByteBuffer getIndexBuffer() { return indexBuffer; }

    public void release() {
        if(vertexBuffer != null)
            MemoryUtil.memFree(vertexBuffer);
        if(indexBuffer != null)
            MemoryUtil.memFree(indexBuffer);
        this.released = true;
    }
}
