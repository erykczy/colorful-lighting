package me.erykczy.colorfullighting.mixin.compat.starlight;

import me.erykczy.colorfullighting.common.ColoredLightEngine;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "ca.spottedleaf.starlight.common.light.StarLightInterface", remap = false)
public class StarlightLevelLightEngineMixin {

    @Inject(method = "propagateChanges", at = @At("TAIL"))
    private void colorfullighting$propagateChanges(CallbackInfo ci) {
        if (!Minecraft.getInstance().isSameThread()) return; // only client side
        ColoredLightEngine.getInstance().onLightUpdate();
    }
}
