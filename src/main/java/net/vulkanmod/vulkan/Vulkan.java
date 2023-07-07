package net.vulkanmod.vulkan;

import net.vulkanmod.Initializer;
import net.vulkanmod.config.VideoResolution;
import net.vulkanmod.vulkan.memory.Buffer;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.StagingBuffer;
import net.vulkanmod.vulkan.queue.QueueFamilyIndices;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.util.VUtil;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWNativeWayland;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.glfw.GLFWNativeX11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.windows.WinBase;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.util.vma.VmaVulkanFunctions;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

import static net.vulkanmod.vulkan.queue.Queues.*;
import static net.vulkanmod.vulkan.util.VUtil.asPointerBuffer;
import static net.vulkanmod.vulkan.util.VUtil.asPointerBuffer;
import static org.lwjgl.glfw.GLFW.*;

import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.vma.Vma.vmaCreateAllocator;
import static org.lwjgl.util.vma.Vma.vmaDestroyAllocator;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.vkEnumerateInstanceVersion;

public class Vulkan {

    public static final boolean ENABLE_VALIDATION_LAYERS = false;
//    public static final boolean ENABLE_VALIDATION_LAYERS = true;

    public static final Set<String> VALIDATION_LAYERS;
    public static final boolean RECOMPILE_SHADERS = true;

    public static final int vkVer;

    static {

        try(MemoryStack stack = MemoryStack.stackPush())
        {
            var a = stack.mallocInt(1);
            vkEnumerateInstanceVersion(a);
            int vkVer1 = a.get(0);
            if(VK_VERSION_MINOR(vkVer1)<2)
            {
                throw new RuntimeException("Vulkan 1.2 not supported!: "+"Only Has: "+ DeviceInfo.decDefVersion(vkVer1));
            }
            vkVer= vkVer1;
        }

        if(ENABLE_VALIDATION_LAYERS) {
            VALIDATION_LAYERS = new HashSet<>();
            VALIDATION_LAYERS.add("VK_LAYER_KHRONOS_validation");
//            VALIDATION_LAYERS.add("VK_LAYER_KHRONOS_synchronization2");

        } else {
            // We are not going to use it, so we don't create it
            VALIDATION_LAYERS = null;
        }
    }

    private static final Set<String> DEVICE_EXTENSIONS = Stream.of(
            VK_KHR_SWAPCHAIN_EXTENSION_NAME)
            .collect(toSet());

    private static int debugCallback(int messageSeverity, int messageType, long pCallbackData, long pUserData) {

        VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);

        String s;
        if((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
            s = "\u001B[31m" + callbackData.pMessageString();

//            System.err.println("Stack dump:");
//            Thread.dumpStack();
        } else {
            s = callbackData.pMessageString();
        }

        System.err.println(s);

        if((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0)
            System.nanoTime();

        return VK_FALSE;
    }

    private static int createDebugUtilsMessengerEXT(VkInstance instance, VkDebugUtilsMessengerCreateInfoEXT createInfo,
                                                    VkAllocationCallbacks allocationCallbacks, LongBuffer pDebugMessenger) {

        if(vkGetInstanceProcAddr(instance, "vkCreateDebugUtilsMessengerEXT") != NULL) {
            return vkCreateDebugUtilsMessengerEXT(instance, createInfo, allocationCallbacks, pDebugMessenger);
        }

        return VK_ERROR_EXTENSION_NOT_PRESENT;
    }

    private static void destroyDebugUtilsMessengerEXT(VkInstance instance, long debugMessenger, VkAllocationCallbacks allocationCallbacks) {

        if(vkGetInstanceProcAddr(instance, "vkDestroyDebugUtilsMessengerEXT") != NULL) {
            vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, allocationCallbacks);
        }

    }

    public static VkDevice getDevice() {
        return device;
    }

    public static long getAllocator() {
        return allocator;
    }
    public static final String surfaceExt = getSurfaceKhr();
    public static long window;

    private static VkInstance instance;
    private static long debugMessenger;
    private static long surface;

    private static VkPhysicalDevice physicalDevice;
    private static VkDevice device;

    private static DeviceInfo deviceInfo;

    public static VkPhysicalDeviceProperties deviceProperties;
    public static VkPhysicalDeviceMemoryProperties memoryProperties;
    private static SwapChain swapChain;

//    private static long commandPool;
    private static VkCommandBuffer immediateCmdBuffer;
    private static long immediateFence;

    private static long allocator;

    private static StagingBuffer[] stagingBuffers;

    public static void initVulkan(long window) {
        createInstance();
        setupDebugMessenger();
        pickPhysicalDevice();
        createSurface(window);
        createLogicalDevice();
        createVma();
        MemoryTypes.createMemoryTypes();

//        Queue.initQueues();
//        Queue.initDevs();


        allocateImmediateCmdBuffer();

        createSwapChain();

        createStagingBuffers();
        Drawer.initDrawer();
    }

    static void createStagingBuffers() {
        if(stagingBuffers != null) {
            Arrays.stream(stagingBuffers).forEach(Buffer::freeBuffer);
        }

        stagingBuffers = new StagingBuffer[getSwapChainImages().size()];

        for(int i = 0; i < stagingBuffers.length; ++i) {
            stagingBuffers[i] = new StagingBuffer(30 * 1024 * 1024);
        }
    }

    private static void createSwapChain() {
        swapChain = new SwapChain();
    }

    public static void recreateSwapChain() {
        swapChain.recreateSwapChain();
    }

    public static void waitIdle() {
        vkDeviceWaitIdle(device);
    }

    public static void cleanUp() {
        vkDeviceWaitIdle(device);
//        vkDestroyCommandPool(device, commandPool -> GraphicsQueue.commandPool.id, null);
        vkDestroyFence(device, immediateFence, null);

        GraphicsQueue.cleanUp();
        TransferQueue.cleanUp();
        ComputeQueue.cleanUp();

        Pipeline.destroyPipelineCache();
        swapChain.cleanUp();

        Drawer.getInstance().cleanUpResources();
        freeStagingBuffers();

        try {
            MemoryManager.getInstance().freeAllBuffers();
        } catch (Exception e) {
            e.printStackTrace();
        }

        vmaDestroyAllocator(allocator);

        vkDestroyDevice(device, null);
        destroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
        KHRSurface.vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);
    }

    private static void freeStagingBuffers() {
        for(StagingBuffer buffer : stagingBuffers) {
            MemoryManager.getInstance().freeBuffer(buffer.getId(), buffer.getAllocation());
        }
    }

    private static void createInstance() {

        if(ENABLE_VALIDATION_LAYERS && !checkValidationLayerSupport()) {
            throw new RuntimeException("Validation requested but not supported");
        }

        try(MemoryStack stack = stackPush()) {

            // Use calloc to initialize the structs with 0s. Otherwise, the program can crash due to random values

            VkApplicationInfo appInfo = VkApplicationInfo.callocStack(stack);

            appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(stack.UTF8Safe("VulkanMod"));
            appInfo.applicationVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.pEngineName(stack.UTF8Safe("No Engine"));
            appInfo.engineVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.apiVersion(vkVer);

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.callocStack(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            createInfo.pApplicationInfo(appInfo);
            // enabledExtensionCount is implicitly set when you call ppEnabledExtensionNames
            PointerBuffer result;
            System.out.println("Selecting Platform (Via GLFW): "+ getPlat());

            PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();



            glfwExtensions.put(1, stack.UTF8(surfaceExt));

            if(ENABLE_VALIDATION_LAYERS) {

                MemoryStack stack1 = stackGet();

                PointerBuffer extensions = stack1.mallocPointer(glfwExtensions.capacity() + 1);

                extensions.put(glfwExtensions);
                extensions.put(stack1.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));

                // Rewind the buffer before returning it to reset its position back to 0
                result = extensions.rewind();
            } else {
                result = glfwExtensions;
            }

            createInfo.ppEnabledExtensionNames(result);

            if(ENABLE_VALIDATION_LAYERS) {

                createInfo.ppEnabledLayerNames(asPointerBuffer(VALIDATION_LAYERS));

                VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.callocStack(stack);
                populateDebugMessengerCreateInfo(debugCreateInfo);
                createInfo.pNext(debugCreateInfo.address());
            }

            // We need to retrieve the pointer of the created instance
            PointerBuffer instancePtr = stack.mallocPointer(1);

            if(vkCreateInstance(createInfo, null, instancePtr) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create instance");
            }

            instance = new VkInstance(instancePtr.get(0), createInfo);
        }
    }

    @NotNull
    private static String getSurfaceKhr() {
        return switch (VideoResolution.getActivePlat())
        {
            case GLFW_PLATFORM_WIN32 -> "VK_KHR_win32_surface";
//            case GLFW_PLATFORM_COCOA -> KHR
            case GLFW_PLATFORM_WAYLAND -> "VK_KHR_wayland_surface";
            default -> throw new IllegalStateException("Unexpected value: " + glfwGetPlatform());
        };
    }

    private static String getPlat() {
        return switch (VideoResolution.getActivePlat())
        {
                    case GLFW_PLATFORM_WIN32 -> "GLFW_PLATFORM_WIN32";
                    case GLFW_PLATFORM_WAYLAND -> "GLFW_PLATFORM_WAYLAND";
                    default -> throw new IllegalStateException("Unexpected value: " + glfwGetPlatform());
        };
    }

    private static void populateDebugMessengerCreateInfo(VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo) {
        debugCreateInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
//        debugCreateInfo.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT);
        debugCreateInfo.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT);
        debugCreateInfo.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
//        debugCreateInfo.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT);
        debugCreateInfo.pfnUserCallback(Vulkan::debugCallback);
    }

    private static void setupDebugMessenger() {

        if(!ENABLE_VALIDATION_LAYERS) {
            return;
        }

        try(MemoryStack stack = stackPush()) {

            VkDebugUtilsMessengerCreateInfoEXT createInfo = VkDebugUtilsMessengerCreateInfoEXT.callocStack(stack);

            populateDebugMessengerCreateInfo(createInfo);

            LongBuffer pDebugMessenger = stack.longs(VK_NULL_HANDLE);

            if(createDebugUtilsMessengerEXT(instance, createInfo, null, pDebugMessenger) != VK_SUCCESS) {
                throw new RuntimeException("Failed to set up debug messenger");
            }

            debugMessenger = pDebugMessenger.get(0);
        }
    }

    private static void createSurface(long handle) {
        window = handle;

        try(MemoryStack stack = stackPush()) {

            LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);

            boolean isSupported = switch (surfaceExt)
            {
                case "VK_KHR_win32_surface" -> KHRWin32Handle(handle, stack, pSurface);
                case "VK_KHR_wayland_surface" -> KHRWaylandHandle(handle, stack, pSurface);
                default -> throw new IllegalStateException("Unrecognised Platform: "+getPlat());
            };
            if(!isSupported) throw new RuntimeException("Unable to Use Platform: "+getPlat()+" Presentation Not Supported!");

            surface = pSurface.get(0);
        }
    }

//    private static boolean KHRX11Handle(long handle, MemoryStack stack, LongBuffer pSurface) {
//        VkXlibSurfaceCreateInfoKHR createSurfaceInfo = VkXlibSurfaceCreateInfoKHR.calloc(stack)
//                .sType(KHRXlibSurface.VK_STRUCTURE_TYPE_XLIB_SURFACE_CREATE_INFO_KHR)
//                .pNext(VK_NULL_HANDLE)
//                .flags(0)
//                .dpy(GLFWNativeX11.glfwGetX11Display())
//                .window(GLFWNativeX11.glfwGetX11Window(handle));
//
//        KHRXlibSurface.vkCreateXlibSurfaceKHR( instance, createSurfaceInfo, null, pSurface);
//    }
    private static boolean KHRWaylandHandle(long handle, MemoryStack stack, LongBuffer pSurface) {

        final long wlDisplay = GLFWNativeWayland.glfwGetWaylandDisplay();
        boolean Supported = KHRWaylandSurface.vkGetPhysicalDeviceWaylandPresentationSupportKHR(physicalDevice, QueueFamilyIndices.graphicsFamily, wlDisplay);
        if(Supported) {
            VkWaylandSurfaceCreateInfoKHR createSurfaceInfo = VkWaylandSurfaceCreateInfoKHR.calloc(stack)
                    .sType(KHRWaylandSurface.VK_STRUCTURE_TYPE_WAYLAND_SURFACE_CREATE_INFO_KHR)
                    .pNext(VK_NULL_HANDLE)
                    .flags(0)
                    .surface(GLFWNativeWayland.glfwGetWaylandWindow(handle))

                    .display(wlDisplay);


            KHRWaylandSurface.vkCreateWaylandSurfaceKHR(instance, createSurfaceInfo, null, pSurface);
        }

        return Supported;
    }



    private static boolean KHRWin32Handle(long handle, MemoryStack stack, LongBuffer pSurface) {
        boolean Supported = KHRWin32Surface.vkGetPhysicalDeviceWin32PresentationSupportKHR(physicalDevice, QueueFamilyIndices.graphicsFamily);

        if(Supported) {
            VkWin32SurfaceCreateInfoKHR createSurfaceInfo = VkWin32SurfaceCreateInfoKHR.calloc(stack)
                    .sType(KHRWin32Surface.VK_STRUCTURE_TYPE_WIN32_SURFACE_CREATE_INFO_KHR)
                    .pNext(VK_NULL_HANDLE)
                    .flags(0)
                    .hinstance(WinBase.nGetModuleHandle(NULL))
                    .hwnd(GLFWNativeWin32.glfwGetWin32Window(handle));


            KHRWin32Surface.vkCreateWin32SurfaceKHR(instance, createSurfaceInfo, null, pSurface);
        }
        return Supported;
    }

    private static void pickPhysicalDevice() {

        try(MemoryStack stack = stackPush()) {

            IntBuffer deviceCount = stack.ints(0);

            vkEnumeratePhysicalDevices(instance, deviceCount, null);

            if(deviceCount.get(0) == 0) {
                throw new RuntimeException("Failed to find GPUs with Vulkan support");
            }

            PointerBuffer ppPhysicalDevices = stack.mallocPointer(deviceCount.get(0));

            vkEnumeratePhysicalDevices(instance, deviceCount, ppPhysicalDevices);

            ArrayList<VkPhysicalDevice> integratedGPUs = new ArrayList<>();
            ArrayList<VkPhysicalDevice> otherDevices = new ArrayList<>();

            VkPhysicalDevice currentDevice = null;
            boolean flag = false;

            for(int i = 0; i < ppPhysicalDevices.capacity();i++) {

                currentDevice = new VkPhysicalDevice(ppPhysicalDevices.get(i), instance);

                VkPhysicalDeviceProperties deviceProperties = VkPhysicalDeviceProperties.callocStack(stack);
                vkGetPhysicalDeviceProperties(currentDevice, deviceProperties);

                if(isDeviceSuitable(currentDevice)) {
                    if(deviceProperties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU){
                        flag = true;
                        break;
                    }
                    else if(deviceProperties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU) integratedGPUs.add(currentDevice);
                    else otherDevices.add(currentDevice);

                }
            }

            if(!flag) {
                if(!integratedGPUs.isEmpty()) currentDevice = integratedGPUs.get(0);
                else if(!otherDevices.isEmpty()) currentDevice = otherDevices.get(0);
                else {
                    Initializer.LOGGER.error(DeviceInfo.debugString(ppPhysicalDevices, DEVICE_EXTENSIONS, instance));
                    throw new RuntimeException("Failed to find a suitable GPU");
                }
            }

            physicalDevice = currentDevice;

            //Get device properties

            deviceProperties = VkPhysicalDeviceProperties.malloc();
            vkGetPhysicalDeviceProperties(physicalDevice, deviceProperties);

            memoryProperties = VkPhysicalDeviceMemoryProperties.malloc();
            vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties);

            deviceInfo = new DeviceInfo(physicalDevice, deviceProperties);
        }
    }

    private static void createLogicalDevice() {

        try(MemoryStack stack = stackPush()) {

            int[] uniqueQueueFamilies = QueueFamilyIndices.unique();


            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.callocStack(uniqueQueueFamilies.length, stack);

            for(int i = 0;i < uniqueQueueFamilies.length;i++) {
                VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get(i);
                queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueCreateInfo.queueFamilyIndex(uniqueQueueFamilies[i]);
                queueCreateInfo.pQueuePriorities(stack.floats(1.0f));
            }

            VkPhysicalDeviceFeatures2 deviceFeatures = VkPhysicalDeviceFeatures2.calloc(stack);
            deviceFeatures.sType$Default();

            //TODO indirect draw option disabled in case it is not supported
            deviceFeatures.features().samplerAnisotropy(deviceInfo.availableFeatures.features().samplerAnisotropy());
            deviceFeatures.features().logicOp(deviceInfo.availableFeatures.features().logicOp());

            VkPhysicalDeviceVulkan11Features deviceVulkan11Features = VkPhysicalDeviceVulkan11Features.calloc(stack);
            deviceVulkan11Features.sType$Default();

            VkPhysicalDeviceVulkan12Features deviceVulkan12Features = VkPhysicalDeviceVulkan12Features.calloc(stack);
            deviceVulkan12Features.sType$Default();

            if(deviceInfo.isDrawIndirectSupported()) {
                deviceFeatures.features().multiDrawIndirect(true);
                deviceFeatures.features().sampleRateShading(true);
                deviceVulkan11Features.shaderDrawParameters(true);
            }

            deviceVulkan12Features.imagelessFramebuffer(true);
            deviceVulkan12Features.separateDepthStencilLayouts(true);

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.callocStack(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            // queueCreateInfoCount is automatically set

            createInfo.pEnabledFeatures(deviceFeatures.features());

            createInfo.pNext(deviceVulkan11Features);
            createInfo.pNext(deviceVulkan12Features);

            //Vulkan 1.3 dynamic rendering
//            VkPhysicalDeviceVulkan13Features deviceVulkan13Features = VkPhysicalDeviceVulkan13Features.calloc(stack);
//            deviceVulkan13Features.sType$Default();
//            if(!deviceInfo.availableFeatures13.dynamicRendering())
//                throw new RuntimeException("Device does not support dynamic rendering feature.");
//
//            deviceVulkan13Features.dynamicRendering(true);
//            createInfo.pNext(deviceVulkan13Features);
//            deviceVulkan13Features.pNext(deviceVulkan11Features.address());

            createInfo.ppEnabledExtensionNames(asPointerBuffer(DEVICE_EXTENSIONS));

//            Configuration.DEBUG_FUNCTIONS.set(true);

            if(ENABLE_VALIDATION_LAYERS) {
                createInfo.ppEnabledLayerNames(asPointerBuffer(VALIDATION_LAYERS));
            }

            PointerBuffer pDevice = stack.mallocPointer(1);

            if(vkCreateDevice(physicalDevice, createInfo, null, pDevice) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create logical device");
            }

            device = new VkDevice(pDevice.get(0), physicalDevice, createInfo, vkVer);

        }
    }

    private static void createVma() {
        try(MemoryStack stack = stackPush()) {

            VmaVulkanFunctions vulkanFunctions = VmaVulkanFunctions.callocStack(stack);
            vulkanFunctions.set(instance, device);

            VmaAllocatorCreateInfo allocatorCreateInfo = VmaAllocatorCreateInfo.callocStack(stack);
            allocatorCreateInfo.physicalDevice(physicalDevice);
            allocatorCreateInfo.device(device);
            allocatorCreateInfo.pVulkanFunctions(vulkanFunctions);
            allocatorCreateInfo.instance(instance);
            allocatorCreateInfo.vulkanApiVersion(vkVer);

            PointerBuffer pAllocator = stack.mallocPointer(1);

            if (vmaCreateAllocator(allocatorCreateInfo, pAllocator) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create command pool");
            }

            allocator = pAllocator.get(0);
        }
    }

    private static int findSupportedFormat(IntBuffer formatCandidates, int tiling, int features) {

        try(MemoryStack stack = stackPush()) {

            VkFormatProperties props = VkFormatProperties.callocStack(stack);

            for(int i = 0; i < formatCandidates.capacity(); ++i) {

                int format = formatCandidates.get(i);

                vkGetPhysicalDeviceFormatProperties(physicalDevice, format, props);

                if(tiling == VK_IMAGE_TILING_LINEAR && (props.linearTilingFeatures() & features) == features) {
                    return format;
                } else if(tiling == VK_IMAGE_TILING_OPTIMAL && (props.optimalTilingFeatures() & features) == features) {
                    return format;
                }

            }
        }

        throw new RuntimeException("Failed to find supported format");
    }

    public static int findDepthFormat() {
        return findSupportedFormat(
                stackGet().ints(VK_FORMAT_D32_SFLOAT, VK_FORMAT_D32_SFLOAT_S8_UINT),
                VK_IMAGE_TILING_OPTIMAL,
                VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT);
    }

    private static void allocateImmediateCmdBuffer() {
        try(MemoryStack stack = stackPush()) {

            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandPool(GraphicsQueue.commandPool.id);
            allocInfo.commandBufferCount(1);

            PointerBuffer pCommandBuffer = stack.mallocPointer(1);
            vkAllocateCommandBuffers(device, allocInfo, pCommandBuffer);
            immediateCmdBuffer = new VkCommandBuffer(pCommandBuffer.get(0), device);

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.callocStack(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            LongBuffer pFence = stack.mallocLong(1);
            vkCreateFence(device, fenceInfo, null, pFence);
            vkResetFences(device,  pFence.get(0));

            immediateFence = pFence.get(0);
        }
    }

    public static VkCommandBuffer beginImmediateCmd() {
        try (MemoryStack stack = stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            vkBeginCommandBuffer(immediateCmdBuffer, beginInfo);
        }
        return immediateCmdBuffer;
    }

//    public static void endImmediateCmd() {
//        try (MemoryStack stack = stackPush()) {
//            vkEndCommandBuffer(immediateCmdBuffer);
//
//            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack);
//            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
//            submitInfo.pCommandBuffers(stack.pointers(immediateCmdBuffer));
//
//            vkQueueSubmit(graphicsQueue, submitInfo, immediateFence);
//
//            vkWaitForFences(device, immediateFence, true, VUtil.UINT64_MAX);
//            vkResetFences(device, immediateFence);
//            vkResetCommandBuffer(immediateCmdBuffer, 0);
//        }
//
//    }


    private static boolean isDeviceSuitable(VkPhysicalDevice device) {



        boolean extensionsSupported = checkDeviceExtensionSupport(device);
//        boolean swapChainAdequate = false;

//        if(extensionsSupported) {
//            try(MemoryStack stack = stackPush()) {
//                SwapChain.SwapChainSupportDetails swapChainSupport = querySwapChainSupport(device, stack);
//                swapChainAdequate = swapChainSupport.formats.hasRemaining() && swapChainSupport.presentModes.hasRemaining() ;
//            }
//        }

        boolean anisotropicFilterSuppoted = false;
        try(MemoryStack stack = stackPush()) {
            VkPhysicalDeviceFeatures supportedFeatures = VkPhysicalDeviceFeatures.mallocStack(stack);
            vkGetPhysicalDeviceFeatures(device, supportedFeatures);
            anisotropicFilterSuppoted = supportedFeatures.samplerAnisotropy();
        }


        return QueueFamilyIndices.findQueueFamilies(device) && extensionsSupported;

    }

    private static boolean checkDeviceExtensionSupport(VkPhysicalDevice device) {

        try(MemoryStack stack = stackPush()) {

            IntBuffer extensionCount = stack.ints(0);

            vkEnumerateDeviceExtensionProperties(device, (String)null, extensionCount, null);

            VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.mallocStack(extensionCount.get(0), stack);

            vkEnumerateDeviceExtensionProperties(device, (String)null, extensionCount, availableExtensions);

            Set<String> extensions = availableExtensions.stream()
                    .map(VkExtensionProperties::extensionNameString)
                    .collect(toSet());

            extensions.removeAll(DEVICE_EXTENSIONS);

            return availableExtensions.stream()
                    .map(VkExtensionProperties::extensionNameString)
                    .collect(toSet())
                    .containsAll(DEVICE_EXTENSIONS);
        }
    }

    private static boolean checkValidationLayerSupport() {

        try(MemoryStack stack = stackPush()) {

            IntBuffer layerCount = stack.ints(0);

            vkEnumerateInstanceLayerProperties(layerCount, null);

            VkLayerProperties.Buffer availableLayers = VkLayerProperties.mallocStack(layerCount.get(0), stack);

            vkEnumerateInstanceLayerProperties(layerCount, availableLayers);

            Set<String> availableLayerNames = availableLayers.stream()
                    .map(VkLayerProperties::layerNameString)
                    .collect(toSet());

            return availableLayerNames.containsAll(VALIDATION_LAYERS);
        }
    }

    public static void setVsync(boolean b) {
        if(swapChain.isVsync() != b) {
            Drawer.shouldRecreate = true;
            Drawer.vsync = b;
            swapChain.setVsync(b);
        }
    }

    public static long getSurface() { return surface; }

    public static long getPresentQueue() { return ComputeQueue.Queue; }

    public static long getGraphicsQueue() { return GraphicsQueue.Queue; }

    public static long getTransferQueue() { return TransferQueue.Queue; }

    public static SwapChain getSwapChain() { return swapChain; }

    public static VkExtent2D getSwapchainExtent()
    {
        return swapChain.getExtent();
    }

    public static List<Long> getSwapChainImages() { return swapChain.getImages(); }

    public static long getCommandPool()
    {
        return GraphicsQueue.commandPool.id;
    }

    public static StagingBuffer getStagingBuffer(int i) { return stagingBuffers[i]; }

    public static DeviceInfo getDeviceInfo() { return deviceInfo; }
}

