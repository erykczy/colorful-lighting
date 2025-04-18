package com.example.examplemod.mixin.render;

import com.example.examplemod.common.util.PackedLightData;
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
        //int blockLight = Math.max(data.blockLight, dataAbove.blockLight);
        int skyLight = Math.max(data.skyLight4, dataAbove.skyLight4);
        int red8 = Math.max(data.red8, dataAbove.red8);
        int green8 = Math.max(data.green8, dataAbove.green8);
        int blue8 = Math.max(data.blue8, dataAbove.blue8);

        cir.setReturnValue(PackedLightData.packData(skyLight, red8, green8, blue8));
    }
}
