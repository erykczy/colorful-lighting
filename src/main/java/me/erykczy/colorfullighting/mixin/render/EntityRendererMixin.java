package me.erykczy.colorfullighting.mixin.render;

import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.PackedLightData;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(method = "getPackedLightCoords", at = @At("HEAD"), cancellable = true)
    private <T extends Entity>void colorfullighting$getPackedLightCoords(T entity, float partialTicks, CallbackInfoReturnable<Integer> cir) {
        if (ModList.get().isLoaded("embeddium")) {
            BlockPos pos = BlockPos.containing(entity.getLightProbePosition(partialTicks));
            int block = entity.isOnFire() ? 15 : entity.level().getBrightness(LightLayer.BLOCK, pos);
            int sky = entity.level().getBrightness(LightLayer.SKY, pos);
            cir.setReturnValue(LightTexture.pack(block, sky));
        } else {
            BlockPos blockpos = BlockPos.containing(entity.getLightProbePosition(partialTicks));
            int skyLight = entity.isOnFire() ? 15 : entity.level().getBrightness(LightLayer.SKY, blockpos);
            ColorRGB8 color = ColoredLightEngine.getInstance().sampleTrilinearLightColor(entity.getLightProbePosition(partialTicks));
            cir.setReturnValue(PackedLightData.packData(skyLight, color));
        }
    }
}
