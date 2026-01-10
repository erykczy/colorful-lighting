package me.erykczy.colorfullighting.common.accessors;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

public interface BlockStateAccessor {
    ResourceKey<Block> getBlockKey();
    int getLightEmission();
    int getLightBlock();
    int getLightEmission(LevelAccessor level, BlockPos pos);
    int getLightBlock(LevelAccessor level, BlockPos pos);
    boolean isAir();
}
