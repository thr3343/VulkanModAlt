package net.vulkanmod.vulkan.shader.parser;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.layout.AlignedStruct;
import net.vulkanmod.vulkan.shader.layout.UBO;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;

public class UniformParser {

    private final GlslConverter converterInstance;
    private final StageUniforms[] stageUniforms = new StageUniforms[GlslConverter.ShaderStage.values().length];
    private StageUniforms currentUniforms;
    List<Uniform> globalUniforms = new ArrayList<>();

    private String type;
    private String name;

    private UBO ubo;
    private List<Pipeline.Sampler> samplers;

    public UniformParser(GlslConverter converterInstance) {
        this.converterInstance = converterInstance;

        for(int i = 0; i < this.stageUniforms.length; ++i) {
            this.stageUniforms[i] = new StageUniforms();
        }
    }

    public boolean parseToken(String token) {
        if(token.matches("uniform")) return false;

        if (this.type == null) {
            this.type = token;

        }
        else if (this.name == null) {
            token = removeSemicolon(token);

            this.name = token;

            //TODO check if already present
            Uniform uniform = new Uniform(this.type, this.name);
            if ("sampler2D".equals(this.type)) {
                if (!this.currentUniforms.samplers.contains(uniform))
                    this.currentUniforms.samplers.add(uniform);
            } else {
                if (!this.globalUniforms.contains(uniform))
                    this.globalUniforms.add(uniform);
            }

            this.resetSate();
            return true;
        }

        return false;
    }

    public void setCurrentUniforms(GlslConverter.ShaderStage shaderStage) {
        this.currentUniforms = stageUniforms[shaderStage.ordinal()];
    }

    private void resetSate() {
        this.type = null;
        this.name = null;
//        this.state = State.None;
    }

    public String createUniformsCode() {
        StringBuilder builder = new StringBuilder();

        this.ubo = this.createUBO();

        //hardcoded 0 binding as it should always be 0 in this case
        builder.append(String.format("layout(binding = %d) uniform UniformBufferObject {\n", 0));
        for(Uniform uniform : this.globalUniforms) {
            builder.append(String.format("%s %s;\n", uniform.type, uniform.name));
        }
        builder.append("};\n\n");

        return builder.toString();
    }

    public String createSamplersCode(GlslConverter.ShaderStage shaderStage) {
        StringBuilder builder = new StringBuilder();

        this.samplers = createSamplerList(shaderStage);

        for(Pipeline.Sampler sampler : this.samplers) {
            builder.append(String.format("layout(binding = %d) uniform %s %s;\n", sampler.binding(), sampler.type(), sampler.name()));
        }
        builder.append("\n");

        return builder.toString();
    }

    private UBO createUBO() {
        AlignedStruct.Builder builder = new AlignedStruct.Builder();

        for(Uniform uniform : this.globalUniforms) {
            builder.addFieldInfo(uniform.type, uniform.name);
        }

        //hardcoded 0 binding as it should always be 0 in this case
        return builder.buildUBO(0, Pipeline.Builder.getTypeFromString("all"));
    }

    private List<Pipeline.Sampler> createSamplerList(GlslConverter.ShaderStage shaderStage) {
        int currentLocation = 1;

        List<Pipeline.Sampler> samplers = new ObjectArrayList<>();

        for(StageUniforms stageUniforms : this.stageUniforms) {
            for(Uniform uniform : stageUniforms.samplers) {
                final int shaderStage1 = switch (shaderStage)
                {

                    case Vertex -> VK_SHADER_STAGE_VERTEX_BIT;
                    case Fragment -> VK_SHADER_STAGE_FRAGMENT_BIT;
                };
                samplers.add(new Pipeline.Sampler(currentLocation, shaderStage1, uniform.type, uniform.name));
                currentLocation++;
            }
        }

        return samplers;
    }

    public static String removeSemicolon(String s) {
        int last = s.length() - 1;
        if((s.charAt(last)) != ';' )
            throw new IllegalArgumentException("last char is not ;");
        return s.substring(0, last);
    }

    public UBO getUbo() {
        return this.ubo;
    }

    public List<Pipeline.Sampler> getSamplers() {
        return this.samplers;
    }

    public record Uniform(String type, String name) {}

    private static class StageUniforms {
        List<Uniform> samplers = new ArrayList<>();
    }

    enum State {
        Uniform,
        Sampler,
        None
    }
}
