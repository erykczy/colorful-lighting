package com.example.examplemod.util;

import net.minecraft.core.Direction;
import net.minecraft.world.level.lighting.LightEngine;

import java.util.ArrayList;
import java.util.List;

public abstract class Debug {
    public static LightDebugEntry lightEntry(long encoded) {
        LightDebugEntry entry = new LightDebugEntry();
        entry.level = LightEngine.QueueEntry.getFromLevel(encoded);
        entry.fromEmptyShape = LightEngine.QueueEntry.isFromEmptyShape(encoded);
        entry.increaseFromEmission = LightEngine.QueueEntry.isIncreaseFromEmission(encoded);
        for(var dir : Direction.values()) {
            if(LightEngine.QueueEntry.shouldPropagateInDirection(encoded, dir))
                entry.directions.add(dir);
        }
        return entry;
    }

    public static class LightDebugEntry {
        public int level;
        public boolean fromEmptyShape;
        public boolean increaseFromEmission;
        public List<Direction> directions = new ArrayList<>();
    }
}
