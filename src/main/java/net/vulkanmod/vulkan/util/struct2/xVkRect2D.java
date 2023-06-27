package net.vulkanmod.vulkan.util.struct2;

public record xVkRect2D(int a, int b, int c, int d)
{

    public static xVkRect2D set(VkExtent vkExtent) {


        return new xVkRect2D(0,0,vkExtent.a(), vkExtent.b());
    }
    public int[] ref() { return new int[]{a,b,c,d};};
//    xVkRect2D offset(int a, int b) { this.pa[0]=a; this.pa[b]=b; return this; }
//    xVkRect2D extent(int c, int d) { this.pa[2]=c; this.pa[3]=d; return this; }
}
