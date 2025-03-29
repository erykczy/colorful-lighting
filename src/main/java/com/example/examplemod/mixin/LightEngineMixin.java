package com.example.examplemod.mixin;

import com.example.examplemod.ColoredLightManager;
import com.example.examplemod.client.debug.ModKeyBinds;
import com.example.examplemod.util.Color3;
import com.example.examplemod.util.FastColor3;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightEngine.class)
public class LightEngineMixin {
    @Inject(at = @At("HEAD"), method = "runLightUpdates", cancellable = true)
    public void runLightUpdates(CallbackInfoReturnable<Integer> cir) {
        LightEngine engine = (LightEngine)(Object)this;
        if(!(engine instanceof BlockLightEngine blockEngine)) return;
        cir.setReturnValue(0);

        synchronized (LightEngineMixin.class) {
            //handleNewChunks(blockEngine);
            LongIterator longiterator = engine.blockNodesToCheck.iterator();

            while (longiterator.hasNext()) {
                blockEngine.checkNode(longiterator.nextLong());
            }

            blockEngine.blockNodesToCheck.clear();
            blockEngine.blockNodesToCheck.trim(512);
            int i = 0;
            i += blockEngine.propagateDecreases();
            if(ModKeyBinds.debug_test2)
                i += blockEngine.propagateIncreases();
            blockEngine.clearChunkCache();
            blockEngine.storage.markNewInconsistencies(blockEngine);
            blockEngine.storage.swapSectionMap();
            cir.setReturnValue(i);

            //ColoredLightManager.getInstance().handleNewChunks(blockEngine);
        }
    }

    @Inject(at = @At("TAIL"), method = "updateSectionStatus")
    public void updateSectionStatus(SectionPos pos, boolean isQueueEmpty, CallbackInfo ci) {
        LightEngine engine = (LightEngine)(Object)this;
        if(!(engine instanceof BlockLightEngine blockEngine)) return;

        ColoredLightManager.getInstance().handleSectionUpdate(blockEngine, pos, engine.storage.getDebugSectionType(pos.asLong()));
    }

    //@Inject(at = @At("TAIL"), method = "setLightEnabled")
    /*public void setLightEnabled(ChunkPos chunkPos, boolean lightEnabled, CallbackInfo ci) {
        LightEngine lightEngine = (LightEngine) (Object)this;
        if(!(lightEngine instanceof BlockLightEngine blockLightEngine)) return;
        if(!lightEnabled) return;

        //handleNewChunks(blockLightEngine);
    }*/

    @Inject(at = @At("HEAD"), method = "propagateIncreases", cancellable = true)
    private void propagateIncreases(CallbackInfoReturnable<Integer> cir) {
        LightEngine engine = (LightEngine)(Object)this;
        if(!(engine instanceof BlockLightEngine blockEngine)) return;

        int i;
        for (i = 0; !engine.increaseQueue.isEmpty() && !ColoredLightManager.getInstance().increaseQueue.isEmpty(); i++) { // added condition after &&
            long blockPos = engine.increaseQueue.dequeueLong();
            long queueEntry = engine.increaseQueue.dequeueLong();
            int lightLevel = engine.storage.getStoredLevel(blockPos);
            int queueEntryLightLevel = LightEngine.QueueEntry.getFromLevel(queueEntry);
            FastColor3 queueEntryLightColor = ColoredLightManager.getInstance().increaseQueue.poll(); // added

            // if increase requested directly by emmisive block and current light level is lower than requested
            // update light level at blockPos
            if (LightEngine.QueueEntry.isIncreaseFromEmission(queueEntry) && lightLevel < queueEntryLightLevel) {
                engine.storage.setStoredLevel(blockPos, queueEntryLightLevel);
                ColoredLightManager.getInstance().storage.setLightColor(BlockPos.getX(blockPos), BlockPos.getY(blockPos), BlockPos.getZ(blockPos), queueEntryLightColor); // added
                lightLevel = queueEntryLightLevel;
            }

            // TODO: why this condition?
            if (lightLevel == queueEntryLightLevel) {
                //blockEngine.propagateIncrease(blockPos, queueEntry, lightLevel);
                lightEngine$propagateIncrease(blockEngine, blockPos, queueEntry, queueEntryLightColor, lightLevel); // added
            }
        }

        cir.setReturnValue(i);
    }

    @Unique
    private static void lightEngine$propagateIncrease(BlockLightEngine engine, long thisBlockPos, long queueEntry, FastColor3 lightColor, int thisLightLevel) {
        BlockState thisBlockState = null;

        for (Direction direction : LightEngine.PROPAGATION_DIRECTIONS) {
            if (LightEngine.QueueEntry.shouldPropagateInDirection(queueEntry, direction)) {
                long neighbourPos = BlockPos.offset(thisBlockPos, direction);
                // if no light data for the section, continue
                if (!engine.storage.storingLightForSection(SectionPos.blockToSection(neighbourPos))) continue;
                if (!ColoredLightManager.getInstance().storage.containsLayer(SectionPos.blockToSection(neighbourPos))) continue; // added

                int neighbourLightLevel = engine.storage.getStoredLevel(neighbourPos);
                // if neighbour has more light than proposed (condition duplication?, see mc code)
                if (thisLightLevel - 1 <= neighbourLightLevel) continue;

                BlockState neighbourState = engine.getState(BlockPos.of(neighbourPos));
                int neighbourOpacity = engine.getOpacity(neighbourState, BlockPos.of(neighbourPos));
                int neighbourNewLightLevel = thisLightLevel - neighbourOpacity;
                // if neighbour has more or equal light than proposed
                if (neighbourNewLightLevel <= neighbourLightLevel) continue;

                // get this block state
                if (thisBlockState == null) {
                    thisBlockState = LightEngine.QueueEntry.isFromEmptyShape(queueEntry)
                            ? Blocks.AIR.defaultBlockState()
                            : engine.getState(BlockPos.of(thisBlockPos));
                }

                if (engine.shapeOccludes(thisBlockPos, thisBlockState, neighbourPos, neighbourState, direction)) continue;

                // set new light for neighbour
                FastColor3 neighbourNewLightColor = new FastColor3(new Color3(lightColor).mul(1.0f - neighbourOpacity/15.0f)); // added
                if(BlockPos.getY(neighbourPos) == 1 && BlockPos.getZ(neighbourPos) == 0) // TODO
                    System.out.println(BlockPos.getX(neighbourPos)+": "+Byte.toUnsignedInt(neighbourNewLightColor.red())+" | from: " + Byte.toUnsignedInt(lightColor.red()));
                ColoredLightManager.getInstance().storage.setLightColor(BlockPos.getX(neighbourPos), BlockPos.getY(neighbourPos), BlockPos.getZ(neighbourPos), neighbourNewLightColor); // added
                engine.storage.setStoredLevel(neighbourPos, neighbourNewLightLevel);

                // let neighbour flood his neighbours with light
                if (neighbourNewLightLevel > 1) {
                    engine.enqueueIncrease(neighbourPos, LightEngine.QueueEntry.increaseSkipOneDirection(neighbourNewLightLevel, engine.isEmptyShape(neighbourState), direction.getOpposite()));
                    ColoredLightManager.getInstance().enqueueIncrease(neighbourNewLightColor);
                }
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "propagateDecreases", cancellable = true)
    private void propagateDecreases(CallbackInfoReturnable<Integer> cir) {
        LightEngine engine = (LightEngine)(Object)this;
        if(!(engine instanceof BlockLightEngine blockEngine)) return;

        int i;
        for (i = 0; !engine.decreaseQueue.isEmpty(); i++) {
            long blockPos = engine.decreaseQueue.dequeueLong();
            long queueEntry = engine.decreaseQueue.dequeueLong();
            //engine.propagateDecrease(j, k);
            lightEngine$propagateDecrease(blockEngine, blockPos, queueEntry); // added
        }

        cir.setReturnValue(i);
    }

    @Unique
    protected void lightEngine$propagateDecrease(BlockLightEngine engine, long thisBlockPos, long thisLightLevel) {
        int queueEntryLightLevel = LightEngine.QueueEntry.getFromLevel(thisLightLevel);

        for (Direction direction : BlockLightEngine.PROPAGATION_DIRECTIONS) {
            if (!LightEngine.QueueEntry.shouldPropagateInDirection(thisLightLevel, direction)) continue;

            long neighbourPos = BlockPos.offset(thisBlockPos, direction);

            // if no light data for the section, continue
            if (!engine.storage.storingLightForSection(SectionPos.blockToSection(neighbourPos))) continue;
            if (!ColoredLightManager.getInstance().storage.containsLayer(SectionPos.blockToSection(neighbourPos))) continue; // added

            int neighbourLightLevel = engine.storage.getStoredLevel(neighbourPos);
            if (neighbourLightLevel == 0) continue; // can't decrease more

            // if neighbour has less or equal light than requested to remove (less than thisBlock had)
            if (neighbourLightLevel <= queueEntryLightLevel - 1) {
                BlockState neighbourState = engine.getState(BlockPos.of(neighbourPos));
                int neighbourEmission = engine.getEmission(neighbourPos, neighbourState);
                FastColor3 neighbourEmissionColor = ColoredLightManager.getInstance().getBlockStateColor(neighbourState); // added

                // set neighbour's light level to 0 | THEORY: light level will be restored by 2 enqueueIncrease function calls below
                engine.storage.setStoredLevel(neighbourPos, 0);
                ColoredLightManager.getInstance().storage.setLightColor(BlockPos.getX(neighbourPos), BlockPos.getY(neighbourPos), BlockPos.getZ(neighbourPos), new FastColor3()); // added

                // if neighbour emits less light than it has (had)
                if (neighbourEmission < neighbourLightLevel) {
                    engine.enqueueDecrease(neighbourPos, LightEngine.QueueEntry.decreaseSkipOneDirection(neighbourLightLevel, direction.getOpposite()));
                    //ColoredLightManager.getInstance().enqueueDecrease(neighbourLightColor); // added
                }

                if (neighbourEmission > 0) {
                    engine.enqueueIncrease(neighbourPos, LightEngine.QueueEntry.increaseLightFromEmission(neighbourEmission, engine.isEmptyShape(neighbourState)));
                    ColoredLightManager.getInstance().enqueueIncrease(neighbourEmissionColor); // added
                }
            } else {
                // if neighbour has more light than thisBlock had
                engine.enqueueIncrease(neighbourPos, LightEngine.QueueEntry.increaseOnlyOneDirection(neighbourLightLevel, false, direction.getOpposite()));
                FastColor3 neighbourLightColor = ColoredLightManager.getInstance().storage.getLightColor(BlockPos.getX(neighbourPos), BlockPos.getY(neighbourPos), BlockPos.getZ(neighbourPos)); // added
                ColoredLightManager.getInstance().enqueueIncrease(neighbourLightColor); // added
            }
        }
    }
}
