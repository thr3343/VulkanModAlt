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


    public enum Family {

        GraphicsQueue(QueueFamilyIndices.graphicsFamily),
        TransferQueue(QueueFamilyIndices.transferFamily),
        ComputeQueue(QueueFamilyIndices.computeFamily);

//        private static final VkDevice DEVICE = Vulkan.getDevice();

        public final CommandPool commandPool;
        public final long Queue;
        private CommandPool.CommandBuffer currentCmdBuffer;
        Family(int computeFamily) {

            commandPool = new CommandPool(computeFamily);
           try(MemoryStack stack = MemoryStack.stackPush())
           {
               PointerBuffer pQueue = stack.mallocPointer(1);
               final VkDevice device = Vulkan.getDevice();
               callPPV(device.address(), computeFamily, 0, pQueue.address(), device.getCapabilities().vkGetDeviceQueue);
               this.Queue =pQueue.get(0);
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

//        public void uploadBufferImmediate(long srcBuffer, long srcOffset, long dstBuffer, long dstOffset, long size) {
//
//            try(MemoryStack stack = stackPush()) {
//                CommandPool.CommandBuffer commandBuffer = beginCommands();
//
//                VkBufferCopy.Buffer copyRegion = VkBufferCopy.callocStack(1, stack);
//                copyRegion.size(size);
//                copyRegion.srcOffset(srcOffset);
//                copyRegion.dstOffset(dstOffset);
//
//                vkCmdCopyBuffer(commandBuffer.getHandle(), srcBuffer, dstBuffer, copyRegion);
//
//                submitCommands(commandBuffer);
//                vkWaitForFences(DEVICE, commandBuffer.fence, true, VUtil.UINT64_MAX);
//                commandBuffer.reset();
//            }
//        }



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

//        public void waitIdle() {
//            vkQueueWaitIdle(Vulkan.getTransferQueue());
//        }

    }



}
