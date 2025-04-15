package com.example.examplemod.mixin.render;

import com.example.examplemod.ColoredLightManager;
import com.example.examplemod.util.ColorRGB4;
import com.example.examplemod.util.PackedLightData;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(method = "getPackedLightCoords", at = @At("HEAD"), cancellable = true)
    private <T extends Entity>void coloredLights$getPackedLightCoords(T entity, float partialTicks, CallbackInfoReturnable<Integer> cir) {
        BlockPos blockpos = BlockPos.containing(entity.getLightProbePosition(partialTicks));
        int blockLight = entity.isOnFire() ? 15 : entity.level().getBrightness(LightLayer.BLOCK, blockpos);
        int skyLight = entity.isOnFire() ? 15 : entity.level().getBrightness(LightLayer.SKY, blockpos);
        ColorRGB4 color = ColoredLightManager.getInstance().sampleTrilinearLightColor(entity.getLightProbePosition(partialTicks));

        cir.setReturnValue(PackedLightData.packData(blockLight, skyLight, color));
    }
}
