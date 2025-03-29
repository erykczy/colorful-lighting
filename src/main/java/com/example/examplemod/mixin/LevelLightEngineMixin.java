package com.example.examplemod.mixin;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLightEngine.class)
public class LevelLightEngineMixin {
    @Inject(at = @At("HEAD"), method = "updateSectionStatus")
    public void updateSectionStatus(SectionPos pos, boolean isEmpty, CallbackInfo ci) {
        // chunk probably unloads TODO
        //if(isEmpty)
        //    ColoredLightManager.getInstance().storage.removeSection(pos.asLong());
    }
}
