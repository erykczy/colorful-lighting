package me.erykczy.colorfullighting.event;

import me.erykczy.colorfullighting.ColorfulLighting;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.ViewArea;
import me.erykczy.colorfullighting.common.util.MathExt;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClientEventListener {
    private float lastFalloff = -1f;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if(event.phase == TickEvent.Phase.START) return;
        var player = ColorfulLighting.clientAccessor.getPlayer();
        if(player == null) return;

        var level = ColorfulLighting.clientAccessor.getLevel();
        if (level != null) {
            float currentFalloff = MathExt.getTimeOfDayFalloff(level.getDayTime());
            if (lastFalloff != -1f && currentFalloff != lastFalloff) {
                ColoredLightEngine.getInstance().refreshLevel();
            }
            lastFalloff = currentFalloff;
        }

        ChunkPos pos = player.getChunkPos();
        int renderDistance = ColorfulLighting.clientAccessor.getRenderDistance();
        ViewArea viewArea = new ViewArea(
                pos.x - renderDistance,
                pos.z - renderDistance,
                pos.x + renderDistance,
                pos.z + renderDistance
        );
        ColoredLightEngine.getInstance().updateViewArea(viewArea);
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if(!event.getLevel().isClientSide()) return;
        ColoredLightEngine.getInstance().reset();
        lastFalloff = -1f;
    }
}
