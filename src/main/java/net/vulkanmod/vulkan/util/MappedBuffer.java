package net.vulkanmod.vulkan.util;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public record MappedBuffer(ByteBuffer buffer, long ptr)
{

    public static MappedBuffer createFromBuffer(ByteBuffer buffer) {
        return new MappedBuffer(buffer, MemoryUtil.memAddress0(buffer));
    }

    public static MappedBuffer getMappedBuffer(int size) {
        var buffer = MemoryUtil.memAlloc(size);
        var ptr = MemoryUtil.memAddress0(buffer);
        return new MappedBuffer(buffer, ptr);
    }

    public void putFloat(int idx, float f) {
        VUtil.UNSAFE.putFloat(ptr + idx, f);
    }

    public void putInt(int idx, int f) {
        VUtil.UNSAFE.putInt(ptr + idx, f);
    }

    public float getFloat(int idx) {
        return VUtil.UNSAFE.getFloat(ptr + idx);
    }

    public int getInt(int idx) {
        return VUtil.UNSAFE.getInt(ptr + idx);
    }
}
