package com.example.examplemod;

import com.example.examplemod.util.FastColor3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;

public class ColoredLightStorage {
    private ConcurrentHashMap<Long, ColoredLightLayer> map = new ConcurrentHashMap<>(); //Long2ObjectOpenHashMap<ColoredLightLayer>

    public ColoredLightLayer getLayer(long sectionPos) {
        return map.get(sectionPos);
    }

    public boolean containsLayer(long sectionPos) {
        return map.containsKey(sectionPos);
    }

    public FastColor3 getLightColor(int x, int y, int z) {
        long sectionPos = SectionPos.blockToSection(BlockPos.asLong(x, y, z));
        ColoredLightLayer layer = getLayer(sectionPos);
        return layer.get(
                SectionPos.sectionRelative(x),
                SectionPos.sectionRelative(y),
                SectionPos.sectionRelative(z)
        );
    }

    public void setLightColor(int x, int y, int z, FastColor3 value) {
        long sectionPos = SectionPos.blockToSection(BlockPos.asLong(x, y, z));
        ColoredLightLayer layer = getLayer(sectionPos);
        layer.set(
                SectionPos.sectionRelative(x),
                SectionPos.sectionRelative(y),
                SectionPos.sectionRelative(z),
                value
        );
    }

    @Nullable
    public ColoredLightLayer initializeSection(long sectionPos) {
        if(!containsLayer(sectionPos)) {
            ColoredLightLayer layer = new ColoredLightLayer();
            map.put(sectionPos, layer);
            return layer;
        }
        return null;
    }

    public void removeSection(long sectionPos) {
        //map.remove(sectionPos);
        if(containsLayer(sectionPos))
            getLayer(sectionPos).clear();
    }
}
