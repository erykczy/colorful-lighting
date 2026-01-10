package me.erykczy.colorfullighting.mixin.render;

import me.erykczy.colorfullighting.ColorfulLighting;
import me.erykczy.colorfullighting.accessors.BlockStateWrapper;
import me.erykczy.colorfullighting.accessors.LevelWrapper;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.Config;
import me.erykczy.colorfullighting.common.accessors.BlockStateAccessor;
import me.erykczy.colorfullighting.common.accessors.LevelAccessor;
import me.erykczy.colorfullighting.common.util.ColorRGB4;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.PackedLightData;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I", at = @At("HEAD"), cancellable = true)
    private static void colorfullighting$getLightColor(BlockAndTintGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int skyLight = level.getBrightness(LightLayer.SKY, pos);
        if(state.emissiveRendering(level, pos)) {
            LevelAccessor levelAccessor = ColorfulLighting.clientAccessor.getLevel();
            if(levelAccessor == null) {
                cir.setReturnValue(PackedLightData.packData(15, 255, 255, 255));
                return;
            }
            BlockStateAccessor stateAccessor = new BlockStateWrapper(state);
            var emission = Config.getLightColor(stateAccessor);
            cir.setReturnValue(PackedLightData.packData(skyLight, ColorRGB8.fromRGB4(emission)));
        }

        ColorRGB4 color = ColoredLightEngine.getInstance().sampleLightColor(pos);
        cir.setReturnValue(PackedLightData.packData(skyLight, ColorRGB8.fromRGB4(color)));
    }

}
