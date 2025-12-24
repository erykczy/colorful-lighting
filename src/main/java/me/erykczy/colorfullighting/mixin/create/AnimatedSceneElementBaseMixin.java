package me.erykczy.colorfullighting.mixin.create;

import me.erykczy.colorfullighting.common.util.PackedLightData;
import net.createmod.ponder.foundation.element.AnimatedSceneElementBase;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnimatedSceneElementBase.class)
public class AnimatedSceneElementBaseMixin {
    @Inject(method = "lightCoordsFromFade", at = @At("HEAD"), cancellable = true)
    protected void lightCoordsFromFade(float fade, CallbackInfoReturnable<Integer> cir) {
        int value4 = 0;//(int)Mth.lerp(fade, 5, 15);
        int value8 = (int)Mth.lerp(fade, 5*15, 15*15);
        cir.setReturnValue(PackedLightData.packData(
            value4, value8, value8, value8
        ));
    }
}
