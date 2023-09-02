package net.vulkanmod.render.chunk.build;

import com.google.common.collect.Queues;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.*;
import net.vulkanmod.render.vertex.TerrainRenderType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Queue;
import java.util.concurrent.Executor;

public class TaskDispatcher implements Executor {
    public static final int availableProcessors = Runtime.getRuntime().availableProcessors();
    private int highPriorityQuota = 2;

    private final Queue<Runnable> toUpload = Queues.newLinkedBlockingDeque();
    public final ThreadBuilderPack fixedBuffers;

    //TODO volatile?
    private boolean stopThreads;
    private Thread[] threads;
    private int idleThreads;
    private final Queue<ChunkTask> highPriorityTasks = Queues.newConcurrentLinkedQueue();
    private final Queue<ChunkTask> lowPriorityTasks = Queues.newConcurrentLinkedQueue();

    public TaskDispatcher() {
        this.fixedBuffers = new ThreadBuilderPack();

        this.stopThreads = true;
    }

    public void createThreads() {
        if(!this.stopThreads)
            return;

        this.stopThreads = false;

        this.threads = new Thread[Initializer.CONFIG.chunkLoadFactor*availableProcessors];

        for (int i = 0; i < threads.length; i++) {
            ThreadBuilderPack builderPack = new ThreadBuilderPack();
            Thread thread = new Thread(
                    () -> runTaskThread(builderPack));

            this.threads[i] = thread;
            thread.start();
        }
    }

    private void runTaskThread(ThreadBuilderPack builderPack) {
        while(!this.stopThreads) {
            ChunkTask task = this.pollTask();

            if(task == null)
                synchronized (this) {
                    try {
                        this.idleThreads++;
                        this.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    this.idleThreads--;
                }

            if(task == null)
                continue;

            task.doTask(builderPack);
        }
    }

    public void schedule(ChunkTask chunkTask) {
        if(chunkTask == null)
            return;

        if (chunkTask.highPriority) {
                this.highPriorityTasks.offer(chunkTask);
            } else {
                this.lowPriorityTasks.offer(chunkTask);
            }

        synchronized (this) {
            notify();
        }
    }

    @Nullable
    private ChunkTask pollTask() {
        ChunkTask task = this.highPriorityTasks.poll();

        if(task == null)
            task = this.lowPriorityTasks.poll();

        return task;
    }

    public void stopThreads() {
        if(this.stopThreads)
            return;

        this.stopThreads = true;

        synchronized (this) {
            notifyAll();
        }

        for (Thread thread : this.threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public boolean uploadAllPendingUploads() {

        boolean flag = !this.toUpload.isEmpty();
        while(!this.toUpload.isEmpty()) {
            this.toUpload.poll().run();
        }

        AreaUploadManager.INSTANCE.submitUploads();

        return flag;
    }

    public void scheduleSectionUpdate(RenderSection section, EnumMap<TerrainRenderType, UploadBuffer> uploadBuffers) {
        this.toUpload.add(
                () -> this.doSectionUpdate(section, uploadBuffers)
        );
    }

    private void doSectionUpdate(RenderSection section, EnumMap<TerrainRenderType, UploadBuffer> uploadBuffers) {
        ChunkArea renderArea = section.getChunkArea();
        DrawBuffers drawBuffers = renderArea.getDrawBuffers();

        for(TerrainRenderType renderType : TerrainRenderType.values()) {
            UploadBuffer uploadBuffer = uploadBuffers.get(renderType);

            if(uploadBuffer != null) {
                drawBuffers.upload(uploadBuffer, section.drawParametersArray[renderType.ordinal()], section.xOffset(), section.yOffset(), section.zOffset(), renderType);
            } else {
                section.drawParametersArray[renderType.ordinal()].reset();
            }
        }
    }

    public void scheduleUploadChunkLayer(RenderSection section, TerrainRenderType renderType, UploadBuffer uploadBuffer) {
        this.toUpload.add(
                () -> this.doUploadChunkLayer(section, renderType, uploadBuffer)
        );
    }

    private void doUploadChunkLayer(RenderSection section, TerrainRenderType renderType, UploadBuffer uploadBuffer) {
        ChunkArea renderArea = section.getChunkArea();
        DrawBuffers drawBuffers = renderArea.getDrawBuffers();

        drawBuffers.upload(uploadBuffer, section.drawParametersArray[renderType.ordinal()], section.xOffset(), section.yOffset(), section.zOffset(), renderType);
    }

    public int getIdleThreadsCount() {
        return this.idleThreads;
    }

    public void clearBatchQueue() {
        while(!this.highPriorityTasks.isEmpty()) {
            ChunkTask chunkTask = this.highPriorityTasks.poll();
            if (chunkTask != null) {
                chunkTask.cancel();
            }
        }

        while(!this.lowPriorityTasks.isEmpty()) {
            ChunkTask chunkTask = this.lowPriorityTasks.poll();
            if (chunkTask != null) {
                chunkTask.cancel();
            }
        }

//        this.toBatchCount = 0;
    }

    public String getStats() {
//        this.toBatchCount = this.highPriorityTasks.size() + this.lowPriorityTasks.size();
//        return String.format("tB: %03d, toUp: %02d, FB: %02d", this.toBatchCount, this.toUpload.size(), this.freeBufferCount);
        return String.format("iT: %d", this.idleThreads);
    }

    @Override
    public void execute(@NotNull Runnable command) {
        command.run();
    }
}
