package me.erykczy.colorfullighting.event;

import me.erykczy.colorfullighting.common.ColoredLightEngine;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClientEventListener {
    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if(!event.getLevel().isClientSide()) return;
        ColoredLightEngine.getInstance().onChunkLoad(event.getChunk().getPos());
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if(!event.getLevel().isClientSide()) return;
        ColoredLightEngine.getInstance().onChunkUnload(event.getChunk().getPos());
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if(!event.getLevel().isClientSide()) return;
        ColoredLightEngine.getInstance().onLevelUnload();
    }
}
