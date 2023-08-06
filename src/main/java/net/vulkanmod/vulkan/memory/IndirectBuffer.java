package net.vulkanmod.vulkan.memory;

import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.queue.CommandPool;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDrawIndexedIndirectCommand;

import java.nio.ByteBuffer;

import static net.vulkanmod.vulkan.queue.Queues.TransferQueue;
import static org.lwjgl.system.JNI.callPJJJPV;
import static org.lwjgl.system.MemoryUtil.memByteBuffer;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.vkCmdUpdateBuffer;

public class IndirectBuffer extends Buffer {
    CommandPool.CommandBuffer commandBuffer;

    public IndirectBuffer(int size, MemoryType type) {
        super(VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT, type);
        this.createBuffer(size);
    }

    public void recordCopyCmd(VkDrawIndexedIndirectCommand.Buffer byteBuffer) {
        final int size = byteBuffer.position()*20;

        if(size > this.bufferSize - this.usedBytes) {
            resizeBuffer();
        }


            if(commandBuffer == null)
                commandBuffer=TransferQueue.beginCommands();

            VkCommandBuffer commandBuffer1 = commandBuffer.getHandle();
            vkCmdUpdateBuffer(commandBuffer1, this.id, size, byteBuffer.address0());
//            TransferQueue.uploadBufferCmd(commandBuffer, stagingBuffer.id, stagingBuffer.offset, this.getId(), this.getUsedBytes(), size);
        

        offset = usedBytes;
        usedBytes += size;
    }

    private void vkCmdUpdateBuffer(VkCommandBuffer commandBuffer1, long id, int size, long param4) {
        callPJJJPV(commandBuffer1.address(), id, 0, size, param4, commandBuffer1.getCapabilities().vkCmdUpdateBuffer);
    }

    private void resizeBuffer() {
        MemoryManager.getInstance().addToFreeable(this);
        int newSize = this.bufferSize + (this.bufferSize >> 1);
        this.createBuffer(newSize);
        this.usedBytes = 0;
    }

    public void submitUploads() {
        if(commandBuffer == null)
            return;

        TransferQueue.submitCommands(commandBuffer);
        Synchronization.INSTANCE.addCommandBuffer(commandBuffer);
        commandBuffer = null;
    }

    //debug
    public ByteBuffer getByteBuffer() {
        return this.data.getByteBuffer(0, this.bufferSize);
    }
}
