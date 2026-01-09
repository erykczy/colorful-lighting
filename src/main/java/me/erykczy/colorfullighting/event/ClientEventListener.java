package me.erykczy.colorfullighting.event;

import me.erykczy.colorfullighting.ColorfulLighting;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.ViewArea;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.commands.Commands;

public class ClientEventListener {
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if(event.phase == TickEvent.Phase.START) return;
        if(ColorfulLighting.clientAccessor == null) return;
        var player = ColorfulLighting.clientAccessor.getPlayer();
        if(player == null) return;
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
    }

    @SubscribeEvent
    public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("cl")
                .then(Commands.literal("purge")
                    .executes(context -> {
                        ColoredLightEngine.getInstance().reset();
                        Minecraft.getInstance().levelRenderer.allChanged();
                        return 1;
                    })
                )
        );
    }
}
