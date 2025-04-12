package com.example.examplemod.util;

public class ColorRGB8 {
    public int red, green, blue;

    public static ColorRGB8 fromRGB4(ColorRGB4 value) {
        return new ColorRGB8(value.red4 * 17, value.green4 * 17, value.blue4 * 17);
    }

    public static ColorRGB8 fromRGB8(int r, int g, int b) {
        return new ColorRGB8(r, g, b);
    }

    public static ColorRGB8 fromRGBFloat(float r, float g, float b) {
        return new ColorRGB8((int)(r * 255), (int)(g * 255), (int)(b * 255));
    }

    private ColorRGB8(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public boolean isInValidState() {
        return  red >= 0 && red < 256 &&
                green >= 0 && green < 256 &&
                blue >= 0 && blue < 256;
    }

    public ColorRGB8 clamp() {
        return new ColorRGB8(
            Math.clamp(red, 0, 255),
            Math.clamp(green, 0, 255),
            Math.clamp(blue, 0, 255)
        );
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
