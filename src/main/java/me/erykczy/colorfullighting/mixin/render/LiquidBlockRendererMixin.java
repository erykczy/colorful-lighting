package me.erykczy.colorfullighting.mixin.render;

import me.erykczy.colorfullighting.common.util.PackedLightData;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidRenderer.class)
public class LiquidBlockRendererMixin {
    @Inject(method = "getLightCoords", at = @At("HEAD"), cancellable = true)
    private void colorfullighting$getLightColor(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int lightColor = LevelRenderer.getLightCoords(level, pos);
        int lightColorAbove = LevelRenderer.getLightCoords(level, pos.above());

        cir.setReturnValue(PackedLightData.max(lightColor, lightColorAbove));
    }
}
