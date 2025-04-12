package com.example.examplemod.mixin.render;

import com.example.examplemod.ColoredLightManager;
import com.example.examplemod.util.BufferUtils;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {
    @Unique
    private BlockPos coloredLights$blockPos;
    @Unique
    private Lock coloredLights$blockPosLock = new ReentrantLock();

    @Inject(method = "tesselate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;vertex(Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFFFFFFFI)V"))
    private void coloredLights$beforeVertex(BlockAndTintGetter level, BlockPos pos, VertexConsumer buffer, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        this.coloredLights$blockPosLock.lock();
        this.coloredLights$blockPos = pos;
    }

    @Redirect(method = "tesselate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;vertex(Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFFFFFFFI)V"))
    private void coloredLights$vertex(
            LiquidBlockRenderer instance,
            VertexConsumer buffer,
            float x,
            float y,
            float z,
            float red,
            float green,
            float blue,
            float alpha,
            float u,
            float v,
            int packedLight
    ) {
        BlockPos blockPos = new BlockPos(this.coloredLights$blockPos);
        this.coloredLights$blockPosLock.unlock();
        //ci.cancel();
        buffer.addVertex(x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setLight(packedLight)
                .setNormal(0.0F, 1.0F, 0.0F);
        SectionPos sectionPos = SectionPos.of(blockPos);
        BlockPos sectionOrigin = sectionPos.origin();
        BufferUtils.forceSetLightColor(buffer, ColoredLightManager.getInstance().sampleSimpleInterpolationLightColor(new Vec3(sectionOrigin.getX() + x, sectionOrigin.getY() + y, sectionOrigin.getZ() + z)));
    }
}
