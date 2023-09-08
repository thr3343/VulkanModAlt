package net.vulkanmod.render.chunk;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.VirtualBuffer;
import net.vulkanmod.render.chunk.util.ResettableQueue;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.Vulkan;
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
    public static VirtualBuffer virtualBufferVtx=new VirtualBuffer((1L<<Initializer.CONFIG.defBaseSize), VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, CUTOUT_MIPPED);
    public static VirtualBuffer TvirtualBufferVtx=new VirtualBuffer((1L<<Initializer.CONFIG.defBaseSize)>>2, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, TerrainRenderType.TRANSLUCENT);
    public static VirtualBuffer TvirtualBufferIdx=new VirtualBuffer((1L<<Initializer.CONFIG.defBaseSize)>>5, VK_BUFFER_USAGE_INDEX_BUFFER_BIT, TerrainRenderType.TRANSLUCENT);
    VirtualBuffer virtualBuffer;
    IndirectBuffer indirectBuffer;


    public static void reload(long size_t)
    {
        long a = 1L<<size_t;
        //Deleting later not idea due to VRAm Spill, but is a quick and dirty mehtod for now
        virtualBufferVtx.cleanUp();
        TvirtualBufferVtx.cleanUp();
        TvirtualBufferIdx.cleanUp();
        WorldRenderer.getInstance().setNeedsUpdate();
        WorldRenderer.getInstance().allChanged();
        virtualBufferVtx=new VirtualBuffer(a, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, CUTOUT_MIPPED);
        TvirtualBufferVtx=new VirtualBuffer(a>>2, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, TerrainRenderType.TRANSLUCENT);
        TvirtualBufferIdx=new VirtualBuffer(a>>5, VK_BUFFER_USAGE_INDEX_BUFFER_BIT, TerrainRenderType.TRANSLUCENT);
    }


}
