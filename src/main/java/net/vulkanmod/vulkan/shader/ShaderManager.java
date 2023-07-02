package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.vertex.CustomVertexFormat;

public class ShaderManager {
    public static final VertexFormat TERRAIN_VERTEX_FORMAT = CustomVertexFormat.COMPRESSED_TERRAIN;
//    public static final VertexFormat TERRAIN_VERTEX_FORMAT = DefaultVertexFormat.BLOCK;

    public static ShaderManager shaderManager;

    public static void initShaderManager() {
        shaderManager = new ShaderManager();
    }

    public static ShaderManager getInstance() { return shaderManager; }

    Pipeline terrainShader;
    public Pipeline terrainDirectShader;

//    public Pipeline testShader;

    public ShaderManager() {
        createBasicPipelines();
    }

    private void createBasicPipelines() {
        this.terrainShader = createPipeline("terrain", "basic/%s/%s", false);

        this.terrainDirectShader = createPipeline("terrain_direct", "basic/%s/%s", false);

//        this.testShader = createPipeline("test", "post/%s/%s", true);
    }

    private Pipeline createPipeline(String name, String baseDir, boolean b) {
        String path = String.format(baseDir, name, name);

        Pipeline.Builder pipelineBuilder = new Pipeline.Builder(TERRAIN_VERTEX_FORMAT, path);
        pipelineBuilder.parseBindingsJSON();
        pipelineBuilder.compileShaders();
        return pipelineBuilder.createPipeline(b);
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
