package me.erykczy.colorfullighting.common.util;

public class ColorRGB4 {
    public static final ColorRGB4 BLACK = new ColorRGB4(0, 0, 0);

    public int red4, green4, blue4;

    public static ColorRGB4 fromRGB8(int r, int g, int b) {
        return fromRGB4(r / 17, g / 17, b / 17); // 0..255 range to 0..15 range
    }

    public static ColorRGB4 fromRGB4(int r, int g, int b) {
        return new ColorRGB4(r, g, b);
    }

    private ColorRGB4(int r4, int g4, int b4) {
        red4 = r4;
        green4 = g4;
        blue4 = b4;
    }

    public boolean isInValidState() {
        return  red4 >= 0 && red4 < 16 &&
                green4 >= 0 && green4 < 16 &&
                blue4 >= 0 && blue4 < 16;
    }

    public ColorRGB4 mul(float scalar) {
        return new ColorRGB4((int)(red4 * scalar), (int)(green4 * scalar), (int)(blue4 * scalar));
    }

    public static ColorRGB4 max(ColorRGB4 a, ColorRGB4 b) {
        return new ColorRGB4(
                Math.max(a.red4, b.red4),
                Math.max(a.green4, b.green4),
                Math.max(a.blue4, b.blue4)
        );
    }

    @Override
    public String toString() {
        return "ColorRGB4["+ red4 +", " + green4 + ", " + blue4 + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        return  obj instanceof ColorRGB4 other &&
                other.red4 == red4 &&
                other.green4 == green4 &&
                other.blue4 == blue4;
    }
}
