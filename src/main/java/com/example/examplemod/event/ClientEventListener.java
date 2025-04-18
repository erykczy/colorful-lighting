package com.example.examplemod.event;

import com.example.examplemod.common.ColoredLightEngine;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

public class ClientEventListener {
    @SubscribeEvent
    private void onChunkLoad(ChunkEvent.Load event) {
        if(!event.getLevel().isClientSide()) return;
        ColoredLightEngine.getInstance().onChunkLoad(event.getChunk().getPos());
    }

    @SubscribeEvent
    private void onChunkUnload(ChunkEvent.Unload event) {
        if(!event.getLevel().isClientSide()) return;
        ColoredLightEngine.getInstance().onChunkUnload(event.getChunk().getPos());
    }

    @SubscribeEvent
    private void onLevelUnload(LevelEvent.Unload event) {
        if(!event.getLevel().isClientSide()) return;
        ColoredLightEngine.getInstance().onLevelUnload();
    }
}
