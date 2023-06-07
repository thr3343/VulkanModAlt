package net.vulkanmod.mixin.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LoadingOverlay.class)
public class LoadingOverlayM {
    @Shadow private float currentProgress;

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void drawProgressBar(GuiGraphics guiGraphics, int i, int j, int k, int l, float f) {
        RenderSystem.setShader(GameRenderer::getRendertypeGuiShader);
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
