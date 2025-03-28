package com.example.examplemod.mixin;

import com.example.examplemod.ColoredLightManager;
import com.example.examplemod.util.BufferUtils;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {

    @Inject(at = @At("HEAD"), method = "vertex(Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFFFFFFFI)V", cancellable = true)
    private void vertex(
            VertexConsumer buffer,
            float x,
            float y,
            float z,
            float red,
            float green,
            float blue,
            float alpha,
            float u,
            float v,
            int packedLight,
            CallbackInfo ci
    ) {
        if(!(buffer instanceof BufferBuilder bufferBuilder)) return;
        ci.cancel();
        buffer.addVertex(x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setLight(packedLight)
                .setNormal(0.0F, 1.0F, 0.0F);
        // TODO is blockPos correct?
        BufferUtils.setLightColor(bufferBuilder, ColoredLightManager.getInstance().sampleLightColor(new BlockPos((int)x, (int)y, (int)z)));
    }
}
