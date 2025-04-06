package com.example.examplemod.util;

public class ColorRGB8 {
    public int red, green, blue;

    public static ColorRGB8 fromRGB4(ColorRGB4 value) {
        return new ColorRGB8(value.red4 * 17, value.green4 * 17, value.blue4 * 17);
    }

    public ColorRGB8() {
        this(0, 0, 0);
    }

    public ColorRGB8(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public ColorRGB8(float red, float green, float blue) {
        this((int)(red * 255), (int)(green * 255), (int)(blue * 255));
    }

    public ColorRGB8 add(ColorRGB8 other) {
        return new ColorRGB8(red + other.red, green + other.green, blue + other.blue);
    }

    public ColorRGB8 sub(ColorRGB8 other) {
        return new ColorRGB8(red - other.red, green - other.green, blue - other.blue);
    }

    public ColorRGB8 intDivide(int scalar) {
        return new ColorRGB8(red / scalar, green / scalar, blue / scalar);
    }

    public ColorRGB8 mul(float scalar) {
        return new ColorRGB8((int)(red * scalar), (int)(green * scalar), (int)(blue * scalar));
    }
}
