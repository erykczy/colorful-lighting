package com.example.examplemod.mixin;

import com.example.examplemod.common.ColoredLightEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockLightEngine.class)
public abstract class BlockLightEngineMixin {
    @Inject(method = "checkNode", at = @At("TAIL"))
    private void coloredLights$checkNode(long packedPos, CallbackInfo ci) {
        if(!Minecraft.getInstance().isSameThread()) return; // only client side
        ColoredLightEngine.getInstance().onBlockLightPropertiesChanged(BlockPos.of(packedPos));
    }
}
