package com.example.examplemod.util;

public class Mathi {
    public static int floor(float scalar) {
        return (int)Math.floor(scalar);
    }

    public static byte add(byte a, byte b) {
        return min(255, a + b).byteValue();
    }

    public static <T extends Comparable<T>>T clamp(T value, T edge0, T edge1) {
        return value.compareTo(edge0) < 0 ? edge0 : (value.compareTo(edge1) > 0 ? edge1 : value);
    }

    public static <T extends Comparable<T>>T max(T value0, T value1) {
        return value0.compareTo(value1) > 0 ? value0 : value1;
    }

    public static <T extends Comparable<T>>T min(T value0, T value1) {
        return value0.compareTo(value1) < 0 ? value0 : value1;
    }
}
