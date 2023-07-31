package net.vulkanmod.render.chunk;

import net.vulkanmod.render.chunk.build.UploadBuffer;
import net.vulkanmod.render.virtualSegmentBuffer;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.StagingBuffer;
import net.vulkanmod.vulkan.shader.ShaderManager;
import net.vulkanmod.vulkan.util.VBOUtil;
import net.vulkanmod.vulkan.util.VUtil;
import org.apache.commons.lang3.Validate;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static net.vulkanmod.vulkan.queue.Queues.TransferQueue;
import static net.vulkanmod.vulkan.util.VBOUtil.*;

public class DrawBuffers {

    static final int VERTEX_SIZE = ShaderManager.TERRAIN_VERTEX_FORMAT.getVertexSize();
    private static final int INDEX_SIZE = Short.BYTES;
    private final int areaIndex;

    private boolean allocated = false;
//    AreaBuffer vertexBuffer;
//    AreaBuffer indexBuffer;

//    final StaticQueue<VkDrawIndexedIndirectCommand2> sectionQueue2 = new StaticQueue<>(512);



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

    private VkDrawIndexedIndirectCommand2 configureVertexFormat(DrawParameters drawParameters, int index, UploadBuffer parameters) {
//        boolean bl = !parameters.format().equals(this.vertexFormat);
        ByteBuffer data = parameters.getVertexBuffer();
        final int remaining = data.remaining();
        Validate.isTrue(remaining % VERTEX_SIZE ==0);
        if(drawParameters.vertexBufferSegment == null || !VBOUtil.virtualBufferVtx.isAlreadyLoaded(drawParameters.vertexBufferSegment))
        {
            drawParameters.vertexBufferSegment = VBOUtil.virtualBufferVtx.allocSubSection(this.areaIndex, index, remaining);
        }

        AreaUploadManager.INSTANCE.uploadAsync(drawParameters.vertexBufferSegment, virtualBufferVtx.bufferPointerSuperSet, virtualBufferVtx.size_t, drawParameters.vertexBufferSegment.i2(), remaining, data);
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
//        this.sectionQueue.clear();
//        this.TsectionQueue.clear();

//        this.vertexBuffer.freeBuffer();
//        this.indexBuffer.freeBuffer();

//        this.vertexBuffer = null;
//        this.indexBuffer = null;
        this.allocated = false;
    }

    public boolean isAllocated() {
        return allocated;
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
