package me.erykczy.colorfullighting.common;

import me.erykczy.colorfullighting.common.util.ColorRGB4;

/**
 * Stores light color for each block in the section
 */
public class ColoredLightSection {
    private static final int LAYER_SIZE = 6144; // = 16 * 16 * 16 * 1.5
    public byte[] data;

    public ColoredLightSection() {}

    public int getColorIndex(int x, int y, int z) {
        return (y << 8 | z << 4 | x);
    }

    public ColorRGB4 get(int x, int y, int z) { return get(getColorIndex(x, y, z)); }
    public ColorRGB4 get(int colorIndex) {
        if(data == null)
            return ColorRGB4.fromRGB4(0, 0, 0);
        else  {
            int packed = getPacked(colorIndex);
            return ColorRGB4.fromRGB4((packed >> 8) & 0xF, (packed >> 4) & 0xF, packed & 0xF);
        }
    }

    public int getPacked(int x, int y, int z) { return getPacked(getColorIndex(x, y, z)); }
    public int getPacked(int colorIndex) {
        if(data == null) return 0;
        int startBit = colorIndex * 12;
        int byteIndex = startBit >> 3;
        int bitOffset = (startBit & 0x7);

        int b0 = data[byteIndex] & 0xFF;
        int b1 = data[byteIndex + 1] & 0xFF;

        if(bitOffset == 0) {
            return ((b0 >>> 4) & 0xF) << 8 | (b0 & 0xF) << 4 | ((b1 >>> 4) & 0xF);
        } else {
            return (b0 & 0xF) << 8 | ((b1 >>> 4) & 0xF) << 4 | (b1 & 0xF);
        }
    }

    public void set(int x, int y, int z, ColorRGB4 value) { set(getColorIndex(x, y, z), value); }
    public void set(int colorIndex, ColorRGB4 value) {
        if(data == null)
            data = new byte[LAYER_SIZE];
        if(!value.isInValidState()) {
            throw new IllegalArgumentException("Invalid ColoredLightSection.Entry: "+value);
        }

        int startBit = colorIndex * 12;
        int byteIndex = startBit >> 3;
        int bitOffset = (startBit & 0x7);

        if(bitOffset == 0) { // whether startBit is divisible by 8 (this means that the color starts at the beginning of the byte)
            data[byteIndex] = (byte) ((value.red4 << 4) | value.green4);
            data[byteIndex + 1] = (byte) (value.blue4 << 4 | (data[byteIndex + 1] & 0x0F));
        }
        else {
            data[byteIndex] = (byte) ((data[byteIndex] & 0xF0) | value.red4);
            data[byteIndex + 1] = (byte) ((value.green4 << 4) | value.blue4);
        }
    }
}