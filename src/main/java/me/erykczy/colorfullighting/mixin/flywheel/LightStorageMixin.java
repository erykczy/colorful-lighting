package me.erykczy.colorfullighting.mixin.flywheel;

import dev.engine_room.flywheel.backend.engine.LightStorage;
import dev.engine_room.flywheel.backend.engine.indirect.StagingBuffer;
import me.erykczy.colorfullighting.ColorfulLighting;
import me.erykczy.colorfullighting.flywheel.ColoredLightFlywheelStorage;
import me.erykczy.colorfullighting.flywheel.FlywheelCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightStorage.class)
public class LightStorageMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void colorfullighting$init(CallbackInfo ci) {
        FlywheelCompat.getInstance().flywheelColoredLightStorage = new ColoredLightFlywheelStorage();
    }

    @Inject(method = "delete", at = @At("TAIL"))
    private void colorfullighting$delete(CallbackInfo ci) {
        FlywheelCompat.getInstance().flywheelColoredLightStorage.delete();
    }

    @Inject(method = "collectSection", at = @At("TAIL"))
    private void colorfullighting$collectSection(long section, CallbackInfo ci) {
        FlywheelCompat.getInstance().flywheelColoredLightStorage.collectSection(section);
    }

    @Inject(method = "uploadChangedSections", at = @At("TAIL"))
    private void colorfullighting$uploadChangedSections(StagingBuffer staging, int dstVbo, CallbackInfo ci) {
        FlywheelCompat.getInstance().flywheelColoredLightStorage.uploadChangedSections(staging);
    }

    @Inject(method = "endTrackingSection", at = @At("TAIL"))
    private void colorfullighting$endTrackingSection(long section, CallbackInfo ci) {
        FlywheelCompat.getInstance().flywheelColoredLightStorage.removeSection(section);
    }
}
