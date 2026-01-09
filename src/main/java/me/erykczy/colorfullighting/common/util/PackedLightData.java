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

    public static int blend(int lightColor0, int lightColor1, int lightColor2, int lightColor3) {
        if (isBlack(lightColor0)) lightColor0 = lightColor3;
        if (isBlack(lightColor1)) lightColor1 = lightColor3;
        if (isBlack(lightColor2)) lightColor2 = lightColor3;

        var data0 = unpackData(lightColor0);
        var data1 = unpackData(lightColor1);
        var data2 = unpackData(lightColor2);
        var data3 = unpackData(lightColor3);

        return packData(
                (data0.skyLight4 + data1.skyLight4 + data2.skyLight4 + data3.skyLight4) >> 2,
                (data0.red8 + data1.red8 + data2.red8 + data3.red8) >> 2,
                (data0.green8 + data1.green8 + data2.green8 + data3.green8) >> 2,
                (data0.blue8 + data1.blue8 + data2.blue8 + data3.blue8) >> 2
        );
    }

    public static int blend(int lightColor0, int lightColor1, int lightColor2, int lightColor3, float weight0, float weight1, float weight2, float weight3) {
        var data0 = unpackData(lightColor0);
        var data1 = unpackData(lightColor1);
        var data2 = unpackData(lightColor2);
        var data3 = unpackData(lightColor3);

        return packData(
                (int)(data0.skyLight4 * weight0 + data1.skyLight4 * weight1 + data2.skyLight4 * weight2 + data3.skyLight4 * weight3),
                (int)(data0.red8 * weight0 + data1.red8 * weight1 + data2.red8 * weight2 + data3.red8 * weight3),
                (int)(data0.green8 * weight0 + data1.green8 * weight1 + data2.green8 * weight2 + data3.green8 * weight3),
                (int)(data0.blue8 * weight0 + data1.blue8 * weight1 + data2.blue8 * weight2 + data3.blue8 * weight3)
        );
    }

    public static int max(int lightColor0, int lightColor1) {
        var firstData = unpackData(lightColor0);
        var secondData = unpackData(lightColor1);

        return packData(
                Math.max(firstData.skyLight4, secondData.skyLight4),
                Math.max(firstData.red8, secondData.red8),
                Math.max(firstData.green8, secondData.green8),
                Math.max(firstData.blue8, secondData.blue8)
        );
    }
}
