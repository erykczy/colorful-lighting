package me.erykczy.colorfullighting.accessors;

import me.erykczy.colorfullighting.common.accessors.PlayerAccessor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;

public class PlayerWrapper implements PlayerAccessor {
    final LocalPlayer player;

    public PlayerWrapper(@NotNull LocalPlayer player) {
        this.player = player;
    }

    @Override
    public ChunkPos getChunkPos() {
        return player.chunkPosition();
    }

    @Override
    public BlockPos getBlockPos() {
        return player.blockPosition();
    }
}
