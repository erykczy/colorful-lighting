package com.example.examplemod;

import com.example.examplemod.util.Color3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class ColoredLightManager {
    private ColoredLightStorage storage = new ColoredLightStorage();

    private static ColoredLightManager instance = new ColoredLightManager();
    public static ColoredLightManager getInstance() {
        return instance;
    }

    public void updateSection(SectionPos pos, boolean isEmpty) {
        storage.updateSection(pos.asLong(), isEmpty);
    }

    public Color3 sampleLightColor(int x, int y, int z) {
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
        return finalColor.divide(8);
    }
}
