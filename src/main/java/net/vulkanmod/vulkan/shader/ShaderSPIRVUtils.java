package net.vulkanmod.vulkan.shader;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import org.lwjgl.util.shaderc.*;
import org.lwjgl.vulkan.VK12;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.shaderc.Shaderc.*;

public class ShaderSPIRVUtils {

    static final long compiler;

    static final long options;
    private static final ShaderIncluder SHADER_INCLUDER = new ShaderIncluder();
    private static final ShaderReleaser SHADER_RELEASER = new ShaderReleaser();

    private static final long pUserData = 0;

    static {

        compiler = shaderc_compiler_initialize();

        if(compiler == NULL) {
            throw new RuntimeException("Failed to create shader compiler");
        }

        options = shaderc_compile_options_initialize();

        if(options == NULL) {
            throw new RuntimeException("Failed to create compiler options");
        }

//        shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_performance);
        shaderc_compile_options_set_target_env(options, shaderc_env_version_vulkan_1_2, VK12.VK_API_VERSION_1_2);
        shaderc_compile_options_set_include_callbacks(options, SHADER_INCLUDER, SHADER_RELEASER, pUserData);

    }


    public static SPIRV compileShaderFile(String shaderFile, ShaderKind shaderKind) {
        //TODO name out
        String path = ShaderSPIRVUtils.class.getResource("/assets/vulkanmod/shaders/" + shaderFile).toExternalForm();
        return compileShaderAbsoluteFile(path, shaderKind);
    }

    public static SPIRV compileShaderAbsoluteFile(String shaderFile, ShaderKind shaderKind) {
        try {
            String source = new String(Files.readAllBytes(Paths.get(new URI(shaderFile))));
            return compileShader(shaderFile, source, shaderKind);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static SPIRV compileShader(String filename, String source, ShaderKind shaderKind) {


        long result = shaderc_compile_into_preprocessed_text(compiler, source, shaderKind.kind, filename, "main", options);




//        shaderc_compiler_release(compiler);

        return new SPIRV((int)shaderc_result_get_length(result), nshaderc_result_get_bytes(result));
    }

    private static SPIRV readFromStream(InputStream inputStream) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.position(0);

            return new SPIRV(bytes.length, MemoryUtil.memAddress0(buffer));
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new RuntimeException("unable to read inputStream");
    }

    public enum ShaderKind {
        VERTEX_SHADER(shaderc_glsl_vertex_shader),
        GEOMETRY_SHADER(shaderc_glsl_geometry_shader),
        FRAGMENT_SHADER(shaderc_glsl_fragment_shader);

        private final int kind;

        ShaderKind(int kind) {
            this.kind = kind;
        }
    }

    public record SPIRV(int size, long bytecode) implements NativeResource {

        //            this.handle = handle;

        @Override
            public void free() {

            MemoryUtil.nmemFree(bytecode);
            }
        }

    private static class ShaderIncluder implements ShadercIncludeResolveI {

        private static final int MAX_PATH_LENGTH = 4096; //Maximum Linux/Unix Path Length

        @Override
        public long invoke(long user_data, long requested_source, int type, long requesting_source, long include_depth) {
            //TODO: try to optimise this if it gets too slow/(i.e. causes too much CPU overhead, if any that is)

            String s = MemoryUtil.memASCII(requested_source);
            String s1 = MemoryUtil.memASCII(requesting_source+6); //Strip the "file:/" prefix from the initial string const char* Address
            try(MemoryStack stack = MemoryStack.stackPush(); FileInputStream fileInputStream = new FileInputStream(s1.substring(0, s1.lastIndexOf("/")+1) + s)) {
                    return ShadercIncludeResult.malloc(stack)
                            .source_name(stack.ASCII(s))
                            .content(stack.bytes(fileInputStream.readAllBytes()))
                            .user_data(user_data).address();
            }
            catch (IOException e) {
                    throw new RuntimeException(e);
            }

        }
    }

    private static class ShaderReleaser implements ShadercIncludeResultReleaseI {
        @Override
        public void invoke(long user_data, long include_result) {
            //TODO:MAybe dump Shader Compiled Binaries here to a .Misc Diretcory to allow easy caching.recompilation...
        }
    }
}