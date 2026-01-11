package me.erykczy.colorfullighting.mixin;

import me.erykczy.colorfullighting.common.ColoredLightEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelLightEngine.class)
public class LevelLightEngineMixin {
    @Inject(method = "runLightUpdates", at = @At("TAIL"), require = 0)
    private void colorfullighting$runLightUpdates(CallbackInfoReturnable<Integer> cir) {
        if(!Minecraft.getInstance().isSameThread()) return; // only client side
        ColoredLightEngine.getInstance().onLightUpdate();
    }
}
