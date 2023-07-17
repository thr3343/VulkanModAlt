package net.vulkanmod.render.chunk;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.vulkanmod.render.VkBufferPointer;
import net.vulkanmod.vulkan.memory.*;
import net.vulkanmod.vulkan.util.VBOUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;

public class AreaBuffer {
    private final MemoryType memoryType;
    private final int index;
    private final int usage;

//    private final LinkedList<Segment> freeSegments = new LinkedList<>();
    final Int2ReferenceOpenHashMap<VkBufferPointer> usedSegments = new Int2ReferenceOpenHashMap<>();

    private final int elementSize;

//    private VkBufferPointer buffer;

    int maxSize;
    int used;

    public AreaBuffer(int index, int usage, int maxSize, int elementSize) {
        this.index = index;

        this.usage = usage;
        this.elementSize = elementSize;
        this.memoryType = MemoryTypes.GPU_MEM;

//        this.buffer = VBOUtil.virtualBufferVtx.addSubIncr(index, size);
        this.maxSize = maxSize;

//        freeSegments.add(new Segment(this.buffer.i2(), this.buffer.size_t()));
    }

    private Buffer allocateBuffer(int size) {

        return this.usage == VK_BUFFER_USAGE_VERTEX_BUFFER_BIT ? new VertexBuffer(size, memoryType) : new IndexBuffer(size, memoryType);
    }

    public synchronized void upload(ByteBuffer byteBuffer, DrawBuffers.DrawParameters uploadSegment) {
        //free old segment

        int size = byteBuffer.remaining();

        if(size % elementSize != 0)
            throw new RuntimeException("unaligned byteBuffer");
//        if(this.maxSize<=this.maxSize+size)
//        {
//            this
//        }
        var section = VBOUtil.virtualBufferVtx.addSubIncr(uploadSegment.index, size);


        final Segment v = new Segment(section.i2(), section.size_t());
        usedSegments.put(section.i2(), section);

//        Buffer dst = this.buffer;
        AreaUploadManager.INSTANCE.uploadAsync(v, VBOUtil.virtualBufferVtx.bufferPointerSuperSet, section.i2(), section.size_t(), byteBuffer);

        uploadSegment.vertexBufferSegment.offset = section.i2();
        uploadSegment.vertexBufferSegment.size = section.size_t();
        uploadSegment.vertexBufferSegment.status = false;

        this.used += size;

    }

    public synchronized void setSegmentFree(int offset) {
        if(usedSegments.isEmpty()) return;
        VkBufferPointer segment = usedSegments.remove(offset);
        VBOUtil.virtualBufferVtx.addFreeableRange(segment);

        //        this.freeSegments.add(segment);
        this.used -= segment!=null ? segment.size_t() : 0;
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
//        this.globalBuffer.freeSubAllocation(subAllocation);
    }

    public static class Segment {

        int offset, size;
        boolean status = false;

        public Segment(int offset1) {
            reset(offset1);
        }

        private Segment(int offset, int size) {
            this.offset = offset;
            this.size = size;
            this.status = false;
        }

        public void reset(int offset1) {
            this.offset = offset1;
            this.size = -1;
            this.status = false;
        }

        public int getOffset() {
            return offset;
        }

        public int getSize() {
            return size;
        }

        void setPending() {
            this.status = false;
        }

        public boolean isPending() {
            return (!this.status);
        }

        public void setReady() {
            this.status = true;
        }

        public boolean isReady() {
            return (this.status);
        }

    }

//    //Debug
//    public List<Segment> findConflicts(int offset) {
//        List<Segment> segments = new ArrayList<>();
//        Segment segment = this.usedSegments.get(offset);
//
//        for(Segment s : this.usedSegments.values()) {
//            if((s.offset >= segment.offset && s.offset < (segment.offset + segment.size))
//              || (segment.offset >= s.offset && segment.offset < (s.offset + s.size))) {
//                segments.add(s);
//            }
//        }
//
//        return segments;
//    }

    public static boolean checkRanges(Segment s1, Segment s2) {
        return (s1.offset >= s2.offset && s1.offset < (s2.offset + s2.size)) || (s2.offset >= s1.offset && s2.offset < (s1.offset + s1.size));
    }

    public VkBufferPointer getSegment(int offset) {
        return this.usedSegments.get(offset);
    }
}
