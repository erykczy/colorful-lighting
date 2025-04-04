package com.example.examplemod.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.debug.LightSectionDebugRenderer;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugRenderer.class)
public class DebugRendererMixin {
    @Unique
    private LightSectionDebugRenderer blockLightSectionDebugRenderer;

    @Inject(at = @At("TAIL"), method = "<init>")
    private void init(Minecraft minecraft, CallbackInfo ci) {
        blockLightSectionDebugRenderer = new LightSectionDebugRenderer(minecraft, LightLayer.BLOCK);
    }

    @Inject(at = @At("HEAD"), method = "render")
    public void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, double camX, double camY, double camZ, CallbackInfo ci) {
        //blockLightSectionDebugRenderer.render(poseStack, bufferSource, camX, camY, camZ);
    }
}
