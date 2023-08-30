package net.vulkanmod.render.chunk;

import java.util.Objects;

public final class VkDrawIndexedIndirectCommand2 {
    private final int indexCount;
    private final int instanceCount;
    int firstIndex;
    private final int vertexOffset;
    private final int firstInstance;
    private final int xOffset;
    private final int zOffset;

    public VkDrawIndexedIndirectCommand2(int indexCount,
                                         int instanceCount,
                                         int firstIndex,
                                         int vertexOffset,
                                         int firstInstance,
                                         int xOffset, int zOffset) {
        this.indexCount = indexCount;
        this.instanceCount = instanceCount;
        this.firstIndex = firstIndex;
        this.vertexOffset = vertexOffset;
        this.firstInstance = firstInstance;
        this.xOffset = xOffset;
        this.zOffset = zOffset;
    }

    public int indexCount() {
        return indexCount;
    }

    public int instanceCount() {
        return instanceCount;
    }

    public int firstIndex() {
        return firstIndex;
    }

    public int vertexOffset() {
        return vertexOffset;
    }

    public int firstInstance() {
        return firstInstance;
    }

    public int xOffset() {
        return xOffset;
    }

    public int zOffset() {
        return zOffset;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (VkDrawIndexedIndirectCommand2) obj;
        return this.indexCount == that.indexCount &&
                this.instanceCount == that.instanceCount &&
                this.firstIndex == that.firstIndex &&
                this.vertexOffset == that.vertexOffset &&
                this.firstInstance == that.firstInstance &&
                this.xOffset == that.xOffset &&
                this.zOffset == that.zOffset;
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexCount, instanceCount, firstIndex, vertexOffset, firstInstance, xOffset, zOffset);
    }

    @Override
    public String toString() {
        return "VkDrawIndexedIndirectCommand2[" +
                "indexCount=" + indexCount + ", " +
                "instanceCount=" + instanceCount + ", " +
                "firstIndex=" + firstIndex + ", " +
                "vertexOffset=" + vertexOffset + ", " +
                "firstInstance=" + firstInstance + ", " +
                "xOffset=" + xOffset + ", " +
                "zOffset=" + zOffset + ']';
    }

}
