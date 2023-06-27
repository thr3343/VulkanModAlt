package net.vulkanmod.vulkan.queue;

import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.stream.IntStream;

import static org.lwjgl.system.Checks.CHECKS;
import static org.lwjgl.system.Checks.check;
import static org.lwjgl.system.JNI.callPPV;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.VK10.*;

public abstract class Queue {
    private static VkDevice DEVICE;

    public enum Family {

        GraphicsQueue(QueueFamilyIndices.graphicsFamily),
        TransferQueue(QueueFamilyIndices.transferFamily),
        ComputeQueue(QueueFamilyIndices.computeFamily);

//        private static final VkDevice DEVICE = Vulkan.getDevice();

        public final CommandPool commandPool;
        public final VkQueue Queue;
        private CommandPool.CommandBuffer currentCmdBuffer;
        Family(int computeFamily) {

            commandPool = new CommandPool(computeFamily);
           try(MemoryStack stack = MemoryStack.stackPush())
           {
               PointerBuffer pQueue = stack.mallocPointer(1);
               callPPV(DEVICE.address(), computeFamily, 0, pQueue.address(), DEVICE.getCapabilities().vkGetDeviceQueue);
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



        public void uploadBufferCmd(CommandPool.CommandBuffer commandBuffer, long srcBuffer, long srcOffset, long dstBuffer, long dstOffset, long size) {

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


    public static void initDevs() {

            DEVICE = Vulkan.getDevice();

        //        return queueFamilyIndices;
    }

    public enum Constants {
        graphicsFamily(QueueFamilyIndices.graphicsFamily),
        transferFamily(QueueFamilyIndices.transferFamily),
        computeFamily(QueueFamilyIndices.presentFamily);

        public final int graphicsFamily1;

        Constants(int graphicsFamily) {

            graphicsFamily1 = graphicsFamily;
        }
    }

    public static class QueueFamilyIndices {

        // We use Integer to use null as the empty value
        public static int graphicsFamily = -1;
        public static int presentFamily = -1;
        public static int transferFamily = -1;
        public static int computeFamily = -1;

        public static boolean findQueueFamilies(VkPhysicalDevice device) {


            try(MemoryStack stack = stackPush()) {

                IntBuffer queueFamilyCount = stack.ints(0);

                vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);

                VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.mallocStack(queueFamilyCount.get(0), stack);

                vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

                //            for(int i = 0; i < queueFamilies.capacity() || !indices.isComplete();i++) {
                for(int i = 0; i < queueFamilies.capacity(); i++) {
                    int queueFlags = queueFamilies.get(i).queueFlags();

                    if ((queueFlags & VK_QUEUE_GRAPHICS_BIT) != 0) {
                        graphicsFamily = i;

//                        vkGetPhysicalDeviceSurfaceSupportKHR(device, i, Vulkan.getSurface(), presentSupport);

                        if((queueFlags & VK_QUEUE_COMPUTE_BIT) != 0) {
                            presentFamily = i;
                        }
                    } else if ((queueFlags & (VK_QUEUE_GRAPHICS_BIT)) == 0
                            && (queueFlags & VK_QUEUE_COMPUTE_BIT) != 0) {
                        computeFamily = i;
                    } else if ((queueFlags & (VK_QUEUE_COMPUTE_BIT | VK_QUEUE_GRAPHICS_BIT)) == 0
                            && (queueFlags & VK_QUEUE_TRANSFER_BIT) != 0) {
                        transferFamily = i;
                    }

                    if(presentFamily == -1) {
//                        vkGetPhysicalDeviceSurfaceSupportKHR(device, i, Vulkan.getSurface(), presentSupport);

                        if((queueFlags & VK_QUEUE_COMPUTE_BIT) != 0) {
                            presentFamily = i;
                        }
                    }

                    if(isComplete()) break;
                }

                if(transferFamily == -1) {

                    int fallback = -1;
                    for(int i = 0; i < queueFamilies.capacity(); i++) {
                        int queueFlags = queueFamilies.get(i).queueFlags();

                        if((queueFlags & VK_QUEUE_TRANSFER_BIT) != 0) {
                            if(fallback == -1)
                                fallback = i;

                            if ((queueFlags & (VK_QUEUE_GRAPHICS_BIT)) == 0) {
                                transferFamily = i;

                                if(i != computeFamily)
                                    break;
                                fallback = i;
                            }
                        }

                        if(fallback == -1)
                            throw new RuntimeException("Failed to find queue family with transfer support");

                        transferFamily = fallback;
                    }
                }

                if(computeFamily == -1) {
                    for(int i = 0; i < queueFamilies.capacity(); i++) {
                        int queueFlags = queueFamilies.get(i).queueFlags();

                        if((queueFlags & VK_QUEUE_COMPUTE_BIT) != 0) {
                            computeFamily = i;
                            break;
                        }
                    }
                }

                if (graphicsFamily == -1)
                    throw new RuntimeException("Unable to find queue family with graphics support.");
                if (presentFamily == -1)
                    throw new RuntimeException("Unable to find queue family with present support.");
                if (computeFamily == -1)
                    throw new RuntimeException("Unable to find queue family with compute support.");

            }
            return isSuitable();
        }

        public static boolean isComplete() {
            return graphicsFamily != -1 && presentFamily != -1 && transferFamily != -1 && computeFamily != -1;
        }

        public static boolean isSuitable() {
            return graphicsFamily != -1 && presentFamily != -1;
        }

        public static int[] unique() {
            return IntStream.of(graphicsFamily, presentFamily, transferFamily, computeFamily).distinct().toArray();
        }

        public static int[] array() {
            return new int[] {graphicsFamily, presentFamily};
        }
    }
}
