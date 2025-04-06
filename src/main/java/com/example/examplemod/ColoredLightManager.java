package com.example.examplemod;

import com.example.examplemod.util.ColorRGB4;
import com.example.examplemod.util.ColorRGB8;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
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
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ColoredLightManager {
    public ColoredLightStorage storage = new ColoredLightStorage();
    private Queue<LightUpdateRequest> propagateIncreases = new ConcurrentLinkedQueue<>();
    private Queue<LightUpdateRequest> propagateDecreases = new LinkedList<>();
    private Set<Long> dirtySections = new HashSet<>();
    private Queue<ChunkAccess> newChunks = new ConcurrentLinkedQueue<>();
    private Thread propagateLightInNewChunksThread;

    private static ColoredLightManager instance = new ColoredLightManager();
    public static ColoredLightManager getInstance() {
        return instance;
    }

    public ColoredLightManager() {
        propagateLightInNewChunksThread = new Thread(new PropagateLightInNewChunks());
        propagateLightInNewChunksThread.start();
    }

    public ColorRGB8 sampleLightColor(BlockPos pos) { return sampleLightColor(pos.getX(), pos.getY(), pos.getZ()); }
    public ColorRGB8 sampleLightColor(int x, int y, int z) {
        // TODO debug
        ClientLevel level = Minecraft.getInstance().level;
        if(level != null && level.isOutsideBuildHeight(y)) return ColorRGB8.fromRGB8(0, 0, 0);
        if(!storage.containsSection(SectionPos.blockToSection(BlockPos.asLong(x, y, z)))) return ColorRGB8.fromRGB8(0, 0, 0);

        var entry = storage.getEntry(x, y, z);
        return ColorRGB8.fromRGB4(entry);
    }

    public ColorRGB8 sampleMixedLightColor(Vector3f pos) {
        Vector3i cornerPos = new Vector3i((int)pos.x, (int)pos.y, (int)pos.z); // reject fraction
        int d = 0;
        ColorRGB8 finalColor = ColorRGB8.fromRGB8(0, 0, 0);
        for(int ox = -1; ox <= 0; ++ox) {
            for(int oy = -1; oy <= 0; ++oy) {
                for(int oz = -1; oz <= 0; ++oz) {
                    ColorRGB8 c = sampleLightColor(cornerPos.x + ox, cornerPos.y + oy, cornerPos.z + oz);
                    if(c.red == 0 && c.green == 0 && c.blue == 0) continue;
                    finalColor = finalColor.add(c);
                    ++d;
                }
            }
        }
        return d == 0 ? finalColor : finalColor.intDivide(d);
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
     * Used with increase = true, when originPos already has light color but neighbours need update.
     * Used with increase = false, when originPos doesn't already have any light color but neighbours need update.
     */
    /*private void requestLightRepropagation(BlockPos originPos, boolean increase) {
        ColorRGB4 lightColor = storage.getEntry(originPos);
        if(lightColor == null) return;
        if(increase) {
            setLightColor(originPos, ColorRGB4.fromRGB4(0, 0, 0)); // the color will be later updated by request
            propagateIncreases.add(new LightUpdateRequest(originPos, lightColor, true));
        }
        else {
            setLightColor(originPos, lightColor); // the color will be later updated by request
            propagateDecreases.add(new LightUpdateRequest(originPos, lightColor, true));
        }
    }*/

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

            if(!request.force && newLightColor.red4 == oldLightColor.red4 && newLightColor.green4 == oldLightColor.green4 && newLightColor.blue4 == oldLightColor.blue4) continue;
            setLightColor(request.blockPos, newLightColor);

            ColorRGB4 neighbourLightColor = ColorRGB4.fromRGB4(
                Math.max(0, request.lightColor.red4 - 1),
                Math.max(0, request.lightColor.green4 - 1),
                Math.max(0, request.lightColor.blue4 - 1)
            );
            if(neighbourLightColor.red4 == 0 && neighbourLightColor.green4 == 0 && neighbourLightColor.blue4 == 0) continue;

            for(var direction : Direction.values()) {
                BlockPos neighbourPos = request.blockPos.relative(direction);
                BlockState neighbourState = level.getBlockState(neighbourPos);
                if(neighbourState.getLightBlock(level, neighbourPos) > 0) continue;
                requestLightPropagation(neighbourPos, neighbourLightColor, true, false);
            }
        }
    }

    private void propagateDecreases(BlockGetter level) {
        while(!propagateDecreases.isEmpty()) {
            LightUpdateRequest request = propagateDecreases.poll();
            ColorRGB4 oldLightColor = storage.getEntry(request.blockPos);
            if(oldLightColor == null) continue;
            if(!request.force && oldLightColor.red4 == 0 && oldLightColor.green4 == 0 && oldLightColor.blue4 == 0) continue;
            setLightColor(request.blockPos, ColorRGB4.fromRGB4(0, 0, 0));

            BlockState blockState = level.getBlockState(request.blockPos);
            if(blockState.getLightEmission(level, request.blockPos) > 0) {
                requestLightPropagation(request.blockPos, Config.getEmissionColor(level, request.blockPos), true, false);
            }

            ColorRGB4 neighbourLightDecrease = ColorRGB4.fromRGB4(
                    Math.max(0, request.lightColor.red4 - 1),
                    Math.max(0, request.lightColor.green4 - 1),
                    Math.max(0, request.lightColor.blue4 - 1)
            );
            boolean decreaseMore = neighbourLightDecrease.red4 != 0 || neighbourLightDecrease.green4 != 0 || neighbourLightDecrease.blue4 != 0;

            for(var direction : Direction.values()) {
                BlockPos neighbourPos = request.blockPos.relative(direction);
                if(level.isOutsideBuildHeight(neighbourPos)) continue;

                if(decreaseMore)
                    requestLightPropagation(neighbourPos, neighbourLightDecrease, false, false);
                else {
                    ColorRGB4 neighbourLightColor = storage.getEntry(neighbourPos);
                    if(neighbourLightColor == null) continue;
                    if(neighbourLightColor.red4 == 0 && neighbourLightColor.green4 == 0 && neighbourLightColor.blue4 == 0)
                        continue;

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
            if(neighbourLight.red4 == 0 && neighbourLight.green4 == 0 && neighbourLight.blue4 == 0) continue;
            requestLightPropagation(neighbourPos, neighbourLight, true, true);
        }
    }

    public void onBlockLightPropertiesChanged(BlockGetter level, BlockPos blockPos) {
        BlockState blockState = level.getBlockState(blockPos);
        int lightEmission = blockState.getLightEmission(level, blockPos);
        ColorRGB4 currentColor = storage.getEntry(blockPos);
        if(currentColor == null) return;

        // TODO
        if(currentColor.red4 == 0 && currentColor.green4 == 0 && currentColor.blue4 == 0)
            requestLightPullIn(blockPos);
        else
            requestLightPropagation(blockPos, currentColor, false, false);

        if(lightEmission > 0)
            requestLightPropagation(blockPos, Config.getEmissionColor(level, blockPos), true, false);
    }

    public void runLightUpdates(BlockGetter level) {
        propagateDecreases(level);
        propagateIncreases(level);

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
        for(int i = 0; i < chunk.getSectionsCount(); i++) {
            int y = chunk.getSectionYFromSectionIndex(i);
            storage.addSection(SectionPos.asLong(chunk.getPos().x, y, chunk.getPos().z));
        }
        for(int x = -1; x <= 1; ++x) {
            for (int z = -1; z <= 1; ++z) {
                boolean allNeighboursLoaded = true;
                for(int x1 = -1; x1 <= 1; ++x1) {
                    for (int z1 = -1; z1 <= 1; ++z1) {
                        if(
                                !chunkSource.hasChunk(chunk.getPos().x + x + x1, chunk.getPos().z + z + z1) ||
                                !storage.containsSection(SectionPos.asLong(chunk.getPos().x + x + x1, chunk.getMinSection(), chunk.getPos().z + z + z1))
                        ) {
                            allNeighboursLoaded = false;
                            break;
                        }
                    }
                    if(!allNeighboursLoaded) break;
                }
                try {
                    if(allNeighboursLoaded)
                        newChunks.add(chunkSource.getChunk(chunk.getPos().x + x, chunk.getPos().z + z, false));
                }
                catch (Exception e) {
                    System.err.println(e.getMessage());
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
    }

    public void onLevelUnload() {
        newChunks.clear();
        storage.clear();
    }

    private static class LightUpdateRequest {
        BlockPos blockPos;
        ColorRGB4 lightColor; // light color to add or subtract
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
                propagateLight();
            }
        }

        private void propagateLight() {
            while (!newChunks.isEmpty()) {
                ChunkAccess chunk = newChunks.poll();

                chunk.findBlockLightSources((blockPos, blockState) -> {
                    requestLightPropagation(new BlockPos(blockPos), Config.getEmissionColor(chunk, blockPos), true, false);
                });
            }
        }
    }
}
