package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;
import jdk.incubator.vector.ShortVector;
import net.vulkanmod.vulkan.Framebuffer;

public class ShaderComponant {

    ShaderUtil.ShaderStage pipelineShaderStage;

    Uniform activeUniforms;

    VertexFormat inputVertexStage;

    UniformGrouping uniformGrouping = new UniformGrouping(32, 8, 0);

    //; Avoidng vulkan Specific features for now...
//    PCArraySet pcArraySet = new PCArraySet(StagderStage, shader dpendiences, )

    ExecutionDependencies shaderExecutionDependencies;
    ExecutionDependants shaderExecutionDependants;

    ShaderPhase shaderPhase = new ShaderPhase(ShaderPhase.Shadervarient shadervarient, Framebuffer.AttachmentTypes... attachments)

    public ShaderComponant(ShaderStage shaderStage, String shaderPath, ShaderPhase.Shadervarient shadervarient,  {
    }
}
