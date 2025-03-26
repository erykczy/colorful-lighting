package com.example.examplemod.mixin;

import com.example.examplemod.util.BufferUtils;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {

    @Inject(at = @At("HEAD"), method = "vertex(Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFFFFFFFI)V", cancellable = true)
    private void vertex(
            VertexConsumer buffer,
            float p_110989_,
            float p_110990_,
            float p_110991_,
            float p_110992_,
            float p_110993_,
            float p_350595_,
            float alpha,
            float p_350459_,
            float p_350437_,
            int p_110994_,
            CallbackInfo ci
    ) {
        ci.cancel();
        buffer.addVertex(p_110989_, p_110990_, p_110991_)
                .setColor(p_110992_, p_110993_, p_350595_, alpha)
                .setUv(p_350459_, p_350437_)
                .setLight(p_110994_)
                .setNormal(0.0F, 1.0F, 0.0F);
        BufferUtils.setLightColor((BufferBuilder) buffer, 20, 20, 255);
    }
}
