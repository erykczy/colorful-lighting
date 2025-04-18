package com.example.examplemod.accessors;

import com.example.examplemod.common.accessors.PlayerAccessor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;

public class PlayerWrapper implements PlayerAccessor {
    final LocalPlayer player;

    public PlayerWrapper(@NotNull LocalPlayer player) {
        this.player = player;
    }

    @Override
    public ChunkPos getPlayerChunkPos() {
        return player.chunkPosition();
    }
}
