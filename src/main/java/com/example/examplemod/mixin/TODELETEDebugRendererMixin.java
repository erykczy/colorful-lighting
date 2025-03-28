package com.example.examplemod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugRenderer.class)
public class TODELETEDebugRendererMixin {
    @Inject(at = @At("HEAD"), method = "render")
    public void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, double camX, double camY, double camZ, CallbackInfo ci) {
        DebugRenderer renderer = (DebugRenderer) (Object)this;

        //renderer.skyLightSectionDebugRenderer.render(poseStack, bufferSource, camX, camY, camZ);
        //renderer.lightDebugRenderer.render(poseStack, bufferSource, camX, camY, camZ);
    }

}
