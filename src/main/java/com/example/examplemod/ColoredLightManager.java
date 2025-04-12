package com.example.examplemod;

import com.example.examplemod.util.ColorRGB4;
import com.example.examplemod.util.ColorRGB8;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.phys.Vec3;
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
     * Mixes light color from blocks neighbouring given position using arithmetic average.
     */
    public ColorRGB8 sampleSimpleInterpolationLightColor(Vec3 pos) {
        Vector3i centerPos = new Vector3i((int)Math.round(pos.x), (int)Math.round(pos.y), (int)Math.round(pos.z));
        Vector3i cornerPos = new Vector3i(centerPos.x - 1, centerPos.y - 1, centerPos.z - 1);
        int coefficientsCount = 0;
        ColorRGB8 finalColor = ColorRGB8.fromRGB8(0, 0, 0);
        for(int ox = 0; ox <= 1; ++ox) {
            for(int oy = 0; oy <= 1; ++oy) {
                for(int oz = 0; oz <= 1; ++oz) {
                    ColorRGB8 coefficient = sampleLightColor(cornerPos.x + ox, cornerPos.y + oy, cornerPos.z + oz);
                    if(coefficient.red == 0 && coefficient.green == 0 && coefficient.blue == 0) continue;
                    finalColor = finalColor.add(coefficient);
                    ++coefficientsCount;
                }
            }
        }
        return coefficientsCount == 0 ? ColorRGB8.fromRGB8(0, 0, 0) : finalColor.intDivide(coefficientsCount);
    }

    /**
     * Mixes light color from blocks neighbouring given position using trilinear interpolation.
     */
    public ColorRGB8 sampleTrilinearLightColor(Vec3 pos) {
        int cornerX = (int)Math.round(pos.x) - 1;
        int cornerY = (int)Math.round(pos.y) - 1;
        int cornerZ = (int)Math.round(pos.z) - 1;
        ColorRGB8 c000 = sampleLightColor(cornerX + 0, cornerY + 0, cornerZ + 0);
        ColorRGB8 c100 = sampleLightColor(cornerX + 1, cornerY + 0, cornerZ + 0);
        ColorRGB8 c101 = sampleLightColor(cornerX + 1, cornerY + 0, cornerZ + 1);
        ColorRGB8 c001 = sampleLightColor(cornerX + 0, cornerY + 0, cornerZ + 1);
        ColorRGB8 c010 = sampleLightColor(cornerX + 0, cornerY + 1, cornerZ + 0);
        ColorRGB8 c110 = sampleLightColor(cornerX + 1, cornerY + 1, cornerZ + 0);
        ColorRGB8 c111 = sampleLightColor(cornerX + 1, cornerY + 1, cornerZ + 1);
        ColorRGB8 c011 = sampleLightColor(cornerX + 0, cornerY + 1, cornerZ + 1);

        double x = (pos.x - cornerX) / 2.0;
        double y = (pos.y - cornerY) / 2.0;
        double z = (pos.z - cornerZ) / 2.0;

        ColorRGB8 c00 = c000.mul(1.0 - x).add(c100.mul(x));
        ColorRGB8 c01 = c001.mul(1.0 - x).add(c101.mul(x));
        ColorRGB8 c11 = c011.mul(1.0 - x).add(c111.mul(x));
        ColorRGB8 c10 = c010.mul(1.0 - x).add(c110.mul(x));

        ColorRGB8 c0 = c00.mul(1.0 - y).add(c10.mul(y));
        ColorRGB8 c1 = c01.mul(1.0 - y).add(c11.mul(y));

        return c0.mul(1.0 - z).add(c1.mul(z));
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

            for(var direction : Direction.values()) {
                BlockPos neighbourPos = request.blockPos.relative(direction);
                if(level.isOutsideBuildHeight(neighbourPos)) continue;
                BlockState neighbourState = level.getBlockState(neighbourPos);

                // light attenuation
                int lightBlock = Math.max(1, neighbourState.getLightBlock(level, neighbourPos));
                ColorRGB4 neighbourLightColor = ColorRGB4.fromRGB4(
                        Math.max(0, request.lightColor.red4 - lightBlock),
                        Math.max(0, request.lightColor.green4 - lightBlock),
                        Math.max(0, request.lightColor.blue4 - lightBlock)
                );
                // if no more color to propagate
                if(neighbourLightColor.red4 == 0 && neighbourLightColor.green4 == 0 && neighbourLightColor.blue4 == 0) continue;

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
            if(Config.getEmissionBrightness(level, request.blockPos) > 0) {
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
        //BlockState blockState = level.getBlockState(blockPos);
        ColorRGB4 lightColor = storage.getEntry(blockPos);
        if(lightColor == null) return;

        // TODO
        if(lightColor.red4 == 0 && lightColor.green4 == 0 && lightColor.blue4 == 0)
            requestLightPullIn(blockPos);
        else
            requestLightPropagation(blockPos, lightColor, false, false);

        // propagate light if new blockState emits light
        int lightEmissionBrightness = Config.getEmissionBrightness(level, blockPos); //blockState.getLightEmission(level, blockPos);
        if(lightEmissionBrightness > 0)
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

                chunk.findBlocks(
                    blockState -> // block state filter
                            blockState.hasDynamicLightEmission() ||
                            blockState.getLightEmission(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) != 0 ||
                            Config.getEmissionBrightness(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, blockState) != 0,
                    (blockState, blockPos) -> // individual block filter
                            blockState.getLightEmission(chunk, blockPos) != 0 ||
                            Config.getEmissionBrightness(chunk, blockPos) != 0,
                    (blockPos, blockState) -> // for each found light source
                            requestLightPropagation(new BlockPos(blockPos), Config.getEmissionColor(chunk, blockPos), true, false)
                );
            }
        }
    }
}
