package com.example.examplemod.mixin;

import com.example.examplemod.ColoredLightManager;
import com.example.examplemod.util.FastColor3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockLightEngine.class)
public class BlockLightEngineMixin {
    @Inject(at = @At("HEAD"), method = "propagateLightSources", cancellable = true)
    public void propagateLightSources(ChunkPos chunkPos, CallbackInfo ci) {
        BlockLightEngine engine = (BlockLightEngine)(Object) this;
        ci.cancel();

        engine.setLightEnabled(chunkPos, true);
        LightChunk lightChunk = engine.chunkSource.getChunkForLighting(chunkPos.x, chunkPos.z);
        if (lightChunk != null) {
            lightChunk.findBlockLightSources((blockPos, blockState) -> {
                int blockEmission = blockState.getLightEmission(engine.chunkSource.getLevel(), blockPos);
                engine.enqueueIncrease(blockPos.asLong(), LightEngine.QueueEntry.increaseLightFromEmission(blockEmission, engine.isEmptyShape(blockState)));
                ColoredLightManager.getInstance().enqueueIncrease(ColoredLightManager.getInstance().getBlockStateColor(blockState)); // added
            });
        }
    }

    @Inject(at = @At("HEAD"), method = "checkNode", cancellable = true)
    protected void checkNode(long blockPos, CallbackInfo ci) {
        ci.cancel();
        BlockLightEngine engine = (BlockLightEngine)(Object)this;

        long sectionPos = SectionPos.blockToSection(blockPos);
        // if no light data for the section, return
        if (!engine.storage.storingLightForSection(sectionPos)) return;

        BlockState blockState = engine.getState(BlockPos.of(blockPos));
        int blockEmission = engine.getEmission(blockPos, blockState);
        FastColor3 blockEmissionColor = ColoredLightManager.getInstance().getBlockStateColor(blockState); // added
        int lightLevel = engine.storage.getStoredLevel(blockPos);

        // THEORY: checkNode function executes only when block pos changes its light properties
        // if the new block lets light through, then lightLevel (that hasn't been updated yet) is 0 and the condition is false
        if (blockEmission < lightLevel) {
            engine.storage.setStoredLevel(blockPos, 0);
            ColoredLightManager.getInstance().storage.setLightColor(BlockPos.getX(blockPos), BlockPos.getY(blockPos), BlockPos.getZ(blockPos), new FastColor3()); // added
            engine.enqueueDecrease(blockPos, LightEngine.QueueEntry.decreaseAllDirections(lightLevel));
        } else {
            // TODO: why? probably this decrease request doesn't take effect on neighbours
            engine.enqueueDecrease(blockPos, BlockLightEngine.PULL_LIGHT_IN_ENTRY);
        }

        // enqueue increase for emmisive block
        if (blockEmission > 0) {
            engine.enqueueIncrease(blockPos, LightEngine.QueueEntry.increaseLightFromEmission(blockEmission, engine.isEmptyShape(blockState)));
            ColoredLightManager.getInstance().enqueueIncrease(blockEmissionColor); // added
        }
    }
}
