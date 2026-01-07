package me.erykczy.colorfullighting.mixin.render;

import me.erykczy.colorfullighting.ColorfulLighting;
import me.erykczy.colorfullighting.accessors.BlockStateWrapper;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.Config;
import me.erykczy.colorfullighting.common.accessors.BlockStateAccessor;
import me.erykczy.colorfullighting.common.accessors.LevelAccessor;
import me.erykczy.colorfullighting.common.util.ColorRGB4;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.PackedLightData;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I", at = @At("HEAD"), cancellable = true)
    private static void colorfullighting$getLightColor(BlockAndTintGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        LevelAccessor levelAccessor = ColorfulLighting.clientAccessor.getLevel();
        if (levelAccessor == null) return;

        int skyLight = level.getBrightness(LightLayer.SKY, pos);

        if (state.emissiveRendering(level, pos)) {
            BlockStateAccessor stateAccessor = new BlockStateWrapper(state);
            var emission = Config.getLightColor(stateAccessor);
            cir.setReturnValue(PackedLightData.packData(skyLight, ColorRGB8.fromRGB4(emission)));
        } else {
            var eng = ColoredLightEngine.getInstance();
            if (eng != null) {
                ColorRGB4 color = eng.sampleLightColor(pos);
                if (color.red4 != 0 || color.green4 != 0 || color.blue4 != 0) {
                    cir.setReturnValue(PackedLightData.packData(skyLight, ColorRGB8.fromRGB4(color)));
                }
            }
        }
    }

    @Inject(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I", at = @At("HEAD"), cancellable = true)
    private static void colorfullighting$getLightColor(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        LevelAccessor levelAccessor = ColorfulLighting.clientAccessor.getLevel();
        if (levelAccessor == null) return;

        var eng = ColoredLightEngine.getInstance();
        if (eng != null) {
            ColorRGB4 color = eng.sampleLightColor(pos);
            if (color.red4 != 0 || color.green4 != 0 || color.blue4 != 0) {
                int skyLight = level.getBrightness(LightLayer.SKY, pos);
                cir.setReturnValue(PackedLightData.packData(skyLight, ColorRGB8.fromRGB4(color)));
            }
        }
    }
}
