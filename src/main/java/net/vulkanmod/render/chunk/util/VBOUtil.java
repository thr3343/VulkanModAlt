package net.vulkanmod.render.chunk.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.render.VBO;
import net.vulkanmod.vulkan.VRenderSystem;

public class VBOUtil {

    public static ObjectArrayList<VBO> uniqueVBOs=new ObjectArrayList<>(1024);
    public static final ObjectArrayList<VBO> alphaVBOs=new ObjectArrayList<>(1024);
    public static double camX;
    //    private static double camY;
    public static double camZ;
    public static double originX;
    public static double originZ;
    public static double prevCamX;
    public static double prevCamZ;

    public static void removeVBO(VBO vbo) {
        if(vbo.translucentAlphaBlending) VBOUtil.alphaVBOs.remove(vbo);
        else VBOUtil.uniqueVBOs.remove(vbo);
    }

    public static void addVBO(VBO vbo) {
        if(vbo.translucentAlphaBlending) VBOUtil.alphaVBOs.add(vbo);
        else VBOUtil.uniqueVBOs.add(vbo);
    }

    public static void clearAll() {
        VBOUtil.alphaVBOs.clear();
        VBOUtil.uniqueVBOs.clear();
    }

    //Use seperate matrix to avoid Incorrect translations propagating to Particles/Lines Layer
    public static void updateVBOTranslationOffset(PoseStack poseStack, double camX_, double camY_, double camZ_, Matrix4f projection) {
        camX = camX_;

        camZ = camZ_;


        originX+= (prevCamX - camX);
        originZ+= (prevCamZ - camZ);
        poseStack.pushPose();
        {
            final Matrix4f pose = poseStack.last().pose();
            pose.multiplyWithTranslation((float) originX, (float) -camY_, (float) originZ);
            VRenderSystem.applyMVP(pose, projection);
        }
        poseStack.popPose();
        prevCamX= camX;
        prevCamZ= camZ;
    }
}
