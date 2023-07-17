package net.vulkanmod.render.chunk;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.vulkanmod.render.VkBufferPointer;
import net.vulkanmod.render.chunk.util.Util;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.*;
import net.vulkanmod.vulkan.queue.CommandPool;
import net.vulkanmod.vulkan.util.VBOUtil;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferCopy;

import java.nio.ByteBuffer;
import java.util.LinkedList;

import static net.vulkanmod.vulkan.queue.Queues.TransferQueue;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.vkCmdCopyBuffer;

public class AreaBuffer {
    private final MemoryType memoryType;
    private final int index;
    private final int usage;

    private final LinkedList<Segment> freeSegments = new LinkedList<>();
    private final Reference2ReferenceOpenHashMap<Segment, Segment> usedSegments = new Reference2ReferenceOpenHashMap<>();

    private final int elementSize;

    private VkBufferPointer buffer;

    int size;
    int used;

    public AreaBuffer(int index, int usage, int size, int elementSize) {
        this.index = index;

        this.usage = usage;
        this.elementSize = elementSize;
        this.memoryType = MemoryTypes.GPU_MEM;

        this.buffer = VBOUtil.virtualBufferVtx.addSubIncr(index, size);
        this.size = size;

        freeSegments.add(new Segment(this.buffer.i2(), this.buffer.size_t()));
    }

    private Buffer allocateBuffer(int size) {

        return this.usage == VK_BUFFER_USAGE_VERTEX_BUFFER_BIT ? new VertexBuffer(size, memoryType) : new IndexBuffer(size, memoryType);
    }

    public synchronized void upload(ByteBuffer byteBuffer, Segment uploadSegment) {
        //free old segment
        if(uploadSegment.offset != -1) {
            this.setSegmentFree(uploadSegment);
        }

        int size = byteBuffer.remaining();

        if(size % elementSize != 0)
            throw new RuntimeException("unaligned byteBuffer");

        Segment segment = findSegment(size);

        if(segment.size - size > 0) {
            freeSegments.add(new Segment(segment.offset + size, segment.size - size));
        }

        usedSegments.put(uploadSegment, new Segment(segment.offset, size));

//        Buffer dst = this.buffer;
        AreaUploadManager.INSTANCE.uploadAsync(uploadSegment, VBOUtil.virtualBufferVtx.bufferPointerSuperSet, segment.offset, size, byteBuffer);

        uploadSegment.offset = segment.offset;
        uploadSegment.size = size;
        uploadSegment.status = false;

        this.used += size;

    }

    public Segment findSegment(int size) {
        Segment segment = null;
        int i = 0;
        int idx = 0;
        int t = Integer.MAX_VALUE;
        for(Segment segment1 : freeSegments) {

            if(segment1.size >= size && segment1.size < t) {
                segment = segment1;
                t = segment1.size;
                idx = i;
            }
            ++i;
        }

        if(segment == null) {
            return this.reallocate(size);
        }

        freeSegments.remove(idx);

        return segment;
    }

    public Segment reallocate(int uploadSize) {
        int oldSize = this.size;
        int increment = this.size >> 1;

        if(increment <= uploadSize) {
            increment *= 2;
        }
        //TODO check size
        if(increment <= uploadSize)
            throw new RuntimeException();

        int newSize = oldSize + increment;


        VBOUtil.virtualBufferVtx.addFreeableRange(this.index, this.buffer);


        final var vkBufferPointer = VBOUtil.virtualBufferVtx.addSubIncr(this.index, newSize);
        this.buffer = vkBufferPointer;

        AreaUploadManager.INSTANCE.submitUploads();
        AreaUploadManager.INSTANCE.waitAllUploads();

//        //Sync upload
//        long dstBuffer = VBOUtil.virtualBufferVtx.bufferPointerSuperSet;
//
//        try (MemoryStack stack = stackPush()) {
//            CommandPool.CommandBuffer commandBuffer = TransferQueue.beginCommands();
//
//            VkBufferCopy.Buffer copyRegion = VkBufferCopy.callocStack(1, stack);
//            copyRegion.size(this.buffer.size_t());
//            copyRegion.srcOffset(this.buffer.i2());
//            copyRegion.dstOffset(0);
//
//            vkCmdCopyBuffer(commandBuffer.getHandle(), VBOUtil.virtualBufferVtx.bufferPointerSuperSet, dstBuffer, copyRegion);
//
//            TransferQueue.submitCommands(commandBuffer);
//            VK10.vkWaitForFences(Vulkan.getDevice(), commandBuffer.getFence(), true, VUtil.UINT64_MAX);
//            commandBuffer.reset();
//        }
//



        this.size = vkBufferPointer.size_t();

        return new Segment(vkBufferPointer.i2(), vkBufferPointer.size_t());
    }

    public synchronized void setSegmentFree(Segment uploadSegment) {
        Segment segment = usedSegments.remove(uploadSegment);

        if(segment == null)
            return;

        this.freeSegments.add(segment);
        this.used -= segment.size;
    }

    public long getId() {
        return VBOUtil.virtualBufferVtx.bufferPointerSuperSet;
    }

    public void freeBuffer() {
        VBOUtil.virtualBufferVtx.addFreeableRange(this.index, this.buffer);
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

    public Segment getSegment(int offset) {
        return this.usedSegments.get(offset);
    }
}
