package net.vulkanmod.render.chunk;

import net.minecraft.util.Mth;
import net.vulkanmod.render.virtualSegmentBuffer;
import net.vulkanmod.render.chunk.build.UploadBuffer;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.shader.ShaderManager;
import net.vulkanmod.vulkan.util.VBOUtil;
import net.vulkanmod.vulkan.util.VUtil;

import static net.vulkanmod.vulkan.util.VBOUtil.*;
import static org.lwjgl.system.Checks.check;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.vulkan.VK10.vkCmdBindDescriptorSets;

public class DrawBuffers {

    static final int VERTEX_SIZE = ShaderManager.TERRAIN_VERTEX_FORMAT.getVertexSize();
    private static final int INDEX_SIZE = Short.BYTES;
    private final int areaIndex;

    private boolean allocated = false;
//    AreaBuffer vertexBuffer;
//    AreaBuffer indexBuffer;


    //                callPJPV(commandBuffer.address(), pipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, 12, new float[]{(float) ((double) section.xOffset - camX), (float) ((double) section.yOffset - camY), (float) ((double) section.zOffset - camZ)}, commandBuffer.getCapabilities().vkCmdPushConstants);

    public DrawBuffers(int areaIndex) {
        this.areaIndex = areaIndex;

    }


    public void allocateBuffers() {
//        this.vertexBuffer = new AreaBuffer(index, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, 2056192, VERTEX_SIZE);
//        this.indexBuffer = new AreaBuffer(VK_BUFFER_USAGE_INDEX_BUFFER_BIT, 96384, INDEX_SIZE);

        this.allocated = true;
    }

    public void upload(UploadBuffer buffer, DrawParameters drawParameters, int xOffset, int yOffset, int zOffset, TerrainRenderType r) {

        if(!buffer.indexOnly)
        {
            translateVBO(buffer, buffer.indexCount, xOffset, yOffset, zOffset);

            drawParameters.vertexBufferSegment1=  VBOUtil.getCurrentUIndex(buffer.vertSize, r).configureVertexFormat(drawParameters, this.areaIndex, drawParameters.index, buffer, r);
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
        final long addr = (buffer.getVertexBuffer());
        final float v = (float) (xOffset - camX - originX);
        final float v1 = (float) (zOffset - camZ - originZ);
//        final float camX1 = (float) (v < (int) v ? v - 1 : v);
//        final float camZ1 = (float) (v1 < (int) v1 ? v1 - 1 : v1)
        final int camX1 = Mth.floor(v);
        final int camZ1 = Mth.floor(v1);

        for (int i = 0; i < buffer.vertSize; i += VERTEX_SIZE) {
            VUtil.UNSAFE.putFloat(addr + i, (VUtil.UNSAFE.getFloat(addr + i) + (camX1)));
            VUtil.UNSAFE.putFloat(addr + i + 4, (VUtil.UNSAFE.getFloat(addr + i + 4) + yOffset));
            VUtil.UNSAFE.putFloat(addr + i + 8, (VUtil.UNSAFE.getFloat(addr + i + 8) + (camZ1)));
        }

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
          VBOUtil.getCurrentUIndex().freeRange(this.areaIndex);
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



        public void reset() {
            if(initialised) {
                initialised = false;
                VBOUtil.getCurrentUIndex().freeBuff(this.vertexBufferSegment);
                this.vertexBufferSegment = null;
                this.vertexBufferSegment1 = null;
            }

        }
    }

}
