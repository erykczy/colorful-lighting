package com.example.examplemod.util;

public class PackedLightData {
    public int blockLight;
    public int skyLight;
    public int red4;
    public int blue4;
    public int green4;
    public int alpha4;

    public static int packData(int blockLight, int skyLight, ColorRGB4 color) { return packData(blockLight, skyLight, color.red4, color.green4, color.blue4); }
    public static int packData(int blockLight, int skyLight, int red4, int green4, int blue4) {
        blockLight = Math.clamp(blockLight, 0, 15);
        skyLight = Math.clamp(skyLight, 0, 15);
        red4 = Math.clamp(red4, 0, 15);
        green4 = Math.clamp(green4, 0, 15);
        blue4 = Math.clamp(blue4, 0, 15);
        int alpha = 15;
        // TODO: big-endian
        return skyLight << 20 | blockLight << 16 | red4 << 0 | green4 << 4 | blue4 << 8 | alpha << 12;
    }

    public static PackedLightData unpackData(int packedData) {
        PackedLightData data = new PackedLightData();
        data.red4 = (packedData) & 0xF;
        data.green4 = (packedData >>> 4) & 0xF;
        data.blue4 = (packedData >>> 8) & 0xF;
        data.alpha4 = (packedData >>> 12) & 0xF;
        data.blockLight = (packedData >>> 16) & 0xF;
        data.skyLight = (packedData >>> 20) & 0xF;
        return data;
    }

    public static boolean isEmpty(int packedData) {
        return packedData == 0xF000 || packedData == 0;
    }
}
