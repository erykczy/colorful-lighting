package me.erykczy.colorfullighting.accessors;

import me.erykczy.colorfullighting.common.Config;
import me.erykczy.colorfullighting.common.accessors.BlockStateAccessor;
import me.erykczy.colorfullighting.common.accessors.LevelAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
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
        return level.getMinSectionY();
    }

    @Override
    public int getMaxSectionY() {
        return level.getMaxSectionY();
    }

    @Override
    public boolean hasChunk(ChunkPos chunkPos) {
        return level.getChunkSource().hasChunk(chunkPos.x, chunkPos.z);
    }

    @Override
    public boolean hasChunkAndNeighbours(ChunkPos chunkPos) {
        for(int ox = -1; ox <= 1; ++ox) {
            for(int oz = -1; oz <= 1; ++oz) {
                if(!hasChunk(new ChunkPos(chunkPos.x+ox, chunkPos.z+oz))) {
                    return false;
                }
            }
        }
        return true;
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
                        Config.getEmissionBrightness(this, blockPos, new BlockStateWrapper(blockState)) != 0,
                (blockPos, blockState) -> // for each found light source
                        consumer.accept(new BlockPos(blockPos))
        );
    }

    @Override
    public BlockStateAccessor getBlockState(BlockPos pos) {
        var chunk = level.getChunkSource().getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()), ChunkStatus.FULL, false);
        if(chunk == null) {
            return null;
        }
        var section = chunk.getSection(chunk.getSectionIndex(pos.getY()));
        return new BlockStateWrapper(section.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15)); //level.getBlockState(pos)
    }

    @Override
    public boolean isInBounds(BlockPos pos) {
        return !level.isOutsideBuildHeight(pos);
    }

    @Override
    public void setSectionDirty(int x, int y, int z) {
        levelRenderer.setSectionDirty(x, y, z);
    }
}
