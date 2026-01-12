package me.erykczy.colorfullighting.event;

import me.erykczy.colorfullighting.ColorfulLighting;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.ViewArea;
import me.erykczy.colorfullighting.compat.oculus.OculusCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ClientEventListener {
    private boolean wasShaderPackInUse = false;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if(event.phase == TickEvent.Phase.START) return;
        
        // Check for Oculus shader state changes
        if (OculusCompat.isOculusLoaded()) {
            boolean isShaderPackInUse = OculusCompat.isShaderPackInUse();
            if (isShaderPackInUse != wasShaderPackInUse) {
                wasShaderPackInUse = isShaderPackInUse;
                if (isShaderPackInUse) {
                    ColoredLightEngine.getInstance().setEnabled(false);
                    ColorfulLighting.LOGGER.info("Oculus shader enabled, disabling colored lighting");
                } else {
                    ColoredLightEngine.getInstance().setEnabled(true);
                    ColorfulLighting.LOGGER.info("Oculus shader disabled, enabling colored lighting");
                }
                if (Minecraft.getInstance().levelRenderer != null) {
                    Minecraft.getInstance().levelRenderer.allChanged();
                }
            }
        }

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
                    .then(Commands.literal("chunk")
                        .executes(context -> {
                            var player = Minecraft.getInstance().player;
                            if (player != null) {
                                ColoredLightEngine.getInstance().rebuildChunk(player.chunkPosition());
                                context.getSource().sendSuccess(() -> Component.literal("Reloading colored light in 3x3 chunk radius..."), false);
                            }
                            return 1;
                        })
                    )
                    .then(Commands.literal("all")
                        .executes(context -> {
                            ColoredLightEngine.getInstance().reset();
                            if (Minecraft.getInstance().levelRenderer != null) {
                                Minecraft.getInstance().levelRenderer.allChanged();
                            }
                            context.getSource().sendSuccess(() -> Component.literal("Reloading all colored lights..."), false);
                            return 1;
                        })
                    )
                    .executes(context -> {
                        // Default behavior (same as 'all') for backward compatibility
                        ColoredLightEngine.getInstance().reset();
                        if (Minecraft.getInstance().levelRenderer != null) {
                            Minecraft.getInstance().levelRenderer.allChanged();
                        }
                        context.getSource().sendSuccess(() -> Component.literal("Reloading all colored lights..."), false);
                        return 1;
                    })
                )
                .then(Commands.literal("on")
                    .executes(context -> {
                        ColoredLightEngine.getInstance().setEnabled(true);
                        if (Minecraft.getInstance().levelRenderer != null) {
                            Minecraft.getInstance().levelRenderer.allChanged();
                        }
                        context.getSource().sendSuccess(() -> Component.literal("Colored lighting enabled"), false);
                        return 1;
                    })
                )
                .then(Commands.literal("off")
                    .executes(context -> {
                        ColoredLightEngine.getInstance().setEnabled(false);
                        if (Minecraft.getInstance().levelRenderer != null) {
                            Minecraft.getInstance().levelRenderer.allChanged();
                        }
                        context.getSource().sendSuccess(() -> Component.literal("Colored lighting disabled"), false);
                        return 1;
                    })
                )
        );
    }
}
