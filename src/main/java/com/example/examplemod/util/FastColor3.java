package com.example.examplemod.util;

public record FastColor3(byte red, byte green, byte blue) {
    public FastColor3() {
        this((byte)0, (byte)0, (byte)0);
    }

    public FastColor3(Color3 other) {
        this(other.redByte(), other.greenByte(), other.blueByte());
    }
}
