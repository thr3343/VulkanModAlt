package net.vulkanmod.vulkan.shader;

import org.lwjgl.util.shaderc.ShadercIncludeResultRelease;

public class ShaderReleaser extends ShadercIncludeResultRelease {
    @Override
    public void invoke(long user_data, long include_result) {
        //TODO:MAybe dump Shader Compiled Binaries here to a .Misc Diretcory to allow easy caching.recompilation...
    }
}
