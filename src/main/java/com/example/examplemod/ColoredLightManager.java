package com.example.examplemod;

import com.example.examplemod.mixin.SectionCompilerMixin;
import com.example.examplemod.util.Color3;
import com.example.examplemod.util.FastColor3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.LightEngine;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ColoredLightManager {
    public ColoredLightStorage storage = new ColoredLightStorage();
    public Queue<FastColor3> increaseQueue = new ConcurrentLinkedQueue<>();
    public Queue<FastColor3> decreaseQueue = new ConcurrentLinkedQueue<>();
    public Queue<ChunkPos> newChunks = new ConcurrentLinkedQueue<>();

    private static ColoredLightManager instance = new ColoredLightManager();
    public static ColoredLightManager getInstance() {
        return instance;
    }

    /*public void updateSection(SectionPos pos, boolean isEmpty) {
        storage.updateSection(pos.asLong());
    }*/

    public void enqueueIncrease(FastColor3 color) {
        assert color != null;
        increaseQueue.add(color);
    }

    public void enqueueDecrease(FastColor3 color) {
        assert color != null;
        decreaseQueue.add(color);
    }

    public FastColor3 getBlockStateColor(BlockState state) {
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
        if(!storage.containsLayer(SectionPos.blockToSection(BlockPos.asLong(x, y, z))))
            return new Color3();
        return new Color3(storage.getLightColor(x, y, z));
    }
    public Color3 sampleLightColor(BlockPos pos) { return sampleLightColor(pos.getX(), pos.getY(), pos.getZ()); }

    public Color3 sampleMixedLightColor(Vector3f pos) {
        Vector3i cornerPos = new Vector3i((int)pos.x, (int)pos.y, (int)pos.z); // reject fraction

        Color3 finalColor = new Color3();
        for(int ox = -1; ox < 1; ++ox) {
            for(int oy = -1; oy < 1; ++oy) {
                for(int oz = -1; oz < 1; ++oz) {
                    Color3 c = sampleLightColor(cornerPos.x + ox, cornerPos.y + oy, cornerPos.z + oz);
                    finalColor = finalColor.add(c);
                }
            }
        }
        return finalColor.intDivide(8);
    }

    public void propagateLight(BlockLightEngine blockEngine, int chunkX, int chunkZ) {
        LightChunk chunk = blockEngine.chunkSource.getChunkForLighting(chunkX, chunkZ);
        chunk.findBlockLightSources(((blockPos, blockState) -> {
            // might not store light because updateSectionStatus function might be called when chunks are being unloaded
            if(!blockEngine.storage.storingLightForSection(SectionPos.blockToSection(blockPos.asLong())))
                return;
            // when chunk is first loaded light data is available
            // if chunk is loaded the second time light data is available later than this method is called

            // remove light
            blockEngine.enqueueDecrease(blockPos.asLong(), LightEngine.QueueEntry.decreaseAllDirections(blockState.getLightEmission(chunk, blockPos)));
            try {
                blockEngine.storage.setStoredLevel(blockPos.asLong(), 0);
            }
            catch(Exception e) {
                System.out.println(e.getMessage());
            }
            // revert light
            blockEngine.checkBlock(blockPos);
        }));
    }

    // should be called on light thread
    public void handleNewChunks(BlockLightEngine engine) {
        synchronized (ColoredLightManager.class) {
            while(!ColoredLightManager.getInstance().newChunks.isEmpty()) { // TODO
                ChunkPos chunkPos = ColoredLightManager.getInstance().newChunks.poll();
                LightChunk chunk = engine.chunkSource.getChunkForLighting(chunkPos.x, chunkPos.z);
                if(chunk == null) continue;

                for(int j = 0; j < chunk.getSectionsCount(); j++) {
                    long sectionPos = SectionPos.of(chunkPos, chunk.getMinSection() + j).asLong();
                    // if(!engine.storage.storingLightForSection(sectionPos)) continue;
                    ColoredLightManager.getInstance().storage.initializeSection(sectionPos); // note that empty sections are also initialized (while vanilla's light sections are not)
                }

                ColoredLightManager.getInstance().propagateLight(engine, chunkPos.x, chunkPos.z);
            }
        }
    }
}
