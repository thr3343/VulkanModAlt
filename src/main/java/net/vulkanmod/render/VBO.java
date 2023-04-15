package net.vulkanmod.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.vulkanmod.render.chunk.util.VBOUtil;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.StagingBuffer;
import net.vulkanmod.vulkan.memory.VertexBuffer;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static net.vulkanmod.render.chunk.util.VBOUtil.*;

@Environment(EnvType.CLIENT)
public class VBO {
    final public int index;
    public boolean preInitalised=true;
    public boolean hasAbort=false;
    public VkBufferPointer fakeVerBufferPointer;
    public int indexCount;
    private int vertexCount;

    public final RenderTypes type;
    private int x;
    private int y;
    private int z;
    private int size_t;

    public VBO(int index, String name, int x, int y, int z) {
        this.index = index;
        this.type = getLayer(name);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void upload(BufferBuilder.RenderedBuffer buffer, boolean sort) {
        BufferBuilder.DrawState parameters = buffer.drawState();

        this.indexCount = parameters.indexCount();
        this.vertexCount = parameters.vertexCount();

        final ByteBuffer vertBuff = buffer.vertexBuffer();
        this.size_t = vertBuff.remaining();
//        final ByteBuffer idxBuff = buffer.indexBuffer();

        if(!sort) translateVBO(vertBuff);


        this.configureVertexFormat(parameters, vertBuff);
//        if (type==RenderTypes.TRANSLUCENT) this.configureIndexBuffer(parameters, idxBuff);

        buffer.release();
        preInitalised=false;

    }

    //WorkAround For PushConstants
    //Likely could be moved to an earlier vertex Assembly/Builder state
    private void translateVBO(ByteBuffer buffer) {
        final long addr = MemoryUtil.memAddress0(buffer);
        //Use constant Attribute size to encourage loop unrolling
        for(int i = 0; i< buffer.remaining(); i+=32)
        {
            VUtil.UNSAFE.putFloat(addr+i,   (VUtil.UNSAFE.getFloat(addr+i)  +(float)(x- camX - originX)));
            VUtil.UNSAFE.putFloat(addr+i+4, (VUtil.UNSAFE.getFloat(addr+i+4)+y));
            VUtil.UNSAFE.putFloat(addr+i+8, (VUtil.UNSAFE.getFloat(addr+i+8)+(float)(z- camZ - originZ)));
        }
    }

    private void configureVertexFormat(BufferBuilder.DrawState parameters, ByteBuffer data) {
//        boolean bl = !parameters.format().equals(this.vertexFormat);
        if (!parameters.indexOnly()) {

            if(this.fakeVerBufferPointer ==null || !VBOUtil.isAlreadyLoaded(this.index, this.type, this.size_t))
            {
                fakeVerBufferPointer =VBOUtil.addSubAlloc(vertexCount, this.size_t, this.type);
            }

            VBOUtil.upload(fakeVerBufferPointer, this.size_t, data, this.type);

        }
    }

    public void DrawChunkLayer() {
        Drawer.drawIndexedBindless(this.fakeVerBufferPointer.i2()>>5, this.indexCount);
    }

    public void close() {
        if(preInitalised) return;
        if(vertexCount <= 0) return;

        removeVBO(this);
        fakeVerBufferPointer = null;

        this.vertexCount = 0;
        this.indexCount = 0;
        preInitalised=true;
    }

    public void updateOrigin(int x, int y, int z) {
        this.x=x;
        this.y=y;
        this.z=z;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }
}
