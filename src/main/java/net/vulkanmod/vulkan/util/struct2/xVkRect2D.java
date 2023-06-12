package net.vulkanmod.vulkan.util.struct2;

public class xVkRect2D
{
    //TODO: record pls...
     public final int[] pa = new int[6];
    public xVkRect2D(int a, int b, int c, int d) {

        this.pa[0] = a;
        this.pa[1] = b;
        this.pa[2] = c;
        this.pa[3] = d;
    }
    public xVkRect2D(VkExtent vkExtent) {


        this.pa[2] = vkExtent.width();
        this.pa[3] = vkExtent.height();
    }

    xVkRect2D offset(int a, int b) { this.pa[0]=a; this.pa[b]=b; return this; }
    xVkRect2D extent(int c, int d) { this.pa[2]=c; this.pa[3]=d; return this; }
}
