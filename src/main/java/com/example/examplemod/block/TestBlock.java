package com.example.examplemod.block;

import com.example.examplemod.block.entity.TestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

public class TestBlock extends TransparentBlock implements EntityBlock {

    public TestBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(2.8F).friction(0.989F).sound(SoundType.GLASS).noOcclusion());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TestBlockEntity(pos, state);
    }
}
