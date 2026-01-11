package me.erykczy.colorfullighting.compat.sodium;

import me.jellysquid.mods.sodium.client.model.light.data.LightDataAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface SodiumAoFaceDataExtension {
    int getBlendedLightMap(float[] w);
    float getBlendedShade(float[] w);
    
    float getBlendedRed(float[] w);
    float getBlendedGreen(float[] w);
    float getBlendedBlue(float[] w);
    float getBlendedSky(float[] w);
    
    void ensureUnpacked();
    
    boolean hasLightData();
    void initLightData(LightDataAccess cache, BlockPos pos, Direction direction, boolean offset);
}
