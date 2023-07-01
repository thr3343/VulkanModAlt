package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;

public class ShaderComponant {

    ShaderUtil.ShaderStage pipelineShaderStage;

    Uniform activeUniforms;

    VertexFormat inputVertexStage;

    UniformGrouping uniformGrouping = new UniformGrouping(32, 8, 0);

    //; Avoidng vulkan Specific features for now...
//    PCArraySet pcArraySet = new PCArraySet(StagderStage, shader dpendiences, )

    ExecutionDependencies shaderExecutionDependencies;
    ExecutionDependants shaderExecutionDependants;

//    ShaderPhase shaderPhase = new ShaderPhase(ShaderPhase.Shadervarient shadervarient, Framebuffer.AttachmentTypes... attachments)

    public ShaderComponant(ShaderUtil.ShaderStage shaderStage, String baseShaderPath) {

    }

    void extractUniforms()
    {

    }
}
