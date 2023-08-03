package net.vulkanmod.render.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.interfaces.FrustumMixed;
import net.vulkanmod.render.chunk.build.ChunkTask;
import net.vulkanmod.render.chunk.build.TaskDispatcher;
import net.vulkanmod.render.profiling.Profiler;
import net.vulkanmod.render.profiling.Profiler2;
import net.vulkanmod.render.chunk.util.AreaSetQueue;
import net.vulkanmod.render.chunk.util.ResettableQueue;
import net.vulkanmod.render.chunk.util.Util;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.IndirectBuffer;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.shader.ShaderManager;
import net.vulkanmod.vulkan.util.VUtil;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.lwjgl.vulkan.VkCommandBuffer;

import javax.annotation.Nullable;
import java.util.*;

import static net.vulkanmod.render.vertex.TerrainRenderType.*;
import static net.vulkanmod.vulkan.util.VBOUtil.*;
import static org.lwjgl.system.JNI.callPPPV;
import static org.lwjgl.system.JNI.callPV;
import static org.lwjgl.vulkan.VK10.nvkCmdBindVertexBuffers;

public class WorldRenderer {
    private static WorldRenderer INSTANCE;

    private final Minecraft minecraft;

    private ClientLevel level;
    private int lastViewDistance;
    private final RenderBuffers renderBuffers;

    private Vec3 cameraPos;
    private int lastCameraSectionX;
    private int lastCameraSectionY;
    private int lastCameraSectionZ;
    private float lastCameraX;
    private float lastCameraY;
    private float lastCameraZ;
    private float lastCamRotX;
    private float lastCamRotY;

    private SectionGrid sectionGrid;

    private boolean needsUpdate;
    private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();

    private final TaskDispatcher taskDispatcher;
    private final ResettableQueue<RenderSection> chunkQueue = new ResettableQueue<>();
    private AreaSetQueue chunkAreaQueue;
    private short lastFrame = 0;

    private double xTransparentOld;
    private double yTransparentOld;
    private double zTransparentOld;

    private VFrustum frustum;

    IndirectBuffer[] indirectBuffers;
//    UniformBuffers uniformBuffers;

    RenderRegionCache renderRegionCache;
    int nonEmptyChunks;
    public static final ObjectArrayList<VkDrawIndexedIndirectCommand2> sectionQueue = new ObjectArrayList<>();
    public static final ObjectArrayList<VkDrawIndexedIndirectCommand2> TsectionQueue = new ObjectArrayList<>();

    private WorldRenderer(RenderBuffers renderBuffers) {
        this.minecraft = Minecraft.getInstance();
        this.renderBuffers = renderBuffers;
        this.taskDispatcher = new TaskDispatcher();
        ChunkTask.setTaskDispatcher(this.taskDispatcher);
    }

    private void allocateIndirectBuffers() {
        this.indirectBuffers = new IndirectBuffer[Vulkan.getSwapChainImages().size()];

        for(int i = 0; i < this.indirectBuffers.length; ++i) {
            this.indirectBuffers[i] = new IndirectBuffer(1000000, MemoryTypes.HOST_MEM);
//            this.indirectBuffers[i] = new IndirectBuffer(1000000, MemoryTypes.GPU_MEM);
        }

//        uniformBuffers = new UniformBuffers(100000, MemoryTypes.GPU_MEM);
    }

    public static WorldRenderer init(RenderBuffers renderBuffers) {
        if(INSTANCE != null)
            throw new RuntimeException("WorldRenderer re-initialization");
        return INSTANCE = new WorldRenderer(renderBuffers);
    }

    public static WorldRenderer getInstance() {
        return INSTANCE;
    }

    public static ClientLevel getLevel() {
        return INSTANCE.level;
    }

    public static Vec3 getCameraPos() {
        return INSTANCE.cameraPos;
    }

    public void setupRenderer(Camera camera, Frustum frustum, boolean isCapturedFrustum, boolean spectator) {
//        Profiler p = Profiler.getProfiler("chunks");
//        p.start();
        Profiler2 profiler = Profiler2.getMainProfiler();
        profiler.push("Setup_Renderer");

//        this.frustum = frustum.offsetToFullyIncludeCameraCube(8);
        this.cameraPos = camera.getPosition();
        if (this.minecraft.options.getEffectiveRenderDistance() != this.lastViewDistance) {
            this.allChanged();
        }

        this.level.getProfiler().push("camera");
        float cameraX = (float)cameraPos.x();
        float cameraY = (float)cameraPos.y();
        float cameraZ = (float)cameraPos.z();
        int sectionX = SectionPos.posToSectionCoord(cameraX);
        int sectionY = SectionPos.posToSectionCoord(cameraY);
        int sectionZ = SectionPos.posToSectionCoord(cameraZ);

        Profiler p2 = Profiler.getProfiler("camera");
        profiler.push("reposition");

        if (this.lastCameraSectionX != sectionX || this.lastCameraSectionY != sectionY || this.lastCameraSectionZ != sectionZ) {

            p2.start();
            this.lastCameraSectionX = sectionX;
            this.lastCameraSectionY = sectionY;
            this.lastCameraSectionZ = sectionZ;
            this.sectionGrid.repositionCamera(cameraX, cameraZ);
            p2.pushMilestone("end-reposition");
            p2.round();
        }
        profiler.pop();

        double entityDistanceScaling = this.minecraft.options.entityDistanceScaling().get();
        Entity.setViewScale(Mth.clamp((double)this.minecraft.options.getEffectiveRenderDistance() / 8.0D, 1.0D, 2.5D) * entityDistanceScaling);

//        this.chunkRenderDispatcher.setCamera(cameraPos);
        this.level.getProfiler().popPush("cull");
        this.minecraft.getProfiler().popPush("culling");
        BlockPos blockpos = camera.getBlockPosition();

        this.minecraft.getProfiler().popPush("update");

        boolean flag = this.minecraft.smartCull;
        if (spectator && this.level.getBlockState(blockpos).isSolidRender(this.level, blockpos)) {
            flag = false;
        }

        float d_xRot = Math.abs(camera.getXRot() - this.lastCamRotX);
        float d_yRot = Math.abs(camera.getYRot() - this.lastCamRotY);
        this.needsUpdate |= d_xRot > 2.0f || d_yRot > 2.0f;

        this.needsUpdate |= cameraX != this.lastCameraX || cameraY != this.lastCameraY || cameraZ != this.lastCameraZ;

        if (!isCapturedFrustum) {

            //Debug
//            this.needsUpdate = true;
//            this.needsUpdate = false;

            if (this.needsUpdate) {
                this.needsUpdate = false;

                this.frustum = (((FrustumMixed)(frustum)).customFrustum()).offsetToFullyIncludeCameraCube(8);
                this.sectionGrid.updateFrustumVisibility(this.frustum);
                this.lastCameraX = cameraX;
                this.lastCameraY = cameraY;
                this.lastCameraZ = cameraZ;
                this.lastCamRotX = camera.getXRot();
                this.lastCamRotY = camera.getYRot();

//                p2.pushMilestone("frustum");

                this.minecraft.getProfiler().push("partial_update");

                this.chunkQueue.clear();
                this.initializeQueueForFullUpdate(camera);

                this.renderRegionCache = new RenderRegionCache();

                if(flag)
                    this.updateRenderChunks();
                else
                    this.updateRenderChunksSpectator();

                this.minecraft.getProfiler().pop();

            }

//            p.pushMilestone("update");
//            p.round();
        }

        this.indirectBuffers[Drawer.getCurrentFrame()].reset();
//        this.uniformBuffers.reset();

        this.minecraft.getProfiler().pop();
        profiler.pop();
    }

    private void initializeQueueForFullUpdate(Camera camera) {
        int i = 16;
        Vec3 vec3 = camera.getPosition();
        BlockPos blockpos = camera.getBlockPosition();
        RenderSection renderSection = this.sectionGrid.getRenderSectionAt(blockpos);

        if (renderSection == null) {
            boolean flag = blockpos.getY() > this.level.getMinBuildHeight();
            int j = flag ? this.level.getMaxBuildHeight() - 8 : this.level.getMinBuildHeight() + 8;
            int k = Mth.floor(vec3.x / 16.0D) * 16;
            int l = Mth.floor(vec3.z / 16.0D) * 16;

            List<RenderSection> list = Lists.newArrayList();

            for(int i1 = -this.lastViewDistance; i1 <= this.lastViewDistance; ++i1) {
                for(int j1 = -this.lastViewDistance; j1 <= this.lastViewDistance; ++j1) {

                    RenderSection renderSection1 = this.sectionGrid.getRenderSectionAt(new BlockPos(k + SectionPos.sectionToBlockCoord(i1, 8), j, l + SectionPos.sectionToBlockCoord(j1, 8)));
                    if (renderSection1 != null) {
                        renderSection1.setGraphInfo(null, (byte) 0);
                        list.add(renderSection1);

                    }
                }
            }

            //Probably not needed
//            list.sort(Comparator.comparingDouble((p_194358_) -> {
//                return blockpos.distSqr(p_194358_.chunk.getOrigin().offset(8, 8, 8));
//            }));

            for (RenderSection chunkInfo : list) {
                this.chunkQueue.add(chunkInfo);
            }

        } else {
            renderSection.setGraphInfo(null, (byte) 0);
            this.chunkQueue.add(renderSection);
        }

    }

    private void initUpdate() {
        this.resetUpdateQueues();

        this.lastFrame++;
        this.nonEmptyChunks = 0;
    }

    private void resetUpdateQueues() {
        this.chunkAreaQueue.clear();
//        this.sectionGrid.chunkAreaManager.resetQueues();
    }

    private void updateRenderChunks() {
        int maxDirectionsChanges = Initializer.CONFIG.advCulling;

        this.initUpdate();

        int rebuildLimit = taskDispatcher.getIdleThreadsCount();
//        int rebuildLimit = 32;

        if(rebuildLimit == 0)
            this.needsUpdate = true;
        sectionQueue.clear();
        TsectionQueue.clear();
        while(this.chunkQueue.hasNext()) {
            RenderSection renderSection = this.chunkQueue.poll();

            for (TerrainRenderType rType : renderSection.getCompiledSection().renderTypes) {
                final VkDrawIndexedIndirectCommand2 buffer = renderSection.drawParametersArray[rType.ordinal()].vertexBufferSegment1;
                if (buffer!=null) {
                    (rType !=TRANSLUCENT ? sectionQueue : TsectionQueue).add(buffer);
                }
            }

//            SolidVBOs.add(renderSection);

            if(!renderSection.isCompletelyEmpty()) {
                this.chunkAreaQueue.add(renderSection.getChunkArea());
                this.nonEmptyChunks++;
            }

            if(this.scheduleUpdate(renderSection, rebuildLimit))
                rebuildLimit--;

            if(renderSection.directionChanges > maxDirectionsChanges)
                continue;

            for(Direction direction : Util.DIRECTIONS) {
                RenderSection relativeChunk = renderSection.getNeighbour(direction);

                if (relativeChunk != null && !renderSection.hasDirection(direction.getOpposite())) {

                    if (renderSection.hasMainDirection()) {
                        if (!renderSection.visibilityBetween(renderSection.mainDir.getOpposite(), direction))
                            continue;
                    }

                    this.addNode(renderSection, relativeChunk, direction);
                }
            }
        }

    }

    private void updateRenderChunksSpectator() {
        int maxDirectionsChanges = Initializer.CONFIG.advCulling;

        this.initUpdate();

        int rebuildLimit = taskDispatcher.getIdleThreadsCount();

        if(rebuildLimit == 0)
            this.needsUpdate = true;

        while(this.chunkQueue.hasNext()) {
            RenderSection renderSection = this.chunkQueue.poll();

//            renderSection.getChunkArea().getDrawBuffers().sectionQueue.add(renderSection);

            if(!renderSection.isCompletelyEmpty()) {
                this.chunkAreaQueue.add(renderSection.getChunkArea());
                this.nonEmptyChunks++;
            }

            if(this.scheduleUpdate(renderSection, rebuildLimit))
                rebuildLimit--;

            for(Direction direction : Util.DIRECTIONS) {
                RenderSection relativeChunk = renderSection.getNeighbour(direction);

                if (relativeChunk != null && !renderSection.hasDirection(direction.getOpposite())) {

                    this.addNode(renderSection, relativeChunk, direction);

                }
            }
        }

    }

    private void addNode(RenderSection renderSection, RenderSection relativeChunk, Direction direction) {
        if (relativeChunk.setLastFrame(this.lastFrame)) {

            int d;
            if (renderSection.mainDir != direction && !renderSection.isCompletelyEmpty())
                d = renderSection.directionChanges + 1;
            else
                d = renderSection.directionChanges;

            relativeChunk.addDir(direction);

            if(d < relativeChunk.directionChanges)
                relativeChunk.directionChanges = (byte) d;

        }

        else if (relativeChunk.getChunkArea().inFrustum(relativeChunk.frustumIndex) < 0 ) {

            if(relativeChunk.getChunkArea().inFrustum(relativeChunk.frustumIndex) == FrustumIntersection.INTERSECT) {
                if(frustum.cubeInFrustum(relativeChunk.xOffset, relativeChunk.yOffset, relativeChunk.zOffset,
                        relativeChunk.xOffset + 16 , relativeChunk.yOffset + 16, relativeChunk.zOffset + 16) >= 0)
                    return;
            }

            relativeChunk.setGraphInfo(direction, (byte) (renderSection.step + 1));
            relativeChunk.setDirections(renderSection.directions, direction);
            this.chunkQueue.add(relativeChunk);

            byte d;
            if ((renderSection.sourceDirs & (1 << direction.ordinal())) == 0 && !renderSection.isCompletelyEmpty())
            {
                if(renderSection.step > 4) {
                    d = (byte) (renderSection.directionChanges + 1);
                }
                else {
                    d = 0;
                }

            }
            else
                d = renderSection.directionChanges;

            relativeChunk.directionChanges = d;
        }
    }

    public boolean scheduleUpdate(RenderSection section, int limit) {
        if(!section.isDirty())
            return false;

        if(limit <= 0)
            return false;

        section.rebuildChunkAsync(this.taskDispatcher, this.renderRegionCache);
        section.setNotDirty();
        return true;
    }

    public void compileChunks(Camera camera) {
        this.minecraft.getProfiler().push("populate_chunks_to_compile");
//        RenderRegionCache renderregioncache = new RenderRegionCache();
//        BlockPos cameraPos = camera.getBlockPosition();
//        List<RenderSection> list = Lists.newArrayList();

        this.minecraft.getProfiler().popPush("upload");

        Profiler2 profiler = Profiler2.getMainProfiler();
        profiler.push("Uploads");
        if(this.taskDispatcher.uploadAllPendingUploads())
            this.needsUpdate = true;
        profiler.pop();
        this.minecraft.getProfiler().popPush("schedule_async_compile");

//        //debug
//        Profiler p = null;
//        if(!list.isEmpty()) {
//            p = Profiler.getProfiler("compileChunks");
//            p.start();
//        }


//        for(RenderSection renderSection : list) {
//            renderSection.rebuildChunkAsync(this.taskDispatcher, renderregioncache);
////            renderSection.rebuildChunkSync(this.taskDispatcher, renderregioncache);
//            renderSection.setNotDirty();
//        }

//        if(!list.isEmpty()) {
//            p.round();
//        }
        this.minecraft.getProfiler().pop();
    }

    public boolean isChunkCompiled(BlockPos blockPos) {
        RenderSection renderSection = this.sectionGrid.getRenderSectionAt(blockPos);
        return renderSection != null && renderSection.isCompiled();
    }

    public void allChanged() {
        resetOrigin();
        if (this.level != null) {
//            this.graphicsChanged();
            this.level.clearTintCaches();

            this.taskDispatcher.createThreads();

            this.needsUpdate = true;
//            this.generateClouds = true;

            this.lastViewDistance = this.minecraft.options.getEffectiveRenderDistance();
            if (this.sectionGrid != null) {
                this.sectionGrid.releaseAllBuffers();
            }

            this.taskDispatcher.clearBatchQueue();
            synchronized(this.globalBlockEntities) {
                this.globalBlockEntities.clear();
            }

            this.sectionGrid = new SectionGrid(this.level, this.minecraft.options.getEffectiveRenderDistance());
            this.chunkAreaQueue = new AreaSetQueue(this.sectionGrid.chunkAreaManager.size);

            if(this.indirectBuffers != null) {
                for (IndirectBuffer buffer : this.indirectBuffers) {
                    buffer.freeBuffer();
                }
            }

            this.allocateIndirectBuffers();

            Entity entity = this.minecraft.getCameraEntity();
            if (entity != null) {
                this.sectionGrid.repositionCamera(entity.getX(), entity.getZ());
            }

        }
    }

    private void resetOrigin() {
        originX=originX-Mth.floor(originX);
        originZ=originZ-Mth.floor(originZ);
    }

    public void setLevel(@Nullable ClientLevel level) {
        this.lastCameraX = Float.MIN_VALUE;
        this.lastCameraY = Float.MIN_VALUE;
        this.lastCameraZ = Float.MIN_VALUE;
        this.lastCameraSectionX = Integer.MIN_VALUE;
        this.lastCameraSectionY = Integer.MIN_VALUE;
        this.lastCameraSectionZ = Integer.MIN_VALUE;
//        this.entityRenderDispatcher.setLevel(level);
        this.level = level;
        if (level != null) {
            if(this.sectionGrid == null) {
                this.allChanged();
            }
        } else {
            if (this.sectionGrid != null) {
                this.sectionGrid.releaseAllBuffers();
                this.sectionGrid = null;
            }

            this.taskDispatcher.stopThreads();

            this.needsUpdate = true;
        }

    }
    @SuppressWarnings("all")
    public void renderChunkLayer(RenderType renderType, double camX, double camY, double camZ, Matrix4f projection) {
        //debug
//        Profiler p = Profiler.getProfiler("chunks");
//        Profiler2 p = Profiler2.getMainProfiler();

        final TerrainRenderType layerName=switch (renderType.name)
        {
            case "cutout_mipped" -> CUTOUT_MIPPED;
            case "translucent" -> TRANSLUCENT;
            default -> null;
        };
        if(!(COMPACT_RENDER_TYPES).contains(layerName)) return;

//        p.pushMilestone("layer " + layerName);
//        if(layerName.equals(CUTOUT_MIPPED))
//            p.push("Opaque_terrain_pass");
//        else if(layerName.equals(TRANSLUCENT))
//        {
//            p.pop();
//            p.push("Translucent_terrain_pass");
//        }




        RenderSystem.assertOnRenderThread();
        renderType.setupRenderState();

        this.sortTranslucentSections(camX, camY, camZ);

//        this.minecraft.getProfiler().push("filterempty");
//        this.minecraft.getProfiler().popPush(() -> {
//            return "render_" + renderType;
//        });
        boolean indirectDraw = Initializer.CONFIG.indirectDraw;

        VRenderSystem.applyMVP(translationOffset, projection);

        Drawer drawer = Drawer.getInstance();
        drawer.bindPipeline(ShaderManager.shaderManager.terrainDirectShader);
        final VkCommandBuffer commandBuffer = Drawer.getCommandBuffer();
        final long address = commandBuffer.address();
        final boolean b = layerName == TRANSLUCENT;
        if(!b) drawer.bindAutoIndexBuffer(commandBuffer, 7);

//        p.push("draw batches");

        final long suPtr = b ? TPtr : SPtr;
        if(Initializer.CONFIG.bindless) {
            nvkCmdBindVertexBuffers(commandBuffer, 0, 1, suPtr, (VUtil.nullptr));
        }
        layerName.setCutoutUniform();
        ShaderManager.shaderManager.terrainDirectShader.bindDescriptorSets(commandBuffer, Drawer.getCurrentFrame());
        if((COMPACT_RENDER_TYPES).contains(layerName)) {
            if(!Initializer.CONFIG.bindless) drawBatchedIndexed(b, address, suPtr);
            else drawBatchedIndexedBindless(b, address);
        }

//        if(layerName.equals(CUTOUT)/* || layerName.equals(TRIPWIRE)*/) {
//            indirectBuffers[Drawer.getCurrentFrame()].submitUploads();
////            uniformBuffers.submitUploads();
//        }
//        p.pop();

        //Need to reset push constant in case the pipeline will still be used for rendering
//        if(!indirectDraw) {
//            VRenderSystem.setChunkOffset(0, 0, 0);
//            drawer.pushConstants(pipeline);
//        }

//        this.minecraft.getProfiler().pop();
        renderType.clearRenderState();
//        VRenderSystem.applyModelViewMatrix(RenderSystem.getModelViewMatrix());
        VRenderSystem.copyMVP();


    }

    private void drawBatchedIndexed(boolean b, long address, long sPtr) {
        for (VkDrawIndexedIndirectCommand2 drawParameters : b ? this.TsectionQueue : this.sectionQueue) {
            {
                VUtil.UNSAFE.putLong(vOffset, drawParameters.vertexOffset());
                callPPPV(address, 0, 1, sPtr, vOffset, functionAddress);

                callPV(address, drawParameters.indexCount(), 1, 0, 0, 0, functionAddress1);
            }
        }
    }
    private void drawBatchedIndexedBindless(boolean b, long address) {
        for (VkDrawIndexedIndirectCommand2 drawParameters : b ? this.TsectionQueue : this.sectionQueue) {
            {
                callPV(address, drawParameters.indexCount(), 1, 0, drawParameters.vertexOffset() / DrawBuffers.VERTEX_SIZE, 0, functionAddress1);
            }
        }
    }

    private void sortTranslucentSections(double camX, double camY, double camZ) {
        this.minecraft.getProfiler().push("translucent_sort");
        double d0 = camX - this.xTransparentOld;
        double d1 = camY - this.yTransparentOld;
        double d2 = camZ - this.zTransparentOld;
//        if (d0 * d0 + d1 * d1 + d2 * d2 > 1.0D) {
        if (d0 * d0 + d1 * d1 + d2 * d2 > 2.0D) {
            this.xTransparentOld = camX;
            this.yTransparentOld = camY;
            this.zTransparentOld = camZ;
            int j = 0;

//                for(QueueChunkInfo chunkInfo : this.sectionsInFrustum) {
//                    if (j < 15 && chunkInfo.chunk.resortTransparency(renderType, this.taskDispatcher)) {
//                        ++j;
//                    }
//                }

            Iterator<RenderSection> iterator = this.chunkQueue.iterator(false);

            while(iterator.hasNext() && j < 15) {
                RenderSection section = iterator.next();

                section.resortTransparency(TRANSLUCENT, this.taskDispatcher);

                ++j;
            }
        }

        this.minecraft.getProfiler().pop();
    }

    public void renderBlockEntities(PoseStack poseStack, double camX, double camY, double camZ,
                                    Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress, float gameTime) {
        MultiBufferSource bufferSource = this.renderBuffers.bufferSource();

        for(RenderSection renderSection : this.chunkQueue) {
            List<BlockEntity> list = renderSection.getCompiledSection().getRenderableBlockEntities();
            if (!list.isEmpty()) {
                for(BlockEntity blockentity1 : list) {
                    BlockPos blockpos4 = blockentity1.getBlockPos();
                    MultiBufferSource multibuffersource1 = bufferSource;
                    poseStack.pushPose();
                    poseStack.translate((double)blockpos4.getX() - camX, (double)blockpos4.getY() - camY, (double)blockpos4.getZ() - camZ);
                    SortedSet<BlockDestructionProgress> sortedset = destructionProgress.get(blockpos4.asLong());
                    if (sortedset != null && !sortedset.isEmpty()) {
                        int j1 = sortedset.last().getProgress();
                        if (j1 >= 0) {
                            PoseStack.Pose posestack$pose1 = poseStack.last();
                            VertexConsumer vertexconsumer = new SheetedDecalTextureGenerator(this.renderBuffers.crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(j1)), posestack$pose1.pose(), posestack$pose1.normal(), 1.0f);
                            multibuffersource1 = (p_194349_) -> {
                                VertexConsumer vertexconsumer3 = bufferSource.getBuffer(p_194349_);
                                return p_194349_.affectsCrumbling() ? VertexMultiConsumer.create(vertexconsumer, vertexconsumer3) : vertexconsumer3;
                            };
                        }
                    }

                    this.minecraft.getBlockEntityRenderDispatcher().render(blockentity1, gameTime, poseStack, multibuffersource1);
                    poseStack.popPose();
                }
            }
        }
    }

//    public UniformBuffers getUniformBuffers() { return this.uniformBuffers; }

    public void setNeedsUpdate() {
        this.needsUpdate = true;
    }

    public void setSectionDirty(int x, int y, int z, boolean flag) {
        this.sectionGrid.setDirty(x, y, z, flag);
    }

    public void setSectionsLightReady(int x, int z) {
//        List<RenderSection> list = this.sectionGrid.getRenderSectionsAt(x, z);
//        list.forEach(section -> section.setLightReady(true));
//        this.needsUpdate = true;
    }

    public ChunkAreaManager getChunkAreaManager() {
        return this.sectionGrid.chunkAreaManager;
    }

    public String getChunkStatistics() {
        int i = this.sectionGrid.chunks.length;
//        int j = this.sectionsInFrustum.size();
        int j = this.chunkQueue.size();
        String tasksInfo = this.taskDispatcher == null ? "null" : this.taskDispatcher.getStats();
        return String.format("Chunks: %d(%d)/%d D: %d, %s", this.nonEmptyChunks, j, i, this.lastViewDistance, tasksInfo);
    }

}
