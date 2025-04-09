package com.example.examplemod;

import com.example.examplemod.util.ColorRGB4;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.HashMap;

public class Config {
    public static final ColorRGB4 defaultColor = ColorRGB4.fromRGB4(15, 15, 15);
    private static HashMap<ResourceLocation, LightColor> emissionColors = new HashMap<>();

    public static void setEmissionColors(HashMap<ResourceLocation, LightColor> colors) {
        emissionColors = colors;
        ColoredLightManager.getInstance().refreshLevel();
    }

    public static ColorRGB4 getEmissionColor(BlockGetter level, BlockPos pos) { return getEmissionColor(level, pos, null); }
    public static ColorRGB4 getEmissionColor(BlockGetter level, BlockPos pos, @Nullable BlockState blockState) {
        if(level == null) return ColorRGB4.fromRGB4(0, 0, 0);
        if(blockState == null)
            blockState = level.getBlockState(pos);
        float lightEmission = blockState.getLightEmission(level, pos)/15.0f;

        ResourceKey<Block> blockResourceKey = blockState.getBlockHolder().getKey();

        if(blockResourceKey != null) {
            LightColor config = emissionColors.get(blockResourceKey.location());
            if(config != null)
                return config.color().mul(config.brightness4 < 0 ? lightEmission : config.brightness4/15.0f);
        }
        return defaultColor.mul(lightEmission);
    }

    public static int getEmissionBrightness(BlockGetter level, BlockPos pos) { return getEmissionBrightness(level, pos, null); }
    public static int getEmissionBrightness(BlockGetter level, BlockPos pos, @Nullable BlockState blockState) {
        if(level == null) return 0;
        if(blockState == null)
            blockState = level.getBlockState(pos);

        ResourceKey<Block> blockResourceKey = blockState.getBlockHolder().getKey();

        if(blockResourceKey != null) {
            LightColor config = emissionColors.get(blockResourceKey.location());
            if(config != null && config.brightness4 >= 0)
                return config.brightness4;
        }
        return blockState.getLightEmission(level, pos);
    }

    /**
     * @param color - light color
     * @param brightness4 - 4 bit value in range 0..15, by which light color is multiplied, if -1, vanilla emission for given block is used
     */
    public record LightColor(ColorRGB4 color, int brightness4) {}
}
