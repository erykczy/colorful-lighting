package me.erykczy.colorfullighting.common.util;

public class ColorRGB8 {
    public int red, green, blue;

    public static ColorRGB8 fromRGB4(ColorRGB4 value) {
        return new ColorRGB8(value.red4 * 17, value.green4 * 17, value.blue4 * 17);
    }

    public static final ColorRGB8 BLACK = new ColorRGB8(0, 0, 0);

    public static ColorRGB8 fromRGB8(int r, int g, int b) {
        if (r == 0 && g == 0 && b == 0) return BLACK;
        return new ColorRGB8(r, g, b);
    }
    
    public int getRed() { return red; }
    public int getGreen() { return green; }
    public int getBlue() { return blue; }

    public static ColorRGB8 fromRGBFloat(float r, float g, float b) {
        return fromRGB8((int)(r * 255), (int)(g * 255), (int)(b * 255));
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
        return fromRGB8(
            MathExt.clamp(red, 0, 255),
            MathExt.clamp(green, 0, 255),
            MathExt.clamp(blue, 0, 255)
        );
    }

    public boolean isZero() {
        return red == 0 && green == 0 && blue == 0;
    }

    public ColorRGB8 add(ColorRGB8 other) {
        return fromRGB8(red + other.red, green + other.green, blue + other.blue);
    }

    public ColorRGB8 sub(ColorRGB8 other) {
        return fromRGB8(red - other.red, green - other.green, blue - other.blue);
    }

    public ColorRGB8 intDivide(int scalar) {
        return fromRGB8(red / scalar, green / scalar, blue / scalar);
    }

    public ColorRGB8 mul(float scalar) {
        return fromRGB8((int)(red * scalar), (int)(green * scalar), (int)(blue * scalar));
    }
    public ColorRGB8 mul(double scalar) {
        return fromRGB8((int)(red * scalar), (int)(green * scalar), (int)(blue * scalar));
    }
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static ColorRGB8 linearInterpolation(ColorRGB8 a, ColorRGB8 b, double x) {
        if(a.red == 0 && a.green == 0 && a.blue == 0) return b;
        if(b.red == 0 && b.green == 0 && b.blue == 0) return a;
        return new ColorRGB8(
            (int)lerp(a.red, b.red, x),
            (int)lerp(a.green, b.green, x),
            (int)lerp(a.blue, b.blue, x)
        );
    }
}
