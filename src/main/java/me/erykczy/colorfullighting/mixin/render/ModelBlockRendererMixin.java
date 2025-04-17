package me.erykczy.colorfullighting.mixin.render;

import net.minecraft.client.renderer.block.ModelBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {
    @Redirect(method = "tesselateWithAO(Lnet/minecraft/world/level/BlockAndTintGetter;Ljava/util/List;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/function/Function;ZI)V",
            at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/model/ao/EnhancedAoRenderStorage;newInstance()Lnet/minecraft/client/renderer/block/ModelBlockRenderer$AmbientOcclusionRenderStorage;"))
    private ModelBlockRenderer.AmbientOcclusionRenderStorage colorfullighting$tesselateWithAO() {
        return new ModelBlockRenderer.AmbientOcclusionRenderStorage();
    }
}
