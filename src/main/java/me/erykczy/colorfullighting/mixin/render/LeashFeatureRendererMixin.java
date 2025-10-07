package me.erykczy.colorfullighting.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.LeashFeatureRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeashFeatureRenderer.class)
public class LeashFeatureRendererMixin {
    @Inject(method = "addVertexPair", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/renderer/LightTexture;pack(II)I"))
    private static void colorfullighting$addVertexPair(VertexConsumer consumer, Matrix4f pose, float startX, float startY, float startZ, float yOffset, float dx, float dz, int index, boolean reverse, EntityRenderState.LeashState leashState, CallbackInfo ci, @Local(ordinal = 3) LocalIntRef k) {
        k.set(leashState.startBlockLight);
    }
}
