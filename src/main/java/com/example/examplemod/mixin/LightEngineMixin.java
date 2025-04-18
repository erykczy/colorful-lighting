package com.example.examplemod.mixin;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.accessors.BlockStateWrapper;
import com.example.examplemod.common.Config;
import com.example.examplemod.common.accessors.BlockStateAccessor;
import com.example.examplemod.common.accessors.LevelAccessor;
import com.example.examplemod.common.util.ColorRGB4;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightEngine.class)
public class LightEngineMixin {
    @Inject(method = "hasDifferentLightProperties", at = @At("HEAD"), cancellable = true)
    private static void coloredLights$hasDifferentLightProperties(BlockGetter level, BlockPos pos, BlockState state1, BlockState state2, CallbackInfoReturnable<Boolean> cir) {
        LevelAccessor clientLevel = ExampleMod.clientAccessor.getLevel();
        BlockStateAccessor blockState1 = new BlockStateWrapper(state1);
        BlockStateAccessor blockState2 = new BlockStateWrapper(state2);
        if(clientLevel == null) return;
        ColorRGB4 color1 = Config.getColorEmission(clientLevel, pos, blockState1);
        ColorRGB4 color2 = Config.getColorEmission(clientLevel, pos, blockState2);
        if(!color1.equals(color2)) {
            cir.setReturnValue(true);
            return;
        }
        color1 = Config.getColoredLightTransmittance(clientLevel, pos, blockState1);
        color2 = Config.getColoredLightTransmittance(clientLevel, pos, blockState2);
        if(!color1.equals(color2)) {
            cir.setReturnValue(true);
            return;
        }
    }
}
