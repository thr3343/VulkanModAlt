package net.vulkanmod.mixin.render;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.vulkanmod.vulkan.memory.MemoryManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow @Final private Map<String, ShaderInstance> shaders;

    @Shadow private @Nullable static ShaderInstance positionShader;
    @Shadow private @Nullable static ShaderInstance positionColorShader;
    @Shadow private @Nullable static ShaderInstance positionColorTexShader;
    @Shadow private @Nullable static ShaderInstance positionTexShader;
    @Shadow private @Nullable static ShaderInstance positionTexColorShader;
//    @Shadow private @Nullable static ShaderInstance blockShader;
//    @Shadow private @Nullable static ShaderInstance newEntityShader;
    @Shadow private @Nullable static ShaderInstance particleShader;
    @Shadow private @Nullable static ShaderInstance positionColorLightmapShader;
    @Shadow private @Nullable static ShaderInstance positionColorTexLightmapShader;
    @Shadow private @Nullable static ShaderInstance positionTexColorNormalShader;
    @Shadow private @Nullable static ShaderInstance positionTexLightmapColorShader;
    @Shadow private @Nullable static ShaderInstance rendertypeSolidShader;
    @Shadow private @Nullable static ShaderInstance rendertypeCutoutMippedShader;
    @Shadow private @Nullable static ShaderInstance rendertypeCutoutShader;
    @Shadow private @Nullable static ShaderInstance rendertypeTranslucentShader;
    @Shadow private @Nullable static ShaderInstance rendertypeTranslucentMovingBlockShader;
    @Shadow private @Nullable static ShaderInstance rendertypeTranslucentNoCrumblingShader;
    @Shadow private @Nullable static ShaderInstance rendertypeArmorCutoutNoCullShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntitySolidShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityCutoutShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityCutoutNoCullShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityCutoutNoCullZOffsetShader;
    @Shadow private @Nullable static ShaderInstance rendertypeItemEntityTranslucentCullShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityTranslucentCullShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityTranslucentShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityTranslucentEmissiveShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntitySmoothCutoutShader;
    @Shadow private @Nullable static ShaderInstance rendertypeBeaconBeamShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityDecalShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityNoOutlineShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityShadowShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityAlphaShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEyesShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEnergySwirlShader;
    @Shadow private @Nullable static ShaderInstance rendertypeLeashShader;
    @Shadow private @Nullable static ShaderInstance rendertypeWaterMaskShader;
    @Shadow private @Nullable static ShaderInstance rendertypeOutlineShader;
    @Shadow private @Nullable static ShaderInstance rendertypeArmorGlintShader;
    @Shadow private @Nullable static ShaderInstance rendertypeArmorEntityGlintShader;
    @Shadow private @Nullable static ShaderInstance rendertypeGlintTranslucentShader;
    @Shadow private @Nullable static ShaderInstance rendertypeGlintShader;
    @Shadow private @Nullable static ShaderInstance rendertypeGlintDirectShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityGlintShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEntityGlintDirectShader;
    @Shadow private @Nullable static ShaderInstance rendertypeTextShader;
    @Shadow private @Nullable static ShaderInstance rendertypeTextIntensityShader;
    @Shadow private @Nullable static ShaderInstance rendertypeTextSeeThroughShader;
    @Shadow private @Nullable static ShaderInstance rendertypeTextIntensitySeeThroughShader;
    @Shadow private @Nullable static ShaderInstance rendertypeLightningShader;
    @Shadow private @Nullable static ShaderInstance rendertypeTripwireShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEndPortalShader;
    @Shadow private @Nullable static ShaderInstance rendertypeEndGatewayShader;
    @Shadow private @Nullable static ShaderInstance rendertypeLinesShader;
    @Shadow private @Nullable static ShaderInstance rendertypeCrumblingShader;

    @Nullable
    private static ShaderInstance rendertypeGuiShader;
    @Nullable
    private static ShaderInstance rendertypeGuiOverlayShader;
    @Nullable
    private static ShaderInstance rendertypeGuiTextHighlightShader;
    @Nullable
    private static ShaderInstance rendertypeGuiGhostRecipeOverlayShader;

    @Shadow protected abstract void shutdownShaders();

    @Shadow public ShaderInstance blitShader;

    @Inject(method = "reloadShaders", at = @At("HEAD"), cancellable = true)
    public void reloadShaders(ResourceProvider provider, CallbackInfo ci) {
        RenderSystem.assertOnRenderThread();
        List<Program> list = Lists.newArrayList();
//        list.addAll(Program.Type.FRAGMENT.getPrograms().values());
//        list.addAll(Program.Type.VERTEX.getPrograms().values());
//        list.forEach(Program::close);
        List<Pair<ShaderInstance, Consumer<ShaderInstance>>> list1 = Lists.newArrayListWithCapacity(this.shaders.size());

        try {
//         list1.add(Pair.of(new ShaderInstance(provider, "block", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), (p_172743_) -> {
//            blockShader = p_172743_;
//         }));
//         list1.add(Pair.of(new ShaderInstance(provider, "new_entity", DefaultVertexFormat.POSITION_COLOR_TEX_OVERLAY_LIGHTMAP), (p_172740_) -> {
//            newEntityShader = p_172740_;
//         }));
            list1.add(Pair.of(new ShaderInstance(provider, "particle", DefaultVertexFormat.PARTICLE), (p_172714_) -> {
                particleShader = p_172714_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "position", DefaultVertexFormat.POSITION), (p_172711_) -> {
                positionShader = p_172711_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "position_color", DefaultVertexFormat.POSITION_COLOR), (p_172708_) -> {
                positionColorShader = p_172708_;
            }));
//         list1.add(Pair.of(new ShaderInstance(provider, "position_color_lightmap", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP), (p_172705_) -> {
//            positionColorLightmapShader = p_172705_;
//         }));
            list1.add(Pair.of(new ShaderInstance(provider, "position_color_tex", DefaultVertexFormat.POSITION_COLOR_TEX), (p_172702_) -> {
                positionColorTexShader = p_172702_;
            }));
//         list1.add(Pair.of(new ShaderInstance(provider, "position_color_tex_lightmap", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), (p_172699_) -> {
//            positionColorTexLightmapShader = p_172699_;
//         }));
            list1.add(Pair.of(new ShaderInstance(provider, "position_tex", DefaultVertexFormat.POSITION_TEX), (p_172696_) -> {
                positionTexShader = p_172696_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "position_tex_color", DefaultVertexFormat.POSITION_TEX_COLOR), (p_172693_) -> {
                positionTexColorShader = p_172693_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "position_tex_color_normal", DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL), (p_172690_) -> {
                positionTexColorNormalShader = p_172690_;
            }));
//         list1.add(Pair.of(new ShaderInstance(provider, "position_tex_lightmap_color", DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR), (p_172687_) -> {
//            positionTexLightmapColorShader = p_172687_;
//         }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_solid", DefaultVertexFormat.BLOCK), (p_172684_) -> {
                rendertypeSolidShader = p_172684_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_cutout_mipped", DefaultVertexFormat.BLOCK), (p_172681_) -> {
                rendertypeCutoutMippedShader = p_172681_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_cutout", DefaultVertexFormat.BLOCK), (p_172678_) -> {
                rendertypeCutoutShader = p_172678_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_translucent", DefaultVertexFormat.BLOCK), (p_172675_) -> {
                rendertypeTranslucentShader = p_172675_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_translucent_moving_block", DefaultVertexFormat.BLOCK), (p_172672_) -> {
                rendertypeTranslucentMovingBlockShader = p_172672_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_translucent_no_crumbling", DefaultVertexFormat.BLOCK), (p_172669_) -> {
                rendertypeTranslucentNoCrumblingShader = p_172669_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_armor_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY), (p_172666_) -> {
                rendertypeArmorCutoutNoCullShader = p_172666_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_solid", DefaultVertexFormat.NEW_ENTITY), (p_172663_) -> {
                rendertypeEntitySolidShader = p_172663_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_cutout", DefaultVertexFormat.NEW_ENTITY), (p_172660_) -> {
                rendertypeEntityCutoutShader = p_172660_;
            }));

            //No diff in these shaders
            ShaderInstance entity_no_cull = new ShaderInstance(provider, "rendertype_entity_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY);
            list1.add(Pair.of(entity_no_cull, (p_172657_) -> {
                rendertypeEntityCutoutNoCullShader = p_172657_;
            }));
//         list1.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_cutout_no_cull_z_offset", DefaultVertexFormat.POSITION_COLOR_TEX_OVERLAY_LIGHTMAP), (p_172654_) -> {
//            rendertypeEntityCutoutNoCullZOffsetShader = p_172654_;
//         }));
            list1.add(Pair.of(entity_no_cull, (p_172654_) -> {
                rendertypeEntityCutoutNoCullZOffsetShader = p_172654_;
            }));

            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_item_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY), (p_172651_) -> {
                rendertypeItemEntityTranslucentCullShader = p_172651_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY), (p_172648_) -> {
                rendertypeEntityTranslucentCullShader = p_172648_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_translucent", DefaultVertexFormat.NEW_ENTITY), (p_172645_) -> {
                rendertypeEntityTranslucentShader = p_172645_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_translucent_emissive", DefaultVertexFormat.NEW_ENTITY), shader -> {
                rendertypeEntityTranslucentEmissiveShader = shader;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_smooth_cutout", DefaultVertexFormat.NEW_ENTITY), (p_172642_) -> {
                rendertypeEntitySmoothCutoutShader = p_172642_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_beacon_beam", DefaultVertexFormat.BLOCK), (p_172639_) -> {
                rendertypeBeaconBeamShader = p_172639_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_decal", DefaultVertexFormat.NEW_ENTITY), (p_172840_) -> {
                rendertypeEntityDecalShader = p_172840_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_no_outline", DefaultVertexFormat.NEW_ENTITY), (p_172837_) -> {
                rendertypeEntityNoOutlineShader = p_172837_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_shadow", DefaultVertexFormat.NEW_ENTITY), (p_172834_) -> {
                rendertypeEntityShadowShader = p_172834_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_alpha", DefaultVertexFormat.NEW_ENTITY), (p_172831_) -> {
                rendertypeEntityAlphaShader = p_172831_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_eyes", DefaultVertexFormat.NEW_ENTITY), (p_172828_) -> {
                rendertypeEyesShader = p_172828_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_energy_swirl", DefaultVertexFormat.NEW_ENTITY), (p_172825_) -> {
                rendertypeEnergySwirlShader = p_172825_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_leash", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP), (p_172822_) -> {
                rendertypeLeashShader = p_172822_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_water_mask", DefaultVertexFormat.POSITION), (p_172819_) -> {
                rendertypeWaterMaskShader = p_172819_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_outline", DefaultVertexFormat.POSITION_COLOR_TEX), (p_172816_) -> {
                rendertypeOutlineShader = p_172816_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_armor_glint", DefaultVertexFormat.POSITION_TEX), (p_172813_) -> {
                rendertypeArmorGlintShader = p_172813_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_armor_entity_glint", DefaultVertexFormat.POSITION_TEX), (p_172810_) -> {
                rendertypeArmorEntityGlintShader = p_172810_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_glint_translucent", DefaultVertexFormat.POSITION_TEX), (p_172807_) -> {
                rendertypeGlintTranslucentShader = p_172807_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_glint", DefaultVertexFormat.POSITION_TEX), (p_172805_) -> {
                rendertypeGlintShader = p_172805_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_glint_direct", DefaultVertexFormat.POSITION_TEX), (p_172803_) -> {
                rendertypeGlintDirectShader = p_172803_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_glint", DefaultVertexFormat.POSITION_TEX), (p_172801_) -> {
                rendertypeEntityGlintShader = p_172801_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_glint_direct", DefaultVertexFormat.POSITION_TEX), (p_172799_) -> {
                rendertypeEntityGlintDirectShader = p_172799_;
            }));

            //Text
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_text", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), (p_172799_) -> {
                rendertypeTextShader = p_172799_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_text_intensity", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), (p_172799_) -> {
                rendertypeTextIntensityShader = p_172799_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_text_see_through", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), (p_172799_) -> {
                rendertypeTextSeeThroughShader = p_172799_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_text_intensity_see_through", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), (p_172799_) -> {
                rendertypeTextIntensitySeeThroughShader = p_172799_;
            }));

            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_lightning", DefaultVertexFormat.POSITION_COLOR), (p_172787_) -> {
                rendertypeLightningShader = p_172787_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_tripwire", DefaultVertexFormat.BLOCK), (p_172785_) -> {
                rendertypeTripwireShader = p_172785_;
            }));
            ShaderInstance endPortalShader = new ShaderInstance(provider, "rendertype_end_portal", DefaultVertexFormat.POSITION);
            list1.add(Pair.of(endPortalShader, (p_172782_) -> {
                rendertypeEndPortalShader = p_172782_;
            }));
            list1.add(Pair.of(endPortalShader, (p_172778_) -> {
                rendertypeEndGatewayShader = p_172778_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL), (p_172774_) -> {
                rendertypeLinesShader = p_172774_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_crumbling", DefaultVertexFormat.BLOCK), (p_172733_) -> {
                rendertypeCrumblingShader = p_172733_;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_gui", DefaultVertexFormat.POSITION_COLOR), (shaderInstance) -> {
                rendertypeGuiShader = shaderInstance;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_gui_overlay", DefaultVertexFormat.POSITION_COLOR), (shaderInstance) -> {
                rendertypeGuiOverlayShader = shaderInstance;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_gui_text_highlight", DefaultVertexFormat.POSITION_COLOR), (shaderInstance) -> {
                rendertypeGuiTextHighlightShader = shaderInstance;
            }));
            list1.add(Pair.of(new ShaderInstance(provider, "rendertype_gui_ghost_recipe_overlay", DefaultVertexFormat.POSITION_COLOR), (shaderInstance) -> {
                rendertypeGuiGhostRecipeOverlayShader = shaderInstance;
            }));

        } catch (IOException ioexception) {
            list1.forEach((p_172772_) -> {
                p_172772_.getFirst().close();
            });
            throw new RuntimeException("could not reload shaders", ioexception);
        }

        final var clearList = ImmutableList.copyOf(this.shaders.values());
        MemoryManager.getInstance().addFrameOp(() -> clearList.forEach((ShaderInstance::close)));
        //this.shutdownShaders();
        //TODO: clear shaders
        list1.forEach((pair) -> {
            ShaderInstance shaderinstance = pair.getFirst();
            this.shaders.put(shaderinstance.getName(), shaderinstance);
            pair.getSecond().accept(shaderinstance);
        });

        ci.cancel();
    }

}
