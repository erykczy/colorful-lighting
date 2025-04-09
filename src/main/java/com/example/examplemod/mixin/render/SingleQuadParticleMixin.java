package com.example.examplemod.mixin.render;

import com.example.examplemod.ColoredLightManager;
import com.example.examplemod.util.BufferUtils;
import com.example.examplemod.util.ColorRGB8;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.BlockPos;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SingleQuadParticle.class)
public class SingleQuadParticleMixin {
    @Inject(method = "renderVertex", at = @At("TAIL"))
    private void coloredLights$afterRenderVertex(VertexConsumer consumer, Quaternionf quaternion, float x, float y, float z, float xOffset, float yOffset, float quadSize, float u, float v, int packedLight, CallbackInfo ci) {
        SingleQuadParticle singleQuadParticle = (SingleQuadParticle)(Object)this;
        ColorRGB8 lightColor = ColoredLightManager.getInstance().sampleLightColor(BlockPos.containing(singleQuadParticle.getPos()));
        if(!(consumer instanceof BufferBuilder bufferBuilder)) return;
        BufferUtils.setLightColor(bufferBuilder, lightColor);
    }
}
