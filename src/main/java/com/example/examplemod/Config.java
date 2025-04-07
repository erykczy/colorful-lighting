package com.example.examplemod;

import com.example.examplemod.util.ColorRGB4;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;

public class Config {
    public static final ColorRGB4 defaultColor = ColorRGB4.fromRGB4(15, 15, 15);
    private static HashMap<ResourceLocation, ColorRGB4> emissionColors = new HashMap<>();

    /*static {
        emissionColors.put(Blocks.BEACON, ColorRGB4.fromRGBFloat(0.1f, 0.1f, 1.0f));
        emissionColors.put(Blocks.FIRE, ColorRGB4.fromRGBFloat(0.9f, 0.1f, 0.1f));
        emissionColors.put(Blocks.LAVA, ColorRGB4.fromRGBFloat(0.9f, 0.1f, 0.1f));
        emissionColors.put(Blocks.GLOWSTONE, ColorRGB4.fromRGBFloat(1.0f, 0.5f, 0.0f)); //0.6f 0.3f 0.1f
        emissionColors.put(Blocks.MAGMA_BLOCK, ColorRGB4.fromRGBFloat(0.9f, 0.1f, 0.1f));
        emissionColors.put(Blocks.LAVA_CAULDRON, ColorRGB4.fromRGBFloat(0.9f, 0.1f, 0.1f));
        emissionColors.put(Blocks.SHROOMLIGHT, ColorRGB4.fromRGBFloat(0.9f, 0.1f, 0.1f));
        emissionColors.put(Blocks.REDSTONE_LAMP, ColorRGB4.fromRGBFloat(0.9f, 0.8f, 0.8f));
        emissionColors.put(Blocks.SEA_LANTERN, ColorRGB4.fromRGBFloat(0.0f, 0.4f, 1.0f));
        emissionColors.put(Blocks.CAVE_VINES, ColorRGB4.fromRGBFloat(0.0f, 1.0f, 0.0f));
        emissionColors.put(Blocks.NETHER_PORTAL, ColorRGB4.fromRGBFloat(1.0f, 0.0f, 1.0f));
        emissionColors.put(Blocks.RESPAWN_ANCHOR, ColorRGB4.fromRGBFloat(1.0f, 0.0f, 1.0f));
        emissionColors.put(Blocks.ENCHANTING_TABLE, ColorRGB4.fromRGBFloat(1.0f, 0.0f, 1.0f));
        emissionColors.put(Blocks.AMETHYST_CLUSTER, ColorRGB4.fromRGBFloat(1.0f, 0.0f, 1.0f));
        emissionColors.put(Blocks.LARGE_AMETHYST_BUD, ColorRGB4.fromRGBFloat(1.0f, 0.0f, 1.0f));
        emissionColors.put(Blocks.CRYING_OBSIDIAN, ColorRGB4.fromRGBFloat(1.0f, 0.0f, 1.0f));
        emissionColors.put(Blocks.SOUL_CAMPFIRE, ColorRGB4.fromRGBFloat(0.2f, 0.3f, 1.0f));
        emissionColors.put(Blocks.SOUL_FIRE, ColorRGB4.fromRGBFloat(0.2f, 0.3f, 1.0f));
        emissionColors.put(Blocks.SOUL_LANTERN, ColorRGB4.fromRGBFloat(0.2f, 0.3f, 1.0f));
        emissionColors.put(Blocks.SOUL_TORCH, ColorRGB4.fromRGBFloat(0.2f, 0.3f, 1.0f));
        emissionColors.put(Blocks.SOUL_WALL_TORCH, ColorRGB4.fromRGBFloat(0.2f, 0.3f, 1.0f));
        emissionColors.put(Blocks.REDSTONE_TORCH, ColorRGB4.fromRGBFloat(1.0f, 0.0f, 0.0f));
        emissionColors.put(Blocks.REDSTONE_WALL_TORCH, ColorRGB4.fromRGBFloat(1.0f, 0.0f, 0.0f));
        emissionColors.put(Blocks.OCHRE_FROGLIGHT, ColorRGB4.fromRGBFloat(1.0f, 1.0f, 0.0f));
        emissionColors.put(Blocks.VERDANT_FROGLIGHT, ColorRGB4.fromRGBFloat(0.0f, 1.0f, 0.0f));
        emissionColors.put(Blocks.PEARLESCENT_FROGLIGHT, ColorRGB4.fromRGBFloat(1.0f, 0.0f, 1.0f));
        emissionColors.put(Blocks.LIME_CANDLE, ColorRGB4.fromRGBFloat(0.0f, 1.0f, 0.0f));
        emissionColors.put(Blocks.GLOW_LICHEN, ColorRGB4.fromRGBFloat(0.53f, 0.53f, 0.53f));
    }*/

    public static void setEmissionColors(HashMap<ResourceLocation, ColorRGB4> colors) {
        emissionColors = colors;
        ColoredLightManager.getInstance().refreshLevel();
    }

    public static ColorRGB4 getEmissionColor(BlockGetter level, BlockPos pos) {
        BlockState state;
        if(level == null)
            state = Blocks.BEDROCK.defaultBlockState();
        else
            state = level.getBlockState(pos);

        ResourceKey<Block> blockResourceKey = state.getBlockHolder().getKey();

        if(blockResourceKey != null && emissionColors.containsKey(blockResourceKey.location())) {
            return emissionColors.get(blockResourceKey.location());
        }
        else
            return defaultColor;
    }
}
