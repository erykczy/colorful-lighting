package me.erykczy.colorfullighting.mixin.render;

import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {
    @Redirect(method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/model/ao/EnhancedBlockModelLighter;newInstance()Lnet/minecraft/client/renderer/block/BlockModelLighter;"))
    private BlockModelLighter colorfullighting$tesselateWithAO() {
        return new BlockModelLighter();
    }
}
