package com.example.examplemod.common;

import com.example.examplemod.common.util.ColorRGB4;

// TODO optimize storage
/**
 * Stores light color for each block in the section
 */
public class ColoredLightLayer {
    private static final int LAYER_SIZE = 16 * 16 * 16 * 2;
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
            int startByte = colorIndex << 1;

            int red = (data[startByte] >>> 4) & 0x0F;
            int green = (data[startByte]) & 0x0F;
            int blue = (data[startByte + 1]) & 0x0F;
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

        int startByte = colorIndex << 1;

        data[startByte] = (byte) ((value.red4 << 4) | value.green4);
        data[startByte + 1] = (byte) value.blue4;
    }


    public void clear() {
        this.data = null;
    }

    /*public ColoredLightLayer copy() {
        return this.data == null ? new ColoredLightLayer() : new ColoredLightLayer(this.data.clone());
    }*/
}
