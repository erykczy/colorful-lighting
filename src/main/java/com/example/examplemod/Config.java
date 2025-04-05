package com.example.examplemod;

import com.example.examplemod.util.Color3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;

public class Config {
    public static HashMap<Block, Color3> emissionColors = new HashMap<>();

    static {
        emissionColors.put(Blocks.BEACON, new Color3(0.1f, 0.1f, 1.0f));
        emissionColors.put(Blocks.FIRE, new Color3(0.9f, 0.1f, 0.1f));
        emissionColors.put(Blocks.LAVA, new Color3(0.9f, 0.1f, 0.1f));
        emissionColors.put(Blocks.GLOWSTONE, new Color3(1.0f, 0.5f, 0.1f)); //0.6f 0.3f 0.1f
        emissionColors.put(Blocks.MAGMA_BLOCK, new Color3(0.9f, 0.1f, 0.1f));
        emissionColors.put(Blocks.LAVA_CAULDRON, new Color3(0.9f, 0.1f, 0.1f));
        emissionColors.put(Blocks.SHROOMLIGHT, new Color3(0.9f, 0.1f, 0.1f));
        emissionColors.put(Blocks.REDSTONE_LAMP, new Color3(0.9f, 0.8f, 0.8f));
        emissionColors.put(Blocks.SEA_LANTERN, new Color3(0.0f, 0.4f, 1.0f));
        emissionColors.put(Blocks.CAVE_VINES, new Color3(0.0f, 1.0f, 0.0f));
        emissionColors.put(Blocks.NETHER_PORTAL, new Color3(1.0f, 0.0f, 1.0f));
        emissionColors.put(Blocks.RESPAWN_ANCHOR, new Color3(1.0f, 0.0f, 1.0f));
        emissionColors.put(Blocks.ENCHANTING_TABLE, new Color3(1.0f, 0.0f, 1.0f));
        emissionColors.put(Blocks.AMETHYST_CLUSTER, new Color3(1.0f, 0.0f, 1.0f));
        emissionColors.put(Blocks.LARGE_AMETHYST_BUD, new Color3(1.0f, 0.0f, 1.0f));
        emissionColors.put(Blocks.CRYING_OBSIDIAN, new Color3(1.0f, 0.0f, 1.0f));
        emissionColors.put(Blocks.SOUL_CAMPFIRE, new Color3(0.2f, 0.3f, 1.0f));
        emissionColors.put(Blocks.SOUL_FIRE, new Color3(0.2f, 0.3f, 1.0f));
        emissionColors.put(Blocks.SOUL_LANTERN, new Color3(0.2f, 0.3f, 1.0f));
        emissionColors.put(Blocks.SOUL_TORCH, new Color3(0.2f, 0.3f, 1.0f));
        emissionColors.put(Blocks.SOUL_WALL_TORCH, new Color3(0.2f, 0.3f, 1.0f));
        emissionColors.put(Blocks.REDSTONE_TORCH, new Color3(1.0f, 0.0f, 0.0f));
        emissionColors.put(Blocks.REDSTONE_WALL_TORCH, new Color3(1.0f, 0.0f, 0.0f));
        emissionColors.put(Blocks.OCHRE_FROGLIGHT, new Color3(1.0f, 1.0f, 0.0f));
        emissionColors.put(Blocks.VERDANT_FROGLIGHT, new Color3(0.0f, 1.0f, 0.0f));
        emissionColors.put(Blocks.PEARLESCENT_FROGLIGHT, new Color3(1.0f, 0.0f, 1.0f));
        emissionColors.put(Blocks.LIME_CANDLE, new Color3(0.0f, 1.0f, 0.0f));
    }

    public static Color3 getEmissionColor(BlockGetter level, BlockPos pos) {
        BlockState state;
        if(level == null)
            state = Blocks.BEDROCK.defaultBlockState();
        else
            state = level.getBlockState(pos);

        if(emissionColors.containsKey(state.getBlock())) {
            return emissionColors.get(state.getBlock());
        }
        else
            return new Color3();
    }
}
