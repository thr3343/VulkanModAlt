package net.vulkanmod.vulkan.util.struct2;


public record VkExtent(int a, int b)
{
    private static  final int widthOff=0;
    private static  final int heightOff=1;

    public static VkExtent create() {return new VkExtent(0, 0);}
    public static VkExtent set(int a, int b) {return new VkExtent(a, b);}
    public int width() { return this.a;}
    public int height() { return this.b;}

//    public VkExtent width(int a) { this.a=a; return this;}
//
//    public VkExtent height(int b) { this.b=b; return this;}

}
