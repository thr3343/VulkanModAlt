package net.vulkanmod.vulkan;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.vulkan.shader.BLAS;
import net.vulkanmod.vulkan.shader.SSBO;
import net.vulkanmod.vulkan.shader.TLAS;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static net.vulkanmod.vulkan.Pipeline.createShaderModule;

public class rPipeline
{
    TLAS tTlas;
    BLAS tBlas;
    SSBO tSSBO;
    private String shdr;
    private long rayGenShaderModule;
    private long ahitShaderModule;
    private long RBuffer;
    private long devAddress;

    public rPipeline(/*VertexFormat vertexFormat, String pat*/)
    {
        createDevAddress();
        //May not need verTformat as will be Trateing TranslucentLayer9s)/BlockRenderLayerz for now
//        System.out.println(!Vulkan.deviceAddress ? "FAIL!: No Support for VK_KHR_buffer_device_address Available!" : "OK!: VK_KHR_buffer_device_address Fully Supported!");
        System.out.println(!Vulkan.isRTCapable? "FAIL!: Not isRTCapable!" : "OK!: is isRTCapable!");
        System.out.println(!Vulkan.getDevice().getCapabilities().VK_KHR_buffer_device_address ? "FAIL!: No Support for VK_KHR_buffer_device_address Available!" : "OK!: VK_KHR_buffer_device_address Fully Supported!");
        System.out.println(!Vulkan.getDevice().getCapabilities().VK_KHR_ray_tracing_pipeline ? "FAIL!: No Support for VK_KHR_ray_tracing_pipeline Available!" : "OK!: VK_KHR_ray_tracing_pipeline Fully Supported!");
        System.out.println(!Vulkan.getDevice().getCapabilities().VK_KHR_ray_query ? "FAIL!: No Support for VK_KHR_ray_query Available!" : "OK!: VK_KHR_ray_query Fully Supported!");
        System.out.println(!Vulkan.getDevice().getCapabilities().VK_KHR_acceleration_structure ? "FAIL!: No Support for VK_KHR_acceleration_structure Available!" : "OK!: VK_KHR_acceleration_structure Fully Supported!");
        System.out.println(!Vulkan.getDevice().getCapabilities().VK_KHR_deferred_host_operations ? "FAIL!: No Support for VK_KHR_deferred_host_operations Available!" : "OK!: VK_KHR_deferred_host_operations Fully Supported!");

//        createRPipeline(1);
    }

    private void createDevAddress()
    {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferDeviceAddressInfo vkBufferDeviceAddressInfo = VkBufferDeviceAddressInfo.malloc(stack)
                    .sType$Default()
                    .pNext(0)
                    .buffer(0);
        }
    }

    private void createRPipeline(int pDepth)
    {
        try(MemoryStack stack = MemoryStack.stackPush())
        {
            VkAccelerationStructureBuildGeometryInfoKHR accelerationStructureBuildGeometryInfoKHR = VkAccelerationStructureBuildGeometryInfoKHR.malloc(stack)
                    .srcAccelerationStructure(1);
            VkAccelerationStructureCreateInfoKHR accelerationStructure = VkAccelerationStructureCreateInfoKHR.malloc(stack)
                    .sType$Default()
                    .buffer(RBuffer)
                    .deviceAddress(devAddress);
            LongBuffer pAccelerationStructure=stack.mallocLong(1);
            KHRAccelerationStructure.vkCreateAccelerationStructureKHR(Vulkan.getDevice(), accelerationStructure, null, pAccelerationStructure);


            //Default File Extensions for RayTraced-based/derived shaders don't exist, so will be using the generic ".glsl" Extension for now

            var rayGenShaderSPIRV = ShaderSPIRVUtils.compileShaderFile(this.shdr+".rgen", ShaderSPIRVUtils.ShaderKind.RAYGEN_SHADER);
            var ahitShaderSPIRV = ShaderSPIRVUtils.compileShaderFile(this.shdr+".ahit", ShaderSPIRVUtils.ShaderKind.ANYHIT_SHADER);
            var rchitShaderSPIRV = ShaderSPIRVUtils.compileShaderFile(this.shdr+".rchit", ShaderSPIRVUtils.ShaderKind.CLOSESTHIT_SHADER);
            var rmissShaderSPIRV = ShaderSPIRVUtils.compileShaderFile(this.shdr+".rmiss", ShaderSPIRVUtils.ShaderKind.MISS_SHADER);
            this.rayGenShaderModule = createShaderModule(rayGenShaderSPIRV.bytecode());
            this.ahitShaderModule = createShaderModule(ahitShaderSPIRV.bytecode());
            VkRayTracingPipelineInterfaceCreateInfoKHR interfaceCreateInfoKHR = VkRayTracingPipelineInterfaceCreateInfoKHR.malloc(stack)
                    .sType$Default()
                    .pNext(0)
                    .maxPipelineRayHitAttributeSize(32)
                    .maxPipelineRayPayloadSize(4);

            VkRayTracingPipelineCreateInfoKHR rayTracingPipelineCreateInfoKHR = VkRayTracingPipelineCreateInfoKHR.malloc(stack)
                    .maxPipelineRayRecursionDepth(pDepth)
                    .pLibraryInterface(interfaceCreateInfoKHR);

        }


    }
}
