package me.erykczy.colorfullighting.event;

import com.mojang.blaze3d.platform.InputConstants;
import me.erykczy.colorfullighting.ColorfulLighting;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.ViewArea;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class ClientEventListener {
    /*@SubscribeEvent
    private void onChunkLoad(ChunkEvent.Load event) {
        if(!event.getLevel().isClientSide()) return;
        ColoredLightEngine.getInstance().onChunkLoad(event.getChunk().getPos());
    }

    @SubscribeEvent
    private void onChunkUnload(ChunkEvent.Unload event) {
        if(!event.getLevel().isClientSide()) return;
        ColoredLightEngine.getInstance().onChunkUnload(event.getChunk().getPos());
    }*/

    @SubscribeEvent
    private void onTick(ClientTickEvent.Post event) {
        var player = ColorfulLighting.clientAccessor.getPlayer();
        if(player == null) return;
        ChunkPos pos = player.getPlayerChunkPos();
        int renderDistance = ColorfulLighting.clientAccessor.getRenderDistance();
        ViewArea viewArea = new ViewArea(
                pos.x - renderDistance,
                pos.z - renderDistance,
                pos.x + renderDistance,
                pos.z + renderDistance
        );
        //if(!InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_G)) return;
        ColoredLightEngine.getInstance().updateViewArea(viewArea);
    }

    @SubscribeEvent
    private void onLevelUnload(LevelEvent.Unload event) {
        if(!event.getLevel().isClientSide()) return;
        ColoredLightEngine.getInstance().onLevelUnload();
    }
}
