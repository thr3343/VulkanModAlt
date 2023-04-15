package net.vulkanmod.render.chunk.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.render.VBO;
import net.vulkanmod.render.VirtualBuffer;
import net.vulkanmod.render.VkBufferPointer;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.StagingBuffer;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.vulkan.VK10;

import java.nio.ByteBuffer;

//Use smaller class instead of WorldRenderer in case it helps GC/Heap fragmentation e.g.
public class VBOUtil {
    public static final ObjectArrayList<VBO> cutoutChunks = new ObjectArrayList<>(1024);
//    public static final ObjectArrayList<VBO> translucentChunks = new ObjectArrayList<>(1024);

    public static final VirtualBuffer UberVertexBuffer = new VirtualBuffer(536870912, VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
//    public static final VirtualBuffer transVertexBuffer = new VirtualBuffer(536870912, VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
    public static Matrix4f translationOffset;
    public static double camX;
    public static double camZ;
    public static double originX;
    public static double originZ;
    private static double prevCamX;
    private static double prevCamZ;

    private static final boolean isMipped= WorldRenderer.getInstance().minecraft.options.mipmapLevels().get()==0;

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
            translationOffset.multiplyWithTranslation((float) originX, (float) -e, (float) originZ);
        }
        pose.popPose();
//        pose.multiplyWithTranslation((float) originX, (float) -e, (float) originZ);
        prevCamX= camX;
        prevCamZ= camZ;
//        VRenderSystem.applyMVP(pose, matrix4f);




    }

    public static void removeVBO(VBO vbo) {

        cutoutChunks.remove(vbo);
        UberVertexBuffer.addFreeableRange(vbo.index, vbo.fakeVerBufferPointer);

    }

    @NotNull
    public static RenderTypes getLayer(RenderType renderType) {
        return switch (renderType.name) {
            case "cutout", "cutout_mipped" -> RenderTypes.CUTOUT;
            case "translucent" -> RenderTypes.TRANSLUCENT;
            default -> throw new IllegalStateException("Bad RenderType: "+renderType.name);
        };
    }

    public static RenderTypes getLayer(String type) {
        return switch (type) {
            case "cutout", "cutout_mipped" -> RenderTypes.CUTOUT;
            case "translucent" -> RenderTypes.TRANSLUCENT;
            default -> throw new IllegalStateException("Bad RenderType: "+type);
        };
    }

    public static boolean isAlreadyLoaded(int index, RenderTypes vbo, int size) {
        if(vbo==RenderTypes.TRANSLUCENT) return true;
        return UberVertexBuffer.isAlreadyLoaded(index, size);
    }

    public static VkBufferPointer addSubAlloc(int index, int size, RenderTypes type) {
        return UberVertexBuffer.addSubIncr(index, size);
    }

    public static void upload(VkBufferPointer fakeVertexBuffer, int size, ByteBuffer data, RenderTypes type) {
        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer(Drawer.getCurrentFrame());
        stagingBuffer.copyBuffer(size, data);

        Vulkan.copyStagingtoLocalBuffer(stagingBuffer.getId(), stagingBuffer.offset, UberVertexBuffer.bufferPointerSuperSet, fakeVertexBuffer.i2(), fakeVertexBuffer.size_t());
    }

    public enum RenderTypes
    {
        CUTOUT(RenderStateShard.ShaderStateShard.RENDERTYPE_CUTOUT_SHADER),
        TRANSLUCENT(RenderStateShard.ShaderStateShard.RENDERTYPE_TRANSLUCENT_SHADER);

        public final String name;

        public final ShaderInstance shader;

        RenderTypes(RenderStateShard.ShaderStateShard solid) {

            this.name = solid.name;
            this.shader = solid.shader.get().get();
        }
    }
}
