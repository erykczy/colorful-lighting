package me.erykczy.colorfullighting.mixin.compat.sodium;

import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.compat.sodium.SodiumAoFaceDataExtension;
import me.erykczy.colorfullighting.compat.sodium.SodiumPackedLightData;
import me.jellysquid.mods.sodium.client.model.light.data.LightDataAccess;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.model.light.smooth.SmoothLightPipeline", remap = false)
public abstract class SodiumSmoothLightPipelineMixin {

    @Shadow private LightDataAccess lightCache;

    @Unique
    private Object[] capturedFaceData;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(LightDataAccess cache, CallbackInfo ci) {
        try {
            // Find the field that holds AoFaceData[]
            // It is initialized to new AoFaceData[12]
            for (Field f : this.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                if (f.getType().isArray() && !f.getType().getComponentType().isPrimitive()) {
                    Object val = f.get(this);
                    if (val != null && java.lang.reflect.Array.getLength(val) == 12) {
                        // Check if component type name contains "AoFaceData"
                        if (f.getType().getComponentType().getName().contains("AoFaceData")) {
                            this.capturedFaceData = (Object[]) val;
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Unique
    private Object getCachedFaceData(BlockPos pos, Direction face, boolean offset) {
        if (this.capturedFaceData == null) {
            // Fallback or error
            return null; 
        }
        
        int index = offset ? face.ordinal() : face.ordinal() + 6;
        Object data = this.capturedFaceData[index];
        
        SodiumAoFaceDataExtension ext = (SodiumAoFaceDataExtension) data;
        if (!ext.hasLightData()) {
            ext.initLightData(this.lightCache, pos, face, offset);
        }
        
        return data;
    }

    /**
     * @author Erykczy
     * @reason Inject colored lighting logic
     */
    @Overwrite
    private void applyAlignedPartialFaceVertex(BlockPos pos, Direction dir, float[] w, int i, QuadLightData out, boolean offset) {
        Object faceDataObj = this.getCachedFaceData(pos, dir, offset);
        if (faceDataObj == null) return; // Should not happen if init succeeded
        
        SodiumAoFaceDataExtension faceData = (SodiumAoFaceDataExtension) faceDataObj;

        int lightMap = faceData.getBlendedLightMap(w);
        float ao = faceData.getBlendedShade(w);

        out.br[i] = ao;
        out.lm[i] = lightMap;
    }
    
    /**
     * @author Erykczy
     * @reason Inject colored lighting logic
     */
    @Overwrite
    private void applyInsetPartialFaceVertex(BlockPos pos, Direction dir, float n1d, float n2d, float[] w, int i, QuadLightData out) {
        Object n1Obj = this.getCachedFaceData(pos, dir, false);
        if (n1Obj == null) return;
        SodiumAoFaceDataExtension n1 = (SodiumAoFaceDataExtension) n1Obj;

        Object n2Obj = this.getCachedFaceData(pos, dir, true);
        if (n2Obj == null) return;
        SodiumAoFaceDataExtension n2 = (SodiumAoFaceDataExtension) n2Obj;

        float ao = (n1.getBlendedShade(w) * n1d) + (n2.getBlendedShade(w) * n2d);
        
        if (!ColoredLightEngine.getInstance().isEnabled()) {
            // Replicate vanilla/Sodium logic
            // We need to get the blended lightmap from n1 and n2, unpack them, interpolate, and repack
            int lm1 = n1.getBlendedLightMap(w);
            int lm2 = n2.getBlendedLightMap(w);
            
            int bl1 = lm1 & 0xFF;
            int sl1 = (lm1 >> 16) & 0xFF;
            
            int bl2 = lm2 & 0xFF;
            int sl2 = (lm2 >> 16) & 0xFF;
            
            int bl = (int)(bl1 * n1d + bl2 * n2d);
            int sl = (int)(sl1 * n1d + sl2 * n2d);
            
            out.br[i] = ao;
            out.lm[i] = bl | (sl << 16);
            return;
        }

        float r = (n1.getBlendedRed(w) * n1d) + (n2.getBlendedRed(w) * n2d);
        float g = (n1.getBlendedGreen(w) * n1d) + (n2.getBlendedGreen(w) * n2d);
        float b = (n1.getBlendedBlue(w) * n1d) + (n2.getBlendedBlue(w) * n2d);
        float s = (n1.getBlendedSky(w) * n1d) + (n2.getBlendedSky(w) * n2d);

        out.br[i] = ao;
        out.lm[i] = SodiumPackedLightData.packData((int)s, (int)r, (int)g, (int)b);
    }
}
