package net.vulkanmod.render.chunk;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.vulkanmod.render.VkBufferPointer;
import net.vulkanmod.vulkan.memory.*;
import net.vulkanmod.vulkan.util.VBOUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;

public class AreaBuffer {
    private final int index;

    //    private final LinkedList<Segment> freeSegments = new LinkedList<>();
//    final Int2ReferenceOpenHashMap<VkBufferPointer> freeSegments = new Int2ReferenceOpenHashMap<>();
    final Int2ReferenceOpenHashMap<VkBufferPointer> usedSegments = new Int2ReferenceOpenHashMap<>();

    private final int elementSize;

//    private VkBufferPointer buffer;

    int maxSize;
    int used;

    public AreaBuffer(int index, int usage, int maxSize, int elementSize) {
        this.index = index;

        this.elementSize = elementSize;

//        this.buffer = VBOUtil.virtualBufferVtx.addSubIncr(index, size);
        this.maxSize = maxSize;

//        freeSegments.add(new Segment(this.buffer.i2(), this.buffer.size_t()));
    }

    public void upload(ByteBuffer byteBuffer, DrawBuffers.DrawParameters uploadSegment) {
        //free old segment

        int size = byteBuffer.remaining();

        if(size % elementSize != 0)
            throw new RuntimeException("unaligned byteBuffer");
//        if(this.maxSize<=this.maxSize+size)
//        {
//            this
//        }
//        var section = checkForFree(uploadSegment.firstIndex);
//        if(section==null)
        var section = VBOUtil.virtualBufferVtx.getActiveRangeFromIdx(this.index, uploadSegment.firstIndex);
        if(section==null)
        {
            section =  VBOUtil.virtualBufferVtx.addSubIncr(this.index, uploadSegment.index, size);

        }
        usedSegments.put(section.i2(), section);

//        final Segment v = new Segment(section.i2(), section.size_t());

//        Buffer dst = this.buffer;
        AreaUploadManager.INSTANCE.uploadAsync(section, VBOUtil.virtualBufferVtx.bufferPointerSuperSet, VBOUtil.virtualBufferVtx.size_t, section.i2(), section.size_t(), byteBuffer);

        uploadSegment.vertexBufferSegment=section;

        this.used += size;

    }

//    private VkBufferPointer checkForFree(int firstIndex) {
//        return freeSegments.remove(firstIndex);
//    }

    public void setSegmentFree(int offset) {
        if(usedSegments.isEmpty()) return;
        VkBufferPointer segment = usedSegments.remove(offset);
        VBOUtil.virtualBufferVtx.addFreeableRange(segment);
        if(segment!=null)
        {
//            this.freeSegments.put(segment.i2(), segment);
            this.used -= segment.size_t();
        }
    }

    public long getId() {
        return VBOUtil.virtualBufferVtx.bufferPointerSuperSet;
    }

    public void freeBuffer() {
        for(var a : usedSegments.values())
        {
            VBOUtil.virtualBufferVtx.addFreeableRange(a);
        }
        usedSegments.clear();
//        freeSegments.clear();
//        this.globalBuffer.freeSubAllocation(subAllocation);
    }

}
