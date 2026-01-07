package me.erykczy.colorfullighting.mixin.compat.starlight;

import ca.spottedleaf.starlight.common.light.BlockStarLightEngine;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LightChunkGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockStarLightEngine.class, remap = false)
public abstract class StarlightBlockLightEngineMixin {
    @Inject(method = "checkBlock", at = @At("TAIL"), require = 0)
    private void colorfullighting$checkBlock(LightChunkGetter lightChunkGetter, int x, int y, int z, CallbackInfo ci) {
        if (!Minecraft.getInstance().isSameThread()) return; // only client side
        ColoredLightEngine.getInstance().onBlockLightPropertiesChanged(new BlockPos(x, y, z));
    }
}
