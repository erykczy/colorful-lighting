package com.example.examplemod.accessors;

import com.example.examplemod.common.accessors.BlockStateAccessor;
import com.example.examplemod.common.accessors.LevelAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class BlockStateWrapper implements BlockStateAccessor {
    final BlockState blockState;

    public BlockStateWrapper(@NotNull BlockState blockState) {
        this.blockState = blockState;
    }

    @Override
    public ResourceKey<Block> getBlockKey() {
        return blockState.getBlockHolder().getKey();
    }

    @Override
    public int getLightEmission() {
        return blockState.getLightEmission(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    @Override
    public int getLightBlock() {
        return blockState.getLightBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    @Override
    public int getLightEmission(LevelAccessor level, BlockPos pos) {
        if(level instanceof LevelWrapper levelWrapper)
            return blockState.getLightEmission(levelWrapper.getWrappedLevel(), pos);
        return getLightEmission();
    }

    @Override
    public int getLightBlock(LevelAccessor level, BlockPos pos) {
        if(level instanceof LevelWrapper levelWrapper)
            return blockState.getLightBlock(levelWrapper.getWrappedLevel(), pos);
        return getLightBlock();
    }
}
