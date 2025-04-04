package com.example.examplemod;

import com.example.examplemod.util.Color3;
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
            return new Entry(0, 0, 0, 0);
        else  {
            byte byte0 = data[index];
            byte byte1 = data[index + 1];
            byte byte2 = data[index + 2];
            int r = byte0 >> 4;
            int g = byte0 & 0x0F;
            int b = byte1 >> 4;
            int n = ((byte1 & 0x0F) << 8) | byte2;

            return new Entry(r, g, b, n);
        }
    }

    public void set(int x, int y, int z, Entry value) { set(getIndex(x, y, z), value); }
    public void set(int index, Entry value) {
        if(data == null)
            data = new byte[LAYER_SIZE];
        if(
            value.r >= 16 ||
            value.g >= 16 ||
            value.b >= 16 ||
            value.count >= 4096
        ) {
            throw new IllegalArgumentException("Invalid ColoredLightLayer.Entry: ["+value.r+", "+value.g+", "+value.b+", "+value.count+"]");
        }

        byte byte0 = (byte) ((value.r << 4) | value.g);
        byte byte1 = (byte) ((value.b << 4) | (value.count >> 8));
        byte byte2 = (byte) (value.count & 0xFF);

        data[index] = byte0;
        data[index + 1] = byte1;
        data[index + 2] = byte2;
    }


    public void clear() {
        this.data = null;
    }

    public ColoredLightLayer copy() {
        return this.data == null ? new ColoredLightLayer() : new ColoredLightLayer(this.data.clone());
    }

    public static class Entry {
        private static final int ENTRY_SIZE = 3;
        private static final int RED_BITS = 4;
        private static final int GREEN_BITS = 4;
        private static final int BLUE_BITS = 4;
        private static final int COUNT_BITS = 12;
        private int r;
        private int g;
        private int b;
        private int count;

        private Entry(int r, int g, int b, int n) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.count = n;
        }

        public Color3 toColor3() {
            return new Color3(r / 16.0f, g / 16.0f, b / 16.0f);
        }

        public int getCount() {
            return count;
        }
    }
}
