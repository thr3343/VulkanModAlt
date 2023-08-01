package net.vulkanmod.render;

import it.unimi.dsi.fastutil.objects.*;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.Vulkan;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.*;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.Pointer.POINTER_SIZE;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;

public final class VirtualBuffer {
    public final long bufferPointerSuperSet;
    private final long virtualBlockBufferSuperSet;
    public final long size_t;
    //    public int subIncr;
    public int usedBytes;
    public int subAllocs;
    public long unusedRangesS;
    public long unusedRangesM;
    public int unusedRangesCount;
    public long allocMin;
    public long allocMax;
//    public boolean bound=false;
    private final long  bufferPtrBackingAlloc;
//    private static long  PUSERDATA=nmemAlignedAlloc(8, 8);
//    private static long  PFNALLOCATION=nmemAlignedAlloc(8, 32);
//    private static long  PFNREALLOCATION=nmemAlignedAlloc(8, 48);
//    private static long  PFNFREE=nmemAlignedAlloc(8, 16);
//    private static long  PFNINTERNALALLOCATION=nmemAlignedAlloc(8, 32);
//    private static long  PFNINTERNALFREE=nmemAlignedAlloc(8, 32);
//    public static int allocBytes;

    public final ObjectArrayList<virtualSegmentBuffer> activeRanges = new ObjectArrayList<>(1024);
    private final int vkBufferType;


    public VirtualBuffer(long size_t, int type)
    {
        this.size_t=size_t;
        this.vkBufferType =type;


        try(MemoryStack stack = MemoryStack.stackPush())
        {
            VmaVirtualBlockCreateInfo blockCreateInfo = VmaVirtualBlockCreateInfo.malloc(stack);
            blockCreateInfo.size(this.size_t);
            blockCreateInfo.flags(0);
            blockCreateInfo.pAllocationCallbacks(null);

            PointerBuffer pAlloc = stack.mallocPointer(1);
            LongBuffer pBuffer = stack.mallocLong(1);

            bufferPointerSuperSet = createBackingBuffer(pAlloc, pBuffer);
            bufferPtrBackingAlloc=pAlloc.get(0);

            PointerBuffer block = stack.mallocPointer(1);
            Vma.vmaCreateVirtualBlock(blockCreateInfo, block);
            virtualBlockBufferSuperSet = block.get(0);

//            size_t=size;
//            bound=true;



        }
    }

    public void reset()
    {


//        subIncr=0;
        subAllocs=0;
        usedBytes=0;
        activeRanges.clear();
        freeThis();
//
    }

    private void freeThis() {
        Vma.vmaClearVirtualBlock(virtualBlockBufferSuperSet);
        {
//            Vma.vmaDestroyVirtualBlock(virtualBlockBufferSuperSet);
//            vkFreeMemory(Vulkan.getDevice(), bufferPtrBackingAlloc, null);
//            vkDestroyBuffer(Vulkan.getDevice(), bufferPointerSuperSet, null);
        }
    }


    private long createBackingBuffer(PointerBuffer pAlloc, LongBuffer pBuffer) {

        try(MemoryStack stack = stackPush()) {

            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.callocStack(stack);
            bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
            bufferInfo.size(size_t);
            bufferInfo.usage(VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT | vkBufferType);
            bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
//
            VmaAllocationCreateInfo allocationInfo  = VmaAllocationCreateInfo.callocStack(stack);
            allocationInfo.usage(VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE|VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT);
            allocationInfo.requiredFlags(VK_MEMORY_HEAP_DEVICE_LOCAL_BIT);

            int result = vmaCreateBuffer(Vulkan.getAllocator(), bufferInfo, allocationInfo, pBuffer, pAlloc, null);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create buffer:" + result);
            }

//            LongBuffer pBufferMem = MemoryUtil.memLongBuffer(MemoryUtil.memAddressSafe(pBufferMemory), 1);
//
//            if(vkCreateBuffer(device, bufferInfo, null, pBuffer) != VK_SUCCESS) {
//                throw new RuntimeException("Failed to create vertex buffer");
//            }
//
//            VkMemoryRequirements memRequirements = VkMemoryRequirements.mallocStack(stack);
//            vkGetBufferMemoryRequirements(device, pBuffer.get(0), memRequirements);
//
//            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.callocStack(stack);
//            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
//            allocInfo.allocationSize(memRequirements.size());
//            allocInfo.memoryTypeIndex(findMemoryType(memRequirements.memoryTypeBits(), properties));
//
//            if(vkAllocateMemory(device, allocInfo, null, pBufferMem) != VK_SUCCESS) {
//                throw new RuntimeException("Failed to allocate vertex buffer memory");
//            }
//
//            vkBindBufferMemory(device, pBuffer.get(0), pBufferMem.get(0), 0);

        }

        return pBuffer.get(0);

//        size_t= size;
    }

    //TODO: Global ChunKArea index....
    public virtualSegmentBuffer allocSubSection(int areaIndex, int subIndex, int size_t) {

        if(this.size_t <=usedBytes+ (size_t))
            reload(size_t);

        try(MemoryStack stack = MemoryStack.stackPush())
        {
            VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.malloc(stack)
                    .size((size_t))
                    .alignment(32)
                    .flags(0)
                    .pUserData(NULL);

            long pAlloc = stack.nmalloc(POINTER_SIZE);
            long pOffset = stack.nmalloc(POINTER_SIZE);

            usedBytes+= (size_t);
        
            subAllocs++;
            
            if(nvmaVirtualAllocate(virtualBlockBufferSuperSet, allocCreateInfo.address(), pAlloc, pOffset) ==VK_ERROR_OUT_OF_DEVICE_MEMORY)
            {
                reload(size_t);
                nvmaVirtualAllocate(virtualBlockBufferSuperSet, allocCreateInfo.address(), pAlloc, pOffset);
            }

//            updateStatistics(stack);
            virtualSegmentBuffer virtualSegmentBuffer
                    = new virtualSegmentBuffer(areaIndex, subIndex, memGetInt(pOffset), size_t, memGetLong(pAlloc));
            activeRanges.add(virtualSegmentBuffer);
            return virtualSegmentBuffer;
        }
    }

    private void reload(int actualSize) {
        System.out.println(size_t+"-->"+(size_t-usedBytes)+"-->"+(usedBytes+ actualSize)+"-->"+ actualSize +"-->"+size_t);
        WorldRenderer.getInstance().setNeedsUpdate();
        WorldRenderer.getInstance().allChanged();
    }

    public boolean isAlreadyLoaded(int index, int remaining) {
        virtualSegmentBuffer virtualSegmentBuffer = getActiveRangeFromIdx(index);
        if(virtualSegmentBuffer ==null) return false;
        if(virtualSegmentBuffer.size_t()>=remaining)
        {
            return true;
        }
        addFreeableRange(virtualSegmentBuffer);
        return false;


    }


    //Not Supported on LWJGL 3.3.1
    private void updateStatistics(MemoryStack stack) {
        VmaDetailedStatistics vmaStatistics = VmaDetailedStatistics.malloc(stack);
        vmaCalculateVirtualBlockStatistics(virtualBlockBufferSuperSet, vmaStatistics);
//        vmaGetVirtualBlockStatistics(virtualBlockBufferSuperSet, vmaStatistics.statistics());
//        usedBytes= (int) vmaStatistics.statistics().allocationBytes();
//        allocs=vmaStatistics.statistics().allocationCount();
//        allocBytes= (int) vmaStatistics.statistics().allocationBytes();
//        blocks=vmaStatistics.statistics().blockCount();
//        blockBytes=vmaStatistics.statistics().blockBytes();
        unusedRangesS=vmaStatistics.unusedRangeSizeMin();
        unusedRangesM=vmaStatistics.unusedRangeSizeMax();
        unusedRangesCount=vmaStatistics.unusedRangeCount();
        allocMin=vmaStatistics.allocationSizeMin();
        allocMax=vmaStatistics.allocationSizeMax();
    }

    public void addFreeableRange(virtualSegmentBuffer bufferPointer)
    {
        if(usedBytes==0) return;
        if(bufferPointer==null) return;
        if(bufferPointer.allocation()==-1) return;
        if(bufferPointer.allocation()==0) return;
//        if(bufferPointer.sizes==0) return;
        boolean freed =false;
        Vma.vmaVirtualFree(virtualBlockBufferSuperSet, bufferPointer.allocation());
        for (int i = 0; i < activeRanges.size(); i++) {
            virtualSegmentBuffer virtualSegmentBuffer = activeRanges.get(i);
            if (virtualSegmentBuffer == bufferPointer) {
                activeRanges.remove(i);
                freed=true;
                break;
            }
        }
        if(freed){
            subAllocs--;
            usedBytes-=bufferPointer.size_t();
        }
    }

    public virtualSegmentBuffer getActiveRangeFromIdx(int index) {
        for (virtualSegmentBuffer virtualSegmentBuffer : activeRanges) {
            if (virtualSegmentBuffer.subIndex() == index) {
                return virtualSegmentBuffer;
            }
        }
        return null;
    }

    //Makes Closing the game very slow
    public void cleanUp()
    {
        Vma.vmaClearVirtualBlock(virtualBlockBufferSuperSet);
        {
            Vma.vmaDestroyVirtualBlock(virtualBlockBufferSuperSet);
            vmaDestroyBuffer(Vulkan.getAllocator(), bufferPointerSuperSet, bufferPtrBackingAlloc);
        }
        System.out.println("FREED");
    }

    public void freeRange(int index) {
        ArrayList<virtualSegmentBuffer> tmpfrees=new ArrayList<>(512);
        for (virtualSegmentBuffer virtualSegmentBuffer : activeRanges) {
            if (virtualSegmentBuffer.areaGlobalIndex() == index) {
               tmpfrees.add(virtualSegmentBuffer);
            }
        }
        for(virtualSegmentBuffer virtualSegmentBuffer : tmpfrees)
        {
            addFreeableRange(virtualSegmentBuffer);
        }
    }
}
