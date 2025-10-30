package me.erykczy.colorfullighting.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

import java.util.Objects;

/// View area is a rectangular area of chunks.
/// It consists of inner chunks and a border (of 1 chunk thickness).
/// Inner chunks actively receive propagation requests.
/// Border chunks are not updated but contain light data.
/// Such a division exists because full propagation of light source's light needs light source's chunk and neighbouring chunks.
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
    public boolean containsBlockInner(BlockPos pos) {
        return containsInner(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
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
