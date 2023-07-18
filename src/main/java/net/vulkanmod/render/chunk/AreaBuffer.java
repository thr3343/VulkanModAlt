package net.vulkanmod.render.chunk;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.render.VirtualBuffer;
import net.vulkanmod.render.VkBufferPointer;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.memory.*;

import java.nio.ByteBuffer;

import static net.vulkanmod.vulkan.util.VBOUtil.virtualBufferVtx;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;

public class AreaBuffer {
    private final MemoryType memoryType;
    private final int index;
    private final int usage;

//    private final LinkedList<Segment> freeSegments = new LinkedList<>();
    final Int2ObjectArrayMap<VkBufferPointer> usedSegments = new Int2ObjectArrayMap<>();

    private final int elementSize;

//    private VkBufferPointer buffer;
        //TODO: DefragThreshold
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

    public synchronized void upload(ByteBuffer byteBuffer, DrawBuffers.DrawParameters uploadSegment, VirtualBuffer virtualBuffer) {
        //free old segment

        int size = byteBuffer.remaining();

        if(size % elementSize != 0)
            throw new RuntimeException("unaligned byteBuffer");
//        if(this.maxSize<=this.maxSize+size)
//        {
//            this
//        }
        var section = virtualBuffer.addSubIncr(uploadSegment.index, size);


//        final Segment v = new Segment(section.i2(), section.size_t());
        usedSegments.put(section.index(), section);

//        Buffer dst = this.buffer;
        final boolean b = uploadSegment.r == TerrainRenderType.TRANSLUCENT;
        AreaUploadManager.INSTANCE.uploadAsync(section, virtualBuffer.bufferPointerSuperSet, section.i2(), section.size_t(), byteBuffer);

        uploadSegment.vertexOffset = section.i2();
        uploadSegment.size = section.size_t();
        uploadSegment.firstIndex= b ? section.i2() : 0;
        uploadSegment.ready = true;
        uploadSegment.i2 = section.i2();
        uploadSegment.vtx=section;

        this.used += size;

    }

    public synchronized void setSegmentFree(int offset, VirtualBuffer virtualBufferVtx1) {
        if(usedSegments.isEmpty()) return;
        VkBufferPointer segment = usedSegments.remove(offset);
        virtualBufferVtx1.addFreeableRange(segment);

        //        this.freeSegments.add(segment);
        this.used -= segment.size_t();
    }

    public long getId() {
        return virtualBufferVtx.bufferPointerSuperSet;
    }

    public void freeBuffer(VirtualBuffer virtualBuffer) {
        for(var a : usedSegments.values())
        {
            virtualBuffer.addFreeableRange(a);
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
