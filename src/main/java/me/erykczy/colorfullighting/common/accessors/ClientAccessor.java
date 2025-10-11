package me.erykczy.colorfullighting.common.accessors;

import javax.annotation.Nullable;

public interface ClientAccessor {
    @Nullable
    LevelAccessor getLevel();
    @Nullable
    PlayerAccessor getPlayer();

    int getRenderDistance();
}
