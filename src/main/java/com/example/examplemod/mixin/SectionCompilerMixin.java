package com.example.examplemod.mixin;

import com.example.examplemod.client.ModRenderTypes;
import com.example.examplemod.client.ModVertexFormats;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(SectionCompiler.class)
public class SectionCompilerMixin {
    @Inject(at = @At("HEAD"), method = "getOrBeginLayer", cancellable = true)
    public void getOrBeginLayer(Map<RenderType, BufferBuilder> bufferLayers, SectionBufferBuilderPack sectionBufferBuilderPack, RenderType renderType, CallbackInfoReturnable<BufferBuilder> cir) {
        if(renderType == RenderType.solid())
            renderType = ModRenderTypes.COLORED_LIGHT_SOLID;
        else if(renderType == RenderType.cutoutMipped())
            renderType = ModRenderTypes.COLORED_LIGHT_CUTOUT_MIPPED;
        else if(renderType == RenderType.cutout())
            renderType = ModRenderTypes.COLORED_LIGHT_CUTOUT;
        else if(renderType == RenderType.translucent())
            renderType = ModRenderTypes.COLORED_LIGHT_TRANSLUCENT;

        BufferBuilder bufferbuilder = bufferLayers.get(renderType);
        if (bufferbuilder == null) {
            ByteBufferBuilder bytebufferbuilder = sectionBufferBuilderPack.buffer(renderType);
            bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, ModVertexFormats.COLORED_LIGHT_BLOCK);
            bufferLayers.put(renderType, bufferbuilder);
        }

        cir.setReturnValue(bufferbuilder);
    }
}
