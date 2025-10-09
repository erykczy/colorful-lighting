package me.erykczy.colorfullighting.common.util;

public class MathExt {
    public static int clamp(int value, int min, int max) {
        if(value < min) value = min;
        if(value > max) value = max;
        return value;
    }
}
