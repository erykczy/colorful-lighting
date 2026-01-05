package me.erykczy.colorfullighting.common.util;

public class MathExt {
    public static int clamp(int value, int min, int max) {
        if(value < min) value = min;
        if(value > max) value = max;
        return value;
    }

    public static float getTimeOfDayFalloff(long dayTime) {
        long time = dayTime % 24000;
        if (time >= 13000 && time < 23000) { // night
            return 1.0f / 255.0f;
        } else if (time >= 0 && time < 12000) { // day
            return 0.3f / 255.0f;
        } else { // dawn or twilight
            return 0.7f / 255.0f;
        }
    }
}
