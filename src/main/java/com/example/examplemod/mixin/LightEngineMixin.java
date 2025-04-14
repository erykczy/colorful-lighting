package com.example.examplemod.mixin;

import com.example.examplemod.Config;
import com.example.examplemod.util.ColorRGB4;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightEngine.class)
public class LightEngineMixin {
    @Inject(method = "hasDifferentLightProperties", at = @At("HEAD"), cancellable = true)
    private static void coloredLights$hasDifferentLightProperties(BlockGetter level, BlockPos pos, BlockState state1, BlockState state2, CallbackInfoReturnable<Boolean> cir) {
        ColorRGB4 color1 = Config.getEmissionColor(level, pos, state1);
        ColorRGB4 color2 = Config.getEmissionColor(level, pos, state2);
        if(!color1.equals(color2))
            cir.setReturnValue(true);
    }
}
