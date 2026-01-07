package me.erykczy.colorfullighting.common.util;

public class PackedLightData {
    public int skyLight4;
    public int red8;
    public int blue8;
    public int green8;
    public int alpha4;

    public static int packData(int skyLight4, ColorRGB8 color) { return packData(skyLight4, color.red, color.green, color.blue); }
    public static int packData(int skyLight4, int red8, int green8, int blue8) {
        skyLight4 = MathExt.clamp(skyLight4, 0, 15);
        red8 = MathExt.clamp(red8, 0, 255);
        green8 = MathExt.clamp(green8, 0, 255);
        blue8 = MathExt.clamp(blue8, 0, 255);
        int alpha4 = 15;
        return red8 | green8 << 8 | skyLight4 << 16 | blue8 << 20 | alpha4 << 28;
    }

    public static int getRed(int packedData) { return (packedData) & 0xFF; }
    public static int getGreen(int packedData) { return (packedData >>> 8) & 0xFF; }
    public static int getSky(int packedData) { return (packedData >>> 16) & 0xF; }
    public static int getBlue(int packedData) { return (packedData >>> 20) & 0xFF; }
    public static int getAlpha(int packedData) { return (packedData >>> 28) & 0xF; }

    public static PackedLightData unpackData(int packedData) {
        PackedLightData data = new PackedLightData();
        data.red8 = getRed(packedData);
        data.green8 = getGreen(packedData);
        data.skyLight4 = getSky(packedData);
        data.blue8 = getBlue(packedData);
        data.alpha4 = getAlpha(packedData);
        return data;
    }

    public static boolean isBlack(int packedData) {
        return (packedData & 0xF0000000) == 0 || (packedData & 0x0FFFFFFF) == 0;
    }

    public static int blend(int lightColor0, int lightColor1, int lightColor2, int lightColor3) {
        if (isBlack(lightColor0)) lightColor0 = lightColor3;
        if (isBlack(lightColor1)) lightColor1 = lightColor3;
        if (isBlack(lightColor2)) lightColor2 = lightColor3;

        int r = (getRed(lightColor0) + getRed(lightColor1) + getRed(lightColor2) + getRed(lightColor3)) >> 2;
        int g = (getGreen(lightColor0) + getGreen(lightColor1) + getGreen(lightColor2) + getGreen(lightColor3)) >> 2;
        int s = (getSky(lightColor0) + getSky(lightColor1) + getSky(lightColor2) + getSky(lightColor3)) >> 2;
        int b = (getBlue(lightColor0) + getBlue(lightColor1) + getBlue(lightColor2) + getBlue(lightColor3)) >> 2;

        return r | g << 8 | s << 16 | b << 20 | 0xF0000000;
    }

    public static int blend(int lightColor0, int lightColor1, int lightColor2, int lightColor3, float weight0, float weight1, float weight2, float weight3) {
        int r = (int)(getRed(lightColor0) * weight0 + getRed(lightColor1) * weight1 + getRed(lightColor2) * weight2 + getRed(lightColor3) * weight3);
        int g = (int)(getGreen(lightColor0) * weight0 + getGreen(lightColor1) * weight1 + getGreen(lightColor2) * weight2 + getGreen(lightColor3) * weight3);
        int s = (int)(getSky(lightColor0) * weight0 + getSky(lightColor1) * weight1 + getSky(lightColor2) * weight2 + getSky(lightColor3) * weight3);
        int b = (int)(getBlue(lightColor0) * weight0 + getBlue(lightColor1) * weight1 + getBlue(lightColor2) * weight2 + getBlue(lightColor3) * weight3);

        return (r & 0xFF) | (g & 0xFF) << 8 | (s & 0xF) << 16 | (b & 0xFF) << 20 | 0xF0000000;
    }

    public static int max(int lightColor0, int lightColor1) {
        int r = Math.max(getRed(lightColor0), getRed(lightColor1));
        int g = Math.max(getGreen(lightColor0), getGreen(lightColor1));
        int s = Math.max(getSky(lightColor0), getSky(lightColor1));
        int b = Math.max(getBlue(lightColor0), getBlue(lightColor1));

        return r | g << 8 | s << 16 | b << 20 | 0xF0000000;
    }
}
