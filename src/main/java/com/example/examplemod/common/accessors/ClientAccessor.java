package com.example.examplemod.common.accessors;

import javax.annotation.Nullable;

public interface ClientAccessor {
    @Nullable
    LevelAccessor getLevel();
    @Nullable
    PlayerAccessor getPlayer();
}
