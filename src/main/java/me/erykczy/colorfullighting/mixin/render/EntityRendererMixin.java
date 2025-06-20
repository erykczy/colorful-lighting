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

    @Inject(method = "addVertexPair", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/renderer/LightTexture;pack(II)I"))
    private static void colorfullighting$addVertexPair(VertexConsumer p_352095_, Matrix4f p_352142_, float p_352462_, float p_352226_, float p_352086_, float p_352293_, float p_352138_, float p_352315_, float p_352162_, int p_352406_, boolean p_352079_, EntityRenderState.LeashState p_418052_, CallbackInfo ci, @Local(ordinal = 3) LocalIntRef k) {
        k.set(p_418052_.startBlockLight);
    }
}
