package com.example.examplemod.accessors;

import com.example.examplemod.common.accessors.ClientAccessor;
import com.example.examplemod.common.accessors.LevelAccessor;
import com.example.examplemod.common.accessors.PlayerAccessor;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class MinecraftWrapper implements ClientAccessor {
    private final Minecraft minecraft;

    public MinecraftWrapper(@NotNull Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public @Nullable LevelAccessor getLevel() {
        if(minecraft.level == null) return null;
        return new LevelWrapper(minecraft.level, minecraft.levelRenderer);
    }

    @Override
    public @Nullable PlayerAccessor getPlayer() {
        if(minecraft.player == null) return null;
        return new PlayerWrapper(minecraft.player);
    }
}
