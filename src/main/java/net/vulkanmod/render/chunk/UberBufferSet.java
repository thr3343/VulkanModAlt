package net.vulkanmod.render.chunk;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.render.VirtualBuffer;
import net.vulkanmod.render.chunk.build.UploadBuffer;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.render.virtualSegmentBuffer;
import net.vulkanmod.vulkan.memory.IndirectBuffer;
import net.vulkanmod.vulkan.util.VUtil;

import static net.vulkanmod.render.vertex.TerrainRenderType.CUTOUT_MIPPED;
import static net.vulkanmod.render.vertex.TerrainRenderType.TRANSLUCENT;
import static net.vulkanmod.vulkan.util.VBOUtil.*;
import static org.lwjgl.system.JNI.callPPPV;
import static org.lwjgl.system.JNI.callPV;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;

public class UberBufferSet {

    public final ObjectArrayList<VkDrawIndexedIndirectCommand2> sectionQueue = new ObjectArrayList<>(1024);
    public final ObjectArrayList<VkDrawIndexedIndirectCommand2> TsectionQueue = new ObjectArrayList<>(1024);
    //TODO: Fix MipMaps Later...
//    public static final ObjectArrayList<VBO> cutoutChunks = new ObjectArrayList<>(1024);
////    public static final ObjectArrayList<VBO> cutoutMippedChunks = new ObjectArrayList<>(1024);
//    public static final ObjectArrayList<VBO> translucentChunks = new ObjectArrayList<>(1024);
//    public static final VirtualBuffer virtualBufferIdx=new VirtualBuffer(16777216, VK_BUFFER_USAGE_INDEX_BUFFER_BIT);
    public final VirtualBuffer virtualBufferVtx;
    public final VirtualBuffer TvirtualBufferVtx;
    public final long Tsize_t;
    public final long Ssize_t;

    public long TusedBytes;
    public long SusedBytes;

    VirtualBuffer virtualBuffer;
    IndirectBuffer indirectBuffer;

    private final IntArrayList areaIndices = new IntArrayList(8);

    public UberBufferSet() {
        virtualBufferVtx = new VirtualBuffer(33554432, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, CUTOUT_MIPPED);
        TvirtualBufferVtx = new VirtualBuffer(8388608, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, TerrainRenderType.TRANSLUCENT);
        this.Tsize_t=TvirtualBufferVtx.size_t;
        this.Ssize_t=virtualBufferVtx.size_t;
    }

    VkDrawIndexedIndirectCommand2 configureVertexFormat(DrawBuffers.DrawParameters drawParameters, int areaIndex, int index, UploadBuffer parameters, TerrainRenderType r) {


        if(!areaIndices.contains(areaIndex))
        {
            areaIndices.add(areaIndex);
        }

        final boolean b = r == TRANSLUCENT;
        VirtualBuffer virtualBufferVtx1 = b ? this.TvirtualBufferVtx : this.virtualBufferVtx;
//        boolean bl = !parameters.format().equals(this.vertexFormat);
        final int size = parameters.vertSize;
        if (b) TusedBytes+= size; else SusedBytes += size;
        if(!virtualBufferVtx1.canAllocate(size))
        {
            throw new RuntimeException();
        }

        virtualBufferVtx1.addFreeableRange(drawParameters.vertexBufferSegment);

        drawParameters.vertexBufferSegment = virtualBufferVtx1.allocSubSection(areaIndex, index, size, r);

        AreaUploadManager.INSTANCE.uploadSync(virtualBufferVtx1.bufferPointerSuperSet, virtualBufferVtx1.size_t, drawParameters.vertexBufferSegment.i2(), size, parameters.getVertexBuffer());

//            this.vertOff= fakeVertexBuffer.i2()>>5;
        return new VkDrawIndexedIndirectCommand2(parameters.indexCount, 1, 0, drawParameters.vertexBufferSegment.i2(), 0);
    }

    public void freeBuff(virtualSegmentBuffer vertexBufferSegment) {
        (vertexBufferSegment.r()==TRANSLUCENT ? TvirtualBufferVtx : virtualBufferVtx).addFreeableRange(vertexBufferSegment);
    }

    void add(TerrainRenderType rType, VkDrawIndexedIndirectCommand2 buffer) {
        (rType !=TRANSLUCENT ? sectionQueue : TsectionQueue).add(buffer);
    }

    public boolean hasAreaIndex(int areaIndex)
    {
        return this.areaIndices.contains(areaIndex);
    }
    void drawBatchedIndexed(boolean b, long address) {
        if(!b) drawSolid(address);
        else drawTranslucent(address);
    }

    private  void drawTranslucent(long address) {
        for (int i = TsectionQueue.size() - 1; i >= 0; i--) {
            final VkDrawIndexedIndirectCommand2 drawParameters = TsectionQueue.get(i);
            VUtil.UNSAFE.putLong(vOffset, drawParameters.vertexOffset());
            callPPPV(address, 0, 1, TvirtualBufferVtx.uPtr, vOffset, functionAddress);

            callPV(address, drawParameters.indexCount(), 1, 0, 0, 0, functionAddress1);
        }
    }

    private  void drawSolid(long address) {
        for (int i = 0; i < sectionQueue.size(); i++) {
            VkDrawIndexedIndirectCommand2 drawParameters = sectionQueue.get(i);
            VUtil.UNSAFE.putLong(vOffset, drawParameters.vertexOffset());
            callPPPV(address, 0, 1, virtualBufferVtx.uPtr, vOffset, functionAddress);

            callPV(address, drawParameters.indexCount(), 1, 0, 0, 0, functionAddress1);
        }
    }

     void drawBatchedIndexedBindless(boolean b, long address) {
        if(b) {
            for (int i = TsectionQueue.size() - 1; i >= 0; i--) {
                VkDrawIndexedIndirectCommand2 drawParameters = TsectionQueue.get(i);
                callPV(address, drawParameters.indexCount(), 1, 0, drawParameters.vertexOffset() / DrawBuffers.VERTEX_SIZE, 0, functionAddress1);
            }
        }
        else {
            for (int i = 0; i < sectionQueue.size(); i++) {
                VkDrawIndexedIndirectCommand2 drawParameters = sectionQueue.get(i);
                callPV(address, drawParameters.indexCount(), 1, 0, drawParameters.vertexOffset() / DrawBuffers.VERTEX_SIZE, 0, functionAddress1);
            }
        }
    }
    public void cleanUp() {
        this.virtualBufferVtx.cleanUp();
        this.TvirtualBufferVtx.cleanUp();
    }

    public void clear() {
        sectionQueue.clear();
        TsectionQueue.clear();
    }

    public void freeRange(int areaIndex) {
        if(this.hasAreaIndex(areaIndex))
        {
            this.virtualBufferVtx.freeRange(areaIndex);
            this.TvirtualBufferVtx.freeRange(areaIndex);
            this.TusedBytes=TvirtualBufferVtx.usedBytes;
            this.SusedBytes=virtualBufferVtx.usedBytes;
        }
        else System.out.println("MissedRange!");
    }

    public boolean hasAvailable(TerrainRenderType r, int size) {
        return (r==TRANSLUCENT ? this.TvirtualBufferVtx : this.virtualBufferVtx).canAllocate(size);
    }
}
