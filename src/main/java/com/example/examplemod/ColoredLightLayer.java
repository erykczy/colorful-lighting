package com.example.examplemod;

import com.example.examplemod.util.Color3;
import com.example.examplemod.util.Mathi;
import net.minecraft.Util;

public class ColoredLightLayer {
    private static final int LAYER_SIZE = 16 * 16 * 16 * Entry.ENTRY_SIZE;
    public byte[] data;

    public ColoredLightLayer() {

    }

    public ColoredLightLayer(byte[] data) {
        this.data = data;
        if (data.length != LAYER_SIZE) {
            throw (IllegalArgumentException) Util.pauseInIde(new IllegalArgumentException("ColoredLightLayer should be "+ LAYER_SIZE +" bytes not: " + data.length));
        }
    }

    public int getIndex(int x, int y, int z) {
        return (y << 8 | z << 4 | x) * Entry.ENTRY_SIZE;
    }

    public Entry get(int x, int y, int z) { return get(getIndex(x, y, z)); }
    public Entry get(int index) {
        if(data == null)
            return new Entry(0, 0, 0);
        else  {
            byte byte0 = data[index];
            byte byte1 = data[index + 1];
            byte byte2 = data[index + 2];

            return new Entry(Byte.toUnsignedInt(byte0), Byte.toUnsignedInt(byte1), Byte.toUnsignedInt(byte2));
        }
    }

    public void set(int x, int y, int z, Entry value) { set(getIndex(x, y, z), value); }
    public void set(int index, Entry value) {
        if(data == null)
            data = new byte[LAYER_SIZE];
        /*if(
            value.r >= 16 || value.r < 0 ||
            value.g >= 16 || value.g < 0 ||
            value.b >= 16 || value.b < 0 ||
            value.count >= 4096 || value.count < 0
        ) {
            throw new IllegalArgumentException("Invalid ColoredLightLayer.Entry: ["+value.r+", "+value.g+", "+value.b+", "+value.count+"]");
        }*/
        if(
                value.r > 255 || value.r < 0 ||
                value.g > 255 || value.g < 0 ||
                value.b > 255 || value.b < 0
        ) {
            throw new IllegalArgumentException("Invalid ColoredLightLayer.Entry: ["+value.r+", "+value.g+", "+value.b+"]");
        }

        data[index] = (byte)value.r;
        data[index + 1] = (byte)value.g;
        data[index + 2] = (byte)value.b;
    }


    public void clear() {
        this.data = null;
    }

    public ColoredLightLayer copy() {
        return this.data == null ? new ColoredLightLayer() : new ColoredLightLayer(this.data.clone());
    }

    public static class Entry {
        private static final int ENTRY_SIZE = 3;
        private int r;
        private int g;
        private int b;

        public static Entry create(int r, int g, int b) {
            // 17.0f = 255.0f / 15.0f
            return new Entry(Math.floorDiv(r, 17), Math.floorDiv(g, 17), Math.floorDiv(b, 17));
        }

        public static Entry create(float r, float g, float b) {
            return new Entry(Mathi.floor(r * 15), Mathi.floor(g * 15), Mathi.floor(b * 15));
        }

        public static Entry create(Color3 color) {
            return create(color.red, color.green, color.blue);
        }

        private Entry(int rawRed, int rawGreen, int rawBlue) {
            this.r = rawRed;
            this.g = rawGreen;
            this.b = rawBlue;
        }

        public Color3 toColor3() {
            return new Color3(r / 15.0f, g / 15.0f, b / 15.0f);
        }
    }
}
