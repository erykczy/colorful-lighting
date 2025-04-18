package com.example.examplemod.common;

import com.example.examplemod.common.accessors.BlockStateAccessor;
import com.example.examplemod.common.accessors.ClientAccessor;
import com.example.examplemod.common.accessors.LevelAccessor;
import com.example.examplemod.common.accessors.PlayerAccessor;
import com.example.examplemod.common.util.ColorRGB4;
import com.example.examplemod.common.util.ColorRGB8;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Class responsible for managing light color values in the client's world and sampling those values.
 * It propagates increases (e.g. new light source has been placed)
 * It propagates decreases (e.g. light source has been destroyed, solid block has been placed in the path of light)
 * It has a thread that finds light sources on newly loaded chunks and requests light propagation
 */
public class ColoredLightEngine {
    public ColoredLightStorage storage = new ColoredLightStorage();
    public ClientAccessor clientAccessor;
    // light increase propagation requests
    private Queue<LightUpdateRequest> propagateIncreases = new ConcurrentLinkedQueue<>();
    // light decrease propagation requests
    private Queue<LightUpdateRequest> propagateDecreases = new LinkedList<>();
    // sections that were modified by requests
    private LinkedList<Long> dirtySections = new LinkedList<>();
    // newly loaded chunks that wait for light propagation
    private Queue<ChunkPos> newChunks = new ConcurrentLinkedQueue<>();
    private HashSet<ChunkPos> fullyLoadedChunks = new HashSet<>();
    // thread that finds light sources in newly loaded chunks and adds propagation requests for those blocks (it is slow task so it is executed in other thread)
    private Thread handleNewChunksThread;

    private static ColoredLightEngine instance;
    public static ColoredLightEngine getInstance() {
        return instance;
    }

    public ColoredLightEngine(ClientAccessor clientAccessor) {
        this.clientAccessor = clientAccessor;
        handleNewChunksThread = new Thread(new PropagateLightInNewChunks());
        handleNewChunksThread.start();
        instance = this;
    }

    public ColorRGB4 sampleLightColor(BlockPos pos) { return sampleLightColor(pos.getX(), pos.getY(), pos.getZ()); }
    public ColorRGB4 sampleLightColor(int x, int y, int z) {
        var entry = storage.getEntry(x, y, z);
        if(entry == null) return ColorRGB4.fromRGB4(0, 0, 0);
        return entry;
    }

    /**
     * Mixes light color from blocks neighbouring given position using trilinear interpolation.
     */
    public ColorRGB8 sampleTrilinearLightColor(Vec3 pos) {
        int cornerX = (int)Math.round(pos.x) - 1;
        int cornerY = (int)Math.round(pos.y) - 1;
        int cornerZ = (int)Math.round(pos.z) - 1;
        ColorRGB8 c000 = ColorRGB8.fromRGB4(sampleLightColor(cornerX + 0, cornerY + 0, cornerZ + 0));
        ColorRGB8 c100 = ColorRGB8.fromRGB4(sampleLightColor(cornerX + 1, cornerY + 0, cornerZ + 0));
        ColorRGB8 c101 = ColorRGB8.fromRGB4(sampleLightColor(cornerX + 1, cornerY + 0, cornerZ + 1));
        ColorRGB8 c001 = ColorRGB8.fromRGB4(sampleLightColor(cornerX + 0, cornerY + 0, cornerZ + 1));
        ColorRGB8 c010 = ColorRGB8.fromRGB4(sampleLightColor(cornerX + 0, cornerY + 1, cornerZ + 0));
        ColorRGB8 c110 = ColorRGB8.fromRGB4(sampleLightColor(cornerX + 1, cornerY + 1, cornerZ + 0));
        ColorRGB8 c111 = ColorRGB8.fromRGB4(sampleLightColor(cornerX + 1, cornerY + 1, cornerZ + 1));
        ColorRGB8 c011 = ColorRGB8.fromRGB4(sampleLightColor(cornerX + 0, cornerY + 1, cornerZ + 1));

        double x = (pos.x - (double) cornerX) / 2.0;
        double y = (pos.y - (double) cornerY) / 2.0;
        double z = (pos.z - (double) cornerZ) / 2.0;

        ColorRGB8 c00 = linearInterpolation(c000, c100, x);
        ColorRGB8 c01 = linearInterpolation(c001, c101, x);
        ColorRGB8 c11 = linearInterpolation(c011, c111, x);
        ColorRGB8 c10 = linearInterpolation(c010, c110, x);

        ColorRGB8 c0 = linearInterpolation(c00, c10, y);
        ColorRGB8 c1 = linearInterpolation(c01, c11, y);

        return linearInterpolation(c0, c1, z);
    }

    private ColorRGB8 linearInterpolation(ColorRGB8 a, ColorRGB8 b, double x) {
        if(a.isZero()) return b;
        if(b.isZero()) return a;
        return a.mul(1.0 - x).add(b.mul(x));
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
    private void propagateIncreases() {
        LevelAccessor level = clientAccessor.getLevel();
        if(level == null) return;
        while(!propagateIncreases.isEmpty()) {
            LightUpdateRequest request = propagateIncreases.poll();
            ColorRGB4 oldLightColor = storage.getEntry(request.blockPos);
            if(oldLightColor == null) continue; // if storage doesn't contain request.blockPos
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
                if(!level.isInBounds(neighbourPos)) continue;
                BlockStateAccessor neighbourState = level.getBlockState(neighbourPos);

                // light attenuation
                int lightBlocked = Math.max(1, neighbourState.getLightBlock(level, neighbourPos)); // vanilla light block
                ColorRGB4 coloredLightTransmittance = Config.getColoredLightTransmittance(level, neighbourPos, neighbourState); // rgb transmittance (example: red stained glass can let only red light through)
                ColorRGB4 neighbourLightColor = ColorRGB4.fromRGB4(
                        Math.clamp(request.lightColor.red4 - lightBlocked, 0, coloredLightTransmittance.red4),
                        Math.clamp(request.lightColor.green4 - lightBlocked, 0, coloredLightTransmittance.green4),
                        Math.clamp(request.lightColor.blue4 - lightBlocked, 0, coloredLightTransmittance.blue4)
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
    private void propagateDecreases() {
        LevelAccessor level = clientAccessor.getLevel();
        if(level == null) return;
        while(!propagateDecreases.isEmpty()) {
            LightUpdateRequest request = propagateDecreases.poll();
            ColorRGB4 oldLightColor = storage.getEntry(request.blockPos);
            if(oldLightColor == null) continue; // if storage doesn't contain request.blockPos

            // if light color didn't change (check is ignored if request is forced)
            if(!request.force && oldLightColor.red4 == 0 && oldLightColor.green4 == 0 && oldLightColor.blue4 == 0) continue;
            setLightColor(request.blockPos, ColorRGB4.fromRGB4(0, 0, 0));

            // repropagate removed light
            if(Config.getEmissionBrightness(level, request.blockPos) > 0) {
                requestLightPropagation(request.blockPos, Config.getColorEmission(level, request.blockPos), true, false);
            }

            // attenuation
            ColorRGB4 neighbourLightDecrease = ColorRGB4.fromRGB4(
                    Math.max(0, request.lightColor.red4 - 1),
                    Math.max(0, request.lightColor.green4 - 1),
                    Math.max(0, request.lightColor.blue4 - 1)
            );
            // whether neighbours' light should be decreased or increased (to repropagate), true on "light edges"
            boolean repropagateNeighbours = neighbourLightDecrease.red4 == 0 && neighbourLightDecrease.green4 == 0 && neighbourLightDecrease.blue4 == 0;

            for(var direction : Direction.values()) {
                BlockPos neighbourPos = request.blockPos.relative(direction);
                if(!level.isInBounds(neighbourPos)) continue;

                if(!repropagateNeighbours) {
                    // propagate decrease
                    requestLightPropagation(neighbourPos, neighbourLightDecrease, false, false);
                }
                else {
                    ColorRGB4 neighbourLightColor = storage.getEntry(neighbourPos);
                    if(neighbourLightColor == null) continue;
                    // if neighbour doesn't have any light
                    if(neighbourLightColor.red4 == 0 && neighbourLightColor.green4 == 0 && neighbourLightColor.blue4 == 0)
                        continue;

                    // force neighbour to propagate light to the region that has been just cleared (decreased)
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

    public void onBlockLightPropertiesChanged(BlockPos blockPos) {
        LevelAccessor level = clientAccessor.getLevel();
        if(level == null) return;
        ColorRGB4 lightColor = storage.getEntry(blockPos);
        if(lightColor == null) return; // if storage doesn't contain data for blockPos

        // TODO
        if(lightColor.red4 == 0 && lightColor.green4 == 0 && lightColor.blue4 == 0)
            requestLightPullIn(blockPos);
        else
            requestLightPropagation(blockPos, lightColor, false, false);

        // propagate light if new blockState emits light
        if(Config.getEmissionBrightness(level, blockPos) > 0)
            requestLightPropagation(blockPos, Config.getColorEmission(level, blockPos), true, false);
    }

    public void runLightUpdates() {
        LevelAccessor level = clientAccessor.getLevel();
        if(level == null) return;

        // handle increase and decrease requests
        propagateDecreases();
        propagateIncreases();

        // set dirty all modified sections
        var iterator = dirtySections.iterator();
        while (iterator.hasNext()) {
            SectionPos sectionPos = SectionPos.of(iterator.next());
            level.setSectionDirtyWithNeighbours(sectionPos.x(), sectionPos.y(), sectionPos.z());
            iterator.remove();
        }
    }

    private void setLightColor(BlockPos blockPos, ColorRGB4 color) {
        storage.setEntry(blockPos.getX(), blockPos.getY(), blockPos.getZ(), color);
        dirtySections.add(SectionPos.asLong(blockPos));
    }

    public void onChunkLoad(ChunkPos chunkPos) {
        LevelAccessor level = clientAccessor.getLevel();
        if(level == null) return;
        // add sections to storage
        for(int i = 0; i < level.getSectionsCount(); i++) {
            int y = level.getMinSectionY() + i;
            storage.addSection(SectionPos.asLong(chunkPos.x, y, chunkPos.z));
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
                        if(!storage.containsSection(SectionPos.asLong(chunkPos.x + x + neighbourX, level.getMinSectionY(), chunkPos.z + z + neighbourZ))) {
                            allNeighboursLoaded = false;
                            break;
                        }
                    }
                    if(!allNeighboursLoaded) break;
                }
                if(allNeighboursLoaded) {
                    ChunkPos chunk1 = new ChunkPos(chunkPos.x + x, chunkPos.z + z);
                    newChunks.add(chunk1);
                    fullyLoadedChunks.add(chunk1);
                }
            }
        }
    }

    public void onChunkUnload(ChunkPos chunkPos) {
        LevelAccessor level = clientAccessor.getLevel();
        if(level == null) return;
        newChunks.remove(chunkPos);
        for(int i = 0; i < level.getSectionsCount(); i++) {
            int y = level.getMinSectionY() + i;;
            storage.removeSection(SectionPos.asLong(chunkPos.x, y, chunkPos.z));
        }
        fullyLoadedChunks.remove(chunkPos);
    }

    public void onLevelUnload() {
        newChunks.clear();
        fullyLoadedChunks.clear();
        storage.clear();
    }

    public void refreshLevel() {
        LevelAccessor level = clientAccessor.getLevel();
        if(level == null) return;
        for(var chunk : fullyLoadedChunks) {
            for(int i = 0; i < level.getSectionsCount(); i++) {
                int y = level.getMinSectionY() + i;
                storage.getSection(SectionPos.asLong(chunk.x, y, chunk.z)).clear();
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
                    Thread.sleep(1);
                }
                catch (Exception e) {
                    System.err.println(e.getMessage());
                }
                doTask();
            }
        }

        private void doTask() {
            if (newChunks.isEmpty()) return;
            if (!propagateIncreases.isEmpty()) return; // do not impose new increases if render thread is still working on them
            LevelAccessor level = clientAccessor.getLevel();
            if(level == null) return;
            PlayerAccessor playerAccessor = clientAccessor.getPlayer();
            if(playerAccessor == null) return;

            // find chunk nearest player
            var iterator = newChunks.iterator();
            int minDistance = Integer.MAX_VALUE;
            ChunkPos nearestChunkPos = null;
            while (iterator.hasNext()) {
                ChunkPos chunkPos = iterator.next();
                int distance = chunkPos.getChessboardDistance(playerAccessor.getPlayerChunkPos());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestChunkPos = chunkPos;
                }
            }
            // remove chunk from queue
            newChunks.remove(nearestChunkPos);

            // find light sources and request their propagation
            level.findLightSources(nearestChunkPos, (blockPos -> requestLightPropagation(blockPos, Config.getColorEmission(level, blockPos), true, false)));
        }
    }
}
