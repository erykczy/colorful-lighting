package me.erykczy.colorfullighting.compat.sodium;

import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.MathExt;

public class SodiumPackedLightData {
    public int skyLight4;
    public int red8;
    public int blue8;
    public int green8;
    public int alpha4;

    public static int packData(int skyLight4, ColorRGB8 color) { return packData(skyLight4, color.red, color.green, color.blue); }
    public static int packData(int skyLight4, int red8, int green8, int blue8) {
        //blockLight = Math.clamp(blockLight, 0, 15);
        skyLight4 = MathExt.clamp(skyLight4, 0, 15);
        red8 = MathExt.clamp(red8, 0, 255);
        green8 = MathExt.clamp(green8, 0, 255);
        blue8 = MathExt.clamp(blue8, 0, 255);
        int alpha4 = 15;
        // TODO: big-endian
        return red8 | green8 << 8 | skyLight4 << 16 | blue8 << 20 | alpha4 << 28;
    }

    public static SodiumPackedLightData unpackData(int packedData) {
        SodiumPackedLightData data = new SodiumPackedLightData();
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

    public static int blend(int lightColor0, int lightColor1, int lightColor2, int lightColor3, float weight0, float weight1, float weight2, float weight3) {
        int r0 = lightColor0 & 0xFF;
        int g0 = (lightColor0 >>> 8) & 0xFF;
        int s0 = (lightColor0 >>> 16) & 0xF;
        int b0 = (lightColor0 >>> 20) & 0xFF;

        int r1 = lightColor1 & 0xFF;
        int g1 = (lightColor1 >>> 8) & 0xFF;
        int s1 = (lightColor1 >>> 16) & 0xF;
        int b1 = (lightColor1 >>> 20) & 0xFF;

        int r2 = lightColor2 & 0xFF;
        int g2 = (lightColor2 >>> 8) & 0xFF;
        int s2 = (lightColor2 >>> 16) & 0xF;
        int b2 = (lightColor2 >>> 20) & 0xFF;

        int r3 = lightColor3 & 0xFF;
        int g3 = (lightColor3 >>> 8) & 0xFF;
        int s3 = (lightColor3 >>> 16) & 0xF;
        int b3 = (lightColor3 >>> 20) & 0xFF;

        int r = (int)(r0 * weight0 + r1 * weight1 + r2 * weight2 + r3 * weight3);
        int g = (int)(g0 * weight0 + g1 * weight1 + g2 * weight2 + g3 * weight3);
        int s = (int)(s0 * weight0 + s1 * weight1 + s2 * weight2 + s3 * weight3);
        int b = (int)(b0 * weight0 + b1 * weight1 + b2 * weight2 + b3 * weight3);

        return r | (g << 8) | (s << 16) | (b << 20) | (15 << 28);
    }

    public static int max(int lightColor0, int lightColor1) {
        int r0 = lightColor0 & 0xFF;
        int g0 = (lightColor0 >>> 8) & 0xFF;
        int s0 = (lightColor0 >>> 16) & 0xF;
        int b0 = (lightColor0 >>> 20) & 0xFF;

        int r1 = lightColor1 & 0xFF;
        int g1 = (lightColor1 >>> 8) & 0xFF;
        int s1 = (lightColor1 >>> 16) & 0xF;
        int b1 = (lightColor1 >>> 20) & 0xFF;

        int r = Math.max(r0, r1);
        int g = Math.max(g0, g1);
        int s = Math.max(s0, s1);
        int b = Math.max(b0, b1);

        return r | (g << 8) | (s << 16) | (b << 20) | (15 << 28);
    }
}