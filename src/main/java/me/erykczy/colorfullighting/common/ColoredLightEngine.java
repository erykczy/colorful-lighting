package me.erykczy.colorfullighting.common;

import me.erykczy.colorfullighting.ColorfulLighting;
import me.erykczy.colorfullighting.common.accessors.BlockStateAccessor;
import me.erykczy.colorfullighting.common.accessors.ClientAccessor;
import me.erykczy.colorfullighting.common.accessors.LevelAccessor;
import me.erykczy.colorfullighting.common.accessors.PlayerAccessor;
import me.erykczy.colorfullighting.common.util.ColorRGB4;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.MathExt;
import me.erykczy.colorfullighting.compat.sodium.SodiumCompat;
import me.erykczy.colorfullighting.mixin.compat.sodium.SodiumWorldRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class responsible for managing light color values in the client's world and sampling those values.
 * Most work is delegated to LightPropagator thread.
 */
public class ColoredLightEngine {
    private ClientAccessor clientAccessor;
    private final ColoredLightStorage storage = new ColoredLightStorage();
    private final Object storageLock = new Object();
    private ViewArea viewArea = new ViewArea();
    private final ConcurrentLinkedQueue<LightUpdateRequest> blockUpdateDecreaseRequests = new ConcurrentLinkedQueue<>(); // those first added will be executed first (this order is required by decrease propagation algorithm)
    private final ConcurrentLinkedQueue<BlockRequests> blockUpdateIncreaseRequests = new ConcurrentLinkedQueue<>(); // those nearest to the player will be executed first
    private final ConcurrentLinkedQueue<ChunkPos> chunksWaitingForPropagation = new ConcurrentLinkedQueue<>(); // those nearest to the player will be executed first
    private final Set<Long> dirtySections = new HashSet<>();
    private final Set<Long> sectionsToRebuildLater = ConcurrentHashMap.newKeySet();

    private final ConcurrentLinkedQueue<DelayedChunkUpdate> delayedChunkUpdates = new ConcurrentLinkedQueue<>();
    private final Set<ChunkPos> pendingDelayedUpdates = ConcurrentHashMap.newKeySet();

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
        synchronized (storageLock) {
            var entry = storage.getEntry(x, y, z);
            if(entry == null) return ColorRGB4.fromRGB4(0, 0, 0);
            return entry;
        }
    }
    /**
     * Mixes light color from blocks neighbouring given position using trilinear interpolation.
     */
    public ColorRGB8 sampleTrilinearLightColor(Vec3 pos) {
        int cornerX = (int)Math.round(pos.x) - 1;
        int cornerY = (int)Math.round(pos.y) - 1;
        int cornerZ = (int)Math.round(pos.z) - 1;

        ColorRGB8 c000 = ColorRGB8.fromRGB4(sampleLightColor(cornerX, cornerY, cornerZ));
        ColorRGB8 c100 = ColorRGB8.fromRGB4(sampleLightColor(cornerX + 1, cornerY, cornerZ));
        ColorRGB8 c101 = ColorRGB8.fromRGB4(sampleLightColor(cornerX + 1, cornerY, cornerZ + 1));
        ColorRGB8 c001 = ColorRGB8.fromRGB4(sampleLightColor(cornerX, cornerY, cornerZ + 1));
        ColorRGB8 c010 = ColorRGB8.fromRGB4(sampleLightColor(cornerX, cornerY + 1, cornerZ));
        ColorRGB8 c110 = ColorRGB8.fromRGB4(sampleLightColor(cornerX + 1, cornerY + 1, cornerZ));
        ColorRGB8 c111 = ColorRGB8.fromRGB4(sampleLightColor(cornerX + 1, cornerY + 1, cornerZ + 1));
        ColorRGB8 c011 = ColorRGB8.fromRGB4(sampleLightColor(cornerX, cornerY + 1, cornerZ + 1));

        double x = pos.x - (cornerX + 0.5);
        double y = pos.y - (cornerY + 0.5);
        double z = pos.z - (cornerZ + 0.5);

        ColorRGB8 c00 = ColorRGB8.linearInterpolation(c000, c100, x);
        ColorRGB8 c10 = ColorRGB8.linearInterpolation(c010, c110, x);
        ColorRGB8 c01 = ColorRGB8.linearInterpolation(c001, c101, x);
        ColorRGB8 c11 = ColorRGB8.linearInterpolation(c011, c111, x);

        ColorRGB8 c0 = ColorRGB8.linearInterpolation(c00, c10, y);
        ColorRGB8 c1 = ColorRGB8.linearInterpolation(c01, c11, y);

        return ColorRGB8.linearInterpolation(c0, c1, z);
    }


    public void updateViewArea(ViewArea newArea) {
        LevelAccessor level = clientAccessor.getLevel();
        if(level == null) return;
        if(viewArea.equals(newArea)) return;

        // unload sections
        // remove propagation requests which are not in newArea's inner area
        blockUpdateIncreaseRequests.removeIf(blockUpdate -> !newArea.containsBlockInner(blockUpdate.blockPos));
        blockUpdateDecreaseRequests.removeIf(blockUpdate -> !newArea.containsBlockInner(blockUpdate.blockPos));
        chunksWaitingForPropagation.removeIf(chunkPos -> !newArea.containsInner(chunkPos.x, chunkPos.z));
        // remove sections from storage
        synchronized (storageLock) {
            for(int x = viewArea.minX; x <= viewArea.maxX; ++x) {
                for(int z = viewArea.minZ; z <= viewArea.maxZ; ++z) {
                    if(newArea.contains(x, z)) continue;
                    for(int y = level.getMinSectionY(); y <= level.getMaxSectionY(); y++) {
                        storage.removeSection(SectionPos.asLong(x, y, z));
                    }
                }
            }
        }

        // load sections
        // add sections to storage and queue chunks for propagation
        synchronized (storageLock) {
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
        }
        viewArea = newArea;
    }

    public void onBlockLightPropertiesChanged(BlockPos blockPos) {
        LevelAccessor level = clientAccessor.getLevel();
        if(level == null) return;

        SectionPos sectionPos = SectionPos.of(blockPos);
        // light should be propagated only in inner chunks as
        // full propagation needs light source's chunk and neighbours
        if(!viewArea.containsInner(sectionPos.x(), sectionPos.z())) return;

        BlockStateAccessor newBlockState = level.getBlockState(blockPos);
        boolean isAir = newBlockState == null || newBlockState.isAir();

        ColorRGB4 oldLightColor;
        synchronized (storageLock) {
            oldLightColor = storage.getEntry(blockPos);
        }
        if (oldLightColor == null) oldLightColor = ColorRGB4.fromRGB4(0,0,0);

        // If a light source was destroyed (replaced by air), we need to handle it specially to prevent lingering light
        if (isAir && (oldLightColor.red4 > 0 || oldLightColor.green4 > 0 || oldLightColor.blue4 > 0)) {
            // 1. Queue the decrease request using the OLD color, so neighbors get updated.
            // We use force=true because we are about to clear the storage, so the propagator's check against current storage would fail otherwise.
            blockUpdateDecreaseRequests.add(new LightUpdateRequest(blockPos, oldLightColor, true));

            // 2. IMMEDIATELY clear the storage for this block.
            // This ensures that any immediate chunk rebuilds (which happen fast with explosions) see the block as dark.
            synchronized (storageLock) {
                storage.setEntryUnsafe(blockPos, ColorRGB4.fromRGB4(0, 0, 0));
            }

            // 3. Mark section for later rebuild to ensure any race conditions are cleaned up
            sectionsToRebuildLater.add(sectionPos.asLong());

            // 4. Schedule a full re-check of the chunk to clean up any lingering light artifacts
            ChunkPos chunkPos = new ChunkPos(blockPos);
            if (pendingDelayedUpdates.add(chunkPos)) {
                delayedChunkUpdates.add(new DelayedChunkUpdate(chunkPos, System.currentTimeMillis() + 500));
            }
            return;
        }

        BlockRequests increaseRequests = new BlockRequests(blockPos);
        handleBlockUpdate(level, increaseRequests.increaseRequests, blockUpdateDecreaseRequests, blockPos);
        if(!increaseRequests.increaseRequests.isEmpty()) blockUpdateIncreaseRequests.add(increaseRequests);
    }
    private void handleBlockUpdate(LevelAccessor level, Queue<LightUpdateRequest> increaseRequests, Queue<LightUpdateRequest> decreaseRequests, BlockPos blockPos) {
        ColorRGB4 lightColor;
        synchronized (storageLock) {
            lightColor = storage.getEntry(blockPos);
        }
        if (lightColor == null) lightColor = ColorRGB4.fromRGB4(0,0,0);

        if(lightColor.red4 == 0 && lightColor.green4 == 0 && lightColor.blue4 == 0)
            requestLightPullIn(increaseRequests, blockPos);  // block probably destroyed/replaced with transparent, light pull in might be needed
        else
            decreaseRequests.add(new LightUpdateRequest(blockPos, lightColor, false)); // block probably placed/replaced with non-transparent, light might need to be decreased

        // propagate light if new blockState emits light
        if(Config.getEmissionBrightness(level, blockPos, 0) > 0)
            increaseRequests.add(new LightUpdateRequest(blockPos, Config.getColorEmission(level, blockPos), false, true));
    }
    private void requestLightPullIn(Queue<LightUpdateRequest> increaseRequests, BlockPos blockPos) {
        for(var direction : Direction.values()) {
            BlockPos neighbourPos = blockPos.relative(direction);
            ColorRGB4 neighbourLight;
            synchronized (storageLock) {
                neighbourLight = storage.getEntry(neighbourPos);
            }
            if(neighbourLight == null) continue;

            if(neighbourLight.red4 == 0 && neighbourLight.green4 == 0 && neighbourLight.blue4 == 0) continue;
            increaseRequests.add(new LightUpdateRequest(neighbourPos, neighbourLight, true));
        }
    }

    public void onLightUpdate() {
        LevelAccessor level = clientAccessor.getLevel();
        if(level == null) return;

        lightPropagator.applyReadyLightChanges();

        Set<Long> sectionsToUpdate;
        synchronized (dirtySections) {
            if (dirtySections.isEmpty()) {
                return;
            }
            sectionsToUpdate = new HashSet<>(dirtySections);
            dirtySections.clear();
        }

        for (Long dirtySection : sectionsToUpdate) {
            SectionPos sectionPos = SectionPos.of(dirtySection);
            level.setSectionDirty(sectionPos.x(), sectionPos.y(), sectionPos.z());

            // Force Sodium rebuild if present
            if (SodiumCompat.isSodiumLoaded()) {
                var renderer = Minecraft.getInstance().levelRenderer;
                if (renderer instanceof SodiumWorldRendererAccessor sodiumRenderer) {
                    sodiumRenderer.scheduleRebuild(sectionPos.x(), sectionPos.y(), sectionPos.z(), true);
                }
            }
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
        blockUpdateIncreaseRequests.clear();
        blockUpdateDecreaseRequests.clear();
        chunksWaitingForPropagation.clear();
        delayedChunkUpdates.clear();
        pendingDelayedUpdates.clear();
        lightPropagator = new LightPropagator();
        lightPropagatorThread = new Thread(lightPropagator);
        lightPropagatorThread.start();
        ColorfulLighting.LOGGER.info("Colored light engine reset");
    }

    /**
     * LightPropagator calculates changes to light values. It runs on another thread to avoid lag on the main thread.
     * It propagates increases (increases of light values, e.g. new light source has been placed).
     * It propagates decreases (decreases of light values, e.g. light source has been destroyed, solid block has been placed in the path of light).
     * Changes caused by block updates are applied on the main thread to avoid light flickering
     */
    private class LightPropagator implements Runnable {
        /**
         * light changes that are not yet ready to be visible on main thread
         */
        private ConcurrentHashMap<BlockPos, ColorRGB4> lightChangesInProgress = new ConcurrentHashMap<>();
        /**
         * light changes ready to be visible on main thread
         */
        private final ConcurrentHashMap<BlockPos, ColorRGB4> lightChangesReady = new ConcurrentHashMap<>();
        private final Lock lightChangesReadyLock = new ReentrantLock();
        private volatile boolean running;

        public boolean hasReadyLightChanges() {
            return !this.lightChangesReady.isEmpty();
        }

        @Override
        public void run() {
            running = true;
            while (running) {
                // Process delayed chunk updates
                long now = System.currentTimeMillis();
                while (!delayedChunkUpdates.isEmpty()) {
                    DelayedChunkUpdate update = delayedChunkUpdates.peek();
                    if (now >= update.executeTime) {
                        delayedChunkUpdates.poll();
                        pendingDelayedUpdates.remove(update.chunkPos);
                        performChunkRebuild(update.chunkPos);
                    } else {
                        break; // Queue is ordered by time
                    }
                }

                boolean hasWork = !blockUpdateDecreaseRequests.isEmpty() || !blockUpdateIncreaseRequests.isEmpty() || !chunksWaitingForPropagation.isEmpty();

                if (hasWork) {
                    propagateLight();
                } else {
                    // If idle, check if we have sections to rebuild from explosions
                    if (!sectionsToRebuildLater.isEmpty()) {
                        synchronized (dirtySections) {
                            dirtySections.addAll(sectionsToRebuildLater);
                        }
                        sectionsToRebuildLater.clear();
                        Minecraft.getInstance().execute(ColoredLightEngine.this::onLightUpdate);
                    }
                }

                if (this.hasReadyLightChanges()) {
                    Minecraft.getInstance().execute(ColoredLightEngine.this::onLightUpdate);
                }

                try {
                    // Sleep to prevent busy-waiting, sleep longer if there was no work
                    Thread.sleep(hasWork ? 1L : 10L);
                } catch (InterruptedException e) {
                    running = false;
                }
            }
        }

        public void stop() {
            running = false;
        }

        private void addLightColorChange(BlockPos blockPos, ColorRGB4 color) {
            lightChangesInProgress.put(blockPos, color);
        }

        public ColorRGB4 getLatestLightColor(BlockPos blockPos) {
            ColorRGB4 inProgress = lightChangesInProgress.get(blockPos);
            if (inProgress != null) return inProgress;

            ColorRGB4 ready = lightChangesReady.get(blockPos);
            if (ready != null) return ready;

            synchronized (storageLock) {
                return storage.getEntry(blockPos);
            }
        }

        private void performChunkRebuild(ChunkPos chunkPos) {
            LevelAccessor level = clientAccessor.getLevel();
            if (level == null) return;

            // 1. Clear storage for the chunk
            synchronized (storageLock) {
                for(int y = level.getMinSectionY(); y <= level.getMaxSectionY(); y++) {
                    long pos = SectionPos.asLong(chunkPos.x, y, chunkPos.z);
                    storage.removeSection(pos);
                    storage.addSection(pos);
                }
            }

            Queue<LightUpdateRequest> increaseRequests = new LinkedList<>();

            // 2. Find internal sources
            level.findLightSources(chunkPos, (blockPos -> {
                increaseRequests.add(new LightUpdateRequest(blockPos, Config.getColorEmission(level, blockPos), false));
            }));

            // 3. Pull from neighbors
            int minBlockY = level.getMinSectionY() * 16;
            int maxBlockY = (level.getMaxSectionY() + 1) * 16 - 1;

            int startX = chunkPos.getMinBlockX();
            int startZ = chunkPos.getMinBlockZ();
            int endX = startX + 15;
            int endZ = startZ + 15;

            for (int y = minBlockY; y <= maxBlockY; y++) {
                // North border (check z-1)
                checkNeighborAndAdd(increaseRequests, startX, endX, y, startZ - 1, true);
                // South border (check z+16)
                checkNeighborAndAdd(increaseRequests, startX, endX, y, endZ + 1, true);
                // West border (check x-1)
                checkNeighborAndAdd(increaseRequests, startZ, endZ, y, startX - 1, false);
                // East border (check x+16)
                checkNeighborAndAdd(increaseRequests, startZ, endZ, y, endX + 1, false);
            }

            propagateIncreases(level, increaseRequests);
            applyLightChangesDirectly();
        }

        private void checkNeighborAndAdd(Queue<LightUpdateRequest> requests, int start, int end, int y, int fixed, boolean isZFixed) {
            for (int i = start; i <= end; i++) {
                BlockPos pos = isZFixed ? new BlockPos(i, y, fixed) : new BlockPos(fixed, y, i);
                ColorRGB4 color = getLatestLightColor(pos);
                if (color != null && (color.red4 > 0 || color.green4 > 0 || color.blue4 > 0)) {
                    requests.add(new LightUpdateRequest(pos, color, true));
                }
            }
        }

        private record NearestBlockRequestsResult(BlockRequests blockUpdate, int distanceBlocks) {}
        private NearestBlockRequestsResult getNearestBlockRequests(PlayerAccessor player) {
            // find chunk nearest player
            var iterator = blockUpdateIncreaseRequests.iterator();
            int minDistance = Integer.MAX_VALUE;
            BlockRequests nearestUpdate = null;
            while (iterator.hasNext()) {
                BlockRequests update = iterator.next();
                int distance = update.blockPos.distManhattan(player.getBlockPos());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestUpdate = update;
                }
            }
            return nearestUpdate == null ? null : new NearestBlockRequestsResult(nearestUpdate, minDistance);
        }

        private record NearestChunkResult(ChunkPos chunkPos, int distanceBlocks) {}
        private NearestChunkResult getNearestWaitingChunk(LevelAccessor level, PlayerAccessor player) {
            // find chunk nearest player
            var iterator = chunksWaitingForPropagation.iterator();
            int minDistance = Integer.MAX_VALUE;
            ChunkPos nearestChunkPos = null;
            while (iterator.hasNext()) {
                ChunkPos chunkPos = iterator.next();
                if(!level.hasChunkAndNeighbours(chunkPos)) continue; // chunk and neighbours must have available block state data
                int distance = chunkPos.getChessboardDistance(player.getChunkPos());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestChunkPos = chunkPos;
                }
            }
            return nearestChunkPos == null ? null : new NearestChunkResult(nearestChunkPos, minDistance * 16); // distanceBlocks is in blocks
        }

        /**
         * apply ready light changes to storage
         */
        private void applyReadyLightChanges() {
            lightChangesReadyLock.lock();
            try {
                if (lightChangesReady.isEmpty()) {
                    return;
                }
                synchronized (storageLock) {
                    synchronized (dirtySections) {
                        for (var entry : lightChangesReady.entrySet()) {
                            storage.setEntryUnsafe(entry.getKey(), entry.getValue());
                            SectionPos.aroundAndAtBlockPos(entry.getKey(), dirtySections::add);
                        }
                    }
                }
                lightChangesReady.clear();
            } finally {
                lightChangesReadyLock.unlock();
            }
        }

        /**
         * move light changes in progress to collection of ready light changes
         */
        private void markLightChangesReady() {
            if (lightChangesInProgress.isEmpty()) {
                return;
            }
            lightChangesReadyLock.lock();
            try {
                lightChangesReady.putAll(lightChangesInProgress);
            } finally {
                lightChangesReadyLock.unlock();
            }
            lightChangesInProgress = new ConcurrentHashMap<>();
        }

        /**
         * apply light changes in progress directly to storage
         */
        private void applyLightChangesDirectly() {
            if (lightChangesInProgress.isEmpty()) {
                return;
            }
            synchronized (storageLock) {
                synchronized (dirtySections) {
                    for (var entry : lightChangesInProgress.entrySet()) {
                        storage.setEntryUnsafe(entry.getKey(), entry.getValue());
                        SectionPos.aroundAndAtBlockPos(entry.getKey(), dirtySections::add);
                    }
                }
            }
            lightChangesInProgress.clear();
        }

        /**
         * propagate light in the nearest waiting chunk, handle block light updates
         */
        private void propagateLight() {
            LevelAccessor level = clientAccessor.getLevel();
            if(level == null) return;
            PlayerAccessor player = clientAccessor.getPlayer();
            if(player == null) return;

            // decrease requests are always executed
            if(!blockUpdateDecreaseRequests.isEmpty()) {
                Queue<LightUpdateRequest> newIncreaseRequests = new LinkedList<>();
                propagateDecreases(level, blockUpdateDecreaseRequests, newIncreaseRequests);
                propagateIncreases(level, newIncreaseRequests);

                markLightChangesReady();
            }

            var nearestChunkResult = getNearestWaitingChunk(level, player);
            var nearestBlockRequests = getNearestBlockRequests(player);

            if(nearestChunkResult != null && (nearestBlockRequests == null || nearestChunkResult.distanceBlocks() < nearestBlockRequests.distanceBlocks())) {
                // propagate chunk
                ChunkPos chunkPos = nearestChunkResult.chunkPos();
                chunksWaitingForPropagation.remove(chunkPos);

                Queue<LightUpdateRequest> increaseRequests = new LinkedList<>();
                // find light sources and request their propagation
                level.findLightSources(chunkPos, (blockPos -> {
                    increaseRequests.add(new LightUpdateRequest(blockPos, Config.getColorEmission(level, blockPos), false));
                }));
                propagateIncreases(level, increaseRequests);
                // new chunks' light propagation is not synchronized with main thread
                applyLightChangesDirectly();
            }
            else if(nearestBlockRequests != null) {
                blockUpdateIncreaseRequests.remove(nearestBlockRequests.blockUpdate);
                propagateIncreases(level, nearestBlockRequests.blockUpdate.increaseRequests);
                markLightChangesReady();
            }
        }

        /**
         * Handles all increase propagation requests.
         */
        private void propagateIncreases(LevelAccessor level, Queue<LightUpdateRequest> requests) {
            while(!requests.isEmpty()) {
                propagateIncrease(requests, requests.poll(), level);
            }
        }

        private boolean propagateIncrease(Queue<LightUpdateRequest> increaseRequests, LightUpdateRequest request, LevelAccessor level) {
            if (request.checkSource) {
                BlockStateAccessor blockState = level.getBlockState(request.blockPos);
                if (blockState == null || Config.getEmissionBrightness(level, request.blockPos, blockState) == 0) {
                    return false;
                }
            }

            ColorRGB4 oldLightColor = getLatestLightColor(request.blockPos);
            if(oldLightColor == null) return false; // section might have got unloaded and propagation should stop
            ColorRGB4 newLightColor = ColorRGB4.fromRGB4(
                    Math.max(oldLightColor.red4, request.lightColor.red4),
                    Math.max(oldLightColor.green4, request.lightColor.green4),
                    Math.max(oldLightColor.blue4, request.lightColor.blue4)
            );

            // if light color didn't change (check is ignored if request is forced)
            if(!request.force && newLightColor.red4 == oldLightColor.red4 && newLightColor.green4 == oldLightColor.green4 && newLightColor.blue4 == oldLightColor.blue4) return true;
            addLightColorChange(request.blockPos, newLightColor);

            for(var direction : Direction.values()) {
                BlockPos neighbourPos = request.blockPos.relative(direction);
                if(!level.isInBounds(neighbourPos)) continue;
                BlockStateAccessor neighbourState = level.getBlockState(neighbourPos);
                if(neighbourState == null) return false; // section might have got unloaded and propagation should stop

                // light attenuation
                int lightBlocked = Math.max(1, neighbourState.getLightBlock(level, neighbourPos)); // vanilla light block
                ColorRGB4 coloredLightTransmittance = Config.getColoredLightTransmittance(level, neighbourPos, neighbourState); // rgb transmittance (example: red stained glass can let only red light through)
                ColorRGB4 neighbourLightColor = ColorRGB4.fromRGB4(
                        MathExt.clamp(request.lightColor.red4 - lightBlocked, 0, coloredLightTransmittance.red4),
                        MathExt.clamp(request.lightColor.green4 - lightBlocked, 0, coloredLightTransmittance.green4),
                        MathExt.clamp(request.lightColor.blue4 - lightBlocked, 0, coloredLightTransmittance.blue4)
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
        private void propagateDecreases(LevelAccessor level, Queue<LightUpdateRequest> decreaseRequests, Queue<LightUpdateRequest> increaseRequests) {
            while(!decreaseRequests.isEmpty()) {
                propagateDecrease(increaseRequests, decreaseRequests, decreaseRequests.poll(), level);
            }
        }

        private boolean propagateDecrease(Queue<LightUpdateRequest> increaseRequests, Queue<LightUpdateRequest> decreaseRequests, LightUpdateRequest request, LevelAccessor level) {
            ColorRGB4 oldLightColor = getLatestLightColor(request.blockPos);
            if(oldLightColor == null) return false; // section might have got unloaded and propagation should stop

            // if light color didn't change (check is ignored if request is forced)
            if(!request.force && oldLightColor.red4 == 0 && oldLightColor.green4 == 0 && oldLightColor.blue4 == 0) return true;
            addLightColorChange(request.blockPos, ColorRGB4.fromRGB4(0, 0, 0));

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

    private static class BlockRequests {
        public BlockPos blockPos;
        public Queue<LightUpdateRequest> increaseRequests = new LinkedList<>();

        public BlockRequests(BlockPos blockPos) {
            this.blockPos = blockPos;
        }
    }

    private static class LightUpdateRequest {
        BlockPos blockPos;
        ColorRGB4 lightColor;
        boolean force;
        boolean checkSource;

        public LightUpdateRequest(BlockPos blockPos, ColorRGB4 lightColor, boolean force) {
            this(blockPos, lightColor, force, false);
        }

        public LightUpdateRequest(BlockPos blockPos, ColorRGB4 lightColor, boolean force, boolean checkSource) {
            this.blockPos = blockPos;
            this.lightColor = lightColor;
            this.force = force;
            this.checkSource = checkSource;
        }
    }

    private record DelayedChunkUpdate(ChunkPos chunkPos, long executeTime) {}
}
