package com.example.examplemod.mixin;

import com.example.examplemod.client.ModRenderTypes;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionBufferBuilderPack.class)
public class SectionBufferBuilderPackMixin {
    @Inject(method = "buffer", at=@At(value = "HEAD"), cancellable = true)
    public void coloredLights$buffer(RenderType renderType, CallbackInfoReturnable<ByteBufferBuilder> ci) {
        SectionBufferBuilderPack pack = (SectionBufferBuilderPack)(Object)this;
        ci.setReturnValue(pack.buffers.get(ModRenderTypes.vanillaToModified(renderType)));
    }
}
