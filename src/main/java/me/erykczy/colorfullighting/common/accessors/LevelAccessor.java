package me.erykczy.colorfullighting.common.accessors;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.function.Consumer;

public interface LevelAccessor {
    int getSectionsCount();
    int getMinSectionY();
    void findLightSources(ChunkPos chunkPos, Consumer<BlockPos> consumer);
    BlockStateAccessor getBlockState(BlockPos pos);
    boolean isInBounds(BlockPos pos);
    void setSectionDirtyWithNeighbours(int x, int y, int z);
}
