package net.vulkanmod.vulkan.shader;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.EXTMeshShader;
import org.lwjgl.vulkan.VK10;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class PushConstant extends AlignedStruct {
    private ByteBuffer buffer;
    private List<Field> fields = new ArrayList<>();
    private int size;
    private int shaderStage;

    public void allocateBuffer() {
        buffer = MemoryUtil.memAlloc(size);
    }

    public void update() {
        for(Field field : fields) {
            field.update(buffer);
        }

        buffer.position(0);
    }

    public void addField(Field field) {
        fields.add(field);
        this.size = (field.getOffset() + field.getSize()) * 4;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public int getSize() { return size; }

    public int getShaderStage() {
        return shaderStage;
    }

    public void setStage(String stage) {
        shaderStage=stage=="fragment"? VK10.VK_SHADER_STAGE_FRAGMENT_BIT : VK10.VK_SHADER_STAGE_VERTEX_BIT;
    }
}
