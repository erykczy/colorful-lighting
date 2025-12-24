package me.erykczy.colorfullighting.mixin.create;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChainConveyorRenderer.class)
public class ChainConveyorRendererMixin {
    @ModifyExpressionValue(
        method = "renderChains",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LightTexture;pack(II)I",
            ordinal = 0
        )
    )
    private int colorfullighting$renderChains0(int original, @Local Level level, @Local(ordinal=0) BlockPos blockPos, @Local(ordinal=1) BlockPos tilePos) {
        return LevelRenderer.getLightColor(level, tilePos);
    }

    @ModifyExpressionValue(
            method = "renderChains",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LightTexture;pack(II)I",
                    ordinal = 1
            )
    )
    private int colorfullighting$renderChains1(int original, @Local Level level, @Local(ordinal=0) BlockPos blockPos, @Local(ordinal=1) BlockPos tilePos) {
        return LevelRenderer.getLightColor(level, tilePos.offset(blockPos));
    }
}
