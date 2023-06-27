package net.vulkanmod.vulkan.shader;



public class UniformGrouping {

    int binding;

    UniformEntityComponant uniformEntityComponant;
    private int usedBytes;

    public UniformGrouping(int defsize, int Alignment, int binding) {
        this.binding= binding;
    }


    void addUniform(UniformEntityComponant uniformEntityComponant)
    {
        this.usedBytes+=uniformEntityComponant.byteSize;
    }


    void PushConstantDeligate()
    {

    }

    private class UniformEntityComponant {
        public int byteSize;

//        Field2 type2;

        String name;
        
    }
}
