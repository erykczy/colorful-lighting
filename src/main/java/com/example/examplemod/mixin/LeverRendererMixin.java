package com.example.examplemod.mixin;

import com.example.examplemod.client.ModRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public class LeverRendererMixin {
    @Redirect(method = "renderSectionLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;setupRenderState()V"))
    private void coloredLights$afterRenderTypeApply(RenderType renderType) {
        ModRenderTypes.vanillaToModified(renderType).setupRenderState();
    }
    
}
