package com.example.examplemod;

import com.example.examplemod.util.FastColor3;
import net.minecraft.Util;

public class ColoredLightLayer {
    private static final int ENTRY_SIZE = 3;
    private static final int DATA_SIZE = 16 * 16 * 16 * ENTRY_SIZE;
    public byte[] data;

    public ColoredLightLayer() {

    }

    public ColoredLightLayer(byte[] data) {
        this.data = data;
        if (data.length != DATA_SIZE) {
            throw (IllegalArgumentException) Util.pauseInIde(new IllegalArgumentException("ColoredLightLayer should be "+ DATA_SIZE +" bytes not: " + data.length));
        }
    }

    public int getIndex(int x, int y, int z) {
        return (y << 8 | z << 4 | x) * ENTRY_SIZE;
    }

    public FastColor3 get(int index) {
        if(data == null)
            return new FastColor3((byte)0, (byte)0, (byte)0);
        else  {
            return new FastColor3(data[index], data[index + 1], data[index + 2]);
        }
    }

    public void set(int index, FastColor3 value) {
        if(data == null)
            data = new byte[DATA_SIZE];

        data[index] = value.red();
        data[index + 1] = value.green();
        data[index + 2] = value.blue();
    }

    public FastColor3 get(int x, int y, int z) {
        return get(getIndex(x, y, z));
    }

    public void set(int x, int y, int z, FastColor3 value) {
        set(getIndex(x, y, z), value);
    }

    public void clear() {
        this.data = null;
    }

    public ColoredLightLayer copy() {
        return this.data == null ? new ColoredLightLayer() : new ColoredLightLayer(this.data.clone());
    }
}
