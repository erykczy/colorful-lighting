package com.example.examplemod.mixin.render;

import com.example.examplemod.ColoredLightManager;
import com.example.examplemod.util.ColorRGB4;
import com.example.examplemod.util.PackedLightData;
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
        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        //int thisEmission = state.getLightEmission(level, pos);
        /*if (blockLight < thisEmission) {
            blockLight = thisEmission;
        }*/

        ColorRGB4 color = ColoredLightManager.getInstance().sampleLightColor(pos);
        cir.setReturnValue(PackedLightData.packData(blockLight, skyLight, color));
    }


}
