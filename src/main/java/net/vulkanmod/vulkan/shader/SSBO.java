package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.Vulkan;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.VkBufferCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;

public class SSBO {
    private final int size;
    private final int flags;
    public long buffer;
    public long alloc;

    public SSBO(int size, int flags)
    {
        this.size = size;
        this.flags = flags;
        buffer=createSSBO(flags);

    }

    private long createSSBO(int flags) {
        final long storageBuffer;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pBufferMemory = stack.mallocPointer(1);
            LongBuffer pBuffer = stack.mallocLong(1);


            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.callocStack(stack);
            bufferInfo.sType$Default();
            bufferInfo.size(size);
            bufferInfo.usage(flags);
            bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            //
            VmaAllocationCreateInfo allocationInfo = VmaAllocationCreateInfo.callocStack(stack);
                allocationInfo.usage(VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE);
            allocationInfo.flags(VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT);
//            allocationInfo.requiredFlags(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            int result = vmaCreateBuffer(Vulkan.getAllocator(), bufferInfo, allocationInfo, pBuffer, pBufferMemory, null);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create buffer:" + result);
            }
            return pBuffer.get(0);
        }
    }
}
