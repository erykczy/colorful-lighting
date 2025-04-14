package com.example.examplemod;

import com.example.examplemod.util.ColorRGB4;

// TODO optimize storage
public class ColoredLightLayer {
    private static final int LAYER_SIZE = 16 * 16 * 16 * ColorRGB4.SIZE;
    public byte[] data;

    public ColoredLightLayer() {}

    public ColoredLightLayer(byte[] data) {
        this.data = data;
        if (data.length != LAYER_SIZE) {
            throw new IllegalArgumentException("ColoredLightLayer should be "+ LAYER_SIZE +" bytes not: " + data.length);
        }
    }

    public int getIndex(int x, int y, int z) {
        return (y << 8 | z << 4 | x) * ColorRGB4.SIZE;
    }

    public ColorRGB4 get(int x, int y, int z) { return get(getIndex(x, y, z)); }
    public ColorRGB4 get(int index) {
        if(data == null)
            return ColorRGB4.fromRGB4(0, 0, 0);
        else  {
            byte byte0 = data[index];
            byte byte1 = data[index + 1];
            byte byte2 = data[index + 2];

            return ColorRGB4.fromRGB4(Byte.toUnsignedInt(byte0), Byte.toUnsignedInt(byte1), Byte.toUnsignedInt(byte2));
        }
    }

    public void set(int x, int y, int z, ColorRGB4 value) { set(getIndex(x, y, z), value); }
    public void set(int index, ColorRGB4 value) {
        if(data == null)
            data = new byte[LAYER_SIZE];
        if(!value.isInValidState()) {
            throw new IllegalArgumentException("Invalid ColoredLightLayer.Entry: "+value);
        }

        data[index] = (byte)value.red4;
        data[index + 1] = (byte)value.green4;
        data[index + 2] = (byte)value.blue4;
    }


    public void clear() {
        this.data = null;
    }

    public ColoredLightLayer copy() {
        return this.data == null ? new ColoredLightLayer() : new ColoredLightLayer(this.data.clone());
    }
}
