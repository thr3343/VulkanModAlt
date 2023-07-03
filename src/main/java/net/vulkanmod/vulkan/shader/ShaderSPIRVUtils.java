package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.Vulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import org.lwjgl.util.shaderc.*;
import org.lwjgl.util.spvc.Spvc;
import org.lwjgl.vulkan.VK12;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.system.Checks.check;
import static org.lwjgl.system.JNI.invokePP;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.shaderc.Shaderc.*;

public class ShaderSPIRVUtils {
    static final long compiler;

    static final long options;
    private static final ShaderIncluder SHADER_INCLUDER = new ShaderIncluder();
    private static final ShaderReleaser SHADER_RELEASER = new ShaderReleaser();

    private static final long pUserData = 0;
    private static final boolean skipCompilation = !Vulkan.ENABLE_VALIDATION_LAYERS;

    static {

        compiler = shaderc_compiler_initialize();

        if(compiler == NULL) {
            throw new RuntimeException("Failed to create shader compiler");
        }

        options = shaderc_compile_options_initialize();

        if(options == NULL) {
            throw new RuntimeException("Failed to create compiler options");
        }

        shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_performance);
        shaderc_compile_options_set_target_env(options, shaderc_env_version_vulkan_1_2, VK12.VK_API_VERSION_1_2);
        shaderc_compile_options_set_include_callbacks(options, SHADER_INCLUDER, SHADER_RELEASER, pUserData);

    }

    public static long compileShaderFile(String shaderFile, ShaderKind shaderKind) {
        //TODO name out
        String path = ShaderSPIRVUtils.class.getResource("/assets/vulkanmod/shaders/" + shaderFile).toExternalForm();
        return compileShaderAbsoluteFile(path, shaderKind);
    }

    public static long compileShaderAbsoluteFile(String shaderFile, ShaderKind shaderKind) {
        try {
            String source = new String(Files.readAllBytes(Paths.get(new URI(shaderFile))));
            return compileShader(shaderFile, source, shaderKind);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return NULL;
    }

    public static long compileShader(String filename, String source, ShaderKind shaderKind) {


        final long shaderc_compilation_result_t = skipCompilation ? shaderc_compile_into_preprocessed_text(compiler, source, shaderKind.kind, filename, "main", options) : shaderc_compile_into_spv(compiler, source, shaderKind.kind, filename, "main", options);

        if(shaderc_compilation_result_t == NULL) {
            throw new RuntimeException("Failed to compile shader " + filename + " into SPIR-V");
        }
        //Spvc.spvc_compiler_get_decoration()
        if(shaderc_compilation_result_compilation_status(shaderc_compilation_result_t) != shaderc_compilation_status_success) {
            throw new RuntimeException("Failed to compile shader " + filename + " into SPIR-V:\n" + shaderc_result_get_error_message(shaderc_compilation_result_t));
        }

//        shaderc_compiler_release(compiler);
        //We already have the shaderc_compilation_result, so on paper don't need to allocate an additional/duplicate byteBuf to store the uint32_t* pCode Pointr

        return (shaderc_compilation_result_t);
    }

    private static int shaderc_compilation_result_compilation_status(long result) {
        return memGetInt(result + 56);
    }

    static int shaderc_compilation_result_output_data_size(long result) {
        return memGetInt(result + 8);
    }

    /*private static SPIRV readFromStream(InputStream inputStream) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.position(0);

            return new SPIRV((int) memAddress(buffer), bytes.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new RuntimeException("unable to read inputStream");
    }*/

    public enum ShaderKind {
        VERTEX_SHADER(shaderc_glsl_vertex_shader),
        GEOMETRY_SHADER(shaderc_glsl_geometry_shader),
        FRAGMENT_SHADER(shaderc_glsl_fragment_shader);

        private final int kind;

        ShaderKind(int kind) {
            this.kind = kind;
        }
    }

//
//    public record SPIRV(long l) implements NativeResource {
//
//
//        //           bytecode= invokePP(l, Functions.result_get_bytes);
//        //           size_t= shaderc_compilation_result_output_data_size(l);
//
//        @Override
//        public void free() {
//            shaderc_result_release(l);
//            //            bytecode = null; // Help the GC
//        }
//    }

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