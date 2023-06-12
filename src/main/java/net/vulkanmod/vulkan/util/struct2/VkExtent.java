package net.vulkanmod.vulkan.util.struct2;


public record VkExtent(int[] a)
{
    private static  final int widthOff=0;
    private static  final int heightOff=1;

    public static VkExtent create() {return new VkExtent(new int[2]);}
    public static VkExtent set(int a, int b) {return new VkExtent(new int[]{a, b});}
    public int width() { return this.a[0];}
    public int height() { return this.a[1];}

    public VkExtent width(int a) { this.a[0]=a; return this;}

    public VkExtent height(int b) { this.a[1]=b; return this;}

}
