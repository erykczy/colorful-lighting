package me.erykczy.colorfullighting.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.PackedLightData;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(method = "getPackedLightCoords", at = @At("HEAD"), cancellable = true)
    private <T extends Entity>void colorfullighting$getPackedLightCoords(T entity, float partialTicks, CallbackInfoReturnable<Integer> cir) {
        BlockPos blockpos = BlockPos.containing(entity.getLightProbePosition(partialTicks));
        int skyLight = entity.isOnFire() ? 15 : entity.level().getBrightness(LightLayer.SKY, blockpos);
        ColorRGB8 color = ColoredLightEngine.getInstance().sampleTrilinearLightColor(entity.getLightProbePosition(partialTicks));

        cir.setReturnValue(PackedLightData.packData(skyLight, color));
    }

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;getBlockLightLevel(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;)I"))
    private <T extends Entity>int colorfullighting$extractRenderState(EntityRenderer instance, T entity, BlockPos pos) {
        int skyLight = entity.isOnFire() ? 15 : entity.level().getBrightness(LightLayer.SKY, pos);
        ColorRGB8 color = ColorRGB8.fromRGB4(ColoredLightEngine.getInstance().sampleLightColor(pos));

        return PackedLightData.packData(skyLight, color);
    }
}
