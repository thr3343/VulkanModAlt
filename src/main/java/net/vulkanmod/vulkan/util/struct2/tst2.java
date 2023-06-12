package net.vulkanmod.vulkan.util.struct2;

public record tst2(int[] a, long[] b, int[] c, long[] d)
{
    static tst2 create(){
        return new tst2(new int[1], new long[1], new int[1], new long[1]);
    }
}
