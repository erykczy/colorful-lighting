package com.example.examplemod.mixin;

import com.example.examplemod.ColoredLightManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelLightEngine.class)
public class LevelLightEngineMixin {
    @Inject(method = "runLightUpdates", at = @At("TAIL"))
    private void coloredLights$runLightUpdates(CallbackInfoReturnable<Integer> cir) {
        if(!Minecraft.getInstance().isSameThread()) return; // only client side
        LevelLightEngine engine = (LevelLightEngine)(Object)this;

        ColoredLightManager.getInstance().runLightUpdates(engine.blockEngine.chunkSource.getLevel());
    }
}
