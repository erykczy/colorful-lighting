package com.example.examplemod;

import com.example.examplemod.util.Color3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;
import org.joml.Vector3i;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class ColoredLightManager {
    public ColoredLightStorage storage = new ColoredLightStorage();

    private static ColoredLightManager instance = new ColoredLightManager();
    public static ColoredLightManager getInstance() {
        return instance;
    }

    public ColoredLightManager() {

    }

    public Color3 sampleLightColor(BlockPos pos) { return sampleLightColor(pos.getX(), pos.getY(), pos.getZ()); }
    public Color3 sampleLightColor(int x, int y, int z) {
        // TODO debug
        ClientLevel level = Minecraft.getInstance().level;
        if(level != null && level.isOutsideBuildHeight(y)) return new Color3();
        if(!storage.containsSection(SectionPos.blockToSection(BlockPos.asLong(x, y, z)))) return new Color3();

        var entry = storage.getEntry(x, y, z);
        return entry.toColor3();
    }

    public Color3 sampleMixedLightColor(Vector3f pos) {
        Vector3i cornerPos = new Vector3i((int)pos.x, (int)pos.y, (int)pos.z); // reject fraction
        int d = 0;
        Color3 finalColor = new Color3();
        for(int ox = -1; ox < 1; ++ox) {
            for(int oy = -1; oy < 1; ++oy) {
                for(int oz = -1; oz < 1; ++oz) {
                    Color3 c = sampleLightColor(cornerPos.x + ox, cornerPos.y + oy, cornerPos.z + oz);
                    if(c.red == 0 && c.green == 0 && c.blue == 0) continue;
                    finalColor = finalColor.add(c);
                    ++d;
                }
            }
        }
        return d == 0 ? finalColor : finalColor.intDivide(d);
    }

    public void propagateLight(BlockGetter level, BlockPos originPos, boolean propagate, @Nullable BlockPos ignore) {
        Color3 emissionColor = Config.getEmissionColor(level, originPos);
        if(level.getBlockState(originPos).is(Blocks.GLOWSTONE))
            emissionColor = new Color3(255, 128, 0);
        int vanillaEmission = level.getLightEmission(originPos);

        Queue<BlockPos> blocksToUpdate = new LinkedList<>();
        Queue<Integer> lightLevels = new LinkedList<>();
        Set<Long> alreadyUpdated = new HashSet<>();
        blocksToUpdate.add(originPos);
        lightLevels.add(vanillaEmission);

        do {
            BlockPos blockPos = blocksToUpdate.poll();
            int lightLevel = lightLevels.poll();

            if(alreadyUpdated.contains(blockPos.asLong())) continue;
            if(blockPos != originPos && !blockPos.equals(ignore) && !level.getBlockState(blockPos).isAir()) continue;

            float distance = 1.0f - lightLevel / (float)vanillaEmission;
            float attenuation = 1.0f - distance;
            Color3 lightColorToAdd = ColoredLightLayer.Entry.create(emissionColor.mul(attenuation)).toColor3();
            /*Color3 lightColorToAdd = new Color3(
                    emissionColor.red/255.0f * (float)Math.pow(0.8f, vanillaEmission - lightLevel),
                    emissionColor.green/255.0f * (float)Math.pow(0.8f, vanillaEmission - lightLevel),
                    emissionColor.blue/255.0f * (float)Math.pow(0.8f, vanillaEmission - lightLevel)
            );*/
            try {
                var currentEntry = storage.getEntry(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                var currentLightColor = currentEntry.toColor3();
                //var currentCount = currentEntry.getCount();
                Color3 newLightColor = currentLightColor;
                if(propagate) {
                    newLightColor = currentLightColor.add(lightColorToAdd);
                    /*newLightColor = new Color3(
                            Math.max(currentLightColor.red, lightColorToAdd.red),
                            Math.max(currentLightColor.green, lightColorToAdd.green),
                            Math.max(currentLightColor.blue, lightColorToAdd.blue)
                    );*/
                }
                else {
                    newLightColor = currentLightColor.sub(lightColorToAdd);
                }
                storage.setEntry(blockPos.getX(), blockPos.getY(), blockPos.getZ(), ColoredLightLayer.Entry.create(newLightColor));
            }
            catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
            }
            alreadyUpdated.add(blockPos.asLong());

            if(lightLevel <= 1) continue;
            for(var direction : Direction.values()) {
                blocksToUpdate.add(blockPos.relative(direction));
                lightLevels.add(lightLevel - 1);
            }
        }
        while (!blocksToUpdate.isEmpty());
    }

    public void blockLights(BlockAndTintGetter world, BlockPos originPos) {
        //int originLightLevel = world.getBrightness(LightLayer.BLOCK, originPos);
        storage.setEntry(originPos.getX(), originPos.getY(), originPos.getZ(), ColoredLightLayer.Entry.create(0, 0, 0));

        /*for(int oy = -14; oy <= 14; ++oy) {
            int xExtent = 14-Math.abs(oy);
            int xWidth = xExtent * 2 + 1;
            for(int ox = -xExtent; ox <= xExtent; ++ox) {
                int zExtent = xExtent - Math.abs(ox);
            }
        }*/

        for(int x = -14; x <= 14; ++x) {
            for(int y = -14; y <= 14; ++y) {
                for(int z = -14; z <= 14; ++z) {
                    BlockPos blockPos = originPos.offset(x, y, z);
                    if(world.getLightEmission(blockPos) > 0) {
                        propagateLight(world, blockPos, false, originPos);
                        propagateLight(world, blockPos, true, null);
                    }
                }
            }
        }

        /*Queue<BlockPos> toCheck = new LinkedList<>();
        toCheck.add(originPos);
        do {
            BlockPos currentPos = toCheck.poll();
            if(world.getLightEmission(currentPos) > 0) {
                propagateLight(world, currentPos, false, currentPos);
                propagateLight(world, currentPos, true, null);
            }
            int currentLightLevel = world.getBrightness(LightLayer.BLOCK, currentPos);
            if(currentLightLevel >= 15) continue;
            for(var direction : Direction.values()) {
                BlockPos neighbourPos = currentPos.relative(direction);
                int neighbourLightLevel = world.getBrightness(LightLayer.BLOCK, neighbourPos);

                if(neighbourLightLevel > currentLightLevel)
                    toCheck.add(neighbourPos);
            }
        }
        while (!toCheck.isEmpty());*/
    }

    /*public void findBlockLightSources(BlockLightEngine blockEngine) {
        if(propagateLightChunks.isEmpty()) return;
        if(!propagateLightBlocks.isEmpty()) return;

        if(Minecraft.getInstance().player == null) return;
        ChunkPos playerChunkPos = Minecraft.getInstance().player.chunkPosition();

        var iterator = propagateLightChunks.iterator();
        int minDistance = Integer.MAX_VALUE;
        ChunkPos nearestChunkPos = new ChunkPos(0, 0);
        while(iterator.hasNext()) {
            ChunkPos pos = new ChunkPos(iterator.next());
            if(blockEngine.chunkSource.getChunkForLighting(pos.x, pos.z) == null) { // TODO temporary (I hope) solution
                iterator.remove();
                continue;
            }
            int distance = Math.abs(pos.x - playerChunkPos.x) + Math.abs(pos.z - playerChunkPos.z);
            if(distance < minDistance) {
                minDistance = distance;
                nearestChunkPos = pos;
            }
        }
        propagateLightChunks.remove(nearestChunkPos.toLong());
        LightChunk chunk = blockEngine.chunkSource.getChunkForLighting(nearestChunkPos.x, nearestChunkPos.z);
        if(chunk == null)
            return;

        chunk.findBlockLightSources(((blockPos, blockState) -> {
            propagateLightBlocks.add(blockPos.asLong());
        }));

        //}
    }*/

    /*public void propagateLight(BlockLightEngine blockEngine) {
        //int i = 400;
        System.out.println("t: "+propagateLightBlocks.size());
        while (!propagateLightBlocks.isEmpty()){
            //if(--i < 0) return;
            BlockPos blockPos = BlockPos.of(propagateLightBlocks.poll());
            ChunkPos chunkPos = new ChunkPos(blockPos);
            LightChunk chunk = blockEngine.chunkSource.getChunkForLighting(chunkPos.x, chunkPos.z);
            if(chunk == null)
                continue;
            BlockState blockState = chunk.getBlockState(blockPos);

            // chunk might not have light data
            if(!blockEngine.storage.storingLightForSection(SectionPos.blockToSection(blockPos.asLong()))) continue;

            // queue light removal
            //blockEngine.enqueueDecrease(blockPos.asLong(), LightEngine.QueueEntry.decreaseAllDirections(blockState.getLightEmission(chunk, blockPos)));
            // queue light revert
            blockEngine.checkBlock(blockPos);

            ///blockEngine.storage.setStoredLevel(blockPos.asLong(), 0);
        }
    }*/

    /*public void handleSectionUpdate(BlockLightEngine engine, SectionPos thisSectionPos, LayerLightSectionStorage.SectionType ss) {
        int minSection = engine.chunkSource.getLevel().getMinSection();
        int maxSection = engine.chunkSource.getLevel().getMaxSection();

        for(int x = -1; x <= 1; ++x) {
            for(int z = -1; z <= 1; ++z) {
                boolean anyAlreadyAvailable = false;
                for(int y = -1; y <= 1; ++y) {
                    if(thisSectionPos.y() < minSection || thisSectionPos.y() > maxSection) continue;
                    long checkedSectionPos = thisSectionPos.offset(x, y, z).asLong();
                    LayerLightSectionStorage.SectionType checkedSectionStatus = engine.storage.getDebugSectionType(checkedSectionPos);

                    if(storage.containsLayer(checkedSectionPos)) {
                        anyAlreadyAvailable = true;
                        continue;
                    }

                    if(checkedSectionStatus == LayerLightSectionStorage.SectionType.EMPTY) {
                        ColoredLightManager.getInstance().storage.removeSection(checkedSectionPos);
                        ColoredLightManager.getInstance().dequeuePropagateLight(checkedSectionPos);
                    }
                    else {
                        ColoredLightManager.getInstance().storage.initializeSection(checkedSectionPos);
                    }
                }

                if(!anyAlreadyAvailable)
                    ColoredLightManager.getInstance().queuePropagateLight(ChunkPos.asLong(thisSectionPos.x() + x, thisSectionPos.z() + z));
            }
        }
    }*/
}
