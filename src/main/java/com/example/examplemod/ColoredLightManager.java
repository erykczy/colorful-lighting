package com.example.examplemod;

import net.minecraft.core.BlockPos;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class ColoredLightManager {
    public static Color3 getLightColor(BlockPos pos) {
        return getLightColor(pos.getX(), pos.getY(), pos.getZ());
    }

    public static Color3 getLightColor(int x, int y, int z) {
        double sin = (Math.sin(x/3.0)+1.0)/2.0;
        int blue = (int)(255 * sin);
        int green = 0;
        int red = 255 - blue;

        if(y < 50) {
            return new Color3(255, 20, 20);
        }
        else {
            return new Color3(255, 255, 255);
        }

        /*if(x > 0)
            return new Color3(255, 0, 0);
        else if(x < 0)
            return new Color3(255, 255, 255);
        else
            return new Color3(255, 255, 255);*/

        //return new Color3(red, green, blue);
    }

    public static Color3 getMixedLightColor(Vector3f pos) {
        Vector3i cornerPos = new Vector3i((int)pos.x, (int)pos.y, (int)pos.z); // reject fraction

        Color3 finalColor = new Color3();
        for(int ox = -1; ox < 1; ++ox) {
            for(int oy = -1; oy < 1; ++oy) {
                for(int oz = -1; oz < 1; ++oz) {
                    Color3 c = getLightColor(cornerPos.x + ox, cornerPos.y + oy, cornerPos.z + oz);
                    finalColor = finalColor.add(c);
                }
            }
        }
        return finalColor.divide(8);
    }
}
