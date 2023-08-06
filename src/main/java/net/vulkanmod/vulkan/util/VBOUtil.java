package net.vulkanmod.vulkan.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.vulkanmod.render.chunk.UberBufferSet;
import net.vulkanmod.render.virtualSegmentBuffer;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.StagingBuffer;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryDedicatedAllocateInfo;

import static net.vulkanmod.render.chunk.UberBufferSet.drawCommands;
import static net.vulkanmod.render.vertex.TerrainRenderType.TRANSLUCENT;
import static net.vulkanmod.vulkan.queue.Queues.TransferQueue;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memByteBuffer;
import static org.lwjgl.vulkan.VK10.*;

//Use smaller class instead of WorldRenderer in case it helps GC/Heap fragmentation e.g.
public class VBOUtil {

    public static final long vOffset = MemoryUtil.nmemAlignedAlloc(8, 8);
    public static final long functionAddress = Vulkan.getDevice().getCapabilities().vkCmdBindVertexBuffers;
    public static final long functionAddress1 = Vulkan.getDevice().getCapabilities().vkCmdDrawIndexed;
    public static final long functionAddress2 = Vulkan.getDevice().getCapabilities().vkCmdDrawIndexedIndirect;

    //    public static final VirtualBuffer virtualBufferVtx2=new VirtualBuffer(536870912, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);

    public static Matrix4f translationOffset;
    public static double camX;
    public static double camZ;
    public static double originX;
    public static double originZ;
    private static double prevCamX;
    private static double prevCamZ;

//    private static final ShaderInstance test;
//    private static final ShaderInstance test2;
//    private static final VertexFormatElement ELEMENT_UV2 = new VertexFormatElement(2,VertexFormatElement.Type.USHORT, VertexFormatElement.Usage.UV, 2);
//    private static final VertexFormat BLOCK2 = new VertexFormat(ImmutableMap.of("Position",ELEMENT_POSITION, "Color",ELEMENT_COLOR, "UV0",ELEMENT_UV0, "UV2",ELEMENT_UV2, "Normal",ELEMENT_NORMAL, "Padding",ELEMENT_PADDING));
    public static  long drawCmdBuffer;
    private static  long drawCmdAlloc;
    private static final int size_t=0x10000;
//    static {
//        try {
//            test = new ShaderInstance(Minecraft.getInstance().getResourceManager(), "rendertype_cutout", BLOCK);
//            test2 = new ShaderInstance(Minecraft.getInstance().getResourceManager(), "rendertype_translucent", BLOCK);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
    static
    {
        setupIndirectBuffer();
    }

    static void setupIndirectBuffer()
    {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pBuffer = stack.pointers(drawCmdBuffer);
            PointerBuffer pAllocation = stack.pointers(drawCmdAlloc);

            extracted(stack, pBuffer, pAllocation);

            drawCmdBuffer = pBuffer.get(0);
            drawCmdAlloc = pAllocation.get(0);

        }
    }

    private static void extracted(MemoryStack stack, PointerBuffer pBuffer, PointerBuffer pAllocation) {


        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.callocStack(stack);
        bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
        bufferInfo.size(size_t);
        bufferInfo.usage(VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT  | VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT);
        bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);


        nvkCreateBuffer(Vulkan.getDevice(), bufferInfo.address(), NULL, pBuffer.address());

        VkMemoryDedicatedAllocateInfo vkMemoryDedicatedAllocateInfo = VkMemoryDedicatedAllocateInfo.calloc(stack)
                .buffer(pBuffer.get(0))
                .sType$Default();
//
        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.callocStack(stack);
        allocInfo.sType$Default();
        allocInfo.pNext(vkMemoryDedicatedAllocateInfo.address());
        allocInfo.allocationSize(size_t);
        allocInfo.memoryTypeIndex(0);
        nvkAllocateMemory(Vulkan.getDevice(), allocInfo.address(), NULL, pAllocation.address0());

//            allocMem(stack, pBuffer, pAllocation);
        vkBindBufferMemory(Vulkan.getDevice(), pBuffer.get(0), pAllocation.get(0), 0);
    }

    public static void AllocIndirectCmds()
    {

        if(drawCommands.position()==0) return;

        StagingBuffer stagingVkBuffer = Vulkan.getStagingBuffer(Drawer.getCurrentFrame());

        VUtil.memcpy2(memByteBuffer(stagingVkBuffer.data.get(0), stagingVkBuffer.getBufferSize()), drawCommands.address0(), stagingVkBuffer.getUsedBytes(), drawCommands.position()*20);

        stagingVkBuffer.offset = stagingVkBuffer.usedBytes;
        stagingVkBuffer.usedBytes += drawCommands.position()*20;

        TransferQueue.uploadBufferImmediate(stagingVkBuffer.getId(), stagingVkBuffer.getOffset(), drawCmdBuffer, 0, drawCommands.position() *20L);
    }

    public static void updateCamTranslation(PoseStack pose, double d, double e, double g, Matrix4f matrix4f)
    {
        VRenderSystem.applyMVP(pose.last().pose(), matrix4f);
        camX =d;
        camZ =g;


        originX+= (prevCamX - camX);
        originZ+= (prevCamZ - camZ);
        pose.pushPose();
        {
            translationOffset= pose.last().pose();
            translationOffset.translate((float) originX, (float) -e, (float) originZ);
        }
        pose.popPose();
//        pose.multiplyWithTranslation((float) originX, (float) -e, (float) originZ);
        prevCamX= camX;
        prevCamZ= camZ;
//        VRenderSystem.applyMVP(pose, matrix4f);




    }

    public static void freeBuff(virtualSegmentBuffer vertexBufferSegment) {
        (vertexBufferSegment.r()==TRANSLUCENT ? UberBufferSet.TvirtualBufferVtx : UberBufferSet.virtualBufferVtx).addFreeableRange(vertexBufferSegment);
    }

//    public static void removeVBO(VBO vbo) {
//        //            case CUTOUT_MIPPED -> cutoutMippedChunks.remove(vbo);
//        (vbo.type == RenderTypes.CUTOUT?cutoutChunks:translucentChunks).remove(vbo);
//
//    }
//
//    @NotNull
//    public static RenderTypes getLayer(RenderType renderType) {
//        return switch (renderType.name) {
//            case "cutout","cutout_mipped" -> RenderTypes.CUTOUT;
//            case "translucent" -> RenderTypes.TRANSLUCENT;
//            default -> throw new IllegalStateException("Bad RenderType: "+renderType.name);
//        };
//    }
//
//    public static RenderTypes getLayer(String type) {
//        return switch (type) {
//            case "cutout" -> RenderTypes.CUTOUT;
//            case "translucent" -> RenderTypes.TRANSLUCENT;
//            default -> throw new IllegalStateException("Bad RenderType: "+type);
//        };
//    }
//
//    public static RenderType getLayerToType(RenderTypes renderType2) {
//        return switch (renderType2) {
//            case CUTOUT-> RenderType.CUTOUT;
//            case TRANSLUCENT-> RenderType.TRANSLUCENT;
//        };
//    }
//
//    public enum RenderTypes
//    {
////        CUTOUT_MIPPED(RenderStateShard.ShaderStateShard.RENDERTYPE_CUTOUT_MIPPED_SHADER),
//        CUTOUT(test, "cutout"),
//        TRANSLUCENT(test2, "translucent");
//
//        public final String name;
//
//        public final ShaderInstance shader;
//
//        RenderTypes(ShaderInstance solid, String name) {
//            this.name = name;
//            this.shader = solid;
//        };
//    }
}
