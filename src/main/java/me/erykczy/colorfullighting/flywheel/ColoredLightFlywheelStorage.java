package me.erykczy.colorfullighting.flywheel;

import dev.engine_room.flywheel.backend.engine.CpuArena;
import dev.engine_room.flywheel.backend.engine.indirect.ResizableStorageArray;
import dev.engine_room.flywheel.backend.engine.indirect.StagingBuffer;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryUtil;

import java.util.BitSet;

public class ColoredLightFlywheelStorage {
    public static final int BLOCKS_PER_SECTION = 18 * 18 * 18;
    public static final int LIGHT_SIZE_BYTES = BLOCKS_PER_SECTION * 4;
    public static final int SECTION_SIZE_BYTES = LIGHT_SIZE_BYTES;
    private static final int DEFAULT_ARENA_CAPACITY_SECTIONS = 64;
    private static final int INVALID_SECTION = -1;

    public static final int COLORED_LIGHT_BUFFER_BINDING = 8;

    public final CpuArena arena;
    private final Long2IntMap section2ArenaIndex;
    private final BitSet changed = new BitSet();
    private final SlowLightCollector collector;

    private final ResizableStorageArray sectionsGPUBuffer = new ResizableStorageArray(SECTION_SIZE_BYTES);

    public ColoredLightFlywheelStorage() {
        this.arena = new CpuArena(SECTION_SIZE_BYTES, DEFAULT_ARENA_CAPACITY_SECTIONS);
        this.section2ArenaIndex = new Long2IntOpenHashMap();
        this.section2ArenaIndex.defaultReturnValue(INVALID_SECTION);
        this.collector = new SlowLightCollector();
    }

    public int capacity() {
        return arena.capacity();
    }

    public void delete() {
        arena.delete();
    }

    private int indexForSection(long section) {
        int out = section2ArenaIndex.get(section);

        // Need to allocate.
        if (out == INVALID_SECTION) {
            out = arena.alloc();
            section2ArenaIndex.put(section, out);
        }
        return out;
    }

    public void removeSection(long section) {
        arena.free(indexForSection(section));
        section2ArenaIndex.remove(section);
    }

    public void collectSection(long section) {
        int index = indexForSection(section);

        changed.set(index);

        long ptr = arena.indexToPointer(index);

        // Zero it out first. This is basically free and makes it easier to handle missing sections later.
        MemoryUtil.memSet(ptr, 0, SECTION_SIZE_BYTES);

        collector.collectLightData(ptr, section);
    }

    public void uploadChangedSections(StagingBuffer staging) {
        sectionsGPUBuffer.ensureCapacity(capacity());
        for (int i = changed.nextSetBit(0); i >= 0; i = changed.nextSetBit(i + 1)) {
            staging.enqueueCopy(arena.indexToPointer(i), SECTION_SIZE_BYTES, sectionsGPUBuffer.handle(), i * SECTION_SIZE_BYTES);
        }
        changed.clear();
    }

    public void bindBuffers() {
        GL46.glBindBufferRange(GL46.GL_SHADER_STORAGE_BUFFER, COLORED_LIGHT_BUFFER_BINDING, sectionsGPUBuffer.handle(), 0, sectionsGPUBuffer.byteCapacity());
    }
}
