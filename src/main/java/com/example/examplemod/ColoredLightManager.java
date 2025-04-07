package com.example.examplemod;

import com.example.examplemod.util.ColorRGB4;
import com.example.examplemod.util.ColorRGB8;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ColoredLightManager {
    public ColoredLightStorage storage = new ColoredLightStorage();
    // light increase propagation requests
    private Queue<LightUpdateRequest> propagateIncreases = new ConcurrentLinkedQueue<>();
    // light decrease propagation requests
    private Queue<LightUpdateRequest> propagateDecreases = new LinkedList<>();
    // sections that were modified by requests
    private LinkedList<Long> dirtySections = new LinkedList<>();
    // newly loaded chunks that wait for light propagation
    private Queue<ChunkAccess> newChunks = new ConcurrentLinkedQueue<>();
    private HashSet<ChunkAccess> fullyLoadedChunks = new HashSet<>();
    // thread that finds light sources in newly loaded chunks and adds propagation requests for those blocks (it is slow task so it is executed in other thread)
    private Thread handleNewChunksThread;

    private static ColoredLightManager instance = new ColoredLightManager();
    public static ColoredLightManager getInstance() {
        return instance;
    }

    public ColoredLightManager() {
        handleNewChunksThread = new Thread(new PropagateLightInNewChunks());
        handleNewChunksThread.start();
    }

    public ColorRGB8 sampleLightColor(BlockPos pos) { return sampleLightColor(pos.getX(), pos.getY(), pos.getZ()); }
    public ColorRGB8 sampleLightColor(int x, int y, int z) {
        var entry = storage.getEntry(x, y, z);
        if(entry == null) return ColorRGB8.fromRGB8(0, 0, 0);
        return ColorRGB8.fromRGB4(entry);
    }

    /**
     * Mixes light color from blocks neighbouring cornerPos. Used to smooth light color transitions.
     */
    public ColorRGB8 sampleMixedLightColor(Vector3f cornerPos) {
        Vector3i flooredCornerPos = new Vector3i((int)cornerPos.x, (int)cornerPos.y, (int)cornerPos.z);
        int coefficientsCount = 0;
        ColorRGB8 finalColor = ColorRGB8.fromRGB8(0, 0, 0);
        for(int ox = -1; ox <= 0; ++ox) {
            for(int oy = -1; oy <= 0; ++oy) {
                for(int oz = -1; oz <= 0; ++oz) {
                    ColorRGB8 coefficient = sampleLightColor(flooredCornerPos.x + ox, flooredCornerPos.y + oy, flooredCornerPos.z + oz);
                    if(coefficient.red == 0 && coefficient.green == 0 && coefficient.blue == 0) continue;
                    finalColor = finalColor.add(coefficient);
                    ++coefficientsCount;
                }
            }
        }
        return coefficientsCount == 0 ? ColorRGB8.fromRGB8(0, 0, 0) : finalColor.intDivide(coefficientsCount);
    }

    private void requestLightPropagation(BlockPos originPos, ColorRGB4 lightColor, boolean increase, boolean force) {
        if(increase) {
            propagateIncreases.add(new LightUpdateRequest(originPos, lightColor, force));
        }
        else {
            propagateDecreases.add(new LightUpdateRequest(originPos, lightColor, force));
        }
    }

    /**
     * Handles all increase propagation requests.
     */
    private void propagateIncreases(BlockGetter level) {
        while(!propagateIncreases.isEmpty()) {
            LightUpdateRequest request = propagateIncreases.poll();
            ColorRGB4 oldLightColor = storage.getEntry(request.blockPos);
            if(oldLightColor == null) continue;
            ColorRGB4 newLightColor = ColorRGB4.fromRGB4(
                Math.max(oldLightColor.red4, request.lightColor.red4),
                Math.max(oldLightColor.green4, request.lightColor.green4),
                Math.max(oldLightColor.blue4, request.lightColor.blue4)
            );

            // if light color didn't change (check is ignored if request is forced)
            if(!request.force && newLightColor.red4 == oldLightColor.red4 && newLightColor.green4 == oldLightColor.green4 && newLightColor.blue4 == oldLightColor.blue4) continue;
            setLightColor(request.blockPos, newLightColor);

            // light attenuation
            ColorRGB4 neighbourLightColor = ColorRGB4.fromRGB4(
                Math.max(0, request.lightColor.red4 - 1),
                Math.max(0, request.lightColor.green4 - 1),
                Math.max(0, request.lightColor.blue4 - 1)
            );
            // if no more color to propagate
            if(neighbourLightColor.red4 == 0 && neighbourLightColor.green4 == 0 && neighbourLightColor.blue4 == 0) continue;

            for(var direction : Direction.values()) {
                BlockPos neighbourPos = request.blockPos.relative(direction);
                if(level.isOutsideBuildHeight(neighbourPos)) continue;

                BlockState neighbourState = level.getBlockState(neighbourPos);
                // if block blocks light
                if(neighbourState.getLightBlock(level, neighbourPos) > 0) continue;
                requestLightPropagation(neighbourPos, neighbourLightColor, true, false);
            }
        }
    }

    /**
     * Handles all decrease propagation requests.
     */
    private void propagateDecreases(BlockGetter level) {
        while(!propagateDecreases.isEmpty()) {
            LightUpdateRequest request = propagateDecreases.poll();
            ColorRGB4 oldLightColor = storage.getEntry(request.blockPos);
            if(oldLightColor == null) continue;

            // if light color didn't change (check is ignored if request is forced)
            if(!request.force && oldLightColor.red4 == 0 && oldLightColor.green4 == 0 && oldLightColor.blue4 == 0) continue;
            setLightColor(request.blockPos, ColorRGB4.fromRGB4(0, 0, 0));

            BlockState blockState = level.getBlockState(request.blockPos);
            // repropagate removed light
            if(blockState.getLightEmission(level, request.blockPos) > 0) {
                requestLightPropagation(request.blockPos, Config.getEmissionColor(level, request.blockPos), true, false);
            }

            // decrease attenuation
            ColorRGB4 neighbourLightDecrease = ColorRGB4.fromRGB4(
                    Math.max(0, request.lightColor.red4 - 1),
                    Math.max(0, request.lightColor.green4 - 1),
                    Math.max(0, request.lightColor.blue4 - 1)
            );
            // whether neighbours' light should be decreased or increased (to repropagate)
            boolean decreaseMore = neighbourLightDecrease.red4 != 0 || neighbourLightDecrease.green4 != 0 || neighbourLightDecrease.blue4 != 0;

            for(var direction : Direction.values()) {
                BlockPos neighbourPos = request.blockPos.relative(direction);
                if(level.isOutsideBuildHeight(neighbourPos)) continue;

                if(decreaseMore) {
                    // propagate decrease
                    requestLightPropagation(neighbourPos, neighbourLightDecrease, false, false);
                }
                else {
                    ColorRGB4 neighbourLightColor = storage.getEntry(neighbourPos);
                    if(neighbourLightColor == null) continue;
                    // if neighbour doesn't already have any light
                    if(neighbourLightColor.red4 == 0 && neighbourLightColor.green4 == 0 && neighbourLightColor.blue4 == 0)
                        continue;

                    // force neighbour to propagate light to the region that has been cleared
                    requestLightPropagation(neighbourPos, neighbourLightColor, true, true);
                }
            }
        }
    }

    public void requestLightPullIn(BlockPos blockPos) {
        for(var direction : Direction.values()) {
            BlockPos neighbourPos = blockPos.relative(direction);
            ColorRGB4 neighbourLight = storage.getEntry(neighbourPos);
            if(neighbourLight == null) continue;

            // if neighbour doesn't have any light
            if(neighbourLight.red4 == 0 && neighbourLight.green4 == 0 && neighbourLight.blue4 == 0) continue;
            requestLightPropagation(neighbourPos, neighbourLight, true, true);
        }
    }

    public void onBlockLightPropertiesChanged(BlockGetter level, BlockPos blockPos) {
        BlockState blockState = level.getBlockState(blockPos);
        int lightEmission = blockState.getLightEmission(level, blockPos);
        ColorRGB4 lightColor = storage.getEntry(blockPos);
        if(lightColor == null) return;

        // TODO
        if(lightColor.red4 == 0 && lightColor.green4 == 0 && lightColor.blue4 == 0)
            requestLightPullIn(blockPos);
        else
            requestLightPropagation(blockPos, lightColor, false, false);

        // propagate light if new blockState emits light
        if(lightEmission > 0)
            requestLightPropagation(blockPos, Config.getEmissionColor(level, blockPos), true, false);
    }

    public void runLightUpdates(BlockGetter level) {
        // handle increase and decrease requests
        propagateDecreases(level);
        propagateIncreases(level);

        // set dirty all modified sections
        var iterator = dirtySections.iterator();
        while (iterator.hasNext()) {
            SectionPos sectionPos = SectionPos.of(iterator.next());
            Minecraft.getInstance().levelRenderer.setSectionDirty(
                    sectionPos.x(),
                    sectionPos.y(),
                    sectionPos.z()
            );
            iterator.remove();
        }
    }

    private void setLightColor(BlockPos blockPos, ColorRGB4 color) {
        storage.setEntry(blockPos.getX(), blockPos.getY(), blockPos.getZ(), color);
        dirtySections.add(SectionPos.asLong(blockPos));
    }

    public void onChunkLoad(ChunkSource chunkSource, ChunkAccess chunk) {
        // add sections to storage
        for(int i = 0; i < chunk.getSectionsCount(); i++) {
            int y = chunk.getSectionYFromSectionIndex(i);
            storage.addSection(SectionPos.asLong(chunk.getPos().x, y, chunk.getPos().z));
        }

        // add fully loaded chunks (that have all neighbours loaded) to newChunks collection
        // for each neighbour
        for(int x = -1; x <= 1; ++x) {
            for (int z = -1; z <= 1; ++z) {
                // check if neighbour's neighbours are loaded
                boolean allNeighboursLoaded = true;
                for(int neighbourX = -1; neighbourX <= 1; ++neighbourX) {
                    for (int neighbourZ = -1; neighbourZ <= 1; ++neighbourZ) {
                        // if neighbour's neighbour is not loaded
                        if(
                            !chunkSource.hasChunk(chunk.getPos().x + x + neighbourX, chunk.getPos().z + z + neighbourZ) ||
                            !storage.containsSection(SectionPos.asLong(chunk.getPos().x + x + neighbourX, chunk.getMinSection(), chunk.getPos().z + z + neighbourZ))
                        ) {
                            allNeighboursLoaded = false;
                            break;
                        }
                    }
                    if(!allNeighboursLoaded) break;
                }
                if(allNeighboursLoaded) {
                    ChunkAccess chunk1 = chunkSource.getChunk(chunk.getPos().x + x, chunk.getPos().z + z, false);
                    newChunks.add(chunk1);
                    fullyLoadedChunks.add(chunk1);
                }
            }
        }
    }

    public void onChunkUnload(ChunkAccess chunk) {
        newChunks.remove(chunk);
        for(int i = 0; i < chunk.getSectionsCount(); i++) {
            int y = chunk.getSectionYFromSectionIndex(i);
            storage.removeSection(SectionPos.asLong(chunk.getPos().x, y, chunk.getPos().z));
        }
        fullyLoadedChunks.remove(chunk);
    }

    public void onLevelUnload() {
        newChunks.clear();
        fullyLoadedChunks.clear();
        storage.clear();
    }

    public void refreshLevel() {
        for(var chunk : fullyLoadedChunks) {
            for(int i = 0; i < chunk.getSectionsCount(); i++) {
                int y = chunk.getSectionYFromSectionIndex(i);
                storage.getSection(SectionPos.asLong(chunk.getPos().x, y, chunk.getPos().z)).clear();
            }
        }
        newChunks.addAll(fullyLoadedChunks);
    }

    private static class LightUpdateRequest {
        BlockPos blockPos;
        ColorRGB4 lightColor;
        boolean force;

        public LightUpdateRequest(BlockPos blockPos, ColorRGB4 lightColor, boolean force) {
            this.blockPos = blockPos;
            this.lightColor = lightColor;
            this.force = force;
        }
    }

    private class PropagateLightInNewChunks implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(20);
                }
                catch (Exception e) {
                    System.err.println(e.getMessage());
                }
                doTask();
            }
        }

        private void doTask() {
            while (!newChunks.isEmpty()) {
                if(!propagateIncreases.isEmpty()) break;
                ChunkAccess chunk = newChunks.poll();

                chunk.findBlockLightSources((blockPos, blockState) -> {
                    requestLightPropagation(new BlockPos(blockPos), Config.getEmissionColor(chunk, blockPos), true, false);
                });
            }
        }
    }
}
