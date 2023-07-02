package net.vulkanmod.vulkan;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.util.Mth;
import net.vulkanmod.Initializer;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.queue.QueueFamilyIndices;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static net.vulkanmod.vulkan.Vulkan.*;
import static net.vulkanmod.vulkan.util.VUtil.UINT32_MAX;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.system.Checks.check;
import static org.lwjgl.system.JNI.callPPPPI;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memAddressSafe;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class SwapChain {

//    final Framebuffer fakeFBO;
    private long swapChain = VK_NULL_HANDLE;
    private List<Long> swapChainImages;
    private VkExtent2D extent2D;
    private List<Long> imageViews;
    public static boolean isBGRAformat;
    private boolean vsync;

    private final int framesNum;

    private int[] currentLayout;
    private int swapChainFormat;
    private final LongArrayList retiredSwapChains = new LongArrayList();
    private boolean modeChange;

    public SwapChain() {

        this.framesNum = Initializer.CONFIG.frameQueueSize-1;
        createSwapChain(this.framesNum);
        MemoryManager.createInstance(this.swapChainImages.size());

//        this.fakeFBO=new Framebuffer(this.swapChainFormat, extent2D.width(), extent2D.height(), true, Framebuffer.AttachmentTypes.COLOR, Framebuffer.AttachmentTypes.DEPTH);


    }

    public void recreateSwapChain() {
        Synchronization.INSTANCE.waitFences();

        createSwapChain(this.framesNum);

        int framesNum = this.swapChainImages.size();

        if (MemoryManager.getFrames() != framesNum) {
            MemoryManager.createInstance(framesNum);
        }

        Drawer.tstFrameBuffer2.recreate(extent2D.width(), extent2D.height());
    }
    private void createSwapChain(int preferredImageCount) {

        try(MemoryStack stack = stackPush()) {
            VkDevice device = Vulkan.getDevice();
            SwapChainSupportDetails swapChainSupport = querySwapChainSupport(device.getPhysicalDevice(), stack);

            VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(swapChainSupport.formats);
            int presentMode = chooseSwapPresentMode(swapChainSupport.presentModes);
            VkExtent2D extent = chooseSwapExtent(swapChainSupport.capabilities);

            //Workaround for Mesa
            IntBuffer imageCount = stack.ints(preferredImageCount);
//            IntBuffer imageCount = stack.ints(Math.max(swapChainSupport.capabilities.minImageCount(), preferredImageCount));


            this.swapChainFormat = surfaceFormat.format();
            this.extent2D = VkExtent2D.create().set(extent);

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.callocStack(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(Vulkan.getSurface());

            // Image settings


            createInfo.minImageCount(imageCount.get(0));
            createInfo.imageFormat(this.swapChainFormat);
            createInfo.imageColorSpace(surfaceFormat.colorSpace());
            createInfo.imageExtent(extent);
            createInfo.imageArrayLayers(1);
            createInfo.imageUsage(VK_IMAGE_USAGE_TRANSFER_SRC_BIT|VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

//            Queue.QueueFamilyIndices indices = Queue.getQueueFamilies();


            if(QueueFamilyIndices.graphicsFamily != QueueFamilyIndices.presentFamily) {
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                createInfo.pQueueFamilyIndices(stack.ints(QueueFamilyIndices.graphicsFamily, QueueFamilyIndices.presentFamily));
            } else {
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }


            createInfo.preTransform(swapChainSupport.capabilities.currentTransform());
            createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode);
            createInfo.clipped(true);
            long oldSwapChain = swapChain;


            //Nvidia bug: With MAILBOX: if prior SwapChain was created with FIFO, it is considered retired, even if vkCreateSwapchainKHR has not been called yet
            createInfo.oldSwapchain(swapChain);

            LongBuffer pSwapChain = stack.longs(VK_NULL_HANDLE);

            final int i1 = vkCreateSwapchainKHR(device, createInfo, null, pSwapChain);
            if(i1 != VK_SUCCESS) {
                throw new RuntimeException("Failed to create swap chain "+i1);
            }



            swapChain = pSwapChain.get(0);
//            if(swapChain==VK_NULL_HANDLE) {
//                swapChain=oldSwapChain;
//                oldSwapChain=VK_NULL_HANDLE;
//            }
//TODO:--->!            vkGetSwapchainImagesKHR(device, swapChain, imageCount, null);

            LongBuffer pSwapchainImages = stack.mallocLong(imageCount.get(0));

            vkGetSwapchainImagesKHR(device, swapChain, imageCount, pSwapchainImages);

            swapChainImages = new ArrayList<>(imageCount.get(0));

            for(int i = 0;i < pSwapchainImages.capacity();i++) {
                swapChainImages.add(pSwapchainImages.get(i));
            }

            if(oldSwapChain != VK_NULL_HANDLE && oldSwapChain!=swapChain) {
                this.imageViews.forEach(imageView -> vkDestroyImageView(device, imageView, null));
//                if(modeChange) retiredSwapChains.add(oldSwapChain);
            }
////                this.imageViews.forEach(imageView -> vkDestroyImageView(device, imageView, null));
//               if(presentMode!=VK_PRESENT_MODE_MAILBOX_KHR) vkDestroySwapchainKHR(device, oldSwapChain, null);
//               else
//               {
//                   oldSwapChains.add(oldSwapChain);
////                   final Drawer instance = Drawer.getInstance();
////                   instance.vkQueuePresent(stack, stack.ints(instance.oldestFrameIndex));
//               }
//
//            }
            createImageViews(this.swapChainFormat);

            currentLayout = new int[this.swapChainImages.size()];
            
            modeChange=false;

        }
    }

    //    public void transitionImageLayout(MemoryStack stack, VkCommandBuffer commandBuffer, int newLayout, int frame) {
//        VulkanImage.transitionImageLayout(stack, commandBuffer, this.getImageId(frame), this.fakeFBO.getFormat(), this.currentLayout[frame], newLayout, 1);
//        this.currentLayout[frame] = newLayout;
//    }

//    public void colorAttachmentLayout(MemoryStack stack, VkCommandBuffer commandBuffer, int frame) {
//            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.callocStack(1, stack);
//            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
//            barrier.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
//            barrier.oldLayout(this.currentLayout[frame]);
////            barrier.oldLayout(VK_IMAGE_LAYOUT_UNDEFINED);
//            barrier.newLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
//            barrier.image(this.swapChainImages.get(frame));
////            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
//
//            barrier.subresourceRange().baseMipLevel(0);
//            barrier.subresourceRange().levelCount(1);
//            barrier.subresourceRange().baseArrayLayer(0);
//            barrier.subresourceRange().layerCount(1);
//
//            barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
//
//            vkCmdPipelineBarrier(commandBuffer,
//                    VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,  // srcStageMask
//                    VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, // dstStageMask
//                    0,
//                    null,
//                    null,
//                    barrier// pImageMemoryBarriers
//            );
//
//            this.currentLayout[frame] = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
//    }

//    public void presentLayout(MemoryStack stack, VkCommandBuffer commandBuffer, int frame) {
//
//        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
//        barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
//        barrier.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
//        barrier.oldLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
//        barrier.newLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
//        barrier.image(this.swapChainImages.get(frame));
//
//        barrier.subresourceRange().baseMipLevel(0);
//        barrier.subresourceRange().levelCount(1);
//        barrier.subresourceRange().baseArrayLayer(0);
//        barrier.subresourceRange().layerCount(1);
//
//        barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
//
//        vkCmdPipelineBarrier(commandBuffer,
//                VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,  // srcStageMask
//                VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, // dstStageMask
//                0,
//                null,
//                null,
//                barrier// pImageMemoryBarriers
//        );
//
//        this.currentLayout[frame] = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
//    }

    public void cleanUp() {
        VkDevice device = Vulkan.getDevice();
        System.out.println("Size: "+ retiredSwapChains.size());
        for (int i = 0; i < retiredSwapChains.size(); i++) {
            long a = retiredSwapChains.getLong(i);
            System.out.println(i +" "+a);
            vkDestroySwapchainKHR(device, a, null);
        }
        vkDestroySwapchainKHR(device, this.swapChain, null);
        imageViews.forEach(imageView -> vkDestroyImageView(device, imageView, null));

        Drawer.tstFrameBuffer2.cleanUp();
    }

    private void createImageViews(int format) {
        imageViews = new ArrayList<>(swapChainImages.size());

        for(long swapChainImage : swapChainImages) {
            imageViews.add(VulkanImage.createImageView(swapChainImage, format, VK_IMAGE_ASPECT_COLOR_BIT, 1));
        }

    }

    public long getId() {
        return swapChain;
    }

    public List<Long> getImages() {
        return swapChainImages;
    }

    public long getImageId(int i) {
        return swapChainImages.get(i);
    }

    public VkExtent2D getExtent() {
        return extent2D;
    }

    public List<Long> getImageViews() {
        return this.imageViews;
    }

    public long getImageView(int i) { return this.imageViews.get(i); }

    static SwapChainSupportDetails querySwapChainSupport(VkPhysicalDevice device, MemoryStack stack) {

        long surface = Vulkan.getSurface();
        SwapChainSupportDetails details = new SwapChainSupportDetails();

        details.capabilities = VkSurfaceCapabilitiesKHR.mallocStack(stack);
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, details.capabilities);

        IntBuffer count = stack.ints(0);

        vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, null);

        if(count.get(0) != 0) {
            details.formats = VkSurfaceFormatKHR.mallocStack(count.get(0), stack);
            vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, details.formats);
        }

        vkGetPhysicalDeviceSurfacePresentModesKHR(device,surface, count, null);

        if(count.get(0) != 0) {
            details.presentModes = stack.mallocInt(count.get(0));
            vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, details.presentModes);
        }

        return details;
    }

    private static VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats) {
        List<VkSurfaceFormatKHR> list = availableFormats.stream().toList();

        VkSurfaceFormatKHR format = list.get(0);
        boolean flag = true;

        for (VkSurfaceFormatKHR availableFormat : list) {
            if (availableFormat.format() == VK_FORMAT_R8G8B8A8_UNORM && availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                return availableFormat;

            if (availableFormat.format() == VK_FORMAT_B8G8R8A8_UNORM && availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                format = availableFormat;
                flag = false;
            }
        }

        if(format.format() == VK_FORMAT_B8G8R8A8_UNORM) isBGRAformat = true;
        return format;
    }

    private int chooseSwapPresentMode(IntBuffer availablePresentModes) {
        return vsync ? VK_PRESENT_MODE_MAILBOX_KHR|VK_PRESENT_MODE_FIFO_KHR : VK_PRESENT_MODE_IMMEDIATE_KHR;
//
//        //fifo mode is the only mode that has to be supported
//        if(requestedMode == VK_PRESENT_MODE_FIFO_KHR) return VK_PRESENT_MODE_FIFO_KHR;
//
//        for(int i = 0;i < availablePresentModes.capacity();i++) {
//            if(availablePresentModes.get(i) == requestedMode) {
//                return requestedMode;
//            }
//        }
//
//        Initializer.LOGGER.warn("Requested mode not supported: using fallback VK_PRESENT_MODE_FIFO_KHR");
//        return VK_PRESENT_MODE_FIFO_KHR;

    }

    private static VkExtent2D chooseSwapExtent(VkSurfaceCapabilitiesKHR capabilities) {

        if(capabilities.currentExtent().width() != UINT32_MAX) {
            return capabilities.currentExtent();
        }

        IntBuffer width = stackGet().ints(0);
        IntBuffer height = stackGet().ints(0);

        glfwGetFramebufferSize(window, width, height);

        VkExtent2D actualExtent = VkExtent2D.mallocStack().set(width.get(0), height.get(0));

        VkExtent2D minExtent = capabilities.minImageExtent();
        VkExtent2D maxExtent = capabilities.maxImageExtent();

        actualExtent.width(Mth.clamp(minExtent.width(), maxExtent.width(), actualExtent.width()));
        actualExtent.height(Mth.clamp(minExtent.height(), maxExtent.height(), actualExtent.height()));

        return actualExtent;
    }

    static class SwapChainSupportDetails {

        VkSurfaceCapabilitiesKHR capabilities;
        VkSurfaceFormatKHR.Buffer formats;
        IntBuffer presentModes;

    }

    public boolean isVsync() {
        return vsync;
    }

    public void setVsync(boolean vsync) {
        this.modeChange=true;
        this.vsync = vsync;
    }
}
