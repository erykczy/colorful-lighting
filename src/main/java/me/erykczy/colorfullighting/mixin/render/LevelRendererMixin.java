package me.erykczy.colorfullighting.mixin.render;

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
        if (ModList.get().isLoaded("embeddium")) {
            int sky = level.getBrightness(LightLayer.SKY, pos);
            int block = level.getBrightness(LightLayer.BLOCK, pos);
            int emission = state.getLightEmission(level, pos);
            block = Math.max(block, emission);
            if (state.emissiveRendering(level, pos)) {
                block = 15;
                sky = 15;
            }
            cir.setReturnValue(LightTexture.pack(block, sky));
        } else {
            int skyLight = level.getBrightness(LightLayer.SKY, pos);

            if (state.emissiveRendering(level, pos)) {
                LevelAccessor levelAccessor = ColoredLightEngine.getInstance().clientAccessor.getLevel();
                BlockStateAccessor stateAccessor = new BlockStateWrapper(state);
                if (levelAccessor != null) {
                    var emission = Config.getLightColor(stateAccessor);
                    int skyAtt = colorful_lighting$attenuateSky(skyLight, emission);
                    cir.setReturnValue(PackedLightData.packData(skyAtt, ColorRGB8.fromRGB4(emission)));
                    return;
                }
                cir.setReturnValue(PackedLightData.packData(15, 255, 255, 255));
                return;
            }

            ColorRGB4 color = ColoredLightEngine.getInstance().sampleLightColor(pos);
            int skyAtt = colorful_lighting$attenuateSky(skyLight, color);
            cir.setReturnValue(PackedLightData.packData(skyAtt, ColorRGB8.fromRGB4(color)));
        }
    }

    @Unique
    private static int colorful_lighting$attenuateSky(int sky4, ColorRGB4 c) {
        float r = (c.red4 & 0xF);
        float g = (c.green4 & 0xF);
        float b = (c.blue4 & 0xF);
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        if (max <= 0.0f) return sky4;

        float strength = max / 15.0f;
        float sat = (max - min) / max;
        float k = 1f * strength * (0.6f + 0.4f*sat);

        int atten = Math.round(sky4 * (1.0f - k));
        if (atten < 0) atten = 0;
        if (atten > 15) atten = 15;
        return atten;
    }
}
