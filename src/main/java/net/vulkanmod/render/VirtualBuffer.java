package net.vulkanmod.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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

    public final ObjectArrayList<VkBufferPointer> activeRanges = new ObjectArrayList<>(1024);
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
    public VkBufferPointer addSubIncr(int i, int index, int actualSize) {

         if(size_t<=usedBytes+ (actualSize))
        {
            System.out.println(size_t+"-->"+(usedBytes+ (actualSize))+"-->"+size_t*2);
            WorldRenderer.getInstance().setNeedsUpdate();
            WorldRenderer.getInstance().allChanged();
        }

        try(MemoryStack stack = MemoryStack.stackPush())
        {

            VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.malloc(stack);
            allocCreateInfo.size((actualSize));
            allocCreateInfo.alignment(0);
            allocCreateInfo.flags(0);
            allocCreateInfo.pUserData(NULL);

            PointerBuffer pAlloc = stack.mallocPointer(1);

            ;
//            subIncr += alignedSize;
            usedBytes+= (actualSize);
            Vma.vmaVirtualAllocate(virtualBlockBufferSuperSet, allocCreateInfo, pAlloc, null);

            long allocation = pAlloc.get(0);


            subAllocs++;
            VmaVirtualAllocationInfo allocCreateInfo1 = VmaVirtualAllocationInfo.malloc(stack);

            if(allocation==0)
            {
                System.out.println(size_t+"-->"+(size_t-usedBytes)+"-->"+(usedBytes+ (actualSize))+"-->"+ (actualSize) +"-->"+size_t);
                WorldRenderer.getInstance().setNeedsUpdate();
                WorldRenderer.getInstance().allChanged();
                pAlloc=stack.mallocPointer(1);
                Vma.vmaVirtualAllocate(virtualBlockBufferSuperSet, allocCreateInfo, pAlloc, stack.longs(0));
                allocation=pAlloc.get(0);

            }
            vmaGetVirtualAllocationInfo(virtualBlockBufferSuperSet, allocation, allocCreateInfo1);

            updateStatistics(stack);
            VkBufferPointer vkBufferPointer = new VkBufferPointer(i, index, (int) allocCreateInfo1.offset(), (int) allocCreateInfo1.size(), pAlloc.get(0));
            activeRanges.add(vkBufferPointer);
            return vkBufferPointer;
        }
    }

    public boolean isAlreadyLoaded(int index, int remaining) {
        VkBufferPointer vkBufferPointer = getActiveRangeFromIdx(index);
        if(vkBufferPointer==null) return false;
        if(vkBufferPointer.size_t()>=remaining)
        {
            return true;
        }
        return !addFreeableRange(vkBufferPointer);


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

    public boolean addFreeableRange(VkBufferPointer bufferPointer)
    {
        if(usedBytes==0) return false;
        if(bufferPointer==null) return false;
        if(bufferPointer.allocation()==-1) return false;
        if(bufferPointer.allocation()==0) return false;
//        if(bufferPointer.sizes==0) return;
        boolean freed =false;
        Vma.vmaVirtualFree(virtualBlockBufferSuperSet, bufferPointer.allocation());
        for (int i = 0; i < activeRanges.size(); i++) {
            VkBufferPointer vkBufferPointer = activeRanges.get(i);
            if (vkBufferPointer.subIndex() == bufferPointer.subIndex()) {
                activeRanges.remove(i);
                freed=true;
                break;
            }
        }
        subAllocs--;
        usedBytes-=bufferPointer.size_t();
        return freed;
    }

    public VkBufferPointer getActiveRangeFromIdx(int index) {
        for (VkBufferPointer vkBufferPointer : activeRanges) {
            if (vkBufferPointer.subIndex() == index) {
                return vkBufferPointer;
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
        ArrayList<VkBufferPointer> tmpfrees=new ArrayList<>(512);
        for (VkBufferPointer vkBufferPointer : activeRanges) {
            if (vkBufferPointer.areaGlobalIndex() == index) {
               tmpfrees.add(vkBufferPointer);
            }
        }
        for(VkBufferPointer vkBufferPointer : tmpfrees)
        {
            addFreeableRange(vkBufferPointer);
        }
    }
}
