package net.vulkanmod.vulkan.shader;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.util.shaderc.ShadercIncludeResolve;
import org.lwjgl.util.shaderc.ShadercIncludeResult;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ShaderIncluder extends ShadercIncludeResolve {

    private static final int MAX_PATH_LENGTH = 4096; //Maximum Linux/Unix Path Length

    @Override
    public long invoke(long user_data, long requested_source, int type, long requesting_source, long include_depth) {
//        System.out.println("#INCLUDE!");
//        System.out.println("user_data!: "+user_data);
//        System.out.println("requested_source!: "+s);
//        System.out.println("type!: "+type);
//        String s1 = MemoryUtil.memUTF8(requesting_source);
//        System.out.println("requesting_source!: "+ s1);
//        System.out.println("include_depth!: "+include_depth);
//        System.out.println("ShaderSPIRVUtils.compiler!: "+ShaderSPIRVUtils.compiler);


        try(MemoryStack stack = MemoryStack.stackPush())
        {
            String s = MemoryUtil.memUTF8(requested_source);
            String s1 = MemoryUtil.memUTF8(requesting_source+6); //Strip the "file:/" prefix from the initial string const char* Address


            //        Shaderc.shaderc_compile_into_preprocessed_text(ShaderSPIRVUtils.compiler, bytes, Shaderc.shaderc_vertex_shader, s, "Main", ShaderSPIRVUtils.options);

            return ShadercIncludeResult.malloc(stack)
                    .source_name(stack.ASCII(s))
                    .content(stack.bytes((Files.readAllBytes(Path.of((s1.substring(0, s1.lastIndexOf("/")) + "/") +s)))))
                    .user_data(user_data).address();
        }
        catch (IOException e) {
                throw new RuntimeException(e);
        }

    }
}
