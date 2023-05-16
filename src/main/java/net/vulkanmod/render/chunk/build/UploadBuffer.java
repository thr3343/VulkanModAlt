package net.vulkanmod.render.chunk.build;

import net.vulkanmod.render.chunk.util.Util;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static net.vulkanmod.render.chunk.VBOUtil.*;
import static net.vulkanmod.render.chunk.VBOUtil.originZ;

public class UploadBuffer {

    public final int indexCount;
    public final boolean autoIndices;
    public final boolean indexOnly;
    private final ByteBuffer vertexBuffer;
    private final ByteBuffer indexBuffer;

    //debug

    public UploadBuffer(TerrainBufferBuilder.RenderedBuffer renderedBuffer, int x, int y, int z) {
        TerrainBufferBuilder.DrawState drawState = renderedBuffer.drawState();
        this.indexCount = drawState.indexCount();
        this.autoIndices = drawState.sequentialIndex();
        this.indexOnly = drawState.indexOnly();

        this.vertexBuffer = !this.indexOnly ? Util.createCopy(renderedBuffer.vertexBuffer()) : null;
        this.indexBuffer = !drawState.sequentialIndex() ? Util.createCopy(renderedBuffer.indexBuffer()) : null;
        if(!this.indexOnly) translateVBO(x, y, z);
    }
    private void translateVBO(int x, int y, int z) {
        final long addr = MemoryUtil.memAddress0(vertexBuffer);
        //Use constant Attribute size to encourage loop unrolling
        final int camX1 = (int)Math.floor(x-camX-originX);
        final int camZ1 = (int)Math.floor(z-camZ-originZ);
        for(int i = 0; i< vertexBuffer.remaining(); i+= 24)
        {
            VUtil.UNSAFE.putFloat(addr+i,   (VUtil.UNSAFE.getFloat(addr+i)  +(camX1)));
            VUtil.UNSAFE.putFloat(addr+i+4, (VUtil.UNSAFE.getFloat(addr+i+4)+y));
            VUtil.UNSAFE.putFloat(addr+i+8, (VUtil.UNSAFE.getFloat(addr+i+8)+(camZ1)));
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
    }
}
