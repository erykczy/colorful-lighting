package me.erykczy.colorfullighting.common;

import me.erykczy.colorfullighting.ColorfulLighting;
import me.erykczy.colorfullighting.common.accessors.BlockStateAccessor;
import me.erykczy.colorfullighting.common.accessors.ClientAccessor;
import me.erykczy.colorfullighting.common.accessors.LevelAccessor;
import me.erykczy.colorfullighting.common.accessors.PlayerAccessor;
import me.erykczy.colorfullighting.common.util.ColorRGB4;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import oshi.util.tuples.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class responsible for managing light color values in the client's world and sampling those values.
 * It propagates increases (e.g. new light source has been placed)
 * It propagates decreases (e.g. light source has been destroyed, solid block has been placed in the path of light)
 * It has a thread that finds light sources on newly loaded chunks and requests light propagation
 */
public class ColoredLightEngine {
    public ClientAccessor clientAccessor;
    private ColoredLightStorage storage = new ColoredLightStorage();
    private ViewArea viewArea = new ViewArea();
    private final Set<Long> dirtySections = new HashSet<>();
    private final ConcurrentLinkedQueue<BlockPos> blocksWaitingForUpdate = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<ChunkPos> chunksWaitingForPropagation = new ConcurrentLinkedQueue<>();
    private LightPropagator lightPropagator;
    private Thread lightPropagatorThread;

    private static ColoredLightEngine instance;
    public static ColoredLightEngine getInstance() {
        return instance;
    }
    public static void create(ClientAccessor clientAccessor) {
        instance = new ColoredLightEngine(clientAccessor);
    }

    private ColoredLightEngine(ClientAccessor clientAccessor) {
        this.clientAccessor = clientAccessor;
        reset();
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

    public void updateViewArea(ViewArea newArea) {
        LevelAccessor level = clientAccessor.getLevel();
        if(level == null) return;
        if(viewArea.equals(newArea)) return;

        // unload sections
        // remove propagation requests which are not in newArea's inner area
        blocksWaitingForUpdate.removeIf(blockPos -> !newArea.containsBlockInner(blockPos));
        chunksWaitingForPropagation.removeIf(chunkPos -> !newArea.containsInner(chunkPos.x, chunkPos.z));
        // remove sections from storage
        for(int x = viewArea.minX; x <= viewArea.maxX; ++x) {
            for(int z = viewArea.minZ; z <= viewArea.maxZ; ++z) {
                if(newArea.contains(x, z)) continue;
                for(int y = level.getMinSectionY(); y <= level.getMaxSectionY(); y++) {
                    storage.removeSection(SectionPos.asLong(x, y, z));
                }
            }
        }

        // load sections
        // add sections to storage and queue chunks for propagation
        for(int x = newArea.minX; x <= newArea.maxX; ++x) {
            for(int z = newArea.minZ; z <= newArea.maxZ; ++z) {
                if(viewArea.containsInner(x, z)) continue; // old area already contains propagated section
                boolean viewAreaContainsOuter = viewArea.contains(x, z);
                if(!viewAreaContainsOuter) {
                    for(int y = level.getMinSectionY(); y <= level.getMaxSectionY(); y++) {
                        long pos = SectionPos.asLong(x, y, z);
                        storage.addSection(pos);
                    }
                }
                if(newArea.containsInner(x, z))
                    chunksWaitingForPropagation.add(new ChunkPos(x, z));
            }
        }
        viewArea = newArea;
    }

    private boolean isChunkAndNeighboursPresent(LevelAccessor level, ChunkPos chunkPos) {
        for(int ox = -1; ox <= 1; ++ox) {
            for(int oz = -1; oz <= 1; ++oz) {
                if(!level.hasChunk(new ChunkPos(chunkPos.x+ox, chunkPos.z+oz))) {
                    return false;
                }
            }
        }
        return true;
    }

    public void onBlockLightPropertiesChanged(BlockPos blockPos) {
        LevelAccessor level = clientAccessor.getLevel();
        if(level == null) return;

        SectionPos sectionPos = SectionPos.of(blockPos);
        // light should be propagated only in inner chunks
        // as full propagation needs light source's chunk and neighbours
        if(!viewArea.containsInner(sectionPos.x(), sectionPos.z())) return;

        blocksWaitingForUpdate.add(blockPos);
    }

    public void onLightUpdate() {
        LevelAccessor level = clientAccessor.getLevel();
        if(level == null) return;

        lightPropagator.pullLightChanges();

        // set all modified sections dirty
        synchronized (dirtySections) {
            for (Long dirtySection : dirtySections) {
                SectionPos sectionPos = SectionPos.of(dirtySection);
                level.setSectionDirty(sectionPos.x(), sectionPos.y(), sectionPos.z());
            }
            dirtySections.clear();
        }
    }
    public void reset() {
        if(lightPropagator != null) {
            lightPropagator.stop();
            try {
                lightPropagatorThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        storage.clear();
        viewArea = new ViewArea();
        dirtySections.clear();
        blocksWaitingForUpdate.clear();
        chunksWaitingForPropagation.clear();
        lightPropagator = new LightPropagator();
        lightPropagatorThread = new Thread(lightPropagator);
        lightPropagatorThread.start();
        ColorfulLighting.LOGGER.info("Colored light engine reset");
    }

    private class LightPropagator implements Runnable {
        private ConcurrentHashMap<BlockPos, ColorRGB4> lightChangesInProgress = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<BlockPos, ColorRGB4> lightChangesReady = new ConcurrentHashMap<>();
        private final Lock lightChangesReadyLock = new ReentrantLock();
        private volatile boolean running;

        public void stop() {
            running = false;
        }

        @Override
        public void run() {
            running = true;
            while (running) {
                executePropagationRequests();
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private void setLightColorInProgress(BlockPos blockPos, ColorRGB4 color) {
            lightChangesInProgress.put(blockPos, color);
        }

        public ColorRGB4 getLatestLightColor(BlockPos blockPos) {
            return lightChangesInProgress.getOrDefault(blockPos, lightChangesReady.getOrDefault(blockPos, storage.getEntry(blockPos)));
        }

        private Pair<ChunkPos, Integer> findNearestChunk(LevelAccessor level, PlayerAccessor player) {
            // find chunk nearest player
            var iterator = chunksWaitingForPropagation.iterator();
            int minDistance = Integer.MAX_VALUE;
            ChunkPos nearestChunkPos = null;
            while (iterator.hasNext()) {
                ChunkPos chunkPos = iterator.next();
                if(!isChunkAndNeighboursPresent(level, chunkPos)) continue; // chunk and neighbours must have available block state data
                int distance = chunkPos.getChessboardDistance(player.getChunkPos());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestChunkPos = chunkPos;
                }
            }
            return nearestChunkPos == null ? null : new Pair<>(nearestChunkPos, minDistance * 16);
        }

        private Pair<BlockPos, Integer> findNearestBlockUpdate(PlayerAccessor player) {
            var iterator = blocksWaitingForUpdate.iterator();
            int minDistance = Integer.MAX_VALUE;
            BlockPos closestBlock = null;
            while(iterator.hasNext()) {
                var element = iterator.next();
                int distance = element.distManhattan(player.getBlockPos());
                if(distance < minDistance){
                    minDistance = distance;
                    closestBlock = element;
                }
            }
            return closestBlock == null ? null : new Pair<>(closestBlock, minDistance);
        }

        private void pullLightChanges() {
            lightChangesReadyLock.lock();
            var it = lightChangesReady.entrySet().iterator();
            synchronized (dirtySections) {
                while(it.hasNext()) {
                    var entry = it.next();
                    storage.setEntryUnsafe(entry.getKey(), entry.getValue());
                    SectionPos.aroundAndAtBlockPos(entry.getKey(), dirtySections::add);
                    it.remove();
                }
            }
            lightChangesReadyLock.unlock();
        }

        private void pushLightChanges() {
            lightChangesReadyLock.lock();
            lightChangesReady.putAll(lightChangesInProgress);
            lightChangesReadyLock.unlock();
            lightChangesInProgress = new ConcurrentHashMap<>();
        }
        private void pushLightChangesDirectly() {
            for (Map.Entry<BlockPos, ColorRGB4> entry : lightChangesInProgress.entrySet()) {
                storage.setEntryUnsafe(entry.getKey(), entry.getValue());
                synchronized (dirtySections) {
                    SectionPos.aroundAndAtBlockPos(entry.getKey(), dirtySections::add);
                }
            }
            lightChangesInProgress.clear();
        }

        private void executePropagationRequests() {
            LevelAccessor level = clientAccessor.getLevel();
            if(level == null) return;
            PlayerAccessor player = clientAccessor.getPlayer();
            if(player == null) return;

            // handle increase and decrease requests
            if(blocksWaitingForUpdate.isEmpty() && chunksWaitingForPropagation.isEmpty()) return;
            var nearestChunkResult = findNearestChunk(level, player);
            var nearestBlockUpdateResult = findNearestBlockUpdate(player);

            if(nearestChunkResult != null && (nearestBlockUpdateResult == null || nearestChunkResult.getB() < nearestBlockUpdateResult.getB())) {
                ChunkPos chunkPos = nearestChunkResult.getA();
                // remove chunk from queue
                chunksWaitingForPropagation.remove(chunkPos);

                Queue<LightUpdateRequest> increaseRequests = new LinkedList<>();
                // find light sources and request their propagation
                level.findLightSources(chunkPos, (blockPos -> {
                    increaseRequests.add(new LightUpdateRequest(blockPos, Config.getColorEmission(level, blockPos), false));
                }));
                propagateIncreases(increaseRequests);
                pushLightChangesDirectly();
            }
            else if(nearestBlockUpdateResult != null) {
                BlockPos blockPos = nearestBlockUpdateResult.getA();
                blocksWaitingForUpdate.remove(blockPos);
                Queue<LightUpdateRequest> increaseRequests = new LinkedList<>();
                Queue<LightUpdateRequest> decreaseRequests = new LinkedList<>();
                handleBlockUpdate(level, increaseRequests, decreaseRequests, blockPos);
                propagateDecreases(decreaseRequests, increaseRequests);
                propagateIncreases(increaseRequests);

                pushLightChanges();
            }
        }

        private void handleBlockUpdate(LevelAccessor level, Queue<LightUpdateRequest> increaseRequests, Queue<LightUpdateRequest> decreaseRequests, BlockPos blockPos) {
            ColorRGB4 lightColor = getLatestLightColor(blockPos);
            assert lightColor != null;
            if(lightColor.red4 == 0 && lightColor.green4 == 0 && lightColor.blue4 == 0)
                requestLightPullIn(increaseRequests, decreaseRequests, blockPos);  // block probably destroyed/replaced, light pull in might be needed
            else
                decreaseRequests.add(new LightUpdateRequest(blockPos, lightColor, false)); // block probably placed/replaced, light might need to be decreased

            // propagate light if new blockState emits light
            if(Config.getEmissionBrightness(level, blockPos, 0) > 0)
                increaseRequests.add(new LightUpdateRequest(blockPos, Config.getColorEmission(level, blockPos), false));
        }
        private void requestLightPullIn(Queue<LightUpdateRequest> increaseRequests, Queue<LightUpdateRequest> decreaseRequests, BlockPos blockPos) {
            for(var direction : Direction.values()) {
                BlockPos neighbourPos = blockPos.relative(direction);
                ColorRGB4 neighbourLight = lightPropagator.getLatestLightColor(neighbourPos);
                if(neighbourLight == null) continue;

                // if neighbour doesn't have any light
                if(neighbourLight.red4 == 0 && neighbourLight.green4 == 0 && neighbourLight.blue4 == 0) continue;
                increaseRequests.add(new LightUpdateRequest(neighbourPos, neighbourLight, true));
            }
        }

        /**
         * Handles all increase propagation requests.
         */
        private void propagateIncreases(Queue<LightUpdateRequest> requests) {
            LevelAccessor level = clientAccessor.getLevel();
            while(!requests.isEmpty()) {
                propagateIncrease(requests, requests.poll(), level);
            }
        }

        private boolean propagateIncrease(Queue<LightUpdateRequest> increaseRequests, LightUpdateRequest request, LevelAccessor level) {
            ColorRGB4 oldLightColor = getLatestLightColor(request.blockPos);
            if(oldLightColor == null) return false; // section might have got unloaded and propagation should stop
            ColorRGB4 newLightColor = ColorRGB4.fromRGB4(
                    Math.max(oldLightColor.red4, request.lightColor.red4),
                    Math.max(oldLightColor.green4, request.lightColor.green4),
                    Math.max(oldLightColor.blue4, request.lightColor.blue4)
            );

            // if light color didn't change (check is ignored if request is forced)
            if(!request.force && newLightColor.red4 == oldLightColor.red4 && newLightColor.green4 == oldLightColor.green4 && newLightColor.blue4 == oldLightColor.blue4) return true;
            setLightColorInProgress(request.blockPos, newLightColor);

            for(var direction : Direction.values()) {
                BlockPos neighbourPos = request.blockPos.relative(direction);
                if(!level.isInBounds(neighbourPos)) continue;
                BlockStateAccessor neighbourState = level.getBlockState(neighbourPos);
                if(neighbourState == null) return false; // section might have got unloaded and propagation should stop

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

                increaseRequests.add(new LightUpdateRequest(neighbourPos, neighbourLightColor, false));
            }
            return true;
        }

        /**
         * Handles all decrease propagation requests.
         */
        private void propagateDecreases(Queue<LightUpdateRequest> decreaseRequests, Queue<LightUpdateRequest> increaseRequests) {
            LevelAccessor level = clientAccessor.getLevel();
            while(!decreaseRequests.isEmpty()) {
                propagateDecrease(increaseRequests, decreaseRequests, decreaseRequests.poll(), level);
            }
        }

        private boolean propagateDecrease(Queue<LightUpdateRequest> increaseRequests, Queue<LightUpdateRequest> decreaseRequests, LightUpdateRequest request, LevelAccessor level) {
            ColorRGB4 oldLightColor = getLatestLightColor(request.blockPos);
            if(oldLightColor == null) return false; // section might have got unloaded and propagation should stop

            // if light color didn't change (check is ignored if request is forced)
            if(!request.force && oldLightColor.red4 == 0 && oldLightColor.green4 == 0 && oldLightColor.blue4 == 0) return true;
            setLightColorInProgress(request.blockPos, ColorRGB4.fromRGB4(0, 0, 0));

            BlockStateAccessor blockState = level.getBlockState(request.blockPos);
            if(blockState == null) return false; // section might have got unloaded and propagation should stop
            // repropagate removed light
            if(Config.getEmissionBrightness(level, request.blockPos, blockState) > 0) {
                increaseRequests.add(new LightUpdateRequest(request.blockPos, Config.getColorEmission(level, request.blockPos), false));
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
                    decreaseRequests.add(new LightUpdateRequest(neighbourPos, neighbourLightDecrease, false));
                }
                else {
                    ColorRGB4 neighbourLightColor = getLatestLightColor(neighbourPos);
                    if(neighbourLightColor == null) return false; // section might have got unloaded and propagation should stop
                    // if neighbour doesn't have any light
                    if(neighbourLightColor.red4 == 0 && neighbourLightColor.green4 == 0 && neighbourLightColor.blue4 == 0)
                        continue;

                    // force neighbour to propagate light to the region that has been just cleared (decreased)
                    increaseRequests.add(new LightUpdateRequest(neighbourPos, neighbourLightColor, true));
                }
            }
            return true;
        }
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
}
