package me.erykczy.colorfullighting.mixin.flywheel;

import dev.engine_room.flywheel.backend.engine.indirect.LightBuffers;
import dev.engine_room.flywheel.backend.engine.indirect.StagingBuffer;
import me.erykczy.colorfullighting.ColorfulLighting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightBuffers.class)
public class LightBuffersMixin {
    @Inject(method = "bind", at = @At("TAIL"))
    private void colorfullighting$uploadChangedSections(CallbackInfo ci) {
        ColorfulLighting.flywheelColoredLightStorage.bindBuffers();
    }
}
