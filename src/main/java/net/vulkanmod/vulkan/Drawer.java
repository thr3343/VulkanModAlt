package net.vulkanmod.vulkan;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.render.RHandler;
import net.vulkanmod.vulkan.memory.*;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.shader.PushConstant;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.vulkanmod.vulkan.Vulkan.*;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class Drawer {

    private static int currentFrame = 0;
    private static final VkDevice device = Vulkan.getDevice();
    public static List<VkCommandBuffer> commandBuffers;

    private static final Set<Pipeline> usedPipelines = new HashSet<>();

    private static final int MAX_FRAMES_IN_FLIGHT = getSwapChainImages().size();
    private static final VertexBuffer[] vertexBuffers = new VertexBuffer[MAX_FRAMES_IN_FLIGHT];;
    private static final AutoIndexBuffer quadsIndexBuffer = new AutoIndexBuffer(100000, AutoIndexBuffer.DrawType.QUADS);
    private static final AutoIndexBuffer triangleFanIndexBuffer = new AutoIndexBuffer(1000, AutoIndexBuffer.DrawType.TRIANGLE_FAN);
    private static final AutoIndexBuffer triangleStripIndexBuffer = new AutoIndexBuffer(1000, AutoIndexBuffer.DrawType.TRIANGLE_STRIP);
    private static final UniformBuffers uniformBuffers = new UniformBuffers(200000);

    private static final PointerBuffer imageAvailableSemaphores = MemoryUtil.memAllocPointer(MAX_FRAMES_IN_FLIGHT);
    private static final PointerBuffer renderFinishedSemaphores = MemoryUtil.memAllocPointer(MAX_FRAMES_IN_FLIGHT);
    public static final PointerBuffer inFlightFences = MemoryUtil.memAllocPointer(MAX_FRAMES_IN_FLIGHT);

    private static final int commandBuffersCount = Vulkan.getSwapChainFramebuffers().size();
    private static final boolean[] activeCommandBuffers = new boolean[Vulkan.getSwapChainFramebuffers().size()];

    public static Pipeline.BlendState currentBlendState = Pipeline.DEFAULT_BLEND_STATE;
    public static Pipeline.DepthState currentDepthState = Pipeline.DEFAULT_DEPTH_STATE;
    public static Pipeline.LogicOpState currentLogicOpState = Pipeline.DEFAULT_LOGICOP_STATE;
    public static Pipeline.ColorMask currentColorMask = Pipeline.DEFAULT_COLORMASK;

//    private static Matrix4f projectionMatrix = new Matrix4f();
//    private static Matrix4f modelViewMatrix = new Matrix4f();

    public static boolean shouldRecreate = false;
    public static boolean rebuild = false;
    public static boolean skipRendering = false;

    static
    {

        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; ++i) {
            vertexBuffers[i] = new VertexBuffer(2000000, MemoryTypes.HOST_MEM);
        }



        createSyncObjects();

        for(boolean bool : activeCommandBuffers) bool = false;


        allocateCommandBuffers();
    }
    //Avoid Linux Crashes
    public static void cleanUp() {
        imageAvailableSemaphores.free();
        renderFinishedSemaphores.free();
        inFlightFences.free();
        RHandler.drawCommands.free();
        RHandler.virtualBuffer.cleanUp();
//        RHandler.virtualBufferIdx.cleanUp();
    }

    public static void draw(ByteBuffer buffer, int drawMode, VertexFormat vertexFormat, int vertexCount)
    {
        if(drawMode!=7) return;
        assertCommandBufferState();

        if(!(vertexCount > 0)) return;

        quadsIndexBuffer.checkCapacity(vertexCount);

        drawAutoIndexed(buffer, vertexBuffers[currentFrame], quadsIndexBuffer.getIndexBuffer(), drawMode, vertexFormat, vertexCount);

    }

    public static void draw(VertexBuffer vertexBuffer, IndexBuffer indexBuffer, int indexCount, int drawMode)
    {
        assertCommandBufferState();

        if(!(indexCount > 0)) return;

        draw(vertexBuffer, indexBuffer, indexCount);

    }

    public static void submitDraw()
    {
        if(skipRendering) return;

        drawFrame();
        //TODO
        //if(rebuild) rebuildInstance();
    }

    public static void initiateRenderPass() {

        try (MemoryStack stack = stackPush()) {

            IntBuffer width = stack.ints(0);
            IntBuffer height = stack.ints(0);

            glfwGetFramebufferSize(window, width, height);
            skipRendering =  Minecraft.getInstance().noRender = (width.get(0) == 0 && height.get(0) == 0);

        }

        if(skipRendering) return;


//        nvkWaitForFences(device, inFlightFences.capacity(), inFlightFences.address0(), 1, VUtil.UINT64_MAX);
        nvkWaitForFences(device, 1, inFlightFences.address(currentFrame), 1, VUtil.UINT64_MAX);

//        CompletableFuture.runAsync(MemoryManager::freeBuffers);
//        MemoryManager.getInstance().setCurrentFrame(currentFrame);
//        MemoryManager.getInstance().freeBuffers();

        resetDescriptors();

//        vkResetCommandBuffer(commandBuffers.get(currentFrame), 0);

        try(MemoryStack stack = stackPush()) {

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType$Default();
            beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack);
            renderPassInfo.sType$Default();
            renderPassInfo.renderPass(getRenderPass());
            renderPassInfo.renderArea(VkRect2D.malloc(stack).offset(VkOffset2D.malloc(stack).set(0, 0)).extent(getSwapchainExtent()));

            VkClearValue.Buffer clearValues = VkClearValue.malloc(2, stack);
            clearValues.get(0).color().float32(0, 0).float32(1, 0).float32(2, 0).float32(3, 1);
            clearValues.get(1).depthStencil().set(1.0f, 0);
            renderPassInfo.pClearValues(clearValues);

            VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

            int err = vkBeginCommandBuffer(commandBuffer, beginInfo);
            if (err != VK_SUCCESS) {
                throw new RuntimeException("Failed to begin recording command buffer:" + err);
            }

            renderPassInfo.framebuffer(getSwapChainFramebuffers().get(currentFrame));

            vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

            vkCmdSetViewport(commandBuffer, 0, Vulkan.viewport(stack));

            vkCmdSetScissor(commandBuffer, 0, Vulkan.scissor(stack));

            vkCmdSetDepthBias(commandBuffer, 0.0F, 0.0F, 0.0F);

            activeCommandBuffers[currentFrame] = true;
        }
    }

    public static void endRenderPass() {
        if(skipRendering) return;

        VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

        vkCmdEndRenderPass(commandBuffer);

        int result = vkEndCommandBuffer(commandBuffer);
        if(result != VK_SUCCESS) {
            throw new RuntimeException("Failed to record command buffer:" + result);
        }

        activeCommandBuffers[currentFrame] = false;

        vertexBuffers[currentFrame].reset();
        uniformBuffers.reset();
        Vulkan.getStagingBuffer(currentFrame).reset();
    }

    private static void allocateCommandBuffers() {
        commandBuffers = new ArrayList<>(commandBuffersCount);

        try(MemoryStack stack = stackPush()) {

            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack);
            allocInfo.sType$Default();
            allocInfo.commandPool(getCommandPool());
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(commandBuffersCount);

            PointerBuffer pCommandBuffers = stack.mallocPointer(commandBuffersCount);

            if (vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate command buffers");
            }

            for (int i = 0; i < commandBuffersCount; i++) {
                commandBuffers.add(new VkCommandBuffer(pCommandBuffers.get(i), device));
            }
        }
    }

    private static void resetDescriptors() {
        for(Pipeline pipeline : usedPipelines) {
            pipeline.resetDescriptorPool(currentFrame);
        }

        usedPipelines.clear();
    }

    private static void assertCommandBufferState() {
        if(!activeCommandBuffers[currentFrame]) throw new RuntimeException("CommandBuffer not active.");
    }

    private static void createSyncObjects() {

//        final int frameNum = Vulkan.getSwapChainImages().size();


//        renderFinishedSemaphores = new ArrayList<>(frameNum);
//        inFlightFences = new ArrayList<>(frameNum);

        try(MemoryStack stack = stackPush()) {

            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.callocStack(stack);
            semaphoreInfo.sType$Default();

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.callocStack(stack);
            fenceInfo.sType$Default();
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

//            LongBuffer pImageAvailableSemaphore = stack.mallocLong(1);
//            LongBuffer pRenderFinishedSemaphore = stack.mallocLong(1);
//            LongBuffer pFence = stack.mallocLong(1);

            for(int i = 0;i < MAX_FRAMES_IN_FLIGHT;i++) {

                if(nvkCreateSemaphore(device, semaphoreInfo.address(), NULL, imageAvailableSemaphores.address(i)) != VK_SUCCESS
                        || nvkCreateSemaphore(device, semaphoreInfo.address(), NULL, renderFinishedSemaphores.address(i)) != VK_SUCCESS
                        || nvkCreateFence(device, fenceInfo.address(), NULL, inFlightFences.address(i)) != VK_SUCCESS) {

                    throw new RuntimeException("Failed to create synchronization objects for the frame " + i);
                }



            }

        }
    }


//    public static Drawer getInstance() { return INSTANCE; }

    public static int getCurrentFrame() { return currentFrame; }

    private static void drawFrame() {

        try(MemoryStack stack = stackPush()) {

            IntBuffer pImageIndex = stack.mallocInt(1);

            int vkResult = vkAcquireNextImageKHR(device, Vulkan.getSwapChain(), -1,
                    imageAvailableSemaphores.get(currentFrame), VK_NULL_HANDLE, pImageIndex);

            if(vkResult == VK_ERROR_OUT_OF_DATE_KHR || vkResult == VK_SUBOPTIMAL_KHR || shouldRecreate) {
                shouldRecreate = false;
                recreateSwapChain();
                return;
            } else if(vkResult != VK_SUCCESS) {
                throw new RuntimeException("Cannot get image: " + vkResult);
            }

            final int imageIndex = pImageIndex.get(0);

            VkSubmitInfo submitInfo = VkSubmitInfo.mallocStack(stack);
            submitInfo.sType$Default();
            submitInfo.pNext(NULL);

            submitInfo.waitSemaphoreCount(1);
            memPutAddress(submitInfo.address() + VkSubmitInfo.PWAITSEMAPHORES, imageAvailableSemaphores.address(currentFrame));
            memPutAddress(submitInfo.address() + VkSubmitInfo.PWAITDSTSTAGEMASK, stack.npointer(VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT));

            memPutAddress(submitInfo.address() + VkSubmitInfo.PSIGNALSEMAPHORES, renderFinishedSemaphores.address(currentFrame));
            VkSubmitInfo.nsignalSemaphoreCount(submitInfo.address(), 1);

            memPutAddress(submitInfo.address() + VkSubmitInfo.PCOMMANDBUFFERS, (stack.npointer(commandBuffers.get(imageIndex))));
            VkSubmitInfo.ncommandBufferCount(submitInfo.address(), 1);

            vkResetFences(device, (inFlightFences.get(currentFrame)));

//            Synchronization.waitFences();

            if((vkResult = vkQueueSubmit(Vulkan.getGraphicsQueue(), submitInfo, inFlightFences.get(currentFrame))) != VK_SUCCESS) {
                vkResetFences(device, (inFlightFences.get(currentFrame)));
                throw new RuntimeException("Failed to submit draw command buffer: " + vkResult);
            }

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.mallocStack(stack);
            presentInfo.sType$Default();
            presentInfo.pNext(NULL);

            memPutAddress(presentInfo.address() + VkPresentInfoKHR.PWAITSEMAPHORES, renderFinishedSemaphores.address(currentFrame));
            VkPresentInfoKHR.nwaitSemaphoreCount(presentInfo.address(), 1);

            presentInfo.swapchainCount(1);
            memPutAddress(presentInfo.address() + VkPresentInfoKHR.PSWAPCHAINS, stack.npointer(Vulkan.getSwapChain()));

            presentInfo.pImageIndices(pImageIndex);
            memPutAddress(presentInfo.address() + VkPresentInfoKHR.PRESULTS, NULL);

            vkResult = vkQueuePresentKHR(Vulkan.getPresentQueue(), presentInfo);

            if(vkResult == VK_ERROR_OUT_OF_DATE_KHR || vkResult == VK_SUBOPTIMAL_KHR || shouldRecreate) {
                shouldRecreate = false;
                recreateSwapChain();
                return;
            } else if(vkResult != VK_SUCCESS) {
                throw new RuntimeException("Failed to present swap chain image");
            }

            currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;
        }
    }

    private static void recreateSwapChain() {
//        for(Long fence : inFlightFences) {
//            vkWaitForFences(device, fence, true, VUtil.UINT64_MAX);
//        }
        vkDeviceWaitIdle(device);
//        vkQueueWaitIdle(Vulkan.getPresentQueue());

        for(int i = 0; i < MAX_FRAMES_IN_FLIGHT; ++i) {
            vkDestroyFence(device, inFlightFences.get(i), null);
            vkDestroySemaphore(device, imageAvailableSemaphores.get(i), null);
            vkDestroySemaphore(device, renderFinishedSemaphores.get(i), null);
        }

//        commandBuffers.forEach(commandBuffer -> vkResetCommandBuffer(commandBuffer, 0));

        createSyncObjects();

        Vulkan.recreateSwapChain();
        currentFrame = 0;
    }

    public void rebuildInstance() {
        vkDeviceWaitIdle(device);

        Vulkan.recreateSwapChain();
//        INSTANCE = new Drawer();
    }

//    public static void setProjectionMatrix(Matrix4f mat4) {
//        projectionMatrix = mat4;
//    }
//
//    public static void setModelViewMatrix(Matrix4f mat4) {
//        modelViewMatrix = mat4;
//    }
//
//    public static Matrix4f getProjectionMatrix() {
//        return projectionMatrix;
//    }

//    public static Matrix4f getModelViewMatrix() {
//        return modelViewMatrix;
//    }

    public static AutoIndexBuffer getQuadsIndexBuffer() {
        return quadsIndexBuffer;
    }

    public static AutoIndexBuffer getTriangleFanIndexBuffer() {
        return triangleFanIndexBuffer;
    }

    private static void draw(VertexBuffer vertexBuffer, IndexBuffer indexBuffer, int indexCount) {

        Pipeline boundPipeline = ((ShaderMixed)(RenderSystem.getShader())).getPipeline();
        usedPipelines.add(boundPipeline);
        bindPipeline(boundPipeline);

        uploadAndBindUBOs(boundPipeline);

        drawIndexed(vertexBuffer, indexBuffer, indexCount);
    }

    private static void drawAutoIndexed(ByteBuffer buffer, VertexBuffer vertexBuffer, IndexBuffer indexBuffer, int drawMode, VertexFormat vertexFormat, int vertexCount) {
        vertexBuffer.copyToVertexBuffer(vertexFormat.getVertexSize(), vertexCount, buffer);

        ShaderInstance shader = RenderSystem.getShader();
        if(shader==null) shader=GameRenderer.getRendertypeCutoutMippedShader();
        Pipeline boundPipeline = ((ShaderMixed) shader).getPipeline();
        usedPipelines.add(boundPipeline);
        bindPipeline(boundPipeline);

        uploadAndBindUBOs(boundPipeline);

        int indexCount = switch (drawMode) {
            case 7 -> vertexCount * 3 / 2;
            case 6, 5 -> (vertexCount - 2) * 3;
            default -> throw new RuntimeException("unknown drawMode: " + drawMode);
        };

        drawIndexed(vertexBuffer, indexBuffer, indexCount);
    }

    public static void uploadAndBindUBOs(Pipeline pipeline) {

        try(MemoryStack stack = stackPush()) {
            VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

            //long uniformBufferOffset = this.uniformBuffers.getUsedBytes();
            long descriptorSet = pipeline.createDescriptorSets(commandBuffer, currentFrame, uniformBuffers);

            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                    pipeline.getLayout() , 0, stack.longs(descriptorSet), null);
        }
    }

    public static void drawIndexed(VertexBuffer vertexBuffer, IndexBuffer indexBuffer, int indexCount) {

        try(MemoryStack stack = stackPush()) {
            VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

            long vertexBuffers = stack.npointer(vertexBuffer.getId());
            long offsets = stack.npointer(vertexBuffer.getOffset());
//            Profiler.Push("bindVertex");
            nvkCmdBindVertexBuffers(commandBuffer, 0, 1, vertexBuffers, offsets);

            vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getId(), indexBuffer.getOffset(), VK_INDEX_TYPE_UINT16);
//            Profiler.Push("draw");
            vkCmdDrawIndexed(commandBuffer, indexCount, 1, 0, 0, 0);
        }
    }

    public static void drawIndexedBindlessIndirect() {

        VK12.vkCmdDrawIndexedIndirect(commandBuffers.get(currentFrame), RHandler.drawCmdBuffer, 0, RHandler.drawCommands.position(), 20);

    }
    public static void drawIndexedBindless(VkDrawIndexedIndirectCommand indirectCommand) {

            vkCmdDrawIndexed(commandBuffers.get(currentFrame), indirectCommand.indexCount(), indirectCommand.instanceCount(), indirectCommand.firstIndex(), indirectCommand.vertexOffset(), indirectCommand.firstInstance());

    }

    public static void bindPipeline(Pipeline pipeline) {
        VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

        currentDepthState = VRenderSystem.getDepthState();
        currentColorMask = new Pipeline.ColorMask(VRenderSystem.getColorMask());
        Pipeline.PipelineState currentState = new Pipeline.PipelineState(currentBlendState, currentDepthState, currentLogicOpState, currentColorMask);
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getHandle(currentState));

        usedPipelines.add(pipeline);
    }

    public static void pushConstants(Pipeline pipeline) {
        VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

        PushConstant pushConstant = pipeline.getPushConstant();
        pushConstant.update();
        vkCmdPushConstants(commandBuffer, pipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstant.getBuffer());
    }

    public static void setDepthBias(float units, float factor) {
        VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

        vkCmdSetDepthBias(commandBuffer, units, 0.0f, factor);
    }

    public static void clearDepthAttachment(int v) {
        if (v != (GL11C.GL_DEPTH_BUFFER_BIT)) return;
        if(skipRendering) return;

        VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

        try(MemoryStack stack = stackPush()) {
            //ClearValues have to be different for each attachment to clear, it seems it works like a buffer: color and depth attributes override themselves
//            VkClearValue colorValue = VkClearValue.callocStack(stack);
//            colorValue.color().float32(VRenderSystem.clearColor);

            //Always use Fast Stencil Clears If Possible


            VkClearAttachment.Buffer clearDepth = VkClearAttachment.malloc(1, stack);
            clearDepth.aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);
            clearDepth.clearValue().depthStencil().set(1, 0);

            //Rect to clear
            VkRect2D renderArea = VkRect2D.malloc(stack);
            renderArea.offset().set(0,0);
            renderArea.extent(getSwapchainExtent());

            VkClearRect.Buffer pRect = VkClearRect.malloc(1, stack);
            pRect.rect(renderArea);
            pRect.baseArrayLayer(0);
            pRect.layerCount(1);

            vkCmdClearAttachments(commandBuffer, clearDepth, pRect);
        }
    }

    public static void setViewport(int x, int y, int width, int height) {

        try(MemoryStack stack = stackPush()) {
            VkViewport.Buffer viewport = VkViewport.mallocStack(1, stack);
            viewport.x(x);
            viewport.y(height + y);
            viewport.width(width);
            viewport.height(-height);
            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);

            vkCmdSetViewport(commandBuffers.get(currentFrame), 0, viewport);
        }

    }

    public static void pushDebugSection(String s) {
//        VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);
//
//        try(MemoryStack stack = stackPush()) {
//            VkDebugUtilsLabelEXT markerInfo = VkDebugUtilsLabelEXT.callocStack(stack);
//            markerInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_LABEL_EXT);
//            ByteBuffer string = stack.UTF8(s);
//            markerInfo.pLabelName(string);
//            vkCmdBeginDebugUtilsLabelEXT(commandBuffer, markerInfo);
//        }
    }

    public static void popDebugSection() {
//        VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);
//
//        vkCmdEndDebugUtilsLabelEXT(commandBuffer);
    }

    public static void popPushDebugSection(String s) {
        popDebugSection();
        pushDebugSection(s);
    }
}
