package me.erykczy.colorfullighting.common;

import java.util.Objects;

public class ViewArea {
    public int minX;
    public int minZ;
    public int maxX;
    public int maxZ;

    public ViewArea() {
        this(0, 0, -1, -1);
    }

    public ViewArea(ViewArea other) {
        this.minX = other.minX;
        this.minZ = other.minZ;
        this.maxX = other.maxX;
        this.maxZ = other.maxZ;
    }

    public ViewArea(int minX, int minZ, int maxX, int maxZ) {
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
    }

    public boolean contains(int x, int z) {
        return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
    }
    public boolean containsInner(int x, int z) {
        return x > this.minX && x < this.maxX && z > this.minZ && z < this.maxZ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ViewArea viewArea = (ViewArea) o;
        return minX == viewArea.minX && minZ == viewArea.minZ && maxX == viewArea.maxX && maxZ == viewArea.maxZ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minX, minZ, maxX, maxZ);
    }
}
