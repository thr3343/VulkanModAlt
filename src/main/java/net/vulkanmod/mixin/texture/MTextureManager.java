package net.vulkanmod.mixin.texture;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.render.texture.SpriteUtil;
import net.vulkanmod.vulkan.Drawer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

import static net.vulkanmod.vulkan.queue.Queues.TransferQueue;

@Mixin(TextureManager.class)
public abstract class MTextureManager {


    @Shadow @Final private Set<Tickable> tickableTextures;


    @Shadow public abstract AbstractTexture getTexture(ResourceLocation resourceLocation, AbstractTexture abstractTexture);

    /**
     * @author
     */
    @Overwrite
    public void tick() {
        if(Drawer.skipRendering)
            return;

        if(SpriteUtil.shouldUpload())
            TransferQueue.startRecording();
        for (Tickable tickable : this.tickableTextures) {
            tickable.tick();
        }
        if(SpriteUtil.shouldUpload()) {
            SpriteUtil.transitionLayouts(TransferQueue.getCommandBuffer());
            TransferQueue.endRecordingAndSubmit();
//            Synchronization.INSTANCE.waitFences();
        }
    }

    /**
     * @author
     */
    @Overwrite
    public void release(ResourceLocation id) {
        AbstractTexture abstractTexture = this.getTexture(id, MissingTextureAtlasSprite.getTexture());
        if (abstractTexture != MissingTextureAtlasSprite.getTexture()) {
            //TODO: delete
            abstractTexture.releaseId();
        }
    }
}
