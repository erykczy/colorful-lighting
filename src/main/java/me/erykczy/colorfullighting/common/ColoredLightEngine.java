package me.erykczy.colorfullighting.common;

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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Class responsible for managing light color values in the client's world and sampling those values.
 * It propagates increases (e.g. new light source has been placed)
 * It propagates decreases (e.g. light source has been destroyed, solid block has been placed in the path of light)
 * It has a thread that finds light sources on newly loaded chunks and requests light propagation
 */
public class ColoredLightEngine {
    public ClientAccessor clientAccessor;
    private ColoredLightStorage storage = new ColoredLightStorage();
    private ConcurrentLinkedQueue<Long> dirtySections = new ConcurrentLinkedQueue<>();
    private LightPropagator lightPropagator;
    private ConcurrentLinkedQueue<ChunkPos> chunksWaitingForPropagation = new ConcurrentLinkedQueue<>();
    private ViewArea viewArea = new ViewArea();

    private static ColoredLightEngine instance;
    public static ColoredLightEngine getInstance() {
        return instance;
    }
    public static void create(ClientAccessor clientAccessor) {
        instance = new ColoredLightEngine(clientAccessor);
    }

    private ColoredLightEngine(ClientAccessor clientAccessor) {
        this.clientAccessor = clientAccessor;
        this.lightPropagator = new LightPropagator();
        this.lightPropagator.start();
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
        for(int x = viewArea.minX; x <= viewArea.maxX; ++x) {
            for(int z = viewArea.minZ; z <= viewArea.maxZ; ++z) {
                if(newArea.contains(x, z)) continue;
                // unload chunk
                for(int y = level.getMinSectionY(); y <= level.getMaxSectionY(); y++) {
                    storage.removeSection(SectionPos.asLong(x, y, z));
                }
            }
        }
        // load sections
        for(int x = newArea.minX; x <= newArea.maxX; ++x) {
            for(int z = newArea.minZ; z <= newArea.maxZ; ++z) {
                if(viewArea.containsInner(x, z)) continue;
                boolean viewAreaContainsOuter = viewArea.contains(x, z);
                // create section
                for(int y = level.getMinSectionY(); y <= level.getMaxSectionY(); y++) {
                    long pos = SectionPos.asLong(x, y, z);
                    if(!viewAreaContainsOuter)
                        storage.addSection(pos);
                }
                if(newArea.containsInner(x, z))
                    chunksWaitingForPropagation.add(new ChunkPos(x, z));
            }
        }
        viewArea = newArea;
    }

    private boolean isChunkAndNeighboursLoaded(int x, int z) {
        LevelAccessor level = clientAccessor.getLevel();
        if(level == null) return false;
        for(int ox = x - 1; ox <= x + 1; ++ox) {
            for(int oz = z - 1; oz <= z + 1; ++oz) {
                if(!storage.containsSection(SectionPos.asLong(ox, level.getMinSectionY(), oz)))
                    return false;
            }
        }
        return true;
    }

    public void onBlockLightPropertiesChanged(BlockPos blockPos) {
        LevelAccessor level = clientAccessor.getLevel();
        if(level == null) return;

        // light shouldn't be propagated when neighbour sections are not loaded, because propagation can affect neighbour sections
        // light will be propagated when neighbour chunks are loaded
        SectionPos sectionPos = SectionPos.of(blockPos);
        if(!isChunkAndNeighboursLoaded(sectionPos.x(), sectionPos.z())) return;
        ColorRGB4 lightColor = storage.getEntry(blockPos); // TODO what if some light is not propagated yet
        assert lightColor != null;

        LightUpdateRequestsGroup requestsGroup = new LightUpdateRequestsGroup();
        // TODO
        if(lightColor.red4 == 0 && lightColor.green4 == 0 && lightColor.blue4 == 0)
            requestLightPullIn(requestsGroup, blockPos);
        else
            requestsGroup.decreaseRequests.add(new LightUpdateRequest(blockPos, lightColor, false));

        // propagate light if new blockState emits light
        if(Config.getEmissionBrightness(level, blockPos) > 0)
            requestsGroup.increaseRequests.add(new LightUpdateRequest(blockPos, Config.getColorEmission(level, blockPos), false));

        lightPropagator.addRequestGroup(requestsGroup);
    }
    private void requestLightPullIn(LightUpdateRequestsGroup group, BlockPos blockPos) {
        for(var direction : Direction.values()) {
            BlockPos neighbourPos = blockPos.relative(direction);
            ColorRGB4 neighbourLight = storage.getEntry(neighbourPos);
            if(neighbourLight == null) continue;

            // if neighbour doesn't have any light
            if(neighbourLight.red4 == 0 && neighbourLight.green4 == 0 && neighbourLight.blue4 == 0) continue;
            group.increaseRequests.add(new LightUpdateRequest(neighbourPos, neighbourLight, true));
        }
    }
    public void onLightUpdate() {
        LevelAccessor level = clientAccessor.getLevel();
        if(level == null) return;

        lightPropagator.updateSections();
        // set all modified sections dirty
        var iterator = dirtySections.iterator();
        while (iterator.hasNext()) {
            SectionPos sectionPos = SectionPos.of(iterator.next());
            level.setSectionDirtyWithNeighbours(sectionPos.x(), sectionPos.y(), sectionPos.z());
            iterator.remove();
        }
    }
    public void onLevelUnload() {
        chunksWaitingForPropagation.clear();
        storage.clear();
        viewArea = new ViewArea();
    }
    public void onResourcesReload() {
        LevelAccessor level = clientAccessor.getLevel();
        if(level == null) return;
        chunksWaitingForPropagation.clear();
        storage.clear();
        ViewArea area = new ViewArea(viewArea);
        viewArea = new ViewArea();
        updateViewArea(area);
    }

    private class LightPropagator extends Thread {
        // light increase propagation requests
        private ConcurrentLinkedQueue<LightUpdateRequest> newSectionsIncreaseRequests = new ConcurrentLinkedQueue<>();
        private ConcurrentLinkedQueue<LightUpdateRequestsGroup> priorityRequestGroups = new ConcurrentLinkedQueue<>();
        private HashMap<BlockPos, ColorRGB4> lightChangesInProgress = new HashMap<>();
        private ConcurrentHashMap<BlockPos, ColorRGB4> lightChangesReadyToGo = new ConcurrentHashMap<>();
        //private AtomicInteger lightChangesReadyToGo = new AtomicInteger(0);

        /*public void requestLightPropagation(BlockPos originPos, ColorRGB4 lightColor, boolean increase, boolean force, boolean priority) {
            if(increase) {
                (priority ? priorityPropagateIncreases : propagateIncreases).add(new LightUpdateRequest(originPos, lightColor, force));
            }
            else {
                (priority ? priorityPropagateDecreases : propagateDecreases).add(new LightUpdateRequest(originPos, lightColor, force));
            }
        }*/

        public void updateSections() {
           /* assert lightChanges.size() >= lightChangesReadyToGo.get();
            int count = lightChangesReadyToGo.getAndSet(0);
            System.out.println("light changes: " + count);
            for(int i = 0; i < count; ++i) {
                LightChange change = lightChanges.poll();
                storage.setEntry(change.pos(), change.color());
                //dirtySections.add(SectionPos.asLong(change.pos()));
            }*/
        }

        public void addRequestGroup(LightUpdateRequestsGroup group) {
            priorityRequestGroups.add(group);
        }

        @Override
        public void run() {
            while (true) {
                //if(!InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_H)) continue;
                requestPropagationForWaitingSections();
                executePropagationRequests();
                try {
                    Thread.sleep(1); // TODO
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private void setLightColor(BlockPos blockPos, ColorRGB4 color) {
            lightChangesInProgress.put(blockPos, color);
            /*storage.setEntry(blockPos.getX(), blockPos.getY(), blockPos.getZ(), color);
            dirtySections.add(SectionPos.asLong(blockPos));*/
        }

        private ColorRGB4 getLightColor(BlockPos blockPos) {
            return lightChangesInProgress.getOrDefault(blockPos, storage.getEntry(blockPos));
        }

        private boolean requestPropagationForWaitingSections() {
            LevelAccessor level = clientAccessor.getLevel();
            if(level == null) return false;

            if (chunksWaitingForPropagation.isEmpty()) return false;

            PlayerAccessor playerAccessor = clientAccessor.getPlayer();
            if(playerAccessor == null) return false;

            // find chunk nearest player
            var iterator = chunksWaitingForPropagation.iterator();
            int minDistance = Integer.MAX_VALUE;
            ChunkPos nearestChunkPos = null;
            while (iterator.hasNext()) {
                ChunkPos chunkPos = iterator.next();
                if(!level.hasChunk(chunkPos)) continue;
                int distance = chunkPos.getChessboardDistance(playerAccessor.getPlayerChunkPos());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestChunkPos = chunkPos;
                }
            }
            if(nearestChunkPos == null) return false;
            // remove chunk from queue
            chunksWaitingForPropagation.remove(nearestChunkPos);

            // find light sources and request their propagation
            level.findLightSources(nearestChunkPos, (blockPos -> {
                newSectionsIncreaseRequests.add(new LightUpdateRequest(blockPos, Config.getColorEmission(level, blockPos), false));
            }));
            return true;
        }

        private void pushLightChanges() {
            /*lightChangesReadyToGo.putAll(lightChangesInProgress);
            lightChangesInProgress.clear();*/
            var it = lightChangesInProgress.entrySet().iterator();
            while(it.hasNext()) {
                var entry = it.next();
                it.remove();
                storage.setEntry(entry.getKey(), entry.getValue());
                dirtySections.add(SectionPos.asLong(entry.getKey()));
            }
        }

        private void executePropagationRequests() {
            LevelAccessor level = clientAccessor.getLevel();
            if(level == null) return;

            // handle increase and decrease requests
            while (!priorityRequestGroups.isEmpty() || !newSectionsIncreaseRequests.isEmpty()) {
                if(!priorityRequestGroups.isEmpty()) {
                    LightUpdateRequestsGroup group = priorityRequestGroups.poll();
                    propagateDecreases(group.decreaseRequests, group.increaseRequests);
                    propagateIncreases(group.increaseRequests);
                    pushLightChanges();
                }
                if(!newSectionsIncreaseRequests.isEmpty()) {
                    propagateIncrease(newSectionsIncreaseRequests, newSectionsIncreaseRequests.poll(), level);
                    //System.out.println(newSectionsIncreaseRequests.size());
                }
            }
            pushLightChanges();

            /*var it = lightChangesReadyToGo.entrySet().iterator();
            while(it.hasNext()) {
                var entry = it.next();
                it.remove();
                storage.setEntry(entry.getKey(), entry.getValue());
                dirtySections.add(SectionPos.asLong(entry.getKey()));
            }*/

            /*try {
                assert lightChanges.size() >= lightChangesReadyToGo.get();
                int count = lightChangesReadyToGo.getAndSet(0);
                System.out.println("light changes: " + count);
                var iterator = lightChanges.entrySet().iterator();
                for(int i = 0; i < count; ++i) {
                    var entry = iterator.next();
                    iterator.remove(); // TODO concurrency
                    storage.setEntry(entry.getKey(), entry.getValue());
                    dirtySections.add(SectionPos.asLong(entry.getKey()));
                }
            }
            catch (NoSuchElementException exception) {
                throw exception;
            }*/

        }

        /**
         * Handles all increase propagation requests.
         */
        private int propagateIncreases(Queue<LightUpdateRequest> requests) {
            LevelAccessor level = clientAccessor.getLevel();
            if(level == null) return 0;
            int changes = 0;
            while(!requests.isEmpty()) {
                changes += propagateIncrease(requests, requests.poll(), level);
            }
            return changes;
        }

        private int propagateIncrease(Queue<LightUpdateRequest> increaseRequests, LightUpdateRequest request, LevelAccessor level) {
            ColorRGB4 oldLightColor = getLightColor(request.blockPos);
            if(oldLightColor == null) return 0; // if storage doesn't contain request.blockPos
            ColorRGB4 newLightColor = ColorRGB4.fromRGB4(
                    Math.max(oldLightColor.red4, request.lightColor.red4),
                    Math.max(oldLightColor.green4, request.lightColor.green4),
                    Math.max(oldLightColor.blue4, request.lightColor.blue4)
            );

            // if light color didn't change (check is ignored if request is forced)
            if(!request.force && newLightColor.red4 == oldLightColor.red4 && newLightColor.green4 == oldLightColor.green4 && newLightColor.blue4 == oldLightColor.blue4) return 0;
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

                increaseRequests.add(new LightUpdateRequest(neighbourPos, neighbourLightColor, false));
            }
            return 1;
        }

        /**
         * Handles all decrease propagation requests.
         */
        private int propagateDecreases(Queue<LightUpdateRequest> decreaseRequests, Queue<LightUpdateRequest> increaseRequests) {
            LevelAccessor level = clientAccessor.getLevel();
            if(level == null) return 0;
            int changes = 0;
            while(!decreaseRequests.isEmpty()) {
                changes += propagateDecrease(increaseRequests, decreaseRequests, decreaseRequests.poll(), level);
            }
            return changes;
        }

        private int propagateDecrease(Queue<LightUpdateRequest> increaseRequests, Queue<LightUpdateRequest> decreaseRequests, LightUpdateRequest request, LevelAccessor level) {
            ColorRGB4 oldLightColor = getLightColor(request.blockPos);
            if(oldLightColor == null) return 0; // if storage doesn't contain request.blockPos

            // if light color didn't change (check is ignored if request is forced)
            if(!request.force && oldLightColor.red4 == 0 && oldLightColor.green4 == 0 && oldLightColor.blue4 == 0) return 0;
            setLightColor(request.blockPos, ColorRGB4.fromRGB4(0, 0, 0));

            // repropagate removed light
            if(Config.getEmissionBrightness(level, request.blockPos) > 0) {
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
                    ColorRGB4 neighbourLightColor = getLightColor(neighbourPos);
                    if(neighbourLightColor == null) continue;
                    // if neighbour doesn't have any light
                    if(neighbourLightColor.red4 == 0 && neighbourLightColor.green4 == 0 && neighbourLightColor.blue4 == 0)
                        continue;

                    // force neighbour to propagate light to the region that has been just cleared (decreased)
                    increaseRequests.add(new LightUpdateRequest(neighbourPos, neighbourLightColor, true));
                }
            }
            return 1;
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

    private static class LightUpdateRequestsGroup {
        public ConcurrentLinkedQueue<LightUpdateRequest> increaseRequests = new ConcurrentLinkedQueue<>();
        public ConcurrentLinkedQueue<LightUpdateRequest> decreaseRequests = new ConcurrentLinkedQueue<>();
    }
}
