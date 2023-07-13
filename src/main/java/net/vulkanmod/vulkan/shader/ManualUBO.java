package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.shader.layout.Field;
import net.vulkanmod.vulkan.shader.layout.UBO;
import org.lwjgl.system.MemoryUtil;

public class ManualUBO extends UBO {

    public final long srcPtr;
    private final int srcSize;

    public ManualUBO(int binding, int type, int size) {
        super(binding, type, size * 4, null);
        srcPtr=MemoryUtil.nmemAlloc(size*4L);
        srcSize = size*4;
    }

    public void update() {
        //update manually
    }

    @Override
    public void update(long ptr) {
        //update manually
        MemoryUtil.memCopy(this.srcPtr, ptr, this.srcSize);
    }

}
