package com.example.examplemod.mixin.render;

import com.example.examplemod.client.ModVertexFormatElements;
import com.example.examplemod.util.BufferUtils;
import com.example.examplemod.util.ColorRGB8;
import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BufferBuilder.class)
public class BufferBuilderMixin {
    // executed when ending a vertex in the buffer with fastFormat=false
    @Inject(method = "endLastVertex", at = @At("HEAD"))
    private void coloredLights$endLastVertex(CallbackInfo ci) {
        BufferBuilder bufferBuilder = (BufferBuilder)(Object)this;
        if(bufferBuilder.vertices == 0) return;
        if(!bufferBuilder.format.contains(ModVertexFormatElements.LIGHT_COLOR)) return;

        if(!BufferUtils.isLightColorFilled(bufferBuilder))
            BufferUtils.forceSetLightColor(bufferBuilder, ColorRGB8.fromRGB8(1, 0, 0)); // this special case is handled in shaders
    }

    // executed when adding a vertex to the buffer with fastFormat=true
    @Inject(method = "addVertex(FFFIFFIIFFF)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/system/MemoryUtil;memPutByte(JB)V", ordinal = 2, shift = At.Shift.AFTER))
    private void coloredLights$addVertex(float x, float y, float z, int color, float u, float v, int packedOverlay, int packedLight, float normalX, float normalY, float normalZ, CallbackInfo ci) {
        BufferBuilder bufferBuilder = (BufferBuilder)(Object)this;
        if(!bufferBuilder.format.contains(ModVertexFormatElements.LIGHT_COLOR)) return;
        BufferUtils.forceSetLightColor(bufferBuilder, ColorRGB8.fromRGB8(1, 0, 0)); // this special case is handled in shaders
    }
}
