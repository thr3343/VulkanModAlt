package net.vulkanmod.render.chunk;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.render.VBO;
import net.vulkanmod.vulkan.VRenderSystem;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Matrix4f;

import java.io.IOException;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.BLOCK;

//Use smaller class instead of WorldRenderer in case it helps GC/Heap fragmentation e.g.
public class VBOUtil {

    //TODO: Fix MipMaps Later...

    public static Matrix4f translationOffset;
    public static double camX;
    public static double camZ;
    public static double originX;
    public static double originZ;
    private static double prevCamX;
    private static double prevCamZ;



    public static void updateCamTranslation(PoseStack pose, double d, double e, double g, Matrix4f matrix4f) {
        VRenderSystem.applyMVP(pose.last().pose(), matrix4f);
        camX = d;
        camZ = g;


        originX += (prevCamX - camX);
        originZ += (prevCamZ - camZ);
        pose.pushPose();
        {
            translationOffset = pose.last().pose().translate((float) originX, (float) -e, (float) originZ);




        }
        pose.popPose();
//        pose.multiplyWithTranslation((float) originX, (float) -e, (float) originZ);
        prevCamX = camX;
        prevCamZ = camZ;
//        VRenderSystem.applyMVP(pose, matrix4f);


    }

}