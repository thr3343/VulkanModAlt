package net.vulkanmod.render.chunk;

public record VkDrawIndexedIndirectCommand2(int indexCount,
                                           int instanceCount,
                                           int firstIndex,
                                           int vertexOffset,
                                           int firstInstance
) {
}
