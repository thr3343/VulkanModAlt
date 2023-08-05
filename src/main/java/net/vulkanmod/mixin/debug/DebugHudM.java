package net.vulkanmod.mixin.debug;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.renderer.GameRenderer;
import net.vulkanmod.render.chunk.UberBufferSet;
import net.vulkanmod.vulkan.DeviceInfo;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.MemoryManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.include.com.google.common.base.Strings;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import static net.vulkanmod.Initializer.getVersion;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugHudM {

    private static final long maxVRAM = Vulkan.memoryProperties.memoryHeaps(0).size();
    @Shadow @Final private Minecraft minecraft;

    @Shadow
    private static long bytesToMegabytes(long bytes) {
        return 0;
    }

    @Shadow @Final private Font font;

    @Shadow protected abstract List<String> getGameInformation();

    @Shadow protected abstract List<String> getSystemInformation();

    @Redirect(method = "getSystemInformation", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList([Ljava/lang/Object;)Ljava/util/ArrayList;"))
    private ArrayList<String> redirectList(Object[] elements) {
        ArrayList<String> strings = new ArrayList<>();

        long l = Runtime.getRuntime().maxMemory();
        long m = Runtime.getRuntime().totalMemory();
        long n = Runtime.getRuntime().freeMemory();
        long o = m - n;

        strings.add(String.format("Java: %s %dbit", System.getProperty("java.version"), this.minecraft.is64Bit() ? 64 : 32));
        strings.add(String.format("Mem: % 2d%% %03d/%03dMB", o * 100L / l, bytesToMegabytes(o), bytesToMegabytes(l)));
        strings.add(String.format("Allocated: % 2d%% %03dMB", m * 100L / l, bytesToMegabytes(m)));
        strings.add(String.format("Off-heap: " + getOffHeapMemory() + "MB"));
        strings.add("NativeMemory: " + MemoryManager.getInstance().getNativeMemoryMB() + "MB");
        strings.add("UsedVRAM: " + (UberBufferSet.TvirtualBufferVtx.usedBytes>>20)+"+"+(UberBufferSet.virtualBufferVtx.usedBytes>>20) + "MB");
        strings.add("ReservedVRAM: " + (UberBufferSet.TvirtualBufferVtx.size_t>>20)+"+"+(UberBufferSet.virtualBufferVtx.size_t>>20) + "MB");
        strings.add("TotalVRAM: " + (maxVRAM >>20)+"MB");
        strings.add("");
        strings.add("VulkanMod " + getVersion());
        strings.add("CPU: " + DeviceInfo.cpuInfo);
        strings.add("GPU: " + Vulkan.getDeviceInfo().deviceName);
        strings.add("Driver: " + Vulkan.getDeviceInfo().driverVersion);
        strings.add("Vulkan: " + Vulkan.getDeviceInfo().vkVersion);
        strings.add("");
        strings.add("");

        strings.add("Vertex-Buffers");
        strings.add("");
        strings.add("-=VBO-DrawCalls=-");
        strings.add("S: "+ UberBufferSet.sectionQueue.size());
        strings.add("T: "+ UberBufferSet.TsectionQueue.size());


        return strings;
    }

    private long getOffHeapMemory() {
        return bytesToMegabytes(ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed());
    }

    /**
     * @author
     */
    @Overwrite
    public void drawGameInformation(GuiGraphics matrices) {
        List<String> list = this.getGameInformation();
        list.add("");
        boolean bl = this.minecraft.getSingleplayerServer() != null;
        list.add("Debug: Pie [shift]: " + (this.minecraft.options.renderDebugCharts ? "visible" : "hidden") + (bl ? " FPS + TPS" : " FPS") + " [alt]: " + (this.minecraft.options.renderFpsChart ? "visible" : "hidden"));
        list.add("For help: press F3 + Q");

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
//        GuiBatchRenderer.beginBatch(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < list.size(); ++i) {
            String string = list.get(i);
            if (Strings.isNullOrEmpty(string)) continue;
            int j = this.font.lineHeight;
            int k = this.font.width(string);
            int l = 2;
            int m = 2 + j * i;

            matrices.fill(1, m - 1, 2 + k + 1, m + j - 1, -1873784752);
        }
//        GuiBatchRenderer.endBatch();

//        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        for (int i = 0; i < list.size(); ++i) {
            String string = list.get(i);
            if (Strings.isNullOrEmpty(string)) continue;
            int j = this.font.lineHeight;
            int k = this.font.width(string);
            int l = 2;
            int m = 2 + j * i;

            matrices.drawString(this.font, string, 2, m, 0xE0E0E0);
        }
//        bufferSource.endBatch();
    }
////TODO --->!
    @Inject(method = "drawGameInformation",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
                    shift = At.Shift.AFTER,
                    ordinal = 2))
    protected void renderStuffOne(GuiGraphics guiGraphics, CallbackInfo ci)
    {

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getRendertypeGuiOverlayShader);
//        GuiBatchRenderer.beginBatch(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }



//    @Redirect(method = "drawGameInformation(Lnet/minecraft/client/gui/GuiGraphics;)V",
//            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;fill(IIIII)V"))
//    protected void renderStuffRedirectTwo(GuiGraphics poseStack, int m, int k, int j, int e, int d)
//    {
//        poseStack.fill(m, k, j, e, d);
//    }
//    @Redirect(method = "drawGameInformation(Lnet/minecraft/client/gui/GuiGraphics;)V",
//            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;draw(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;FFI)I"))
//    protected int renderStuffRedirectThree(Font instance, GuiGraphics $$0, String $$1, float $$2, float $$3, int $$4)
//    {
//        return 0;
//    }

    @Inject(method = "drawGameInformation(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("TAIL"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    public void renderStuff3(GuiGraphics poseStack, CallbackInfo ci, List<String> list)
    {
//        GuiBatchRenderer.endBatch();

//        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        for (int i = 0; i < list.size(); ++i) {
            String string = list.get(i);
            if (Strings.isNullOrEmpty(string)) continue;
            int j = this.font.lineHeight;
            int k = this.font.width(string);
            int l = 2;
            int m = 2 + j * i;

            poseStack.drawString(this.font, string, 2, m, 0xE0E0E0);
        }
//        bufferSource.endBatch();
    }

    /**
     * @author
     */
    @Overwrite
    public void drawSystemInformation(GuiGraphics matrices) {
        List<String> list = this.getSystemInformation();

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getRendertypeGuiOverlayShader);
//        GuiBatchRenderer.beginBatch(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < list.size(); ++i) {
            String string = list.get(i);
            if (Strings.isNullOrEmpty(string)) continue;
            int j = this.font.lineHeight;
            int k = this.font.width(string);
            int l = this.minecraft.getWindow().getGuiScaledWidth() - 2 - k;
            int m = 2 + j * i;

            matrices.fill(l - 1, m - 1, l + k + 1, m + j - 1, -1873784752);
        }
//        GuiBatchRenderer.endBatch();

//        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        for (int i = 0; i < list.size(); ++i) {
            String string = list.get(i);
            if (Strings.isNullOrEmpty(string)) continue;
            int j = this.font.lineHeight;
            int k = this.font.width(string);
            int l = this.minecraft.getWindow().getGuiScaledWidth() - 2 - k;
            int m = 2 + j * i;

            matrices.drawString(this.font, string, l, m, 0xE0E0E0);
        }
//        bufferSource.endBatch();
    }
}
