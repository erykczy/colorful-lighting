package me.erykczy.colorfullighting.mixin.compat.sodium;

import me.erykczy.colorfullighting.ColorfulLighting;
import me.erykczy.colorfullighting.accessors.BlockStateWrapper;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.Config;
import me.erykczy.colorfullighting.common.accessors.BlockStateAccessor;
import me.erykczy.colorfullighting.common.accessors.LevelAccessor;
import me.erykczy.colorfullighting.common.util.ColorRGB4;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.compat.sodium.SodiumPackedLightData;
import me.jellysquid.mods.sodium.client.model.light.data.LightDataAccess;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(targets = "me.jellysquid.mods.sodium.client.model.light.flat.FlatLightPipeline", remap = false, priority = 10000)
public abstract class SodiumFlatLightPipelineMixin {

    @Shadow private LightDataAccess lightCache;
    @Shadow private boolean useQuadNormalsForShading;

    @Shadow private void applySidedBrightnessFromNormals(ModelQuadView quad, QuadLightData out, boolean shade) {}

    /**
     * @author Erykczy
     * @reason Inject colored lighting logic
     */
    @Inject(method = "getOffsetLightmap", at = @At("HEAD"), cancellable = true)
    private void getOffsetLightmap(BlockPos pos, Direction face, CallbackInfoReturnable<Integer> cir) {
        if (!ColoredLightEngine.getInstance().isEnabled()) {
            return;
        }

        int word = this.lightCache.get(pos);

        if (LightDataAccess.unpackEM(word)) {
             BlockAndTintGetter level = this.lightCache.getWorld();
             BlockState state = level.getBlockState(pos);
             
             LevelAccessor levelAccessor = ColorfulLighting.clientAccessor.getLevel();
             if(levelAccessor != null) {
                BlockStateAccessor stateAccessor = new BlockStateWrapper(state);
                var emission = Config.getLightColor(stateAccessor);
                cir.setReturnValue(SodiumPackedLightData.packData(15, ColorRGB8.fromRGB4(emission)));
                return;
             }
             cir.setReturnValue(0xF000F0);
             return;
        }

        BlockPos offsetPos = pos.relative(face);
        ColorRGB4 color = ColoredLightEngine.getInstance().sampleLightColor(offsetPos);
        
        int adjWord = this.lightCache.get(pos, face);
        int skyLight = LightDataAccess.unpackSL(adjWord);
        
        cir.setReturnValue(SodiumPackedLightData.packData(skyLight, ColorRGB8.fromRGB4(color)));
    }

    @Inject(method = "calculate", at = @At("RETURN"))
    private void calculate(ModelQuadView quad, BlockPos pos, QuadLightData out, Direction cullFace, Direction lightFace, boolean shade, CallbackInfo ci) {
        if (!ColoredLightEngine.getInstance().isEnabled()) {
            return;
        }

        int lightmap;

        if (cullFace != null) {
            // getOffsetLightmap is injected, so we can call it (or replicate logic if private/shadowed)
            // Since we injected into getOffsetLightmap, calling it via shadow/invoker would work if we had one.
            // But getOffsetLightmap is private. We can't easily call it without an invoker.
            // However, we can just replicate the logic here since we are overwriting the result anyway.
            
            // Replicate getOffsetLightmap logic:
            int word = this.lightCache.get(pos);
            if (LightDataAccess.unpackEM(word)) {
                 BlockAndTintGetter level = this.lightCache.getWorld();
                 BlockState state = level.getBlockState(pos);
                 LevelAccessor levelAccessor = ColorfulLighting.clientAccessor.getLevel();
                 if(levelAccessor != null) {
                    BlockStateAccessor stateAccessor = new BlockStateWrapper(state);
                    var emission = Config.getLightColor(stateAccessor);
                    lightmap = SodiumPackedLightData.packData(15, ColorRGB8.fromRGB4(emission));
                 } else {
                    lightmap = 0xF000F0;
                 }
            } else {
                BlockPos offsetPos = pos.relative(cullFace);
                ColorRGB4 color = ColoredLightEngine.getInstance().sampleLightColor(offsetPos);
                int adjWord = this.lightCache.get(pos, cullFace);
                int skyLight = LightDataAccess.unpackSL(adjWord);
                lightmap = SodiumPackedLightData.packData(skyLight, ColorRGB8.fromRGB4(color));
            }

        } else {
            int flags = quad.getFlags();
            if ((flags & ModelQuadFlags.IS_ALIGNED) != 0 || ((flags & ModelQuadFlags.IS_PARALLEL) != 0 && LightDataAccess.unpackFC(this.lightCache.get(pos)))) {
                // Replicate getOffsetLightmap logic with lightFace
                int word = this.lightCache.get(pos);
                if (LightDataAccess.unpackEM(word)) {
                     BlockAndTintGetter level = this.lightCache.getWorld();
                     BlockState state = level.getBlockState(pos);
                     LevelAccessor levelAccessor = ColorfulLighting.clientAccessor.getLevel();
                     if(levelAccessor != null) {
                        BlockStateAccessor stateAccessor = new BlockStateWrapper(state);
                        var emission = Config.getLightColor(stateAccessor);
                        lightmap = SodiumPackedLightData.packData(15, ColorRGB8.fromRGB4(emission));
                     } else {
                        lightmap = 0xF000F0;
                     }
                } else {
                    BlockPos offsetPos = pos.relative(lightFace);
                    ColorRGB4 color = ColoredLightEngine.getInstance().sampleLightColor(offsetPos);
                    int adjWord = this.lightCache.get(pos, lightFace);
                    int skyLight = LightDataAccess.unpackSL(adjWord);
                    lightmap = SodiumPackedLightData.packData(skyLight, ColorRGB8.fromRGB4(color));
                }
            } else {
                // Original: lightmap = getEmissiveLightmap(this.lightCache.get(pos));
                int word = this.lightCache.get(pos);
                if (LightDataAccess.unpackEM(word)) {
                     BlockAndTintGetter level = this.lightCache.getWorld();
                     BlockState state = level.getBlockState(pos);
                     LevelAccessor levelAccessor = ColorfulLighting.clientAccessor.getLevel();
                     if(levelAccessor != null) {
                        BlockStateAccessor stateAccessor = new BlockStateWrapper(state);
                        var emission = Config.getLightColor(stateAccessor);
                        lightmap = SodiumPackedLightData.packData(15, ColorRGB8.fromRGB4(emission));
                     } else {
                        lightmap = 0xF000F0;
                     }
                } else {
                    ColorRGB4 color = ColoredLightEngine.getInstance().sampleLightColor(pos);
                    int skyLight = LightDataAccess.unpackSL(word);
                    lightmap = SodiumPackedLightData.packData(skyLight, ColorRGB8.fromRGB4(color));
                }
            }
        }

        Arrays.fill(out.lm, lightmap);
    }
}
