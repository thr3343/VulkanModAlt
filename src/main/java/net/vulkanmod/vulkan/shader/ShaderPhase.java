package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.Framebuffer;

public class ShaderPhase {


    public ShaderPhase(ShaderPhase.Shadervarient shadervarient, Framebuffer.AttachmentTypes... attachments) {
    }

    /*TODO
    *  CHEC if Multiple RenderPasses are Possible
    * if not will need to */
    enum Shadervarient
    {
        COLOUR,
        DEPTH,
        FORWARDS,
        BACKWARDS,

    }
//    RenderAccess renderAccess;
}
