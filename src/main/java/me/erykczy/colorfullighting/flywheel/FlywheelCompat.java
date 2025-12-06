package me.erykczy.colorfullighting.flywheel;

import com.mojang.blaze3d.systems.RenderSystem;

public class FlywheelCompat {
    private static FlywheelCompat instance;
    public ColoredLightFlywheelStorage flywheelColoredLightStorage;

    public static void create() {
        RenderSystem.recordRenderCall(() -> instance = new FlywheelCompat());
    }

    public static FlywheelCompat getInstance() {
        return instance;
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    public FlywheelCompat() {
        flywheelColoredLightStorage = new ColoredLightFlywheelStorage();
    }
}
