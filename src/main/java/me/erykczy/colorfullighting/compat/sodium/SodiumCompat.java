package me.erykczy.colorfullighting.compat.sodium;

import net.minecraftforge.fml.ModList;

public class SodiumCompat {
    public static boolean isSodiumLoaded() {
        return ModList.get().isLoaded("embeddium") || ModList.get().isLoaded("sodium");
    }
}
