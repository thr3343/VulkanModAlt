package net.vulkanmod.vulkan.shader.SPVTransformer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.NativeType;

import static org.lwjgl.util.spvc.Spvc.spvc_compiler_create_shader_resources;

public class SPVUtilAsm
{
    private static final long compiler;

    static{
        PointerBuffer refRes;
        spvc_compiler_create_shader_resources(compiler, refRes)

    }




    enum Decoration{
        Uniform,
        PC,
        Sampler,
        InputAttach,

    }

    record UnfiformOp(String opName, String OpTypeStruct, int type, int size_t, int bindingSlot)
    {

    }
}
