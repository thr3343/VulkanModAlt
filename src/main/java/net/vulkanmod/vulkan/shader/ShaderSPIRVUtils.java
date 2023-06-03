package net.vulkanmod.vulkan.shader;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import org.lwjgl.system.NativeType;
import org.lwjgl.util.shaderc.ShadercIncludeResolve;
import org.lwjgl.util.shaderc.ShadercIncludeResult;
import org.lwjgl.util.shaderc.ShadercIncludeResultRelease;
import org.lwjgl.vulkan.VK12;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memGetAddress;
import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_result_release;

public class ShaderSPIRVUtils {
    static final long compiler;

    static final long options;
    private static final ShaderIncluder SHADER_INCLUDER = new ShaderIncluder();
    private static final ShaderReleaser SHADER_RELEASER = new ShaderReleaser();

    private static final long pUserData = 0; //Not sure if we need this TBH: may be for custom Struct//Pointers.user Callbacks (i..e to allo apssing inanddiitonal infomation/Strucvts as and we needed e.g.

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


        long result = shaderc_compile_into_spv(compiler, source, shaderKind.kind, filename, "main", options);

        if(result == NULL) {
            throw new RuntimeException("Failed to compile shader " + filename + " into SPIR-V");
        }

        if(shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
            throw new RuntimeException("Failed to compile shader " + filename + " into SPIR-V:\n" + shaderc_result_get_error_message(result));
        }

//        shaderc_compiler_release(compiler);

        return new SPIRV(result, shaderc_result_get_bytes(result));
    }

    private static SPIRV readFromStream(InputStream inputStream) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.position(0);

            return new SPIRV(MemoryUtil.memAddress(buffer), buffer);
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

    public static final class SPIRV implements NativeResource {

        private final long handle;
        private ByteBuffer bytecode;

        public SPIRV(long handle, ByteBuffer bytecode) {
            this.handle = handle;
            this.bytecode = bytecode;
        }

        public ByteBuffer bytecode() {
            return bytecode;
        }

        @Override
        public void free() {
            shaderc_result_release(handle);
            bytecode = null; // Help the GC
        }
    }

    private static final class ShaderIncluder extends ShadercIncludeResolve {

        private static final int MAX_PATH_LENGTH = 4096; //Maximum Linux/Unix Path Length

        @Override
        public long invoke(@NativeType("void *") long user_data, @NativeType("char const *") long requested_source, int type, @NativeType("char const *") long requesting_source, @NativeType("size_t") long include_depth) {
            //TODO: try to optimise this if it gets too slow/(i.e. causes too much CPU overhead, if any that is)
            try(MemoryStack stack = MemoryStack.stackPush())
            {
                String s = MemoryUtil.memUTF8(requested_source);
                String s1 = MemoryUtil.memUTF8(requesting_source+6); //Strip the "file:/" prefix from the initial string const char* Address

                final ByteBuffer values = stack.bytes(Files.readAllBytes(Path.of(s1.substring(s1.lastIndexOf("/")) + "/" + s)));
                return ShadercIncludeResult.malloc(stack)
                        .set(stack.ASCII(s), values, user_data)
                        .address();
            }
            catch (IOException e) {
                    throw new RuntimeException(e);
            }

        }
    }

    private static final class ShaderReleaser extends ShadercIncludeResultRelease {
        //Don;t need this AFAIk due to being able to exploit MemoryStack to handle this anyway
        @Override
        public void invoke(@NativeType("void *") long user_data, @NativeType("shaderc_include_result *") long include_result) {
//            shaderc_result_release(memGetAddress(include_result + ShadercIncludeResult.CONTENT));
            //TODO:MAybe dump Shader Compiled Binaries here to a .Misc Diretcory to allow easy caching.recompilation...
        }
    }
}