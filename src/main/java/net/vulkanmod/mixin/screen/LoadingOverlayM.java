package net.vulkanmod.mixin.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayM {
    @Shadow private float currentProgress;

    @Shadow @Final private boolean fadeIn;
    @Shadow private long fadeInStart;
    @Shadow private long fadeOutStart;
    @Shadow @Final private Minecraft minecraft;

    @Shadow
    private static int replaceAlpha(int i, int j) {
        return 0;
    }

    @Shadow @Final private static IntSupplier BRAND_BACKGROUND;
    @Shadow @Final
    static ResourceLocation MOJANG_STUDIOS_LOGO_LOCATION;
    @Shadow @Final private ReloadInstance reload;
    @Shadow @Final private Consumer<Optional<Throwable>> onFinish;

    @Overwrite
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        int k = guiGraphics.guiWidth();
        int l = guiGraphics.guiHeight();
        long m = Util.getMillis();
        if (this.fadeIn && this.fadeInStart == -1L) {
            this.fadeInStart = m;
        }

        float g = this.fadeOutStart > -1L ? (float) (m - this.fadeOutStart) / 1000.0F : -1.0F;
        float h = this.fadeInStart > -1L ? (float) (m - this.fadeInStart) / 500.0F : -1.0F;
        float o;
        int n;
        if (g >= 1.0F) {
            if (this.minecraft.screen != null) {
                this.minecraft.screen.render(guiGraphics, 0, 0, f);
            }

            n = Mth.ceil((1.0F - Mth.clamp(g - 1.0F, 0.0F, 1.0F)) * 255.0F);
            guiGraphics.fill(RenderType.guiOverlay(), 0, 0, k, l, replaceAlpha(BRAND_BACKGROUND.getAsInt(), n));
            o = 1.0F - Mth.clamp(g - 1.0F, 0.0F, 1.0F);
        } else if (this.fadeIn) {
            if (this.minecraft.screen != null && h < 1.0F) {
                this.minecraft.screen.render(guiGraphics, i, j, f);
            }

            n = Mth.ceil(Mth.clamp((double) h, 0.15, 1.0) * 255.0);
            guiGraphics.fill(RenderType.guiOverlay(), 0, 0, k, l, replaceAlpha(BRAND_BACKGROUND.getAsInt(), n));
            o = Mth.clamp(h, 0.0F, 1.0F);
        } else {
            n = BRAND_BACKGROUND.getAsInt();
            float p = (float) (n >> 16 & 255) / 255.0F;
            float q = (float) (n >> 8 & 255) / 255.0F;
            float r = (float) (n & 255) / 255.0F;
            GlStateManager._clearColor(p, q, r, 1.0F);
            GlStateManager._clear(16384, Minecraft.ON_OSX);
            o = 1.0F;
        }

        n = (int) ((double) guiGraphics.guiWidth() * 0.5);
        int s = (int) ((double) guiGraphics.guiHeight() * 0.5);
        double d = Math.min((double) guiGraphics.guiWidth() * 0.75, (double) guiGraphics.guiHeight()) * 0.25;
        int t = (int) (d * 0.5);
        double e = d * 4.0;
        int u = (int) (e * 0.5);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(770, 1);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, o);
        guiGraphics.blit(MOJANG_STUDIOS_LOGO_LOCATION, n - u, s - t, u, (int) d, -0.0625F, 0.0F, 120, 60, 120, 120);
        guiGraphics.blit(MOJANG_STUDIOS_LOGO_LOCATION, n, s - t, u, (int) d, 0.0625F, 60.0F, 120, 60, 120, 120);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        int v = (int) ((double) guiGraphics.guiHeight() * 0.8325);
        float w = this.reload.getActualProgress();
        this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + w * 0.050000012F, 0.0F, 1.0F);
        if (g < 1.0F) {
            this.drawProgressBar(guiGraphics, k / 2 - u, v - 5, k / 2 + u, v + 5, 1.0F - Mth.clamp(g, 0.0F, 1.0F));
        }

        if (g >= 2.0F) {
            this.minecraft.setOverlay((Overlay) null);
        }

        if (this.fadeOutStart == -1L && this.reload.isDone() && (!this.fadeIn || h >= 2.0F)) {
            try {
                this.reload.checkExceptions();
                this.onFinish.accept(Optional.empty());
            } catch (Throwable var23) {
                this.onFinish.accept(Optional.of(var23));
            }

            this.fadeOutStart = Util.getMillis();
            if (this.minecraft.screen != null) {
                this.minecraft.screen.init(this.minecraft, guiGraphics.guiWidth(), guiGraphics.guiHeight());
            }
        }

    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void drawProgressBar(GuiGraphics guiGraphics, int i, int j, int k, int l, float f) {
        int m = Mth.ceil((float) (k - i - 2) * this.currentProgress);
        int n = Math.round(f * 255.0F);
        int o = FastColor.ARGB32.color(n, 255, 255, 255);
        guiGraphics.fill(i + 2, j + 2, i + m, l - 2, o);
        guiGraphics.fill(i + 1, j, k - 1, j + 1, o);
        guiGraphics.fill(i + 1, l, k - 1, l - 1, o);
        guiGraphics.fill(i, j, i + 1, l, o);
        guiGraphics.fill(k, j, k - 1, l, o);
    }
}
