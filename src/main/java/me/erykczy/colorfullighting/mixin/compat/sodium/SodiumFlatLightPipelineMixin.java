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
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Arrays;

@Mixin(targets = "me.jellysquid.mods.sodium.client.model.light.flat.FlatLightPipeline", remap = false, priority = 500)
public abstract class SodiumFlatLightPipelineMixin {

    @Shadow private LightDataAccess lightCache;
    @Shadow private boolean useQuadNormalsForShading;

    @Shadow private void applySidedBrightnessFromNormals(ModelQuadView quad, QuadLightData out, boolean shade) {}

    /**
     * @author Erykczy
     * @reason Inject colored lighting logic
     */
    @Overwrite
    private int getOffsetLightmap(BlockPos pos, Direction face) {
        int word = this.lightCache.get(pos);

        if (LightDataAccess.unpackEM(word)) {
             BlockAndTintGetter level = this.lightCache.getWorld();
             BlockState state = level.getBlockState(pos);
             
             LevelAccessor levelAccessor = ColorfulLighting.clientAccessor.getLevel();
             if(levelAccessor != null) {
                BlockStateAccessor stateAccessor = new BlockStateWrapper(state);
                var emission = Config.getLightColor(stateAccessor);
                return SodiumPackedLightData.packData(15, ColorRGB8.fromRGB4(emission));
             }
             return 0xF000F0;
        }

        BlockPos offsetPos = pos.relative(face);
        ColorRGB4 color = ColoredLightEngine.getInstance().sampleLightColor(offsetPos);
        
        int adjWord = this.lightCache.get(pos, face);
        int skyLight = LightDataAccess.unpackSL(adjWord);
        
        return SodiumPackedLightData.packData(skyLight, ColorRGB8.fromRGB4(color));
    }

    /**
     * @author Erykczy
     * @reason Ensure colored lighting is used even for non-offset faces (plants, etc.)
     */
    @Overwrite
    public void calculate(ModelQuadView quad, BlockPos pos, QuadLightData out, Direction cullFace, Direction lightFace, boolean shade) {
        int lightmap;

        if (cullFace != null) {
            lightmap = getOffsetLightmap(pos, cullFace);
        } else {
            int flags = quad.getFlags();
            if ((flags & ModelQuadFlags.IS_ALIGNED) != 0 || ((flags & ModelQuadFlags.IS_PARALLEL) != 0 && LightDataAccess.unpackFC(this.lightCache.get(pos)))) {
                lightmap = getOffsetLightmap(pos, lightFace);
            } else {
                // Original: lightmap = getEmissiveLightmap(this.lightCache.get(pos));
                // We need to use our custom logic here too!
                
                int word = this.lightCache.get(pos);
                if (LightDataAccess.unpackEM(word)) {
                     // Same emission logic
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
                    // Sample light at pos
                    ColorRGB4 color = ColoredLightEngine.getInstance().sampleLightColor(pos);
                    int skyLight = LightDataAccess.unpackSL(word);
                    lightmap = SodiumPackedLightData.packData(skyLight, ColorRGB8.fromRGB4(color));
                }
            }
        }

        Arrays.fill(out.lm, lightmap);
        if((quad.getFlags() & ModelQuadFlags.IS_VANILLA_SHADED) != 0 || !this.useQuadNormalsForShading) {
            Arrays.fill(out.br, this.lightCache.getWorld().getShade(lightFace, shade));
        } else {
            this.applySidedBrightnessFromNormals(quad, out, shade);
        }
    }
}
