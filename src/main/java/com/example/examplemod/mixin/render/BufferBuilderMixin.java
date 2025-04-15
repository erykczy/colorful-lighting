package com.example.examplemod.mixin.render;

import com.example.examplemod.util.BufferUtils;
import com.example.examplemod.util.ColorRGB8;
import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BufferBuilder.class)
public class BufferBuilderMixin {
    @Inject(method = "endLastVertex", at = @At("HEAD"))
    private void coloredLights$endLastVertex(CallbackInfo ci) {
        BufferBuilder bufferBuilder = (BufferBuilder)(Object)this;
        if(bufferBuilder.vertices == 0) return;

        if(!BufferUtils.isLightColorFilled(bufferBuilder))
            BufferUtils.forceSetLightColor(bufferBuilder, ColorRGB8.fromRGB8(0, 0, 0), true);
    }
}
