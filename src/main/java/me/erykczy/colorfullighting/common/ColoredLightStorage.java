package me.erykczy.colorfullighting.common;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.erykczy.colorfullighting.common.util.ColorRGB4;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class responsible for storing light color values for each block in each section of the world
 */
public class ColoredLightStorage {
    private ConcurrentHashMap<Long, ColoredLightSection> map = new ConcurrentHashMap<>();

    @Nullable
    public ColorRGB4 getEntry(BlockPos blockPos) { return getEntry(blockPos.getX(), blockPos.getY(), blockPos.getZ()); }
    @Nullable
    public ColorRGB4 getEntry(int x, int y, int z) {
        long sectionPos = SectionPos.blockToSection(BlockPos.asLong(x, y, z));
        ColoredLightSection layer = getSection(sectionPos);
        if(layer == null) return null;
        return layer.get(
                SectionPos.sectionRelative(x),
                SectionPos.sectionRelative(y),
                SectionPos.sectionRelative(z)
        );
    }

    /*public void setEntry(BlockPos blockPos, ColorRGB4 value) { setEntry(blockPos.getX(), blockPos.getY(), blockPos.getZ(), value); }
    public void setEntry(int x, int y, int z, ColorRGB4 value) {
        long sectionPos = SectionPos.blockToSection(BlockPos.asLong(x, y, z));
        map.computeIfPresent(sectionPos, (pos, layer) -> {
            layer.set(
                    SectionPos.sectionRelative(x),
                    SectionPos.sectionRelative(y),
                    SectionPos.sectionRelative(z),
                    value
            );
            return layer;
        });
    }*/
    public void setEntryUnsafe(BlockPos blockPos, ColorRGB4 value) { setEntryUnsafe(blockPos.getX(), blockPos.getY(), blockPos.getZ(), value); }
    public void setEntryUnsafe(int x, int y, int z, ColorRGB4 value) {
        long sectionPos = SectionPos.blockToSection(BlockPos.asLong(x, y, z));
        var layer = map.get(sectionPos);
        if(layer == null) return;
        layer.set(
                SectionPos.sectionRelative(x),
                SectionPos.sectionRelative(y),
                SectionPos.sectionRelative(z),
                value
        );
    }

    public boolean containsEntry(BlockPos blockPos) { return containsEntry(blockPos.getX(), blockPos.getY(), blockPos.getZ()); }
    public boolean containsEntry(int x, int y, int z) {
        return containsSection(SectionPos.blockToSection(BlockPos.asLong(x, y, z)));
    }

    public boolean containsSection(long sectionPos) {
        return map.containsKey(sectionPos);
    }

    public ColoredLightSection getSection(long sectionPos) {
        return map.get(sectionPos);
    }

    public void addSection(long sectionPos) {
        map.put(sectionPos, new ColoredLightSection());
    }

    public void removeSection(long sectionPos) {
        map.remove(sectionPos);
    }

    public void clear() {
        map.clear();
    }
}
