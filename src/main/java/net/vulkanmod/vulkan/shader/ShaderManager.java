package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.vertex.CustomVertexFormat;

public class ShaderManager {
    public static final VertexFormat TERRAIN_VERTEX_FORMAT = CustomVertexFormat.COMPRESSED_TERRAIN;
//    public static final VertexFormat TERRAIN_VERTEX_FORMAT = DefaultVertexFormat.BLOCK;

    public static ShaderManager shaderManager;
//    public Pipeline testShader2;

    public static void initShaderManager() {
        shaderManager = new ShaderManager();
    }

    public static ShaderManager getInstance() { return shaderManager; }

    Pipeline terrainShader;
    public Pipeline terrainDirectShader;

    public Pipeline testShader;
//    public Pipeline tstBlitShader;
//    public Pipeline tstBlitShader2;

    public ShaderManager() {
        createBasicPipelines();
    }

    public void createBasicPipelines() {
        this.terrainShader = createPipeline("terrain", "basic/%s/%s", 0);

        this.terrainDirectShader = createPipeline("terrain_direct", "basic/%s/%s", 0);

        this.testShader = createPipeline("test", "post/%s/%s", 0);
//        this.tstBlitShader = createPipeline("tstBlit", "post/test/tstBlit", 1);
//        this.tstBlitShader2 = createPipeline("tstBlit2", "post/test/tstBlit2", 2);
//        this.testShader2 = createPipeline("test2", "post/%s/%s", true);
    }

    private Pipeline createPipeline(String name, String baseDir, int b) {
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
