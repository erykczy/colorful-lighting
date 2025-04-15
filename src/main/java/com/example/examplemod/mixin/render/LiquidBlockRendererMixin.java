package com.example.examplemod.mixin.render;

import com.example.examplemod.ColoredLightManager;
import com.example.examplemod.util.BufferUtils;
import com.example.examplemod.util.PackedLightData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {
    @Inject(method = "getLightColor", at = @At("HEAD"), cancellable = true)
    private void coloredLights$getLightColor(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int lightColor = LevelRenderer.getLightColor(level, pos);
        int lightColorAbove = LevelRenderer.getLightColor(level, pos.above());
        PackedLightData data = PackedLightData.unpackData(lightColor);
        PackedLightData dataAbove = PackedLightData.unpackData(lightColorAbove);
        int blockLight = Math.max(data.blockLight, dataAbove.blockLight);
        int skyLight = Math.max(data.skyLight, dataAbove.skyLight);
        int red4 = Math.max(data.red4, dataAbove.red4);
        int green4 = Math.max(data.green4, dataAbove.green4);
        int blue4 = Math.max(data.blue4, dataAbove.blue4);

        cir.setReturnValue(PackedLightData.packData(blockLight, skyLight, red4, green4, blue4));
    }

    /*@Unique
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
        BufferUtils.forceSetLightColor(buffer, ColoredLightManager.getInstance().sampleSimpleInterpolationLightColor(new Vec3(sectionOrigin.getX() + x, sectionOrigin.getY() + y, sectionOrigin.getZ() + z)), false);
    }*/
}
