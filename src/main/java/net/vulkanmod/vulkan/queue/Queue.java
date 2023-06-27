package net.vulkanmod.vulkan.queue;

import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.stream.IntStream;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.VK10.*;

public abstract class Queue {
    private static VkDevice DEVICE;

    private static QueueFamilyIndices queueFamilyIndices;


    public enum Family {
        GraphicsQueue(Constants.graphicsFamily),
        TransferQueue(Constants.transferFamily),
        ComputeQueue(Constants.computeFamily);

//        private static final VkDevice DEVICE = Vulkan.getDevice();

        public final CommandPool commandPool;
        public final VkQueue Queue;
        private CommandPool.CommandBuffer currentCmdBuffer;
        Family(Constants computeFamily) {

            commandPool = new CommandPool(computeFamily.graphicsFamily1);
           try(MemoryStack stack = MemoryStack.stackPush())
           {
               PointerBuffer pQueue = stack.mallocPointer(1);
               vkGetDeviceQueue(DEVICE, computeFamily.graphicsFamily1, 0, pQueue);
               this.Queue = new VkQueue(pQueue.get(0), DEVICE);
           }

        }

        public CommandPool.CommandBuffer beginCommands() {

            return commandPool.beginCommands();
        }

//    public abstract long submitCommands(CommandPool.CommandBuffer commandBuffer);

        public void cleanUp() {
            commandPool.cleanUp();
        }


        public long copyBufferCmd(long srcBuffer, long srcOffset, long dstBuffer, long dstOffset, long size) {

            try(MemoryStack stack = stackPush()) {

                CommandPool.CommandBuffer commandBuffer = beginCommands();

                VkBufferCopy.Buffer copyRegion = VkBufferCopy.callocStack(1, stack);
                copyRegion.size(size);
                copyRegion.srcOffset(srcOffset);
                copyRegion.dstOffset(dstOffset);

                vkCmdCopyBuffer(commandBuffer.getHandle(), srcBuffer, dstBuffer, copyRegion);

                submitCommands(commandBuffer);
                Synchronization.INSTANCE.addCommandBuffer(commandBuffer);

                return commandBuffer.fence;
            }
        }

        public void uploadBufferImmediate(long srcBuffer, long srcOffset, long dstBuffer, long dstOffset, long size) {

            try(MemoryStack stack = stackPush()) {
                CommandPool.CommandBuffer commandBuffer = beginCommands();

                VkBufferCopy.Buffer copyRegion = VkBufferCopy.callocStack(1, stack);
                copyRegion.size(size);
                copyRegion.srcOffset(srcOffset);
                copyRegion.dstOffset(dstOffset);

                vkCmdCopyBuffer(commandBuffer.getHandle(), srcBuffer, dstBuffer, copyRegion);

                submitCommands(commandBuffer);
                vkWaitForFences(DEVICE, commandBuffer.fence, true, VUtil.UINT64_MAX);
                commandBuffer.reset();
            }
        }



        public static void uploadBufferCmd(CommandPool.CommandBuffer commandBuffer, long srcBuffer, long srcOffset, long dstBuffer, long dstOffset, long size) {

            try(MemoryStack stack = stackPush()) {

                VkBufferCopy.Buffer copyRegion = VkBufferCopy.callocStack(1, stack);
                copyRegion.size(size);
                copyRegion.srcOffset(srcOffset);
                copyRegion.dstOffset(dstOffset);

                vkCmdCopyBuffer(commandBuffer.getHandle(), srcBuffer, dstBuffer, copyRegion);
            }
        }



//    public abstract long submitCommands(CommandPool.CommandBuffer commandBuffer);


        public void startRecording() {
            currentCmdBuffer = beginCommands();
        }

        public void endRecordingAndSubmit() {
            long fence = submitCommands(currentCmdBuffer);
            Synchronization.INSTANCE.addCommandBuffer(currentCmdBuffer);

            currentCmdBuffer = null;
        }

        public CommandPool.CommandBuffer getCommandBuffer() {
            if (currentCmdBuffer != null) {
                return currentCmdBuffer;
            } else {
                return beginCommands();
            }
        }

        public long endIfNeeded(CommandPool.CommandBuffer commandBuffer) {
            if (currentCmdBuffer != null) {
                return VK_NULL_HANDLE;
            } else {
                return submitCommands(commandBuffer);
            }
        }

        public long submitCommands(CommandPool.CommandBuffer commandBuffer) {

            return commandPool.submitCommands(commandBuffer, this.Queue);
        }

        public void waitIdle() {
            vkQueueWaitIdle(Vulkan.getTransferQueue());
        }

    }


    public static void initDevs(QueueFamilyIndices queueFamilyIndices1) {

            DEVICE = Vulkan.getDevice();

        queueFamilyIndices = queueFamilyIndices1;
        //        return queueFamilyIndices;
    }

    public static QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device) {

        QueueFamilyIndices indices = new QueueFamilyIndices();

        try(MemoryStack stack = stackPush()) {

            IntBuffer queueFamilyCount = stack.ints(0);

            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);

            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.mallocStack(queueFamilyCount.get(0), stack);

            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

            IntBuffer presentSupport = stack.ints(VK_FALSE);

//            for(int i = 0; i < queueFamilies.capacity() || !indices.isComplete();i++) {
            for(int i = 0; i < queueFamilies.capacity(); i++) {
                int queueFlags = queueFamilies.get(i).queueFlags();

                if ((queueFlags & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    indices.graphicsFamily = i;

                    vkGetPhysicalDeviceSurfaceSupportKHR(device, i, Vulkan.getSurface(), presentSupport);

                    if((queueFlags & VK_QUEUE_COMPUTE_BIT) != 0) {
                        indices.presentFamily = i;
                    }
                } else if ((queueFlags & (VK_QUEUE_GRAPHICS_BIT)) == 0
                        && (queueFlags & VK_QUEUE_COMPUTE_BIT) != 0) {
                    indices.computeFamily = i;
                } else if ((queueFlags & (VK_QUEUE_COMPUTE_BIT | VK_QUEUE_GRAPHICS_BIT)) == 0
                        && (queueFlags & VK_QUEUE_TRANSFER_BIT) != 0) {
                    indices.transferFamily = i;
                }

                if(indices.presentFamily == -1) {
                    vkGetPhysicalDeviceSurfaceSupportKHR(device, i, Vulkan.getSurface(), presentSupport);

                    if((queueFlags & VK_QUEUE_COMPUTE_BIT) != 0) {
                        indices.presentFamily = i;
                    }
                }

                if(indices.isComplete()) break;
            }

            if(indices.transferFamily == -1) {

                int fallback = -1;
                for(int i = 0; i < queueFamilies.capacity(); i++) {
                    int queueFlags = queueFamilies.get(i).queueFlags();

                    if((queueFlags & VK_QUEUE_TRANSFER_BIT) != 0) {
                        if(fallback == -1)
                            fallback = i;

                        if ((queueFlags & (VK_QUEUE_GRAPHICS_BIT)) == 0) {
                            indices.transferFamily = i;

                            if(i != indices.computeFamily)
                                break;
                            fallback = i;
                        }
                    }

                    if(fallback == -1)
                        throw new RuntimeException("Failed to find queue family with transfer support");

                    indices.transferFamily = fallback;
                }
            }

            if(indices.computeFamily == -1) {
                for(int i = 0; i < queueFamilies.capacity(); i++) {
                    int queueFlags = queueFamilies.get(i).queueFlags();

                    if((queueFlags & VK_QUEUE_COMPUTE_BIT) != 0) {
                        indices.computeFamily = i;
                        break;
                    }
                }
            }

            if (indices.graphicsFamily == -1)
                throw new RuntimeException("Unable to find queue family with graphics support.");
            if (indices.presentFamily == -1)
                throw new RuntimeException("Unable to find queue family with present support.");
            if (indices.computeFamily == -1)
                throw new RuntimeException("Unable to find queue family with compute support.");

            return indices;
        }
    }

    public enum Constants {
        graphicsFamily(queueFamilyIndices.graphicsFamily),
        transferFamily(queueFamilyIndices.transferFamily),
        computeFamily(queueFamilyIndices.presentFamily);

        public final int graphicsFamily1;

        Constants(int graphicsFamily) {

            graphicsFamily1 = graphicsFamily;
        }
    }

    public static class QueueFamilyIndices {

        // We use Integer to use null as the empty value
        public int graphicsFamily,presentFamily,transferFamily,computeFamily=-1;

        public boolean isComplete() {
            return graphicsFamily != -1 && presentFamily != -1 && transferFamily != -1 && computeFamily != -1;
        }

        public boolean isSuitable() {
            return graphicsFamily != -1 && presentFamily != -1;
        }

        public int[] unique() {
            return IntStream.of(graphicsFamily, presentFamily, transferFamily, computeFamily).distinct().toArray();
        }

        public int[] array() {
            return new int[] {graphicsFamily, presentFamily};
        }
    }
}
