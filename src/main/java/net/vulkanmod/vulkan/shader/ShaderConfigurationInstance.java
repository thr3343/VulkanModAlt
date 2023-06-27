package net.vulkanmod.vulkan.shader;

import java.util.EnumSet;

public class ShaderConfigurationInstance {
    final EnumSet<ShaderUtil.ShaderStage> shaderUtils; //TODO: All (Currently) active ShaderStages

    ShaderComponant shaderComponant;

    int[] shaderStageAbtrastedIterator; //abstrast shader stages by aliaisngabstracing idncies to specific Shader stages, which will iterate by one each time a specific stage stage is used in order...

    //Initla stage will eitehr alias to the first stage of the ShaderCinfig congiurtaions atte and.or referr to a common "base" stage commong. ubqotitous.inereht to all stages )i;.e. Compisuite or Basic Stage e.g.)
    //helps to ensure that each shaderOCnfigurtinstance always has at leats one ShadferStage
    public ShaderConfigurationInstance(ShaderUtil.ShaderStage initialStage, ShaderUtil.ShaderStage... shaderUtils) {
        this.shaderUtils = EnumSet.of(initialStage, shaderUtils);
    }
}


