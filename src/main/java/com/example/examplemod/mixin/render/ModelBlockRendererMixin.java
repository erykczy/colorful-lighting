package com.example.examplemod.mixin.render;

import com.example.examplemod.util.BufferUtils;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {
    @Inject(at = @At("HEAD"), method = "putQuadData", cancellable = true)
    public void putQuadData(
            BlockAndTintGetter level,
            BlockState state,
            BlockPos pos,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            BakedQuad quad,
            float brightness0,
            float brightness1,
            float brightness2,
            float brightness3,
            int lightmap0,
            int lightmap1,
            int lightmap2,
            int lightmap3,
            int packedOverlay,
            CallbackInfo ci)
    {
        if(!(consumer instanceof BufferBuilder bufferBuilder)) return;
        ci.cancel();
        ModelBlockRenderer renderer = (ModelBlockRenderer) (Object)this;

        float f;
        float f1;
        float f2;
        if (quad.isTinted()) {
            int i = renderer.blockColors.getColor(state, level, pos, quad.getTintIndex());
            f = (float)(i >> 16 & 0xFF) / 255.0F;
            f1 = (float)(i >> 8 & 0xFF) / 255.0F;
            f2 = (float)(i & 0xFF) / 255.0F;
        } else {
            f = 1.0F;
            f1 = 1.0F;
            f2 = 1.0F;
        }

        BufferUtils.putQuadWithColoredLighting(
                bufferBuilder,
                pose,
                quad,
                new float[]{brightness0, brightness1, brightness2, brightness3},
                f,
                f1,
                f2,
                1.0F,
                new int[]{lightmap0, lightmap1, lightmap2, lightmap3},
                packedOverlay,
                true,
                pos
                //ColoredLightManager.getLightColor(pos)
        );
    }
}
