package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.render.vertex.CustomVertexFormat;

import static net.vulkanmod.vulkan.shader.ShaderSPIRVUtils.compileShaderFile;

public class ShaderManager {
    public static final VertexFormat TERRAIN_VERTEX_FORMAT = CustomVertexFormat.COMPRESSED_TERRAIN;
//    public static final VertexFormat TERRAIN_VERTEX_FORMAT = DefaultVertexFormat.BLOCK;

    public static ShaderManager shaderManager;
    private final ShaderInstance2 vertShaderSPIRV1 = new ShaderInstance2(ShaderSPIRVUtils.ShaderKind.VERTEX_SHADER, "basic/terrain_direct/terrain_direct" + ".vsh");
    private final ShaderInstance2 fragShaderSPIRV1 = new ShaderInstance2(ShaderSPIRVUtils.ShaderKind.FRAGMENT_SHADER, "basic/terrain_direct/terrain_direct" + ".fsh");
    private final ShaderInstance2 fragShaderSPIRV2 = new ShaderInstance2(ShaderSPIRVUtils.ShaderKind.FRAGMENT_SHADER, "basic/terrain_direct/terrain_direct2" + ".fsh");

    public static void initShaderManager() {
        shaderManager = new ShaderManager();
    }

    public static ShaderManager getInstance() { return shaderManager; }

//    Pipeline terrainShader;
    public final Pipeline terrainDirectShader;
    public final Pipeline terrainDirectShader2;

    public ShaderManager() {
        //        this.terrainShader = createPipeline("terrain");

        this.terrainDirectShader = createPipeline("terrain_direct", vertShaderSPIRV1.shaderMdle, fragShaderSPIRV1.shaderMdle);
        this.terrainDirectShader2 = createPipeline("terrain_direct", vertShaderSPIRV1.shaderMdle, fragShaderSPIRV2.shaderMdle);
    }

    private Pipeline createPipeline(String name, ShaderSPIRVUtils.SPIRV vertShaderSPIRV11, ShaderSPIRVUtils.SPIRV fragShaderSPIRV11) {
        String path = String.format("basic/%s/%s", name, name);

        Pipeline.Builder pipelineBuilder = new Pipeline.Builder(TERRAIN_VERTEX_FORMAT, path);
        pipelineBuilder.parseBindingsJSON();
        pipelineBuilder.compileShaders2(vertShaderSPIRV11, fragShaderSPIRV11);
        return pipelineBuilder.createPipeline(name);
    }

    public void destroyPipelines() {
//        this.terrainShader.cleanUp();
        this.terrainDirectShader.cleanUp();
        this.terrainDirectShader2.cleanUp();

    }
}
