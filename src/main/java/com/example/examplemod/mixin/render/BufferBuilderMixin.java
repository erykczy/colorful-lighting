package com.example.examplemod.mixin.render;

import com.example.examplemod.client.ModVertexFormatElements;
import com.example.examplemod.util.BufferUtils;
import com.example.examplemod.util.ColorRGB8;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
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
        VertexFormat format = bufferBuilder.format;
        if(!format.contains(ModVertexFormatElements.LIGHT_COLOR)) return;
        if(BufferUtils.isLightColorFilled(bufferBuilder)) return;
        BufferUtils.setLightColor(bufferBuilder, ColorRGB8.fromRGB8(255, 0, 0));
    }
}
