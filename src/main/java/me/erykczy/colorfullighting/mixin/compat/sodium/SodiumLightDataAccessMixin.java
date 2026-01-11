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
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightDataAccess.class)
public abstract class SodiumLightDataAccessMixin {

    @Shadow protected BlockAndTintGetter world;

    @Shadow public static int packBL(int blockLight) { return 0; }
    @Shadow public static int packSL(int skyLight) { return 0; }
    @Shadow public static int packLU(int luminance) { return 0; }
    @Shadow public static int packAO(float ao) { return 0; }
    @Shadow public static int packEM(boolean emissive) { return 0; }
    @Shadow public static int packOP(boolean opaque) { return 0; }
    @Shadow public static int packFO(boolean opaque) { return 0; }
    @Shadow public static int packFC(boolean fullCube) { return 0; }

    /**
     * @author Erykczy
     * @reason Inject colored lighting logic into Sodium's light data computation
     */
    @Overwrite(remap = false)
    protected int compute(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockAndTintGetter world = this.world;

        BlockState state = world.getBlockState(pos);

        boolean em = state.emissiveRendering(world, pos);
        boolean op = state.isViewBlocking(world, pos) && state.getLightBlock(world, pos) != 0;
        boolean fo = state.isSolidRender(world, pos);
        boolean fc = state.isCollisionShapeFullBlock(world, pos);

        int lu = state.getLightEmission(world, pos);

        int bl;
        int sl;
        
        if (fo && lu == 0) {
            bl = 0;
            sl = 0;
        } else {
            if (em) {
                bl = world.getBrightness(LightLayer.BLOCK, pos);
                sl = world.getBrightness(LightLayer.SKY, pos);
            } else {
                int packedCoords = LevelRenderer.getLightColor(world, state, pos);
                
                // Check if it is our packed format (alpha bits set to 0xF)
                if ((packedCoords >>> 28) == 0xF) {
                     var data = SodiumPackedLightData.unpackData(packedCoords);
                     sl = data.skyLight4;
                     bl = 0; // We don't use cached BL anymore as we fetch color separately
                } else {
                     bl = LightTexture.block(packedCoords);
                     sl = LightTexture.sky(packedCoords);
                }
            }
        }

        float ao;
        if (lu == 0) {
            ao = state.getShadeBrightness(world, pos);
        } else {
            ao = 1.0f;
        }

        return packFC(fc) | packFO(fo) | packOP(op) | packEM(em) | packAO(ao) | packLU(lu) | packSL(sl) | packBL(bl);
    }
}
