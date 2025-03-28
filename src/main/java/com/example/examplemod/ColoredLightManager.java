package com.example.examplemod;

import com.example.examplemod.util.Color3;
import com.example.examplemod.util.FastColor3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ColoredLightManager {
    public ColoredLightStorage storage = new ColoredLightStorage();
    public Queue<FastColor3> increaseQueue = new ConcurrentLinkedQueue<>();
    public Queue<FastColor3> decreaseQueue = new ConcurrentLinkedQueue<>();

    private static ColoredLightManager instance = new ColoredLightManager();
    public static ColoredLightManager getInstance() {
        return instance;
    }

    public void updateSection(SectionPos pos, boolean isEmpty) {
        storage.updateSection(pos.asLong(), isEmpty);
    }

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
        Color3 sampledColor = new Color3(storage.getLightColor(x, y, z));
        int test = sampledColor.red + sampledColor.green + sampledColor.blue;
        return sampledColor;//new Color3(test, 0, 0);
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
}
