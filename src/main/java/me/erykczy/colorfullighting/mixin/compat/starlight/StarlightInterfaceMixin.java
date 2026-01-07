package me.erykczy.colorfullighting.mixin.compat.starlight;

import ca.spottedleaf.starlight.common.light.StarLightInterface;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StarLightInterface.class, remap = false)
public abstract class StarlightInterfaceMixin {
    @Inject(method = "propagateChanges", at = @At("TAIL"), require = 0)
    private void colorfullighting$propagateChanges(CallbackInfo ci) {
        if (!Minecraft.getInstance().isSameThread()) return; // only client side
        ColoredLightEngine.getInstance().onLightUpdate();
    }
}
