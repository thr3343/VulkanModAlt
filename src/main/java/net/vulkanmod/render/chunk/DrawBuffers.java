package net.vulkanmod.render.chunk;

import net.vulkanmod.render.VkBufferPointer;
import net.vulkanmod.render.chunk.build.UploadBuffer;
import net.vulkanmod.render.chunk.util.StaticQueue;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.memory.IndirectBuffer;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.ShaderManager;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;

import static net.vulkanmod.vulkan.util.VBOUtil.*;
import static org.lwjgl.system.Checks.check;
import static org.lwjgl.system.JNI.callPJPV;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkCmdBindDescriptorSets;

public class DrawBuffers {

    private static final int VERTEX_SIZE = ShaderManager.TERRAIN_VERTEX_FORMAT.getVertexSize();

    private static final int INDEX_SIZE = Short.BYTES;
    private final int index;
    private static final long aLong = MemoryUtil.nmemAlignedAlloc(8, 8);
    private static final long aLong2 = MemoryUtil.nmemAlignedAlloc(8, 8);

    static
    {
        VUtil.UNSAFE.putLong(aLong, virtualBufferVtx.bufferPointerSuperSet);
    }
    private boolean allocated = false;
    AreaBuffer vertexBuffer;
    AreaBuffer indexBuffer;

    final StaticQueue<DrawParameters> sectionQueue = new StaticQueue<>(512);
    final StaticQueue<DrawParameters> TsectionQueue = new StaticQueue<>(512);

    public DrawBuffers(int index) {
        this.index = index;

    }


    public void allocateBuffers() {
        this.vertexBuffer = new AreaBuffer(index, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, 2056192, VERTEX_SIZE);
        this.indexBuffer = new AreaBuffer(index, VK_BUFFER_USAGE_INDEX_BUFFER_BIT, 96384, INDEX_SIZE);

        this.allocated = true;
    }

    public DrawParameters upload(UploadBuffer buffer, DrawParameters drawParameters) {
        int vertexOffset = drawParameters.vertexOffset;
        int firstIndex = 0;

        if(!buffer.indexOnly)
        {
            translateVBO(buffer, drawParameters, drawParameters.indexCount);

            this.vertexBuffer.upload(buffer.getVertexBuffer(), drawParameters, virtualBufferVtx);
//            drawParameters.vertexOffset = drawParameters.vertexBufferSegment.getOffset() / VERTEX_SIZE;
            vertexOffset = drawParameters.vertexOffset / VERTEX_SIZE;

            //debug
//            if(drawParameters.vertexBufferSegment.getOffset() % VERTEX_SIZE != 0) {
//                throw new RuntimeException("misaligned vertex buffer");
//            }
        }

        if(!buffer.autoIndices) {
            this.indexBuffer.upload(buffer.getIndexBuffer(), drawParameters, virtualBufferIdx);
//            drawParameters.firstIndex = drawParameters.indexBufferSegment.getOffset() / INDEX_SIZE;
            firstIndex = drawParameters.firstIndex / INDEX_SIZE;
        }

//        AreaUploadManager.INSTANCE.enqueueParameterUpdate(
//                new ParametersUpdate(drawParameters, buffer.indexCount, firstIndex, vertexOffset));

        drawParameters.indexCount = buffer.indexCount;
        drawParameters.firstIndex = firstIndex;
        drawParameters.vertexOffset = vertexOffset;

//        Drawer.getInstance().getQuadsIndexBuffer().checkCapacity(buffer.indexCount * 2 / 3);

        buffer.release();

        return drawParameters;
    }

    private void translateVBO(UploadBuffer buffer, DrawParameters drawParameters, int indexCount) {
        final long addr = MemoryUtil.memAddress0(buffer.getVertexBuffer());
        final float v = (float) (drawParameters.xOffset - camX - originX);
        final float v1 = (float) (drawParameters.zOffset - camZ - originZ);
//        final float camX1 = (float) (v < (int) v ? v - 1 : v);
//        final float camZ1 = (float) (v1 < (int) v1 ? v1 - 1 : v1)
        final int camX1 = Math.round(v);
        final int camZ1 = Math.round(v1);

        for (int i = 0; i < buffer.getVertexBuffer().capacity(); i += VERTEX_SIZE) {
            VUtil.UNSAFE.putFloat(addr + i, (VUtil.UNSAFE.getFloat(addr + i) + (camX1)));
            VUtil.UNSAFE.putFloat(addr + i + 4, (VUtil.UNSAFE.getFloat(addr + i + 4) + drawParameters.yOffset));
            VUtil.UNSAFE.putFloat(addr + i + 8, (VUtil.UNSAFE.getFloat(addr + i + 8) + (camZ1)));
        }

    }

    public int buildDrawBatchesIndirect(IndirectBuffer indirectBuffer, TerrainRenderType renderType, double camX, double camY, double camZ) {
        int stride = 20;

        int drawCount;

        Pipeline pipeline = ShaderManager.getInstance().getTerrainShader();

        final int size = this.sectionQueue.size();
        if(size ==0) return 0;
        try (MemoryStack stack = MemoryStack.stackPush()) {

            renderType.setCutoutUniform();
//
//            {
//                vkCmdBindIndexBuffer(Drawer.getCommandBuffer(), Drawer.getInstance().getQuadIndexBuffer().getIndexBuffer().getId(), 0, VK_INDEX_TYPE_UINT16);
//            }

//        var iterator = queue.iterator(isTranslucent);

            drawCount = getDrawCount(renderType, stack.malloc(20 * size), indirectBuffer);

//            if (drawCount == 0) return 0;




//            pipeline.getManualUBO().setSrc(uboPtr, 16 * drawCount);

            vkCmdBindVertexBuffers(Drawer.getCommandBuffer(), 0, stack.longs(vertexBuffer.getId()), stack.longs(0));

            pipeline.bindDescriptorSets(Drawer.getCommandBuffer(), Drawer.getCurrentFrame());

            vkCmdDrawIndexedIndirect(Drawer.getCommandBuffer(), indirectBuffer.getId(), indirectBuffer.getOffset(), drawCount, stride);

//            fakeIndirectCmd(Drawer.getCommandBuffer(), indirectBuffer, drawCount, uboBuffer);

//        MemoryUtil.memFree(byteBuffer);
        }

        return drawCount;
    }

    private int getDrawCount(TerrainRenderType terrainRenderType, ByteBuffer byteBuffer, IndirectBuffer indirectBuffer) {
        long bufferPtr = MemoryUtil.memAddress0(byteBuffer);
        int drawCount = 0;
        for (DrawParameters drawParameters : this.sectionQueue) {
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
            MemoryUtil.memPutLong(ptr, (long) 1 <<32 | drawParameters.indexCount);
            MemoryUtil.memPutInt(ptr + 8, drawParameters.firstIndex);
            //            MemoryUtil.memPutInt(ptr + 12, drawParameters.vertexBufferSegment.getOffset() / VERTEX_SIZE);
            MemoryUtil.memPutInt(ptr + 12, drawParameters.vertexOffset);
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

    public void buildDrawBatchesDirect(TerrainRenderType renderType, double camX, double camY, double camZ) {
        renderType.setCutoutUniform();


            if (renderType == TerrainRenderType.TRANSLUCENT) {
                vkCmdBindIndexBuffer(Drawer.getCommandBuffer(), virtualBufferIdx.bufferPointerSuperSet, 0, VK_INDEX_TYPE_UINT16);
            }

        //        Drawer.getInstance().bindPipeline(pipeline);
        ShaderManager.shaderManager.terrainDirectShader.bindDescriptorSets(Drawer.getCommandBuffer(), Drawer.getCurrentFrame());

        for (DrawParameters drawParameters : renderType ==TerrainRenderType.TRANSLUCENT ? this.TsectionQueue : this.sectionQueue) {
//            DrawParameters drawParameters = section[renderType.ordinal()];

            VUtil.UNSAFE.putLong(aLong2, drawParameters.vertexOffset << 5);
            nvkCmdBindVertexBuffers(Drawer.getCommandBuffer(), 0, 1, aLong, aLong2);
//                callPJPV(commandBuffer.address(), pipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, 12, new float[]{(float) ((double) section.xOffset - camX), (float) ((double) section.yOffset - camY), (float) ((double) section.zOffset - camZ)}, commandBuffer.getCapabilities().vkCmdPushConstants);

            vkCmdDrawIndexed(Drawer.getCommandBuffer(), drawParameters.indexCount, 1, drawParameters.firstIndex, 0, 0);
            //                drawIndexedBindless(drawParameters);


        }


    }

    private void drawIndexedBindless(DrawParameters drawParameters) {
        vkCmdDrawIndexed(Drawer.getCommandBuffer(), drawParameters.indexCount, 1, drawParameters.firstIndex, drawParameters.vertexOffset, 0);

    }

    private void drawIndexed2(MemoryStack stack, DrawParameters drawParameters) {
        final int value = drawParameters.vertexOffset << 5;
        nvkCmdBindVertexBuffers(Drawer.getCommandBuffer(), 0, 1, (stack.npointer(vertexBuffer.getId())), (stack.npointer(value)));
//                callPJPV(commandBuffer.address(), pipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, 12, new float[]{(float) ((double) section.xOffset - camX), (float) ((double) section.yOffset - camY), (float) ((double) section.zOffset - camZ)}, commandBuffer.getCapabilities().vkCmdPushConstants);

        vkCmdDrawIndexed(Drawer.getCommandBuffer(), drawParameters.indexCount, 1, 0, 0, 0);
    }

    public void releaseBuffers() {
        if(!this.allocated)
            return;

        this.vertexBuffer.freeBuffer(virtualBufferVtx);
        this.indexBuffer.freeBuffer(virtualBufferIdx);

//        this.vertexBuffer = null;
//        this.indexBuffer = null;
        this.allocated = false;
    }

    public boolean isAllocated() {
        return allocated;
    }

    public void addSection(DrawParameters renderSection) {
        this.sectionQueue.add(renderSection);
    }

    public static class DrawParameters {
        public int size;
        public int i2;
        public VkBufferPointer vtx;
        //        public boolean status;
        private int xOffset;
        private int yOffset;
        private int zOffset;
        public final int index;
                public final TerrainRenderType r;
        int indexCount;
        int firstIndex;
        int vertexOffset;
        boolean ready = false;

        DrawParameters(int xOffset, int yOffset, int zOffset, int index, TerrainRenderType r) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.zOffset = zOffset;
//            this.r = translucent;
//            if(translucent==TerrainRenderType.TRANSLUCENT) {
//                indexBufferSegment = new AreaBuffer.Segment(-1);
//            }
            this.index = index;
            this.r = r;
        }

        public void reset(ChunkArea chunkArea) {
            this.indexCount = 0;
            this.firstIndex = 0;
            this.vertexOffset = 0;

            if(chunkArea != null && chunkArea.drawBuffers.isAllocated() && ready) {
                chunkArea.drawBuffers.vertexBuffer.setSegmentFree(this.vtx.index(), virtualBufferVtx);
               if(r==TerrainRenderType.TRANSLUCENT) chunkArea.drawBuffers.indexBuffer.setSegmentFree(this.vtx.index(), virtualBufferIdx);
//                chunkArea.drawBuffers.vertexBuffer.setSegmentFree(this.vertexBufferSegment);
            }
        }

        public void resetOrigin(int x, int y, int z) {
            this.xOffset=x;
            this.yOffset=y;
            this.zOffset=z;
        }
    }

    public record ParametersUpdate(DrawParameters drawParameters, int indexCount, int firstIndex, int vertexOffset) {

        public void setDrawParameters() {
            this.drawParameters.indexCount = indexCount;
            this.drawParameters.firstIndex = firstIndex;
            this.drawParameters.vertexOffset = vertexOffset;
//            this.drawParameters.ready = true;
        }
    }

}
