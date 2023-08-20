package net.vulkanmod.vulkan.shader;

import static net.vulkanmod.vulkan.shader.ShaderSPIRVUtils.compileShaderFile;

public class ShaderInstance2
{


    final ShaderSPIRVUtils.SPIRV shaderMdle;
    public ShaderInstance2(ShaderSPIRVUtils.ShaderKind shahderKind, String shaderFile)
    {
        shaderMdle = compileShaderFile(shaderFile, shahderKind);
    }
}
