package com.example.examplemod;

public class Color3 {
    public int red, green, blue;

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

    public Color3 divide(int scalar) {
        return new Color3(red / scalar, green / scalar, blue / scalar);
    }

    public byte redByte() { return (byte)red; }
    public byte greenByte() { return (byte)green; }
    public byte blueByte() { return (byte)blue; }
}
