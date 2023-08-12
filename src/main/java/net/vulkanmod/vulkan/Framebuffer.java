package net.vulkanmod.vulkan;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static net.vulkanmod.vulkan.Framebuffer.AttachmentTypes.COLOR;
import static net.vulkanmod.vulkan.Vulkan.*;
import static net.vulkanmod.vulkan.texture.VulkanImage.createImageView;
import static org.lwjgl.system.Checks.CHECKS;
import static org.lwjgl.system.Checks.check;
import static org.lwjgl.system.JNI.callPPPPI;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class Framebuffer {

    private static final int colorID=0;
    private static final int depthID=1;
    public static final int DEFAULT_FORMAT = SwapChain.isBGRAformat ? VK_FORMAT_B8G8R8A8_UNORM : VK_FORMAT_R8G8B8A8_UNORM;
    private long frameBuffer;

    private final int format;
    private static final int depthFormat = findDepthFormat();
//    private static final ObjectArrayList<FramebufferInfo> frameBuffers = new ObjectArrayList<>(8);

    public int width, height;
    public final long renderPass;


//    private List<VulkanImage> images;
    private final VulkanImage colorAttachment;
    protected VulkanImage depthAttachment;
    private final imageAttachmentReference[] attachments;
    private final boolean isSwapChainMode;
    private final AttachmentTypes[] attachmentTypes;



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


    public enum AttachmentTypes
    {
        COLOR(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, DEFAULT_FORMAT, VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT),
        DEPTH(getDeviceInfo().depthAttachmentOptimal, depthFormat, VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT);

        private final int layout, format, usage;

        AttachmentTypes(int depthAttachmentOptimal, int depthFormat, int i) {

            this.layout = depthAttachmentOptimal;
            format = depthFormat;
            this.usage = i;
        }
    }
    public Framebuffer(VulkanImage colorAttachment, AttachmentTypes... attachmentTypes) {
        this.width = colorAttachment.width;
        this.height = colorAttachment.height;
        this.isSwapChainMode = false;
        this.colorAttachment = colorAttachment;
        this.format=colorAttachment.format;
        this.attachmentTypes = attachmentTypes;


        attachments = new imageAttachmentReference[attachmentTypes.length];
        this.renderPass=createRenderPass(this.attachmentTypes);

        createDepthResources(false);
        this.frameBuffer=createFramebuffers(this.attachmentTypes);
    }

    protected Framebuffer(int swapChainFormat, int width, int height, boolean isSwapChain, AttachmentTypes... attachmentTypes)
    {
        this.width = width;
        this.height = height;
        this.format=swapChainFormat;
        this.isSwapChainMode = isSwapChain;
        this.attachmentTypes = attachmentTypes;

        attachments = new imageAttachmentReference[attachmentTypes.length];
        this.renderPass=createRenderPass(attachmentTypes);
        this.colorAttachment = (isSwapChain) ? null : new VulkanImage(this.format, 1, width, height, COLOR.usage, 0);
       if(!isSwapChain) {
           colorAttachment.createImage(1, width, height, format,  COLOR.usage);
           colorAttachment.imageView = createImageView(colorAttachment.getId(), format, VK_IMAGE_ASPECT_COLOR_BIT, 1);
       }
        createDepthResources(false);
        this.frameBuffer=createFramebuffers(attachmentTypes);
    }

    private  long createFramebuffers(AttachmentTypes[] attachmentTypes) {
        try (MemoryStack stack = stackPush()) {

            //attachments = stack.mallocLong(1);
            LongBuffer pFramebuffer = stack.mallocLong(1);


            VkFramebufferAttachmentImageInfo.Buffer vkFramebufferAttachmentImageInfo = VkFramebufferAttachmentImageInfo.calloc(attachmentTypes.length, stack);
            int i=0;
            for(var attachmentImageInfo : attachmentTypes) {

                VkFramebufferAttachmentImageInfo vkFramebufferAttachmentImageInfos = vkFramebufferAttachmentImageInfo.get(i)
                        .sType$Default()
                        .pNext(NULL)
                        .flags(0)
                        .width(width)
                        .height(height)
                        .pViewFormats(stack.ints(attachmentImageInfo.format))
                        .layerCount(1)
                        .usage(attachmentImageInfo.usage);
                i++;
            }


            VkFramebufferAttachmentsCreateInfo vkFramebufferAttachmentsCreateInfo = VkFramebufferAttachmentsCreateInfo.calloc(stack)
                    .sType$Default()
                    .pAttachmentImageInfos(vkFramebufferAttachmentImageInfo);
            // Lets allocate the create info struct once and just update the pAttachments field each iteration

            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack)
                    .sType$Default()
                    .pNext(vkFramebufferAttachmentsCreateInfo)
                    .flags(VK12.VK_FRAMEBUFFER_CREATE_IMAGELESS_BIT)
                    .renderPass(renderPass)
                    .width(width)
                    .height(height)
                    .layers(1)
                    .attachmentCount(this.attachmentTypes.length)
                    .pAttachments(null);


            if (vkCreateFramebuffer(getDevice(), framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create framebuffer");
            }

            return (pFramebuffer.get(0));

        }
    }


    private long createRenderPass(AttachmentTypes[] attachmentTypes) {

        try(MemoryStack stack = stackPush()) {

            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.callocStack(this.attachmentTypes.length, stack);
            VkAttachmentReference.Buffer attachmentRefs = VkAttachmentReference.callocStack(this.attachmentTypes.length, stack);

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


            VkAttachmentDescription depthAttachment = attachments.get(1)
                    .format(depthFormat)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(getDeviceInfo().depthAttachmentOptimal);

            VkAttachmentReference depthAttachmentRef = attachmentRefs.get(1).set(1, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

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

            if(vkCreateRenderPass(getDevice(), renderPassInfo, null, pRenderPass) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create render pass");
            }

            return pRenderPass.get(0);
        }
    }


    private void addAttachment(VkAttachmentDescription.Buffer attachments, VkAttachmentReference vkAttachmentReference, long renderPass, AttachmentTypes[] attachmentTypes) {
        int i = vkAttachmentReference.attachment();
        final VkAttachmentDescription attachment = attachments.get(i);
        this.attachments[i]=new imageAttachmentReference(renderPass, attachment.loadOp(), attachment.storeOp(), attachmentTypes[i]);
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
    public void beginRendering(VkCommandBuffer commandBuffer, MemoryStack stack) {
        VkRect2D renderArea = VkRect2D.malloc(stack);
        renderArea.offset().set(0, 0);
        renderArea.extent().set(this.width, this.height);

        VkRenderPassAttachmentBeginInfo vkRenderPassAttachmentBeginInfo = VkRenderPassAttachmentBeginInfo.calloc(stack)
                .sType$Default()
                .pAttachments(stack.longs(isSwapChainMode ? Vulkan.getSwapChain().getImageView(Drawer.getCurrentFrame()) : colorAttachment.getImageView(), depthAttachment.getImageView()));
        //Clear Color value is ignored if Load Op is Not set to Clear

        VkClearValue.Buffer clearValues = VkClearValue.malloc(this.attachmentTypes.length, stack);

        clearValues.get(0).color(VkClearValue.ncolor(VRenderSystem.clearColor.ptr));
        clearValues.get(1).depthStencil().set(1.0f, 0);

        VkRenderPassBeginInfo renderingInfo = VkRenderPassBeginInfo.calloc(stack)
                .sType$Default()
                .pNext(vkRenderPassAttachmentBeginInfo)
                .renderPass(this.renderPass)
                .renderArea(renderArea)
                .framebuffer(this.frameBuffer)
                .pClearValues(clearValues)
                .clearValueCount(this.attachmentTypes.length);

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

    //attachments don't care about Res, but does care about the number if Images (i.e. Attachments) Format+LoadStoreOps+layouts afaik
    private record imageAttachmentReference(long parentRenderPass, int loadOp, int storeOp, AttachmentTypes attachmentTypes){};

    //framebuffers can use any renderPass, as long as the renderpass matches the AttachmentImageInfos configuration used to create the framebuffer handle: (i.e.attachment count + format (as long as the res Matches))
    private record FramebufferInfo(int width, int height, long frameBuffer, imageAttachmentReference... attachments){};
    public void recreate(int width, int height) {
        this.width = width;
        this.height = height;
        vkDestroyFramebuffer(getDevice(), this.frameBuffer, null);
        this.frameBuffer = createFramebuffers(this.attachmentTypes);;

//        this.depthFormat = findDepthFormat();
        depthAttachment.free();
        if(colorAttachment!=null) this.colorAttachment.free();
        createDepthResources(false);
    }


}
