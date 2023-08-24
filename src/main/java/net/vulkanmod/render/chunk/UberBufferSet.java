package net.vulkanmod.render.chunk;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.render.VirtualBuffer;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.memory.IndirectBuffer;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDrawIndexedIndirectCommand;

import static net.vulkanmod.render.vertex.TerrainRenderType.CUTOUT_MIPPED;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;

public class UberBufferSet {

    public static final ObjectArrayList<VkDrawIndexedIndirectCommand2> sectionQueue = new ObjectArrayList<>();
    public static final ObjectArrayList<VkDrawIndexedIndirectCommand2> TsectionQueue = new ObjectArrayList<>();
    //TODO: Fix MipMaps Later...
//    public static final ObjectArrayList<VBO> cutoutChunks = new ObjectArrayList<>(1024);
////    public static final ObjectArrayList<VBO> cutoutMippedChunks = new ObjectArrayList<>(1024);
//    public static final ObjectArrayList<VBO> translucentChunks = new ObjectArrayList<>(1024);
//    public static final VirtualBuffer virtualBufferIdx=new VirtualBuffer(16777216, VK_BUFFER_USAGE_INDEX_BUFFER_BIT);
    public static final VirtualBuffer virtualBufferVtx=new VirtualBuffer(536870912, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, CUTOUT_MIPPED);
    public static final VirtualBuffer TvirtualBufferVtx=new VirtualBuffer(134217728, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, TerrainRenderType.TRANSLUCENT);
    public static final long SPtr = MemoryUtil.nmemAlignedAlloc(8, 8);
    public static final long TPtr = MemoryUtil.nmemAlignedAlloc(8, 8);
    VirtualBuffer virtualBuffer;
    IndirectBuffer indirectBuffer;

    static
    {
        VUtil.UNSAFE.putLong(SPtr, UberBufferSet.virtualBufferVtx.bufferPointerSuperSet);
        VUtil.UNSAFE.putLong(TPtr, UberBufferSet.TvirtualBufferVtx.bufferPointerSuperSet);
    }
}
