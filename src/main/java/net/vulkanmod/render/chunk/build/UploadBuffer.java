package net.vulkanmod.render.chunk.build;

import net.vulkanmod.render.chunk.DrawBuffers;
import net.vulkanmod.render.chunk.util.Util;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class UploadBuffer {

    public final int indexCount;
    public final boolean autoIndices;
    public final boolean indexOnly;
    private final ByteBuffer vertexBuffer;
    private final ByteBuffer indexBuffer;
    private final int xOffset;
    private final int yOffset;
    private final int zOffset;

    //debug
    private boolean released = false;

    public UploadBuffer(int xOffset, int yOffset, int zOffset, TerrainBufferBuilder.RenderedBuffer renderedBuffer) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
        TerrainBufferBuilder.DrawState drawState = renderedBuffer.drawState();
        this.indexCount = drawState.indexCount();
        this.autoIndices = drawState.sequentialIndex();
        this.indexOnly = drawState.indexOnly();

        this.vertexBuffer = !this.indexOnly ? Util.createCopy(renderedBuffer.vertexBuffer()) : null;
        this.indexBuffer = !drawState.sequentialIndex() ? Util.createCopy(renderedBuffer.indexBuffer()) : null;
        if(!this.indexOnly) translateVBO(xOffset, yOffset, zOffset);
    }


    private void translateVBO(int x, int y, int z) {
        final long addr = MemoryUtil.memAddress0(vertexBuffer);
        //Broken with Signed Integers
        final int camX1 = Math.floorMod(x, 128);
        final int camZ1 = Math.floorMod(z, 128);
        for(int i = 0; i< vertexBuffer.remaining(); i+= DrawBuffers.VERTEX_SIZE)
        {
            VUtil.UNSAFE.putFloat(addr+i,   ((VUtil.UNSAFE.getFloat(addr + i)) + camX1 ));
            VUtil.UNSAFE.putFloat(addr+i+4, ((VUtil.UNSAFE.getFloat(addr + i + 4)) + y));
            VUtil.UNSAFE.putFloat(addr+i+8, ((VUtil.UNSAFE.getFloat(addr + i + 8)) + camZ1));
        }
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
