package net.vulkanmod.vulkan.shader.layout;

import java.util.List;

public class PushConstants extends AlignedStruct {

    public final int stage;

    protected PushConstants(List<Field.FieldInfo> infoList, int size, int stage) {
        super(infoList, size);
        this.stage = stage;
    }

}
