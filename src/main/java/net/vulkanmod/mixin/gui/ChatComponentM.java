package net.vulkanmod.mixin.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.vulkanmod.render.gui.GuiBatchRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;

import java.util.List;
import java.util.Objects;

@Mixin(ChatComponent.class)
public abstract class ChatComponentM {

    @Shadow @Final private Minecraft minecraft;

    @Shadow protected abstract boolean isChatHidden();

    @Shadow protected abstract boolean isChatFocused();

    @Shadow public abstract int getWidth();

    @Shadow public abstract int getHeight();

    @Shadow public abstract int getLinesPerPage();

    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;

    @Shadow public abstract double getScale();

    @Shadow protected abstract int getLineHeight();

    @Shadow private int chatScrollbarPos;

    @Shadow protected abstract int getTagIconLeft(GuiMessage.Line line);

    @Shadow private boolean newMessageSinceScroll;

    @Shadow protected abstract int getMessageEndIndexAt(double d, double e);

    @Shadow protected abstract double screenToChatX(double d);

    @Shadow protected abstract double screenToChatY(double d);

//    private static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation("textures/gui/chat_tags.png");

//    /**
//     * @author
//     */
//    @Overwrite
//    public void render(PoseStack poseStack, int i, int j, int k) {
//        int v;
//        int u;
//        int t;
//        int s;
//        int r;
//        int p;
//        if (this.isChatHidden()) {
//            return;
//        }
//        int j = this.getLinesPerPage();
//        int k = this.trimmedMessages.size();
//        if (k <= 0) {
//            return;
//        }
//        boolean bl = this.isChatFocused();
//        float f = (float)this.getScale();
//        int l = Mth.ceil((float)this.getWidth() / f);
//        poseStack.pushPose();
//        poseStack.translate(4.0, 8.0, 0.0);
//        poseStack.scale(f, f, 1.0f);
//        double d = this.minecraft.options.chatOpacity().get() * (double)0.9f + (double)0.1f;
//        double e = this.minecraft.options.textBackgroundOpacity().get();
//        double g = this.minecraft.options.chatLineSpacing().get();
//        int m = this.getLineHeight();
//        double h = -8.0 * (g + 1.0) + 4.0 * g;
//        int n = 0;
//
//        poseStack.pushPose();
//        poseStack.translate(0.0, 0.0, 50.0);
//        Matrix4f mat1 = poseStack.last().pose();
//        poseStack.pushPose();
//        poseStack.translate(0.0, 0.0, 50.0);
//        Matrix4f mat2 = poseStack.last().pose();
//
//        GuiBatchRenderer.beginBatch(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
//        RenderSystem.setShader(GameRenderer::getPositionColorShader);
//        RenderSystem.enableBlend();
//
//
//        for (int o = 0; o + this.chatScrollbarPos < this.trimmedMessages.size() && o < j; ++o) {
//            GuiMessage.Line line = this.trimmedMessages.get(o + this.chatScrollbarPos);
//            if (line == null || (p = i - line.addedTime()) >= 200 && !bl) continue;
//            double q = bl ? 1.0 : getTimeFactor(p);
//            r = (int)(255.0 * q * d);
//            s = (int)(255.0 * q * e);
//            ++n;
//            if (r <= 3) continue;
//            t = 0;
//            u = -o * m;
//            v = (int)((double)u + h);
////            poseStack.pushPose();
////            poseStack.translate(0.0, 0.0, 50.0);
////            ChatComponent.fill(poseStack, -4, u - m, 0 + l + 4 + 4, u, s << 24);
//            GuiBatchRenderer.fill(mat1, -4, u - m, 0 + l + 4 + 4, u, s << 24);
//            GuiMessageTag guiMessageTag = line.tag();
//            if (guiMessageTag != null) {
//                int w = guiMessageTag.indicatorColor() | r << 24;
////                ChatComponent.fill(poseStack, -4, u - m, -2, u, w);
//                GuiBatchRenderer.fill(mat1, -4, u - m, -2, u, w);
//                if (bl && line.endOfEntry() && guiMessageTag.icon() != null) {
//                    int x = this.getTagIconLeft(line);
//                    int y = v + this.minecraft.font.lineHeight;
//                    this.drawTagIcon(mat1, x, y, guiMessageTag.icon());
//                }
//            }
////            RenderSystem.enableBlend();
////            poseStack.translate(0.0, 0.0, 50.0);
//
////            GuiBatchRenderer.drawShadow(this.minecraft.font, bufferSource, mat2, line.content(), 0.0f, (float)((int)(s + h)), 0xFFFFFF + (p << 24));
////            this.minecraft.font.drawShadow(poseStack, line.content(), 0.0f, (float)v, 0xFFFFFF + (r << 24));
//
////            RenderSystem.disableBlend();
////            poseStack.popPose();
//        }
//
//        GuiBatchRenderer.endBatch();
//        RenderSystem.disableBlend();
//        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
//
//        for (int o = 0; o + this.chatScrollbarPos < this.trimmedMessages.size() && o < j; ++o) {
//            GuiMessage.Line line = this.trimmedMessages.get(o + this.chatScrollbarPos);
//            if (line == null || (p = i - line.addedTime()) >= 200 && !bl) continue;
//            double q = bl ? 1.0 : getTimeFactor(p);
//            r = (int)(255.0 * q * d);
//            s = (int)(255.0 * q * e);
////            ++n;
//            if (r <= 3) continue;
//            t = 0;
//            u = -o * m;
//            v = (int)((double)u + h);
////            poseStack.pushPose();
////            poseStack.translate(0.0, 0.0, 50.0);
////            ChatComponent.fill(poseStack, -4, u - m, 0 + l + 4 + 4, u, s << 24);
////            GuiBatchRenderer.fill(mat1, -4, u - m, 0 + l + 4 + 4, u, s << 24);
////            GuiMessageTag guiMessageTag = line.tag();
////            if (guiMessageTag != null) {
////                int w = guiMessageTag.indicatorColor() | r << 24;
////                ChatComponent.fill(poseStack, -4, u - m, -2, u, w);
////                if (bl && line.endOfEntry() && guiMessageTag.icon() != null) {
////                    int x = this.getTagIconLeft(line);
////                    int y = v + this.minecraft.font.lineHeight;
////                    this.drawTagIcon(poseStack, x, y, guiMessageTag.icon());
////                }
////            }
////            RenderSystem.enableBlend();
////            poseStack.translate(0.0, 0.0, 50.0);
//
//            GuiBatchRenderer.drawShadow(this.minecraft.font, bufferSource, mat2, line.content(), 0.0f, (float)((int)(v)), 0xFFFFFF + (r << 24));
////            this.minecraft.font.drawShadow(poseStack, line.content(), 0.0f, (float)v, 0xFFFFFF + (r << 24));
//
////            RenderSystem.disableBlend();
////            poseStack.popPose();
//        }
//
//        bufferSource.endBatch();
//        poseStack.popPose();
//        poseStack.popPose();
//
//
//        long z = this.minecraft.getChatListener().queueSize();
//        if (z > 0L) {
//            p = (int)(128.0 * d);
//            int aa = (int)(255.0 * e);
//            poseStack.pushPose();
//            poseStack.translate(0.0, 0.0, 50.0);
//            ChatComponent.fill(poseStack, -2, 0, l + 4, 9, aa << 24);
//            RenderSystem.enableBlend();
//            poseStack.translate(0.0, 0.0, 50.0);
//            this.minecraft.font.drawShadow(poseStack, Component.translatable("chat.queue", z), 0.0f, 1.0f, 0xFFFFFF + (p << 24));
//            poseStack.popPose();
//            RenderSystem.disableBlend();
//        }
//        if (bl) {
//            p = this.getLineHeight();
//            int aa = k * p;
//            int ab = n * p;
//            r = this.chatScrollbarPos * ab / k;
//            s = ab * ab / aa;
//            if (aa != ab) {
//                t = r > 0 ? 170 : 96;
//                u = this.newMessageSinceScroll ? 0xCC3333 : 0x3333AA;
//                v = l + 4;
//                ChatComponent.fill(poseStack, v, -r, v + 2, -r - s, u + (t << 24));
//                ChatComponent.fill(poseStack, v + 2, -r, v + 1, -r - s, 0xCCCCCC + (t << 24));
//            }
//        }
//        poseStack.popPose();
//    }

    /**
     * @author
     */
    @Overwrite
    public void render(GuiGraphics poseStack, int i, int j, int k) {
        if (!this.isChatHidden()) {
            int l = this.getLinesPerPage();
            int m = this.trimmedMessages.size();
            if (m > 0) {
                boolean bl = this.isChatFocused();
                float f = (float)this.getScale();
                int n = Mth.ceil((float)this.getWidth() / f);
                int o = this.minecraft.getWindow().getGuiScaledHeight();
                poseStack.pose().pushPose();
                poseStack.pose().scale(f, f, 1.0F);
                poseStack.pose().translate(4.0F, 0.0F, 0.0F);
                int p = Mth.floor((float)(o - 40) / f);
                int q = this.getMessageEndIndexAt(this.screenToChatX(j), this.screenToChatY(k));
                double d = this.minecraft.options.chatOpacity().get() * 0.8999999761581421 + 0.10000000149011612;
                double e = this.minecraft.options.textBackgroundOpacity().get();
                double g = this.minecraft.options.chatLineSpacing().get();
                int r = this.getLineHeight();
                int s = (int)Math.round(-8.0 * (g + 1.0) + 4.0 * g);
                int t = 0;

                int w;
                int x;
                int y;
                int aa;

                poseStack.pose().pushPose();
                poseStack.pose().translate(0.0, 0.0, 50.0);
//                Matrix4f mat1 = poseStack.pose().last().pose();
//                poseStack.pushPose();
                poseStack.pose().translate(0.0, 0.0, 50.0);
//                Matrix4f mat2 = poseStack.pose().last().pose();

//                GuiBatchRenderer.beginBatch(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                RenderSystem.enableBlend();
//                MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

                for(int u = 0; u + this.chatScrollbarPos < this.trimmedMessages.size() && u < l; ++u) {
                    int v = u + this.chatScrollbarPos;
                    GuiMessage.Line line = this.trimmedMessages.get(v);
                    if (line != null) {
                        w = i - line.addedTime();
                        if (w < 200 || bl) {
                            double h = bl ? 1.0 : getTimeFactor(w);
                            x = (int)(255.0 * h * d);
                            y = (int)(255.0 * h * e);
                            ++t;
                            if (x > 3) {
                                boolean z = false;
                                aa = p - u * r;
                                int ab = aa + s;
//                                poseStack.pushPose();
//                                poseStack.translate(0.0F, 0.0F, 50.0F);
                                poseStack.fill(-4, aa - r, n + 4 + 4, aa, y << 24);
                                GuiMessageTag guiMessageTag = line.tag();
                                if (guiMessageTag != null) {
                                    int ac = guiMessageTag.indicatorColor() | x << 24;
                                    poseStack.fill(-4, aa - r, -2, aa, ac);
                                    if (v == q && guiMessageTag.icon() != null) {
                                        int ad = this.getTagIconLeft(line);
                                        Objects.requireNonNull(this.minecraft.font);
                                        int ae = ab + 9;
                                        this.drawTagIcon(poseStack, ad, ae, guiMessageTag.icon());
                                    }
                                }

//                                RenderSystem.enableBlend();
//                                poseStack.translate(0.0F, 0.0F, 50.0F);
//                                this.minecraft.font.drawShadow(poseStack, line.content(), 0.0F, (float)ab, 16777215 + (x << 24));
//                                GuiBatchRenderer.drawShadow(this.minecraft.font, bufferSource, mat2, line.content(), 0.0F, (float)ab, 16777215 + (x << 24));
//                                RenderSystem.disableBlend();
//                                poseStack.popPose();
                            }
                        }
                    }
                }

//                GuiBatchRenderer.endBatch();

                for(int u = 0; u + this.chatScrollbarPos < this.trimmedMessages.size() && u < l; ++u) {
                    int v = u + this.chatScrollbarPos;
                    GuiMessage.Line line = this.trimmedMessages.get(v);
                    if (line != null) {
                        w = i - line.addedTime();
                        if (w < 200 || bl) {
                            double h = bl ? 1.0 : getTimeFactor(w);
                            x = (int)(255.0 * h * d);
                            y = (int)(255.0 * h * e);
                            ++t;
                            if (x > 3) {
                                boolean z = false;
                                aa = p - u * r;
                                int ab = aa + s;
//                                poseStack.pushPose();
//                                poseStack.translate(0.0F, 0.0F, 50.0F);
//                                GuiBatchRenderer.fill(mat1, -4, aa - r, 0 + n + 4 + 4, aa, y << 24);
//                                GuiMessageTag guiMessageTag = line.tag();
//                                if (guiMessageTag != null) {
//                                    int ac = guiMessageTag.indicatorColor() | x << 24;
//                                    GuiBatchRenderer.fill(mat1, -4, aa - r, -2, aa, ac);
//                                    if (v == q && guiMessageTag.icon() != null) {
//                                        int ad = this.getTagIconLeft(line);
//                                        Objects.requireNonNull(this.minecraft.font);
//                                        int ae = ab + 9;
//                                        this.drawTagIcon(mat1, ad, ae, guiMessageTag.icon());
//                                    }
//                                }

//                                RenderSystem.enableBlend();
//                                poseStack.translate(0.0F, 0.0F, 50.0F);
//                                this.minecraft.font.drawShadow(poseStack, line.content(), 0.0F, (float)ab, 16777215 + (x << 24));
                                poseStack.drawString(this.minecraft.font, line.content(), 0, ab, 16777215 + (x << 24));
//                                RenderSystem.disableBlend();
//                                poseStack.popPose();
                            }
                        }
                    }
                }

                poseStack.pose().popPose();

                long af = this.minecraft.getChatListener().queueSize();
                int ag;
                if (af > 0L) {
                    ag = (int)(128.0 * d);
                    w = (int)(255.0 * e);
                    poseStack.pose().pushPose();
                    poseStack.pose().translate(0.0F, (float)p, 50.0F);
                    poseStack.fill(-2, 0, n + 4, 9, w << 24);
                    RenderSystem.enableBlend();
                    poseStack.pose().translate(0.0F, 0.0F, 50.0F);
//                    this.minecraft.font.drawShadow(poseStack.pose(), Component.translatable("chat.queue", new Object[]{af}), 0.0F, 1.0F, 16777215 + (ag << 24));
                    poseStack.drawString(this.minecraft.font, Component.translatable("chat.queue", af).getVisualOrderText(), 0, 1, 16777215 + (ag << 24));
                    poseStack.pose().popPose();
//                    RenderSystem.disableBlend();
                }

//                bufferSource.endBatch();

                if (bl) {
                    ag = this.getLineHeight();
                    w = m * ag;
                    int ah = t * ag;
                    int ai = this.chatScrollbarPos * ah / m - p;
                    x = ah * ah / w;
                    if (w != ah) {
                        y = ai > 0 ? 170 : 96;
                        int z = this.newMessageSinceScroll ? 13382451 : 3355562;
                        aa = n + 4;
                        poseStack.fill(aa, -ai, aa + 2, -ai - x, z + (y << 24));
                        poseStack.fill(aa + 2, -ai, aa + 1, -ai - x, 13421772 + (y << 24));
                    }
                }

                poseStack.pose().popPose();
            }
        }
    }

    @Unique
    private void drawTagIcon(GuiGraphics poseStack, int i, int j, GuiMessageTag.Icon icon) {
        int k = j - icon.height - 1;
        icon.draw(poseStack, i, k);

    }

//    private void drawTagIcon(Matrix4f mat4f, int i, int j, GuiMessageTag.Icon icon) {
//        int k = j - icon.height - 1;
//        RenderSystem.setShaderTexture(0, TEXTURE_LOCATION);
//
//        GuiBatchRenderer.blit(mat4f, i, k, icon.u, icon.v, icon.width, icon.height, 32, 32);
//    }

    @Unique
    private static double getTimeFactor(int i) {
        double d = (double)i / 200.0;
        d = 1.0 - d;
        d *= 10.0;
        d = Mth.clamp(d, 0.0, 1.0);
        d *= d;
        return d;
    }
}
