package net.vulkanmod.vulkan;

import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static net.vulkanmod.vulkan.Vulkan.*;
import static org.lwjgl.system.Checks.CHECKS;
import static org.lwjgl.system.Checks.check;
import static org.lwjgl.system.JNI.callPPPPI;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memAddressSafe;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class Framebuffer {

    private static final int colorID=0;
    private static final int depthID=1;
    public static final int DEFAULT_FORMAT = VK_FORMAT_R8G8B8A8_UNORM;
    private long frameBuffer;

    private final int format;
    private static final int depthFormat = findDepthFormat();
    public int width, height;
    public final long renderPass;

    private final int attachmentCount;

//    private List<VulkanImage> images;
    private VulkanImage colorAttachment;
    protected VulkanImage depthAttachment;

//    public Framebuffer(int width, int height, int format) {
//        this(width, height, format, false);
//    }
//
//    public Framebuffer(int width, int height, int format, boolean blur) {
//        this.format = format;
//        this.depthFormat = Vulkan.findDepthFormat();
//        this.width = width;
//        this.height = height;
//
//        this.colorAttachment = VulkanImage.createTextureImage(format, 1, width, height, VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT, 0, blur, true);
//
//        createDepthResources(blur);
////        createFramebuffers(width, height);
//    }

    public Framebuffer(VulkanImage colorAttachment, int attachmentCount) {
        this.width = colorAttachment.width;
        this.height = colorAttachment.height;

        this.colorAttachment = colorAttachment;
        this.format=colorAttachment.format;
        this.attachmentCount = attachmentCount;

        this.renderPass=createRenderPass();

        createDepthResources(false);
        this.frameBuffer=createFramebuffers();
    }

    protected Framebuffer(int swapChainFormat, VkExtent2D extent2D, int attachmentCount)
    {
        this.width = extent2D.width();
        this.height = extent2D.height();
        this.format=swapChainFormat;
        this.attachmentCount = attachmentCount;

        this.renderPass=createRenderPass();

        createDepthResources(false);
        this.frameBuffer=createFramebuffers();
    }
    private  long createFramebuffers() {
        try (MemoryStack stack = stackPush()) {

            if(this.frameBuffer!=VK_NULL_HANDLE) {
                vkDestroyFramebuffer(getDevice(), this.frameBuffer, null);
            }


            //attachments = stack.mallocLong(1);
            LongBuffer pFramebuffer = stack.mallocLong(1);

            VkFramebufferAttachmentImageInfo.Buffer vkFramebufferAttachmentImageInfo = VkFramebufferAttachmentImageInfo.calloc(attachmentCount, stack);
            VkFramebufferAttachmentImageInfo vkFramebufferAttachmentImageInfos = vkFramebufferAttachmentImageInfo.get(0)
                    .sType$Default()
                    .flags(0)
                    .width(width)
                    .height(height)
                    .pViewFormats(stack.ints(this.format))
                    .layerCount(1)
                    .usage(VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
            VkFramebufferAttachmentImageInfo vkFramebufferAttachmentImageInfos1 = vkFramebufferAttachmentImageInfo.get(1)
                    .sType$Default()
                    .flags(0)
                    .width(width)
                    .height(height)
                    .pViewFormats(stack.ints(depthFormat))
                    .layerCount(1)
                    .usage(this.depthAttachment.usage);

            VkFramebufferAttachmentsCreateInfo vkFramebufferAttachmentsCreateInfo = VkFramebufferAttachmentsCreateInfo.calloc(stack)
                    .sType$Default()
                    .pAttachmentImageInfos(vkFramebufferAttachmentImageInfo);
            // Lets allocate the create info struct once and just update the pAttachments field each iteration
            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.callocStack(stack)
                    .sType$Default()
                    .pNext(vkFramebufferAttachmentsCreateInfo)
                    .flags(VK12.VK_FRAMEBUFFER_CREATE_IMAGELESS_BIT)
                    .renderPass(renderPass)
                    .width(width)
                    .height(height)
                    .layers(1)
                    .attachmentCount(this.attachmentCount)
                    .pAttachments(null);


            if (vkCreateFramebuffer(getDevice(), framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create framebuffer");
            }

            return (pFramebuffer.get(0));

        }
    }

    private long createRenderPass() {

        try(MemoryStack stack = stackPush()) {

            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.callocStack(this.attachmentCount, stack);
            VkAttachmentReference.Buffer attachmentRefs = VkAttachmentReference.callocStack(this.attachmentCount, stack);

            // Color attachments
            VkAttachmentDescription colorAttachment = attachments.get(0);
            colorAttachment.format(this.format);
            colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
            colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(getDeviceInfo().hasLoadStoreOpNone ? VK13.VK_ATTACHMENT_STORE_OP_NONE : VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachment.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            int y = attachments.get(0).samples();

            VkAttachmentReference colorAttachmentRef = attachmentRefs.get(0)
                    .attachment(0)
                    .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            // Depth-Stencil attachments

            VkAttachmentDescription depthAttachment = attachments.get(1);
            depthAttachment.format(depthFormat);
            depthAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
            depthAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            depthAttachment.storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            depthAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            depthAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            depthAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            depthAttachment.finalLayout(getDeviceInfo().depthAttachmentOptimal);

            VkAttachmentReference depthAttachmentRef = attachmentRefs.get(1).set(1, getDeviceInfo().depthAttachmentOptimal);

            VkSubpassDescription.Buffer subpass = VkSubpassDescription.callocStack(1, stack);
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.colorAttachmentCount(1);
            subpass.pColorAttachments(VkAttachmentReference.malloc(1, stack).put(0, colorAttachmentRef));
            subpass.pDepthStencilAttachment(depthAttachmentRef);


            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.callocStack(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            renderPassInfo.pAttachments(attachments);
            renderPassInfo.pSubpasses(subpass);
            //renderPassInfo.pDependencies(dependency);

            LongBuffer pRenderPass = stack.mallocLong(1);

            if (CHECKS) {
                check(pRenderPass, 1);
            }
            long pCreateInfo = renderPassInfo.address();
            long pAllocator = memAddressSafe((VkAllocationCallbacks) null);
            long pRenderPass1 = memAddress(pRenderPass);
            long __functionAddress = getDevice().getCapabilities().vkCreateRenderPass;
            if (CHECKS) {
                VkRenderPassCreateInfo.validate(pCreateInfo);
            }
            if(callPPPPI(getDevice().address(), pCreateInfo, pAllocator, pRenderPass1, __functionAddress) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create render pass");
            }

            return pRenderPass.get(0);
        }
    }

    protected void createDepthResources(boolean blur) {

        this.depthAttachment = VulkanImage.createDepthImage(depthFormat, this.width, this.height,
                VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT/* | VK_IMAGE_USAGE_SAMPLED_BIT*/,
                blur, false);

//        VkCommandBuffer commandBuffer = Vulkan.beginImmediateCmd();
//        //Not Sure if we need this
//        this.depthAttachment.transitionImageLayout(stackPush(), commandBuffer, VK_IMAGE_LAYOUT_UNDEFINED);
//        Vulkan.endImmediateCmd();

    }
    //TODO: Start Multiple Framebuffers at the same time...
    public void beginRendering(VkCommandBuffer commandBuffer, MemoryStack stack, long colorAttachmentImageView) {
        VkRect2D renderArea = VkRect2D.malloc(stack);
        renderArea.offset().set(0, 0);
        renderArea.extent(getSwapchainExtent());

        VkRenderPassAttachmentBeginInfo vkRenderPassAttachmentBeginInfo = VkRenderPassAttachmentBeginInfo.calloc(stack)
                .sType$Default()
                .pAttachments(stack.longs(colorAttachmentImageView, depthAttachment.getImageView()));
        //Clear Color value is ignored if Load Op is Not set to Clear
        VkClearValue.Buffer clearValues = VkClearValue.malloc(2, stack);

        clearValues.get(0).color(VkClearValue.ncolor(VRenderSystem.clearColor.ptr));
        clearValues.get(1).depthStencil().set(1.0f, 0);

        VkRenderPassBeginInfo renderingInfo = VkRenderPassBeginInfo.calloc(stack)
                .sType$Default()
                .pNext(vkRenderPassAttachmentBeginInfo)
                .renderPass(this.renderPass)
                .renderArea(renderArea)
                .framebuffer(this.frameBuffer)
                .pClearValues(clearValues)
                .clearValueCount(2);

        vkCmdBeginRenderPass(commandBuffer, renderingInfo, VK_SUBPASS_CONTENTS_INLINE);
    }

    public void bindAsTexture() {
//        this.colorAttachment.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        VTextureSelector.bindFramebufferTexture(this.colorAttachment);
    }

    public VkViewport.Buffer viewport(MemoryStack stack) {
        VkViewport.Buffer viewport = VkViewport.malloc(1, stack);
        viewport.x(0.0f);
        viewport.y(this.height);
        viewport.width(this.width);
        viewport.height(-this.height);
        viewport.minDepth(0.0f);
        viewport.maxDepth(1.0f);

        return viewport;
    }

    public VkRect2D.Buffer scissor(MemoryStack stack) {
        VkRect2D.Buffer scissor = VkRect2D.malloc(1, stack);
        scissor.offset().set(0, 0);
        scissor.extent().set(this.width, this.height);

        return scissor;
    }

    public void cleanUp() {
        vkDestroyFramebuffer(getDevice(), this.frameBuffer, null);
        vkDestroyRenderPass(getDevice(), this.renderPass, null);

        if(colorAttachment!=null) this.colorAttachment.free();
        this.depthAttachment.free();
    }

    public long getDepthImageView() { return depthAttachment.getImageView(); }

    public VulkanImage getDepthAttachment() { return depthAttachment; }

    public VulkanImage getColorAttachment() { return colorAttachment; }

    public int getFormat() {
        return format;
    }

//    public void setFormat(int format) {
//        this.format = format;
//    }

    public int getDepthFormat() {
        return depthFormat;
    }

//    public void setDepthFormat(int depthFormat) {
//        this.depthFormat = depthFormat;
//    }

    public void recreate(int width, int height) {
        this.width = width;
        this.height = height;
        this.frameBuffer = createFramebuffers();
//        this.depthFormat = findDepthFormat();
        depthAttachment.free();
        if(colorAttachment!=null) this.colorAttachment.free();
        createDepthResources(false);
    }
}
