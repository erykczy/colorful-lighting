package com.example.examplemod;

import com.example.examplemod.util.FastColor3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.lighting.LevelLightEngine;

import java.util.concurrent.ConcurrentHashMap;

public class ColoredLightStorage {
    private ConcurrentHashMap<Long, ColoredLightLayer>  map = new ConcurrentHashMap<>(); //Long2ObjectOpenHashMap<ColoredLightLayer>

    public ColoredLightLayer getLayer(long sectionPos) {
        return map.get(sectionPos);
    }

    public boolean containsLayer(long sectionPos) {
        return map.containsKey(sectionPos);
    }

    public FastColor3 getLightColor(int x, int y, int z) {
        long sectionPos = SectionPos.blockToSection(BlockPos.asLong(x, y, z));
        return getLayer(sectionPos).get(
                SectionPos.sectionRelative(x),
                SectionPos.sectionRelative(y),
                SectionPos.sectionRelative(z)
        );
    }

    public void setLightColor(int x, int y, int z, FastColor3 value) {
        long sectionPos = SectionPos.blockToSection(BlockPos.asLong(x, y, z));
        getLayer(sectionPos).set(
                SectionPos.sectionRelative(x),
                SectionPos.sectionRelative(y),
                SectionPos.sectionRelative(z),
                value
        );
    }

    public void initializeSection(long sectionPos, LevelLightEngine engine) {
        if(!containsLayer(sectionPos)) {
            ColoredLightLayer layer = new ColoredLightLayer();
            map.put(sectionPos, layer);
        }
    }

    public void removeSection(long sectionPos) {
        //map.remove(sectionPos);
        if(containsLayer(sectionPos))
            getLayer(sectionPos).clear();
    }
}
