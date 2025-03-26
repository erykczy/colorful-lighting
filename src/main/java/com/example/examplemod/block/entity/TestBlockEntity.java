package com.example.examplemod.block.entity;

import com.example.examplemod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TestBlockEntity extends BlockEntity {

    public TestBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlocks.TEST_BLOCK_ENTITY_TYPE.get(), pos, blockState);
    }
}
