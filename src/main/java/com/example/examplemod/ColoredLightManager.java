package com.example.examplemod;

import com.example.examplemod.util.Color3;
import com.example.examplemod.util.FastColor3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LightEngine;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ColoredLightManager {
    public ColoredLightStorage storage = new ColoredLightStorage();
    public Queue<FastColor3> increaseQueue = new ConcurrentLinkedQueue<>();
    public Queue<Long> propagateLightChunks = new ConcurrentLinkedQueue<>();
    public Queue<Long> propagateLightBlocks = new ConcurrentLinkedQueue<>();
    public static HashMap<Block, Color3> emissionColors = new HashMap<>();
    private final Thread thread;

    static {
        emissionColors.put(Blocks.BEACON, new Color3(0.1f, 0.1f, 1.0f));
        emissionColors.put(Blocks.FIRE, new Color3(0.9f, 0.1f, 0.1f));
        emissionColors.put(Blocks.LAVA, new Color3(0.9f, 0.1f, 0.1f));
        emissionColors.put(Blocks.GLOWSTONE, new Color3(0.6f, 0.3f, 0.1f));
        emissionColors.put(Blocks.MAGMA_BLOCK, new Color3(0.9f, 0.1f, 0.1f));
        emissionColors.put(Blocks.LAVA_CAULDRON, new Color3(0.9f, 0.1f, 0.1f));
        emissionColors.put(Blocks.SHROOMLIGHT, new Color3(0.9f, 0.1f, 0.1f));
        emissionColors.put(Blocks.REDSTONE_LAMP, new Color3(0.9f, 0.8f, 0.8f));
        emissionColors.put(Blocks.SEA_LANTERN, new Color3(0.0f, 0.4f, 1.0f));
        emissionColors.put(Blocks.CAVE_VINES, new Color3(0.0f, 1.0f, 0.0f));
        emissionColors.put(Blocks.NETHER_PORTAL, new Color3(1.0f, 0.0f, 1.0f));
        emissionColors.put(Blocks.RESPAWN_ANCHOR, new Color3(1.0f, 0.0f, 1.0f));
        emissionColors.put(Blocks.ENCHANTING_TABLE, new Color3(1.0f, 0.0f, 1.0f));
        emissionColors.put(Blocks.AMETHYST_CLUSTER, new Color3(1.0f, 0.0f, 1.0f));
        emissionColors.put(Blocks.LARGE_AMETHYST_BUD, new Color3(1.0f, 0.0f, 1.0f));
        emissionColors.put(Blocks.CRYING_OBSIDIAN, new Color3(1.0f, 0.0f, 1.0f));
        emissionColors.put(Blocks.SOUL_CAMPFIRE, new Color3(0.2f, 0.3f, 1.0f));
        emissionColors.put(Blocks.SOUL_FIRE, new Color3(0.2f, 0.3f, 1.0f));
        emissionColors.put(Blocks.SOUL_LANTERN, new Color3(0.2f, 0.3f, 1.0f));
        emissionColors.put(Blocks.SOUL_TORCH, new Color3(0.2f, 0.3f, 1.0f));
        emissionColors.put(Blocks.SOUL_WALL_TORCH, new Color3(0.2f, 0.3f, 1.0f));
        emissionColors.put(Blocks.REDSTONE_TORCH, new Color3(1.0f, 0.0f, 0.0f));
        emissionColors.put(Blocks.REDSTONE_WALL_TORCH, new Color3(1.0f, 0.0f, 0.0f));
        emissionColors.put(Blocks.OCHRE_FROGLIGHT, new Color3(1.0f, 1.0f, 0.0f));
        emissionColors.put(Blocks.VERDANT_FROGLIGHT, new Color3(0.0f, 1.0f, 0.0f));
        emissionColors.put(Blocks.PEARLESCENT_FROGLIGHT, new Color3(1.0f, 0.0f, 1.0f));
        emissionColors.put(Blocks.LIME_CANDLE, new Color3(0.0f, 1.0f, 0.0f));
    }

    private static ColoredLightManager instance = new ColoredLightManager();
    public static ColoredLightManager getInstance() {
        return instance;
    }

    public ColoredLightManager() {
        thread = new Thread() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(20);
                    }
                    catch (Exception e) {
                        System.err.println(e.getMessage());
                        Thread.currentThread().interrupt();
                        break;
                    }
                    ClientLevel clientLevel = Minecraft.getInstance().level;
                    if(clientLevel == null) continue;
                    findBlockLightSources((BlockLightEngine) clientLevel.getLightEngine().blockEngine);
                }
            }
        };
        thread.start();
    }

    public void enqueueIncrease(FastColor3 color) {
        increaseQueue.add(color);
    }

    public FastColor3 getEmissionColor(BlockGetter level, BlockPos pos) {
        BlockState state;
        if(level == null)
            state = Blocks.BEDROCK.defaultBlockState();
        else
            state = level.getBlockState(pos);

        if(emissionColors.containsKey(state.getBlock())) {
            return new FastColor3(emissionColors.get(state.getBlock()));
        }
        else
            return new FastColor3((byte)255, (byte)255, (byte)255);
    }

    public Color3 sampleLightColor(int x, int y, int z) {
        // TODO debug
        ClientLevel level = Minecraft.getInstance().level;
        if(level != null) {
            if(level.isOutsideBuildHeight(y))
                return new Color3();
        }
        if(!storage.containsLayer(SectionPos.blockToSection(BlockPos.asLong(x, y, z))))
            return new Color3();
        return new Color3(storage.getLightColor(x, y, z));
    }
    public Color3 sampleLightColor(BlockPos pos) { return sampleLightColor(pos.getX(), pos.getY(), pos.getZ()); }

    public Color3 sampleMixedLightColor(Vector3f pos) {
        Vector3i cornerPos = new Vector3i((int)pos.x, (int)pos.y, (int)pos.z); // reject fraction
        int d = 0;
        Color3 finalColor = new Color3();
        for(int ox = -1; ox < 1; ++ox) {
            for(int oy = -1; oy < 1; ++oy) {
                for(int oz = -1; oz < 1; ++oz) {
                    Color3 c = sampleLightColor(cornerPos.x + ox, cornerPos.y + oy, cornerPos.z + oz);
                    if(c.red == 0 && c.green == 0 && c.blue == 0) continue;;
                    finalColor = finalColor.add(c);
                    ++d;
                }
            }
        }
        return d == 0 ? finalColor : finalColor.intDivide(d);
    }

    public void queuePropagateLight(long chunkPos) {
        if(!propagateLightChunks.contains(chunkPos))
            propagateLightChunks.add(chunkPos);
    }

    public void dequeuePropagateLight(long chunkPos) {
        propagateLightChunks.remove(chunkPos);
    }

    public void findBlockLightSources(BlockLightEngine blockEngine) {
        if(propagateLightChunks.isEmpty()) return;
        if(!propagateLightBlocks.isEmpty()) return;

        if(Minecraft.getInstance().player == null) return;
        ChunkPos playerChunkPos = Minecraft.getInstance().player.chunkPosition();

        var iterator = propagateLightChunks.iterator();
        int minDistance = Integer.MAX_VALUE;
        ChunkPos nearestChunkPos = new ChunkPos(0, 0);
        while(iterator.hasNext()) {
            ChunkPos pos = new ChunkPos(iterator.next());
            int distance = Math.abs(pos.x - playerChunkPos.x) + Math.abs(pos.z - playerChunkPos.z);
            if(distance < minDistance) {
                minDistance = distance;
                nearestChunkPos = pos;
            }
        }
        propagateLightChunks.remove(nearestChunkPos.toLong());
        LightChunk chunk = blockEngine.chunkSource.getChunkForLighting(nearestChunkPos.x, nearestChunkPos.z);
        if(chunk == null) return;

        chunk.findBlockLightSources(((blockPos, blockState) -> {
            propagateLightBlocks.add(blockPos.asLong());
        }));

        //}
    }

    public void propagateLight(BlockLightEngine blockEngine) {
        //int i = 400;
        while (!propagateLightBlocks.isEmpty()) {
            //if(--i < 0) return;
            BlockPos blockPos = BlockPos.of(propagateLightBlocks.poll());
            ChunkPos chunkPos = new ChunkPos(blockPos);
            LightChunk chunk = blockEngine.chunkSource.getChunkForLighting(chunkPos.x, chunkPos.z);
            if(chunk == null) continue;
            BlockState blockState = chunk.getBlockState(blockPos);

            // chunk might not have light data
            if(!blockEngine.storage.storingLightForSection(SectionPos.blockToSection(blockPos.asLong()))) continue;

            // queue light removal
            blockEngine.enqueueDecrease(blockPos.asLong(), LightEngine.QueueEntry.decreaseAllDirections(blockState.getLightEmission(chunk, blockPos)));
            // queue light revert
            blockEngine.checkBlock(blockPos);

            blockEngine.storage.setStoredLevel(blockPos.asLong(), 0);
        }
    }

    public void handleSectionUpdate(BlockLightEngine engine, SectionPos pos, LayerLightSectionStorage.SectionType ss) {
        for(int x = -1; x <= 1; ++x) {
            for(int z = -1; z <= 1; ++z) {
                boolean anyAlreadyAvailable = false;
                for(int y = -1; y <= 1; ++y) {
                    long sectionPos = pos.offset(x, y, z).asLong();
                    LayerLightSectionStorage.SectionType sectionStatus = engine.storage.getDebugSectionType(sectionPos);

                    if(storage.containsLayer(SectionPos.blockToSection(sectionPos))) {
                        anyAlreadyAvailable = true;
                        continue;
                    }

                    if(sectionStatus == LayerLightSectionStorage.SectionType.EMPTY) {
                        ColoredLightManager.getInstance().storage.removeSection(sectionPos);
                        ColoredLightManager.getInstance().dequeuePropagateLight(sectionPos);
                    }
                    else {
                        ColoredLightManager.getInstance().storage.initializeSection(sectionPos);
                    }
                }

                if(!anyAlreadyAvailable)
                    ColoredLightManager.getInstance().queuePropagateLight(ChunkPos.asLong(pos.x() + x, pos.z() + z));
            }
        }
    }
}
