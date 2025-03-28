package com.example.examplemod.util;

public class Color3 {
    public int red, green, blue;

    public Color3(FastColor3 fastColor) {
        this(Byte.toUnsignedInt(fastColor.red()), Byte.toUnsignedInt(fastColor.green()), Byte.toUnsignedInt(fastColor.blue()));
    }

    public Color3() {
        this(0, 0, 0);
    }

    public Color3(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public Color3 add(Color3 other) {
        return new Color3(red + other.red, green + other.green, blue + other.blue);
    }

    public Color3 intDivide(int scalar) {
        return new Color3(red / scalar, green / scalar, blue / scalar);
    }

    public Color3 mul(float scalar) {
        return new Color3((int)(red * scalar), (int)(green * scalar), (int)(blue * scalar));
    }

    public byte redByte() { return (byte)red; }
    public byte greenByte() { return (byte)green; }
    public byte blueByte() { return (byte)blue; }
}
