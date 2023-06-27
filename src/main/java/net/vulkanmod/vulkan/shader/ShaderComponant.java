package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;

public class ShaderComponant {

    ShaderUtil.ShaderStage pipelineShaderStage;

    Uniform activeUniforms;

    VertexFormat inputVertexStage;

    UniformGrouping uniformGrouping = new UniformGrouping(32, 8, 0);

    //; Avoidng vulkan Specific features for now...

}
