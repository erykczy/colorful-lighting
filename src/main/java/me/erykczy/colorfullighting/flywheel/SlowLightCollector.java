package me.erykczy.colorfullighting.flywheel;

import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.util.ColorRGB4;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.PackedLightData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.lwjgl.system.MemoryUtil;

public class SlowLightCollector {
    public SlowLightCollector() {

    }

    protected void collectLightData(long ptr, long section) {
        var blockPos = new BlockPos.MutableBlockPos();
        int xMin = SectionPos.sectionToBlockCoord(SectionPos.x(section));
        int yMin = SectionPos.sectionToBlockCoord(SectionPos.y(section));
        int zMin = SectionPos.sectionToBlockCoord(SectionPos.z(section));

        for (int y = -1; y < 17; y++) {
            for (int z = -1; z < 17; z++) {
                for (int x = -1; x < 17; x++) {
                    blockPos.set(xMin + x, yMin + y, zMin + z);
                    write(ptr, x, y, z, ColoredLightEngine.getInstance().sampleLightColor(blockPos));
                }
            }
        }
    }

    protected static void write(long ptr, int x, int y, int z, ColorRGB4 color) {
        int x1 = x + 1;
        int y1 = y + 1;
        int z1 = z + 1;

        int offset = (x1 + z1 * 18 + y1 * 18 * 18) * 4;

        MemoryUtil.memPutInt(ptr + offset, PackedLightData.packData(0, ColorRGB8.fromRGB4(color)));
    }
}
