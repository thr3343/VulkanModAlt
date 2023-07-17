package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.Initializer;

import static net.vulkanmod.vulkan.shader.ShaderSPIRVUtils.compileShaderFile;

public class ShaderManager {
//    public static final VertexFormat TERRAIN_VERTEX_FORMAT = CustomVertexFormat.COMPRESSED_TERRAIN;
    public static final VertexFormat TERRAIN_VERTEX_FORMAT = DefaultVertexFormat.BLOCK;

    public static ShaderManager shaderManager;
    private final ShaderSPIRVUtils.SPIRV vertShaderSPIRV1 = compileShaderFile("basic/terrain_direct/terrain_direct" + ".vsh", ShaderSPIRVUtils.ShaderKind.VERTEX_SHADER);
    private final ShaderSPIRVUtils.SPIRV fragShaderSPIRV1 = compileShaderFile("basic/terrain_direct/terrain_direct" + ".fsh", ShaderSPIRVUtils.ShaderKind.FRAGMENT_SHADER);

    public static void initShaderManager() {
        shaderManager = new ShaderManager();
    }

    public static ShaderManager getInstance() { return shaderManager; }

    Pipeline terrainShader;
    public Pipeline terrainDirectShader;

    public ShaderManager() {
        createBasicPipelines();
    }

    private void createBasicPipelines() {
        this.terrainShader = createPipeline("terrain");

        this.terrainDirectShader = createPipeline("terrain_direct");
    }

    private Pipeline createPipeline(String name) {
        String path = String.format("basic/%s/%s", name, name);

        Pipeline.Builder pipelineBuilder = new Pipeline.Builder(TERRAIN_VERTEX_FORMAT, path);
        pipelineBuilder.parseBindingsJSON();
        pipelineBuilder.compileShaders2(vertShaderSPIRV1, fragShaderSPIRV1);
        return pipelineBuilder.createPipeline();
    }

    public Pipeline getTerrainShader() {
        if(Initializer.CONFIG.indirectDraw) {
            return this.terrainShader;
        }
        else {
            return this.terrainDirectShader;
        }

    }

    public void destroyPipelines() {
        this.terrainShader.cleanUp();
        this.terrainDirectShader.cleanUp();
    }
}
