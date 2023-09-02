package net.vulkanmod.render.chunk;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.render.VirtualBuffer;
import net.vulkanmod.render.chunk.util.ResettableQueue;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.memory.IndirectBuffer;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDrawIndexedIndirectCommand;

import static net.vulkanmod.render.vertex.TerrainRenderType.CUTOUT_MIPPED;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;

public class UberBufferSet {

    public static final ResettableQueue<VkDrawIndexedIndirectCommand2> sectionQueue = new ResettableQueue<>(1024);
    public static final ResettableQueue<VkDrawIndexedIndirectCommand2> TsectionQueue = new ResettableQueue<>(1024);
    //TODO: Fix MipMaps Later...
//    public static final ObjectArrayList<VBO> cutoutChunks = new ObjectArrayList<>(1024);
////    public static final ObjectArrayList<VBO> cutoutMippedChunks = new ObjectArrayList<>(1024);
//    public static final ObjectArrayList<VBO> translucentChunks = new ObjectArrayList<>(1024);
//    public static final VirtualBuffer virtualBufferIdx=new VirtualBuffer(16777216, VK_BUFFER_USAGE_INDEX_BUFFER_BIT);
    public static final VirtualBuffer virtualBufferVtx=new VirtualBuffer(536870912, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, CUTOUT_MIPPED);
    public static final VirtualBuffer TvirtualBufferVtx=new VirtualBuffer(134217728, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, TerrainRenderType.TRANSLUCENT);
    public static final VirtualBuffer TvirtualBufferIdx=new VirtualBuffer(16777216, VK_BUFFER_USAGE_INDEX_BUFFER_BIT, TerrainRenderType.TRANSLUCENT);
    VirtualBuffer virtualBuffer;
    IndirectBuffer indirectBuffer;

}
