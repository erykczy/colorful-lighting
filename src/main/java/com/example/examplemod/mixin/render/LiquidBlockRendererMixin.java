package com.example.examplemod.mixin.render;

import com.example.examplemod.util.PackedLightData;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {
    @Inject(method = "getLightColor", at = @At("HEAD"), cancellable = true)
    private void coloredLights$getLightColor(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int lightColor = LevelRenderer.getLightColor(level, pos);
        int lightColorAbove = LevelRenderer.getLightColor(level, pos.above());
        PackedLightData data = PackedLightData.unpackData(lightColor);
        PackedLightData dataAbove = PackedLightData.unpackData(lightColorAbove);
        int blockLight = Math.max(data.blockLight, dataAbove.blockLight);
        int skyLight = Math.max(data.skyLight, dataAbove.skyLight);
        int red4 = Math.max(data.red4, dataAbove.red4);
        int green4 = Math.max(data.green4, dataAbove.green4);
        int blue4 = Math.max(data.blue4, dataAbove.blue4);

        cir.setReturnValue(PackedLightData.packData(blockLight, skyLight, red4, green4, blue4));
    }
}
