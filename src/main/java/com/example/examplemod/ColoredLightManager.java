package com.example.examplemod;

import com.example.examplemod.util.Color3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.joml.Vector3f;
import org.joml.Vector3i;

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
        if(!storage.containsLayer(SectionPos.blockToSection(BlockPos.asLong(x, y, z)))) return new Color3();

        return storage.getEntry(x, y, z).toColor3();
    }

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
