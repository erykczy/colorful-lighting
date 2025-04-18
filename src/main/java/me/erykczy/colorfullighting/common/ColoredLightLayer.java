package me.erykczy.colorfullighting.common;

import me.erykczy.colorfullighting.common.util.ColorRGB4;

// TODO optimize storage
/**
 * Stores light color for each block in the section
 */
public class ColoredLightLayer {
    private static final int LAYER_SIZE = 6144; // = 16 * 16 * 16 * 1.5
    public byte[] data;

    public ColoredLightLayer() {}

    /*public ColoredLightLayer(byte[] data) {
        this.data = data;
        if (data.length != LAYER_SIZE) {
            throw new IllegalArgumentException("ColoredLightLayer should be "+ LAYER_SIZE +" bytes not: " + data.length);
        }
    }*/

    public int getColorIndex(int x, int y, int z) {
        return (y << 8 | z << 4 | x);
    }

    public ColorRGB4 get(int x, int y, int z) { return get(getColorIndex(x, y, z)); }
    public ColorRGB4 get(int colorIndex) {
        if(data == null)
            return ColorRGB4.fromRGB4(0, 0, 0);
        else  {
            int startBit = colorIndex * 12;
            int bitOffset = (startBit & 0x7);
            int byteIndex = startBit >> 3;
            int rawData = ((data[byteIndex] << 8) & 0xFFFF) | (data[byteIndex + 1] & 0xFF);
            int offsetData = (rawData << bitOffset);

            int red = (offsetData >>> 12) & 0x0F;
            int green = (offsetData >>> 8) & 0x0F;
            int blue = (offsetData >>> 4) & 0x0F;
            return ColorRGB4.fromRGB4(red, green, blue);
        }
    }

    public void set(int x, int y, int z, ColorRGB4 value) { set(getColorIndex(x, y, z), value); }
    public void set(int colorIndex, ColorRGB4 value) {
        if(data == null)
            data = new byte[LAYER_SIZE];
        if(!value.isInValidState()) {
            throw new IllegalArgumentException("Invalid ColoredLightLayer.Entry: "+value);
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


    public void clear() {
        this.data = null;
    }

    /*public ColoredLightLayer copy() {
        return this.data == null ? new ColoredLightLayer() : new ColoredLightLayer(this.data.clone());
    }*/
}
