package com.example.examplemod.util;

public class PackedLightData {
    public int skyLight4;
    public int red8;
    public int blue8;
    public int green8;
    public int alpha4;

    public static int packData(int skyLight4, ColorRGB8 color) { return packData(skyLight4, color.red, color.green, color.blue); }
    public static int packData(int skyLight4, int red8, int green8, int blue8) {
        //blockLight = Math.clamp(blockLight, 0, 15);
        skyLight4 = Math.clamp(skyLight4, 0, 15);
        red8 = Math.clamp(red8, 0, 255);
        green8 = Math.clamp(green8, 0, 255);
        blue8 = Math.clamp(blue8, 0, 255);
        int alpha4 = 15;
        // TODO: big-endian
        return red8 | green8 << 8 | skyLight4 << 16 | blue8 << 20 | alpha4 << 28;
    }

    public static PackedLightData unpackData(int packedData) {
        PackedLightData data = new PackedLightData();
        data.red8 = (packedData) & 0xFF;
        data.green8 = (packedData >>> 8) & 0xFF;
        data.skyLight4 = (packedData >>> 16) & 0xF;
        data.blue8 = (packedData >>> 20) & 0xFF;
        data.alpha4 = (packedData >>> 28) & 0xF;
        return data;
    }

    public static boolean isBlack(int packedData) {
        return packedData == 0xF0000000 || packedData == 0;
    }
}
