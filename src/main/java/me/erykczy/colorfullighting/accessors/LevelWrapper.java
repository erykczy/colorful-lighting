package me.erykczy.colorfullighting.accessors;

import me.erykczy.colorfullighting.common.Config;
import me.erykczy.colorfullighting.common.accessors.BlockStateAccessor;
import me.erykczy.colorfullighting.common.accessors.LevelAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class LevelWrapper implements LevelAccessor {
    private final ClientLevel level;
    private final LevelRenderer levelRenderer;

    public LevelWrapper(@NotNull ClientLevel level, @NotNull LevelRenderer levelRenderer) {
        this.level = level;
        this.levelRenderer = levelRenderer;
    }

    public ClientLevel getWrappedLevel() {
        return level;
    }

    @Override
    public int getSectionsCount() {
        return level.getSectionsCount();
    }

    @Override
    public int getMinSectionY() {
        return level.getMinSection();
    }

    @Override
    public int getMaxSectionY() {
        return level.getMaxSection()-1;
    }

    @Override
    public boolean hasChunk(ChunkPos chunkPos) {
        return level.getChunkSource().hasChunk(chunkPos.x, chunkPos.z);
    }

    @Override
    public void findLightSources(ChunkPos chunkPos, Consumer<BlockPos> consumer) {
        ChunkAccess chunk = level.getChunk(chunkPos.x, chunkPos.z);
        chunk.findBlocks(
                blockState -> // block state filter
                        blockState.hasDynamicLightEmission() ||
                        Config.getEmissionBrightness(new BlockStateWrapper(blockState)) != 0,
                (blockState, blockPos) -> // individual block filter
                        blockState.getLightEmission(chunk, blockPos) != 0 ||
                        Config.getEmissionBrightness(this, blockPos) != 0,
                (blockPos, blockState) -> // for each found light source
                        consumer.accept(new BlockPos(blockPos))
        );
    }

    @Override
    public BlockStateAccessor getBlockState(BlockPos pos) {
        return new BlockStateWrapper(level.getBlockState(pos));
    }

    @Override
    public boolean isInBounds(BlockPos pos) {
        return !level.isOutsideBuildHeight(pos);
    }

    @Override
    public void setSectionDirtyWithNeighbours(int x, int y, int z) {
        levelRenderer.setSectionDirtyWithNeighbors(x, y, z);
    }
}
