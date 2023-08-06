package net.vulkanmod.vulkan.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.vulkanmod.render.chunk.UberBufferSet;
import net.vulkanmod.render.virtualSegmentBuffer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.IndirectBuffer;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import static net.vulkanmod.render.vertex.TerrainRenderType.TRANSLUCENT;

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
    public static  final IndirectBuffer SCmdAlloc = new IndirectBuffer(1024*20, MemoryTypes.GPU_MEM);
    public static  final IndirectBuffer TCmdAlloc = new IndirectBuffer(1024*20, MemoryTypes.GPU_MEM);
    //    static {
//        try {
//            test = new ShaderInstance(Minecraft.getInstance().getResourceManager(), "rendertype_cutout", BLOCK);
//            test2 = new ShaderInstance(Minecraft.getInstance().getResourceManager(), "rendertype_translucent", BLOCK);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

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
