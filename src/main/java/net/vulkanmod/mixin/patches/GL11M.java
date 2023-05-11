package net.vulkanmod.mixin.patches;

import net.vulkanmod.vulkan.Vulkan;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static org.lwjgl.system.MemoryStack.stackPush;

@Mixin(GL11.class)
public class GL11M {

    @Redirect(method = "glEnable", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11C;glEnable(I)V"), remap = false)
    private static void glEnable(@NativeType("GLenum") int target) {}


    @Redirect(method = "glDisable", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11C;glDisable(I)V"), remap = false)
    private static void glDisable(@NativeType("GLenum") int target) {}


    @Redirect(method = "glScissor", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11C;glScissor(IIII)V"), remap = false)
    private static void glScissor(@NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height) {
        try(MemoryStack stack = stackPush()) {
            Vulkan.scissor(stack);
        }
    }
}
