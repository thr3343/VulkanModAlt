package net.vulkanmod.vulkan.shader;

public class ShaderModule {
    /*
    *TODO: Alias for ShaderPack
    * term for mODule utilsied insteasd of Pack due to the emhapsi on ma ny ShaderPacks using componats.being aggreates in nature rtaher than singular shaders
    * teh VUlkan Spec refers to compiled SPIRV Modules as VkShaderModules as well
    *ss */
    //TODO: check Pipeline Integration: Might be abke to get way wity using VulkaNMod Pipleiens, instead of neeidng to cerate fully customShaderPipelines
    public ShaderModule(String loadedShaderModule) {
        loadShaderModule(loadedShaderModule);
    }

    private void loadShaderModule(String loadedShaderModule) {

    }

    void determineActiveShaderModules()
    {
//        loadShaderModule()
    }
}
