package me.erykczy.colorfullighting.mixin.render;

import me.erykczy.colorfullighting.ColorfulLighting;
import me.erykczy.colorfullighting.accessors.BlockStateWrapper;
import me.erykczy.colorfullighting.common.Config;
import me.erykczy.colorfullighting.common.util.ColorRGB4;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.PackedLightData;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelBlockRenderer.AmbientOcclusionFace.class)
public class AmbientOcclusionFaceMixin {

    @Shadow int[] lightmap;
    @Shadow float[] brightness;

    @Inject(method = "blend(IIII)I", at = @At("HEAD"), cancellable = true)
    private void colorfullighting$blend(int lightColor0, int lightColor1, int lightColor2, int lightColor3, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(PackedLightData.blend(lightColor0, lightColor1, lightColor2, lightColor3));
    }

    @Inject(method = "blend(IIIIFFFF)I", at = @At("HEAD"), cancellable = true)
    private void colorfullighting$blend(int lightColor0, int lightColor1, int lightColor2, int lightColor3, float weight0, float weight1, float weight2, float weight3, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(PackedLightData.blend(lightColor0, lightColor1, lightColor2, lightColor3, weight0, weight1, weight2, weight3));
    }

    @Inject(method = "calculate(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;[FLjava/util/BitSet;Z)V", at = @At("HEAD"), cancellable = true)
    private void colorfullighting$calculate(BlockAndTintGetter level, BlockState state, BlockPos pos, net.minecraft.core.Direction direction, float[] shape, java.util.BitSet bitSet, boolean shade, CallbackInfo ci) {
        if(ColorfulLighting.clientAccessor != null && ColorfulLighting.clientAccessor.getLevel() != null) {
            if (Config.getEmissionBrightness(ColorfulLighting.clientAccessor.getLevel(), pos, new BlockStateWrapper(state)) > 0) {
                ColorRGB4 emission = Config.getColorEmission(ColorfulLighting.clientAccessor.getLevel(), pos, new BlockStateWrapper(state));
                int emissiveLm = PackedLightData.packData(15, ColorRGB8.fromRGB4(emission));
                for(int i=0; i<4; i++) {
                    this.lightmap[i] = emissiveLm;
                    this.brightness[i] = 1.0f;
                }
                ci.cancel();
            }
        }
    }
}
