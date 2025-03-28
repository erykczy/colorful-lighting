package com.example.examplemod.mixin;

import com.example.examplemod.ColoredLightManager;
import net.minecraft.Util;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(ThreadedLevelLightEngine.class)
public class ThreadedLevelLightEngineMixin {
    @Inject(at = @At("TAIL"), method = "initializeLight")
    public void initializeLight(ChunkAccess chunk, boolean lightEnabled, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> ci) {
        ThreadedLevelLightEngine engine = (ThreadedLevelLightEngine) (Object)this;

        ChunkPos chunkPos = chunk.getPos();
        engine.addTask(chunkPos.x, chunkPos.z, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, Util.name(() -> {
            for (int i = 0; i < chunk.getSectionsCount(); i++) {
                int y = engine.levelHeightAccessor.getSectionYFromSectionIndex(i);
                ColoredLightManager.getInstance().storage.initializeSection(SectionPos.of(chunkPos, y).asLong(), engine);
            }
        }, () -> "initializeColoredLight: " + chunkPos));
    }

    @Inject(at = @At("TAIL"), method = "lightChunk")
    public void propagateLightSources(ChunkAccess chunk, boolean isLighted, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> ci) {
        ChunkPos chunkPos = chunk.getPos();
        ThreadedLevelLightEngine engine = (ThreadedLevelLightEngine) (Object)this;
        BlockLightEngine blockEngine = (BlockLightEngine) engine.blockEngine;
        assert blockEngine != null;

        engine.addTask(
                chunkPos.x,
                chunkPos.z,
                ThreadedLevelLightEngine.TaskType.PRE_UPDATE,
                Util.name(() -> {

                    chunk.findBlockLightSources(((blockPos, blockState) -> {
                        //blockEngine.enqueueIncrease(blockPos.asLong(), LightEngine.QueueEntry.increaseLightFromEmission(blockState.getLightEmission(chunk, blockPos), LightEngine.isEmptyShape(blockState)));
                        //ColoredLightManager.getInstance().enqueueIncrease(ColoredLightManager.getInstance().getBlockStateColor(blockState));
                        if(blockState.is(Blocks.GLOWSTONE)) {
                            // colored light recalculation on chunk load:

                            // remove light
                            blockEngine.enqueueDecrease(blockPos.asLong(), LightEngine.QueueEntry.decreaseAllDirections(blockState.getLightEmission(chunk, blockPos)));
                            blockEngine.storage.setStoredLevel(blockPos.asLong(), 0);
                            // revert light
                            blockEngine.checkBlock(blockPos);
                        }
                    }));

                }, () -> "propagateColoredLight " + chunkPos)
        );
    }
}
