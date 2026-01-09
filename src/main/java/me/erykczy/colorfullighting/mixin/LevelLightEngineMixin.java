package me.erykczy.colorfullighting.mixin;

import me.erykczy.colorfullighting.common.ColoredLightEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelLightEngine.class)
public class LevelLightEngineMixin {
    @Inject(method = "runLightUpdates", at = @At("TAIL"))
    private void colorfullighting$runLightUpdates(CallbackInfoReturnable<Integer> cir) {
        if (ColoredLightEngine.getInstance() == null) return;
        ColoredLightEngine.getInstance().onLightUpdate();
    }

    @Inject(method = "checkBlock", at = @At("HEAD"))
    private void colorfullighting$checkBlock(BlockPos pos, CallbackInfo ci) {
        if (ColoredLightEngine.getInstance() == null) return;
        ColoredLightEngine.getInstance().onBlockLightPropertiesChanged(pos);
    }
}
