package com.example.examplemod;

import com.example.examplemod.util.FastColor3;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

public class ColoredLightStorage {
    private Long2ObjectOpenHashMap<ColoredLightLayer> map = new Long2ObjectOpenHashMap<>();

    public ColoredLightLayer getLayer(long sectionPos) {
        return map.get(sectionPos);
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

    public void updateSection(long sectionPos, boolean isEmpty) {
        if(!map.containsKey(sectionPos)) {
            map.put(sectionPos, new ColoredLightLayer());
        }
        ColoredLightLayer layer = map.get(sectionPos);
        layer.clear();
        /*if(isEmpty) {
            layer.clear();
        }
        else {
            BlockPos sectionOrigin = SectionPos.of(sectionPos).origin();
            for(int y = 0; y < 16; ++y) {
                boolean even = y % 2 == 0;
                for(int x = 0; x < 16; ++x) {
                    for(int z = 0; z < 16; ++z) {
                        setLightColor(sectionOrigin.getX() + x, sectionOrigin.getY() + y, sectionOrigin.getZ() + z, even ?
                                new FastColor3((byte)255, (byte)0, (byte)0) :
                                new FastColor3((byte)0, (byte)255,(byte)0)
                        );
                    }
                }
            }
        }*/
    }
}
