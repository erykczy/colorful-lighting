package me.erykczy.colorfullighting.mixin.compat.starlight;

import me.erykczy.colorfullighting.common.ColoredLightEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "ca.spottedleaf.starlight.common.light.StarLightInterface", remap = false)
public class StarlightInterfaceMixin {

    @Inject(method = "blockChange", at = @At("TAIL"))
    private void colorfullighting$blockChange(BlockPos pos, CallbackInfoReturnable<?> cir) {
        if (!Minecraft.getInstance().isSameThread()) return; // only client side
        ColoredLightEngine.getInstance().onBlockLightPropertiesChanged(pos);
    }
}
