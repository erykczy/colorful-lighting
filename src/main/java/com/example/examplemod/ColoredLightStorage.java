package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

import java.util.concurrent.ConcurrentHashMap;

public class ColoredLightStorage {
    private ConcurrentHashMap<Long, ColoredLightLayer> map = new ConcurrentHashMap<>();

    public ColoredLightLayer.Entry getEntry(int x, int y, int z) {
        long sectionPos = SectionPos.blockToSection(BlockPos.asLong(x, y, z));
        ColoredLightLayer layer = getSection(sectionPos);
        return layer.get(
                SectionPos.sectionRelative(x),
                SectionPos.sectionRelative(y),
                SectionPos.sectionRelative(z)
        );
    }

    public void setEntry(int x, int y, int z, ColoredLightLayer.Entry value) {
        long sectionPos = SectionPos.blockToSection(BlockPos.asLong(x, y, z));
        ColoredLightLayer layer = getSection(sectionPos);
        layer.set(
                SectionPos.sectionRelative(x),
                SectionPos.sectionRelative(y),
                SectionPos.sectionRelative(z),
                value
        );
    }

    public boolean containsSection(long sectionPos) {
        return map.containsKey(sectionPos);
    }

    public ColoredLightLayer getSection(long sectionPos) {
        return map.get(sectionPos);
    }

    public void addSection(long sectionPos) {
        map.put(sectionPos, new ColoredLightLayer());
    }

    public void removeSection(long sectionPos) {
        map.remove(sectionPos);
    }
}
