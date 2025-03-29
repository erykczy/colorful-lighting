package com.example.examplemod;

import com.example.examplemod.util.Color3;
import com.example.examplemod.util.FastColor3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LightEngine;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ColoredLightManager {
    public ColoredLightStorage storage = new ColoredLightStorage();
    public Queue<FastColor3> increaseQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<Long> propagateLightChunks = new ConcurrentLinkedQueue<>();

    private static ColoredLightManager instance = new ColoredLightManager();
    public static ColoredLightManager getInstance() {
        return instance;
    }

    public void enqueueIncrease(FastColor3 color) {
        assert color != null;
        increaseQueue.add(color);
    }

    public FastColor3 getEmissionColor(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if(state.is(Blocks.GLOWSTONE) || state.is(Blocks.REDSTONE_LAMP))
            return new FastColor3((byte)255, (byte)0, (byte)0);
        else if(state.is(Blocks.VERDANT_FROGLIGHT))
            return new FastColor3((byte)0, (byte)255, (byte)0);
        else if(state.is(Blocks.SEA_LANTERN))
            return new FastColor3((byte)0, (byte)50, (byte)200);
        else
            return new FastColor3((byte)0, (byte)0, (byte)255);
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

    // should be called on light thread
    public void queuePropagateLight(long chunkPos) {
        if(!propagateLightChunks.contains(chunkPos))
            propagateLightChunks.add(chunkPos);
    }

    public void propagateLight(BlockLightEngine blockEngine) {
        while(!propagateLightChunks.isEmpty()) {
            ChunkPos chunkPos = new ChunkPos(propagateLightChunks.poll());
            LightChunk chunk = blockEngine.chunkSource.getChunkForLighting(chunkPos.x, chunkPos.z);
            if(chunk == null) continue;

            chunk.findBlockLightSources(((blockPos, blockState) -> {
                // chunk might not have light data
                if(!blockEngine.storage.storingLightForSection(SectionPos.blockToSection(blockPos.asLong()))) return;

                // queue light removal
                blockEngine.enqueueDecrease(blockPos.asLong(), LightEngine.QueueEntry.decreaseAllDirections(blockState.getLightEmission(chunk, blockPos)));
                // queue light revert
                blockEngine.checkBlock(blockPos);

                blockEngine.storage.setStoredLevel(blockPos.asLong(), 0);
            }));
        }
    }

    // should be called on light thread
    public void handleSectionUpdate(BlockLightEngine engine, SectionPos pos, LayerLightSectionStorage.SectionType ss) {
        for(int x = -1; x <= 1; ++x) {
            for(int z = -1; z <= 1; ++z) {
                LightChunk chunk = engine.chunkSource.getChunkForLighting(pos.x(), pos.z());
                if(chunk == null) continue;

                for(int y = -1; y <= 1; ++y) {
                    long sectionPos = pos.offset(x, y, z).asLong();
                    LayerLightSectionStorage.SectionType sectionStatus = engine.storage.getDebugSectionType(sectionPos);

                    if(sectionStatus == LayerLightSectionStorage.SectionType.EMPTY) {
                        ColoredLightManager.getInstance().storage.removeSection(sectionPos);
                    }
                    else {
                        ColoredLightManager.getInstance().storage.initializeSection(sectionPos);
                    }
                }

                ColoredLightManager.getInstance().queuePropagateLight(ChunkPos.asLong(pos.x() + x, pos.z() + z));
            }
        }
    }
}
