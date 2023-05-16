package net.vulkanmod.render.chunk;

import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.render.chunk.build.UploadBuffer;
import net.vulkanmod.render.chunk.util.ResettableQueue;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.memory.IndirectBuffer;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.ShaderManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Iterator;

import static org.lwjgl.system.Checks.check;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.vulkan.VK10.*;

public class DrawBuffers {

    public static final int VERTEX_SIZE = ShaderManager.TERRAIN_VERTEX_FORMAT.getVertexSize();
    private static final int INDEX_SIZE = Short.BYTES;

    private boolean allocated = false;
    AreaBuffer vertexBuffer;
    AreaBuffer indexBuffer;
    private int prevSize;
    private int drawCount;
    private boolean updated=false;

    public void allocateBuffers() {
        //TODO size
        this.vertexBuffer = new AreaBuffer(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, 3500000, VERTEX_SIZE);
        this.indexBuffer = new AreaBuffer(VK_BUFFER_USAGE_INDEX_BUFFER_BIT, 1000000, INDEX_SIZE);

        this.allocated = true;
    }

    public DrawParameters upload(UploadBuffer buffer, DrawParameters drawParameters) {
        int vertexOffset = drawParameters.vertexOffset;
        int firstIndex = 0;

        if(!buffer.indexOnly) {
            this.vertexBuffer.upload(buffer.getVertexBuffer(), drawParameters.vertexBufferSegment);
//            drawParameters.vertexOffset = drawParameters.vertexBufferSegment.getOffset() / VERTEX_SIZE;
            vertexOffset = drawParameters.vertexBufferSegment.getOffset() / VERTEX_SIZE;

            //debug
//            if(drawParameters.vertexBufferSegment.getOffset() % VERTEX_SIZE != 0) {
//                throw new RuntimeException("misaligned vertex buffer");
//            }
        }

        if(!buffer.autoIndices) {
            this.indexBuffer.upload(buffer.getIndexBuffer(), drawParameters.indexBufferSegment);
//            drawParameters.firstIndex = drawParameters.indexBufferSegment.getOffset() / INDEX_SIZE;
            firstIndex = drawParameters.indexBufferSegment.getOffset() / INDEX_SIZE;
        }

//        AreaUploadManager.INSTANCE.enqueueParameterUpdate(
//                new ParametersUpdate(drawParameters, buffer.indexCount, firstIndex, vertexOffset));

        drawParameters.indexCount = buffer.indexCount;
        drawParameters.firstIndex = firstIndex;
        drawParameters.vertexOffset = vertexOffset;

        Drawer.getInstance().getQuadsIndexBuffer().checkCapacity(buffer.indexCount * 2 / 3);

        buffer.release();
        updated=true;
        return drawParameters;
    }

    public void buildDrawBatchesIndirect(IndirectBuffer indirectBuffer, ChunkArea chunkArea, RenderType renderType, double camX, double camY, double camZ) {



        Pipeline pipeline = ShaderManager.getInstance().getTerrainShader();

        try(MemoryStack stack = MemoryStack.stackPush()) {
            final int size = chunkArea.sectionQueue.size();
            final ByteBuffer byteBuffer = stack.calloc(20 * size);

            TerrainRenderType terrainRenderType = TerrainRenderType.get(renderType);
            terrainRenderType.setCutoutUniform();
            final boolean isTranslucent = terrainRenderType == TerrainRenderType.TRANSLUCENT;

            if (isTranslucent) {
                vkCmdBindIndexBuffer(Drawer.getCommandBuffer(), this.indexBuffer.getId(), 0, VK_INDEX_TYPE_UINT16);
            }


            getDrawCount(chunkArea, MemoryUtil.memAddress0(byteBuffer), terrainRenderType, isTranslucent);

            if (drawCount == 0) {
                return;
            }


            indirectBuffer.recordCopyCmd(byteBuffer.position(0));

//            pipeline.getManualUBO().setSrc(uboPtr, 16 * drawCount);

            nvkCmdBindVertexBuffers(Drawer.getCommandBuffer(), 0, 1, stack.npointer(vertexBuffer.getId()), stack.npointer(0));

//            pipeline.bindDescriptorSets(Drawer.getCommandBuffer(), WorldRenderer.getInstance().getUniformBuffers(), Drawer.getCurrentFrame());
            pipeline.bindDescriptorSets(Drawer.getCommandBuffer(), Drawer.getCurrentFrame());
            vkCmdDrawIndexedIndirect(Drawer.getCommandBuffer(), indirectBuffer.getId(), indirectBuffer.getOffset(), drawCount, 20);

//            fakeIndirectCmd(Drawer.getCommandBuffer(), indirectBuffer, drawCount, uboBuffer);

//        MemoryUtil.memFree(byteBuffer);

        }

    }

    public void getDrawCount(ChunkArea chunkArea, long bufferPtr, TerrainRenderType terrainRenderType, boolean isTranslucent) {
        drawCount=0;
        if(!chunkArea.drawBuffers.updated) return;
        for (final Iterator<RenderSection> iter = chunkArea.sectionQueue.iterator(isTranslucent); iter.hasNext(); ) {

            final DrawParameters drawParameters = iter.next().getDrawParameters(terrainRenderType);

            if (drawParameters.indexCount == 0) {
                continue;
            }

            //TODO
            if (!drawParameters.ready && drawParameters.vertexBufferSegment.getOffset() != -1) {
                if (!drawParameters.vertexBufferSegment.isReady())
                    continue;
                drawParameters.ready = true;
            }

            final long ptr = bufferPtr + (this.drawCount * 20L);
            MemoryUtil.memPutInt(ptr, drawParameters.indexCount);
            MemoryUtil.memPutInt(ptr + 4, 1);
            MemoryUtil.memPutInt(ptr + 8, drawParameters.firstIndex);
//            MemoryUtil.memPutInt(ptr + 12, drawParameters.vertexBufferSegment.getOffset() / VERTEX_SIZE);
            MemoryUtil.memPutInt(ptr + 12, drawParameters.vertexOffset);
//            MemoryUtil.memPutInt(ptr + 12, drawParameters.vertexBufferSegment.getOffset());
            MemoryUtil.memPutInt(ptr + 16, 0);

//            ptr = uboPtr + (drawCount * 16L);
//            MemoryUtil.memPutFloat(ptr, (float)((double) section.xOffset - camX));
//            MemoryUtil.memPutFloat(ptr + 4, (float)((double) section.yOffset - camY));
//            MemoryUtil.memPutFloat(ptr + 8, (float)((double) section.zOffset - camZ));

            this.drawCount++;
        }
        chunkArea.drawBuffers.updated=false;
    }

    public void buildDrawBatchesDirect(ChunkArea chunkArea, RenderType renderType, double camX, double camY, double camZ) {
        TerrainRenderType terrainRenderType = TerrainRenderType.get(renderType);
        terrainRenderType.setCutoutUniform();

        ResettableQueue<RenderSection> queue = chunkArea.sectionQueue;
        final long bufferPtr;
        int drawCount = 0;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            nvkCmdBindVertexBuffers(Drawer.getCommandBuffer(), 0, 1,  stack.npointer(vertexBuffer.getId()), stack.npointer(0));

            bufferPtr = stack.ncalloc(8, queue.size(), 24);
        }

        if(terrainRenderType == TerrainRenderType.TRANSLUCENT) {
            vkCmdBindIndexBuffer(Drawer.getCommandBuffer(), this.indexBuffer.getId(), 0, VK_INDEX_TYPE_UINT16);
        }

        Pipeline pipeline = ShaderManager.shaderManager.terrainDirectShader;
//        Drawer.getInstance().bindPipeline(pipeline);
        pipeline.bindDescriptorSets(Drawer.getCommandBuffer(), Drawer.getCurrentFrame());


        for (final Iterator<RenderSection> iterator = queue.iterator(terrainRenderType == TerrainRenderType.TRANSLUCENT); iterator.hasNext(); ) {

            DrawParameters drawParameters = iterator.next().getDrawParameters(terrainRenderType);

            long ptr = bufferPtr + (drawCount * 24L);
            MemoryUtil.memPutInt(ptr, drawParameters.indexCount);
            MemoryUtil.memPutInt(ptr + 4, drawParameters.firstIndex);
            MemoryUtil.memPutInt(ptr + 8, drawParameters.vertexOffset);

            drawCount++;

        }

        if(drawCount > 0) {
            long offset;
            int indexCount;
            for(int i = 0; i < drawCount; ++i) {

                offset = i * 24 + bufferPtr;

                indexCount    = MemoryUtil.memGetInt(offset);

                if(indexCount == 0) {
                    continue;
                }

//                nvkCmdPushConstants(commandBuffer, pipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, 12, offset + 12);

                vkCmdDrawIndexed(Drawer.getCommandBuffer(), indexCount, 1, MemoryUtil.memGetInt(offset + 4), MemoryUtil.memGetInt(offset + 8), 0);
            }
        }


    }

    public void releaseBuffers() {
        if(!this.allocated)
            return;

        this.vertexBuffer.freeBuffer();
        this.indexBuffer.freeBuffer();

        this.vertexBuffer = null;
        this.indexBuffer = null;
        this.allocated = false;
    }

    public boolean isAllocated() {
        return allocated;
    }

    public static class DrawParameters {
        int indexCount;
        int firstIndex;
        int vertexOffset;
        AreaBuffer.Segment vertexBufferSegment = new AreaBuffer.Segment();
        AreaBuffer.Segment indexBufferSegment;
        boolean ready = false;

        DrawParameters(boolean translucent) {
            if(translucent) {
                indexBufferSegment = new AreaBuffer.Segment();
            }
        }

        public void reset(ChunkArea chunkArea) {
            this.indexCount = 0;
            this.firstIndex = 0;
            this.vertexOffset = 0;

            int segmentOffset = this.vertexBufferSegment.getOffset();
            if(chunkArea != null && chunkArea.drawBuffers.isAllocated() && segmentOffset != -1) {
//                this.chunkArea.drawBuffers.vertexBuffer.setSegmentFree(segmentOffset);
                chunkArea.drawBuffers.vertexBuffer.setSegmentFree(this.vertexBufferSegment);
            }
        }
    }

    public record ParametersUpdate(DrawParameters drawParameters, int indexCount, int firstIndex, int vertexOffset) {

        public void setDrawParameters() {
            this.drawParameters.indexCount = indexCount;
            this.drawParameters.firstIndex = firstIndex;
            this.drawParameters.vertexOffset = vertexOffset;
            this.drawParameters.ready = true;
        }
    }

}
