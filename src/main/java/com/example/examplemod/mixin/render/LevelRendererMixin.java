package com.example.examplemod.mixin.render;

import com.example.examplemod.common.ColoredLightEngine;
import com.example.examplemod.common.util.ColorRGB4;
import com.example.examplemod.common.util.ColorRGB8;
import com.example.examplemod.common.util.PackedLightData;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I", at = @At("HEAD"), cancellable = true)
    private static void coloredLights$getLightColor(BlockAndTintGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int skyLight = level.getBrightness(LightLayer.SKY, pos);

        ColorRGB4 color = ColoredLightEngine.getInstance().sampleLightColor(pos);
        cir.setReturnValue(PackedLightData.packData(skyLight, ColorRGB8.fromRGB4(color)));
    }

}
