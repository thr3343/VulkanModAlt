package net.vulkanmod.render.chunk;

import net.vulkanmod.Initializer;
import net.vulkanmod.render.virtualSegmentBuffer;
import net.vulkanmod.render.chunk.build.UploadBuffer;
import net.vulkanmod.render.chunk.util.StaticQueue;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.IndirectBuffer;
import net.vulkanmod.vulkan.memory.StagingBuffer;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.ShaderManager;
import net.vulkanmod.vulkan.util.VBOUtil;
import net.vulkanmod.vulkan.util.VUtil;
import org.apache.commons.lang3.Validate;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;

import static net.vulkanmod.render.vertex.TerrainRenderType.TRANSLUCENT;
import static net.vulkanmod.vulkan.queue.Queues.TransferQueue;
import static net.vulkanmod.vulkan.util.VBOUtil.*;
import static org.lwjgl.system.Checks.check;
import static org.lwjgl.system.JNI.*;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkCmdBindDescriptorSets;

public class DrawBuffers {

    static final int VERTEX_SIZE = ShaderManager.TERRAIN_VERTEX_FORMAT.getVertexSize();
    private static final int INDEX_SIZE = Short.BYTES;
    private final int areaIndex;

    private boolean allocated = false;
//    AreaBuffer vertexBuffer;
//    AreaBuffer indexBuffer;

//    final StaticQueue<VkDrawIndexedIndirectCommand2> sectionQueue2 = new StaticQueue<>(512);
    final StaticQueue<VkDrawIndexedIndirectCommand2> sectionQueue = new StaticQueue<>(512);
    final StaticQueue<VkDrawIndexedIndirectCommand2> TsectionQueue = new StaticQueue<>(512);


    //                callPJPV(commandBuffer.address(), pipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, 12, new float[]{(float) ((double) section.xOffset - camX), (float) ((double) section.yOffset - camY), (float) ((double) section.zOffset - camZ)}, commandBuffer.getCapabilities().vkCmdPushConstants);

    public DrawBuffers(int areaIndex, Vector3i position) {
        this.areaIndex = areaIndex;

    }


    public void allocateBuffers() {
//        this.vertexBuffer = new AreaBuffer(index, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, 2056192, VERTEX_SIZE);
//        this.indexBuffer = new AreaBuffer(VK_BUFFER_USAGE_INDEX_BUFFER_BIT, 96384, INDEX_SIZE);

        this.allocated = true;
    }

    public void upload(UploadBuffer buffer, DrawParameters drawParameters, int xOffset, int yOffset, int zOffset) {

        if(!buffer.indexOnly)
        {
            translateVBO(buffer, buffer.indexCount, xOffset, yOffset, zOffset);

            drawParameters.vertexBufferSegment1=  this.configureVertexFormat(drawParameters, drawParameters.index, buffer);
//            drawParameters.vertexOffset = drawParameters.vertexBufferSegment.getOffset() / VERTEX_SIZE;
            drawParameters.initialised =true;
//            drawParameters.firstIndex = 0;
//            sectionQueue2.add(new VkDrawIndexedIndirectCommand2(buffer.indexCount, 1, 0, drawParameters.vertexBufferSegment.i2(), 0));
//            drawParameters.vertexOffset = drawParameters.vertexBufferSegment.i2();

        }

//        if(!buffer.autoIndices) {
//            this.indexBuffer.upload(buffer.getIndexBuffer(), drawParameters.indexBufferSegment);
////            drawParameters.firstIndex = drawParameters.indexBufferSegment.getOffset() / INDEX_SIZE;
//            firstIndex = drawParameters.indexBufferSegment.getOffset() / INDEX_SIZE;
//        }

//        AreaUploadManager.INSTANCE.enqueueParameterUpdate(
//                new ParametersUpdate(drawParameters, buffer.indexCount, firstIndex, vertexOffset));


//        Drawer.getInstance().getQuadsIndexBuffer().checkCapacity(buffer.indexCount * 2 / 3);

        buffer.release();

//        return drawParameters;
    }

    private void translateVBO(UploadBuffer buffer, int indexCount, int xOffset, int yOffset, int zOffset) {
        final long addr = MemoryUtil.memAddress0(buffer.getVertexBuffer());
        final float v = (float) (xOffset - camX - originX);
        final float v1 = (float) (zOffset - camZ - originZ);
//        final float camX1 = (float) (v < (int) v ? v - 1 : v);
//        final float camZ1 = (float) (v1 < (int) v1 ? v1 - 1 : v1)
        final int camX1 = Math.round(v);
        final int camZ1 = Math.round(v1);

        for (int i = 0; i < buffer.getVertexBuffer().capacity(); i += VERTEX_SIZE) {
            VUtil.UNSAFE.putFloat(addr + i, (VUtil.UNSAFE.getFloat(addr + i) + (camX1)));
            VUtil.UNSAFE.putFloat(addr + i + 4, (VUtil.UNSAFE.getFloat(addr + i + 4) + yOffset));
            VUtil.UNSAFE.putFloat(addr + i + 8, (VUtil.UNSAFE.getFloat(addr + i + 8) + (camZ1)));
        }

    }

    public void buildDrawBatchesIndirect(IndirectBuffer indirectBuffer, TerrainRenderType renderType, double camX, double camY, double camZ) {
        int stride = 20;

        final int size = this.sectionQueue.size();
        if(size ==0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {

//
//            {
//                vkCmdBindIndexBuffer(Drawer.getCommandBuffer(), Drawer.getInstance().getQuadIndexBuffer().getIndexBuffer().getId(), 0, VK_INDEX_TYPE_UINT16);
//            }

//        var iterator = queue.iterator(isTranslucent);

            int drawCount = getDrawCount(renderType, stack.malloc(20 * size), indirectBuffer);

//            if (drawCount == 0) return 0;




//            pipeline.getManualUBO().setSrc(uboPtr, 16 * drawCount);

            if(!Initializer.CONFIG.bindless) {
                nvkCmdBindVertexBuffers(Drawer.getCommandBuffer(), 0, 1, stack.npointer(virtualBufferVtx.bufferPointerSuperSet), VUtil.nullptr);
            }


            vkCmdDrawIndexedIndirect(Drawer.getCommandBuffer(), indirectBuffer.getId(), indirectBuffer.getOffset(), drawCount, stride);

//            fakeIndirectCmd(Drawer.getCommandBuffer(), indirectBuffer, drawCount, uboBuffer);

//        MemoryUtil.memFree(byteBuffer);
        }

    }

    private int getDrawCount(TerrainRenderType terrainRenderType, ByteBuffer byteBuffer, IndirectBuffer indirectBuffer) {
        long bufferPtr = MemoryUtil.memAddress0(byteBuffer);
        int drawCount = 0;
        for (VkDrawIndexedIndirectCommand2 drawParameters : this.sectionQueue) {
            //            RenderSection section = iterator.next();
//            DrawParameters drawParameters = section.drawParametersArray[terrainRenderType.ordinal()];

            //Debug
            //            BlockPos o = section.origin;
            ////            BlockPos pos = new BlockPos(-2188, 65, -1674);
            //
            ////            Vec3 cameraPos = WorldRenderer.getCameraPos();
            //            BlockPos pos = new BlockPos(Minecraft.getInstance().getCameraEntity().blockPosition());
            //            if(o.getX() <= pos.getX() && o.getY() <= pos.getY() && o.getZ() <= pos.getZ() &&
            //                    o.getX() + 16 >= pos.getX() && o.getY() + 16 >= pos.getY() && o.getZ() + 16 >= pos.getZ()) {
            //                System.nanoTime();
            //
            //                }
            //
            //            }




            long ptr = bufferPtr + (drawCount * 20L); //TODO: For some reason, this line increased Performance (i.e. reduced CPU Overhead) by at least 10%
            MemoryUtil.memPutLong(ptr, (long) 1 <<32 | drawParameters.indexCount());
            MemoryUtil.memPutInt(ptr + 8, drawParameters.firstIndex());
            //            MemoryUtil.memPutInt(ptr + 12, drawParameters.vertexBufferSegment.getOffset() / VERTEX_SIZE);
            MemoryUtil.memPutInt(ptr + 12, drawParameters.vertexOffset() / VERTEX_SIZE);
            //            MemoryUtil.memPutInt(ptr + 12, drawParameters.vertexBufferSegment.getOffset());
            MemoryUtil.memPutInt(ptr + 16, 0);

//            ptr = uboPtr + (drawCount * 16L);
//            MemoryUtil.memPutFloat(ptr, (float) ((double) section.xOffset - camX));
//            MemoryUtil.memPutFloat(ptr + 4, (float) ((double) section.yOffset - camY));
//            MemoryUtil.memPutFloat(ptr + 8, (float) ((double) section.zOffset - camZ));

            drawCount++;
        }
        byteBuffer.position(0);

        indirectBuffer.recordCopyCmd(byteBuffer);
        return drawCount;
    }

    private static void fakeIndirectCmd(VkCommandBuffer commandBuffer, IndirectBuffer indirectBuffer, int drawCount, ByteBuffer offsetBuffer) {
        Pipeline pipeline = ShaderManager.shaderManager.terrainDirectShader;
//        Drawer.getInstance().bindPipeline(pipeline);
        pipeline.bindDescriptorSets(Drawer.getCommandBuffer(), Drawer.getCurrentFrame());
//        pipeline.bindDescriptorSets(Drawer.getCommandBuffer(), WorldRenderer.getInstance().getUniformBuffers(), Drawer.getCurrentFrame());

        ByteBuffer buffer = indirectBuffer.getByteBuffer();
        long address = MemoryUtil.memAddress0(buffer);
        long offsetAddress = MemoryUtil.memAddress0(offsetBuffer);
        int baseOffset = (int) indirectBuffer.getOffset();
        long offset;
        int stride = 20;

        int indexCount;
        int instanceCount;
        int firstIndex;
        int vertexOffset;
        int firstInstance;
        for(int i = 0; i < drawCount; ++i) {
            offset = (long) i * stride + baseOffset + address;

            indexCount    = MemoryUtil.memGetInt(offset);
            instanceCount = MemoryUtil.memGetInt(offset + 4);
            firstIndex    = MemoryUtil.memGetInt(offset + 8);
            vertexOffset  = MemoryUtil.memGetInt(offset + 12);
            firstInstance = MemoryUtil.memGetInt(offset + 16);


            long uboOffset = i * 16L + offsetAddress;

            nvkCmdPushConstants(commandBuffer, pipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, 12, uboOffset);

            vkCmdDrawIndexed(commandBuffer, indexCount, instanceCount, firstIndex, vertexOffset, firstInstance);
        }
    }

    public void buildDrawBatchesDirect(TerrainRenderType renderType, double camX, double camY, double camZ, long address) {


        //            DrawParameters drawParameters = section[renderType.ordinal()];
        //                drawIndexedBindless(drawParameters);
        for (VkDrawIndexedIndirectCommand2 drawParameters : renderType ==TerrainRenderType.TRANSLUCENT ? this.TsectionQueue : this.sectionQueue) {
            {
                VUtil.UNSAFE.putLong(npointer, drawParameters.vertexOffset());
                callPPPV(address, 0, 1, npointer1, npointer, functionAddress);

                callPV(address, drawParameters.indexCount(), 1, 0, 0, 0, functionAddress1);
            }
        }


    }

    private VkDrawIndexedIndirectCommand2 configureVertexFormat(DrawParameters drawParameters, int index, UploadBuffer parameters) {
//        boolean bl = !parameters.format().equals(this.vertexFormat);
        ByteBuffer data = parameters.getVertexBuffer();
        final int remaining = data.remaining();
        Validate.isTrue(remaining % VERTEX_SIZE ==0);
        if(drawParameters.vertexBufferSegment1 == null || VBOUtil.virtualBufferVtx.isAlreadyLoaded(areaIndex, index, remaining))
        {
            drawParameters.vertexBufferSegment = VBOUtil.virtualBufferVtx.allocSubSection(this.areaIndex, index, remaining);
        }
        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer(Drawer.getCurrentFrame());
        stagingBuffer.copyBuffer(remaining, data);

        Validate.isTrue(drawParameters.vertexBufferSegment.i2()<virtualBufferVtx.size_t);
        Validate.isTrue(drawParameters.vertexBufferSegment.size_t()<virtualBufferVtx.size_t);
        TransferQueue.uploadBufferImmediate(stagingBuffer.getId(), stagingBuffer.getOffset(), virtualBufferVtx.bufferPointerSuperSet, drawParameters.vertexBufferSegment.i2(), remaining);
//            this.vertOff= fakeVertexBuffer.i2()>>5;
        return new VkDrawIndexedIndirectCommand2(parameters.indexCount, 1, 0, drawParameters.vertexBufferSegment.i2(), 0);
    }
    public void releaseBuffers() {
        if(!this.allocated)
            return;

//        for(var a : sectionQueue)
//        {
//            virtualBufferVtx.addFreeableRange(a.vertexBufferSegment);
//            a.initialised=false;
//            a.vertexBufferSegment=null;
//        }
//        for(var a : TsectionQueue)
//        {
//            virtualBufferVtx.addFreeableRange(a.vertexBufferSegment);
//            a.initialised=false;
//            a.vertexBufferSegment=null;
//        }
        virtualBufferVtx.freeRange(this.areaIndex);
        this.sectionQueue.clear();
        this.TsectionQueue.clear();

//        this.vertexBuffer.freeBuffer();
//        this.indexBuffer.freeBuffer();

//        this.vertexBuffer = null;
//        this.indexBuffer = null;
        this.allocated = false;
    }

    public boolean isAllocated() {
        return allocated;
    }

    public void addSection(TerrainRenderType rType, VkDrawIndexedIndirectCommand2 renderSection) {
        (rType!=TRANSLUCENT ? sectionQueue : TsectionQueue).add(renderSection);
    }

    public static class DrawParameters {
        final int index;
        //        private final TerrainRenderType r;
//        int indexCount;
//        int firstIndex;
        VkDrawIndexedIndirectCommand2 vertexBufferSegment1 = null;
        //        int vertexOffset;
        virtualSegmentBuffer vertexBufferSegment = null;
//        AreaBuffer.Segment indexBufferSegment;
        boolean initialised = false;

        DrawParameters(int index) {//            this.r = translucent;
//            if(translucent==TerrainRenderType.TRANSLUCENT) {
//                indexBufferSegment = new AreaBuffer.Segment(-1);
//            }
            this.index = index;
        }

        public void reset(ChunkArea chunkArea) {
//            this.indexCount = 0;
//            this.firstIndex = 0;
//            this.initialised =false;
            //                VBOUtil.virtualBufferVtx.addFreeableRange(this.vertexBufferSegment);
            //                VBOUtil.virtualBufferVtx.addFreeableRange(this.vertexBufferSegment);
            //                    this.vertexBufferSegment = null;
            //                chunkArea.drawBuffers.vertexBuffer.setSegmentFree(this.vertexBufferSegment);
            //            this.vertexBufferSegment = null;
        }

        public void reset() {

            if(this.initialised)
            {
                VBOUtil.virtualBufferVtx.addFreeableRange(this.vertexBufferSegment);
                this.vertexBufferSegment = null;
                this.vertexBufferSegment1 = null;
                initialised=false;
            }

        }
    }

}
