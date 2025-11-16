package me.erykczy.colorfullighting.common;

import me.erykczy.colorfullighting.common.accessors.BlockStateAccessor;
import me.erykczy.colorfullighting.common.accessors.LevelAccessor;
import me.erykczy.colorfullighting.common.util.ColorRGB4;
import me.erykczy.colorfullighting.common.util.JsonHelper;
import com.google.gson.JsonElement;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class Config {
    public static final ColorRGB4 defaultColor = ColorRGB4.fromRGB4(15, 15, 15);
    private static HashMap<ResourceLocation, ColorEmitter> colorEmitters = new HashMap<>();
    private static HashMap<ResourceLocation, ColorFilter> colorFilters = new HashMap<>();

    public static void setColorEmitters(HashMap<ResourceLocation, ColorEmitter> colors) {
        colorEmitters = colors;
    }

    public static void setColorFilters(HashMap<ResourceLocation, ColorFilter> filters) {
        colorFilters = filters;
    }

    public static ColorRGB4 getColorEmission(@NotNull LevelAccessor level, BlockPos pos) { return getColorEmission(level, pos, level.getBlockState(pos)); }
    public static ColorRGB4 getColorEmission(@NotNull LevelAccessor level, BlockPos pos, @NotNull BlockStateAccessor blockState) {
        float lightEmission = blockState.getLightEmission(level, pos)/15.0f;

        ResourceKey<Block> blockResourceKey = blockState.getBlockKey();

        if(blockResourceKey != null) {
            ColorEmitter config = colorEmitters.get(blockResourceKey.location());
            if(config != null)
                return config.color().mul(config.overriddenBrightness4 < 0 ? lightEmission : config.overriddenBrightness4 /15.0f);
        }
        return defaultColor.mul(lightEmission);
    }
    public static ColorRGB4 getLightColor(@NotNull BlockStateAccessor blockState) {
        return getLightColor(blockState.getBlockKey());
    }
    public static ColorRGB4 getLightColor(@Nullable ResourceKey<Block> blockLocation) {
        if(blockLocation != null) {
            ColorEmitter config = colorEmitters.get(blockLocation.location());
            if(config != null)
                return config.color();
        }
        return defaultColor;
    }

    public static ColorRGB4 getColoredLightTransmittance(@NotNull LevelAccessor level, BlockPos pos, ColorRGB4 defaultValue) {
        var blockState = level.getBlockState(pos);
        return blockState == null ? defaultValue : getColoredLightTransmittance(level, pos, blockState);
    }
    public static ColorRGB4 getColoredLightTransmittance(@NotNull LevelAccessor level, BlockPos pos, @NotNull BlockStateAccessor blockState) {
        ResourceKey<Block> blockResourceKey = blockState.getBlockKey();
        if(blockResourceKey == null) return ColorRGB4.fromRGB4(15, 15, 15);
        ColorFilter config = colorFilters.get(blockResourceKey.location());
        if(config == null) return ColorRGB4.fromRGB4(15, 15, 15);
        return config.transmittance;
    }

    public static int getEmissionBrightness(@NotNull LevelAccessor level, BlockPos pos, int defaultValue) {
        var blockState = level.getBlockState(pos);
        return blockState == null ? defaultValue : getEmissionBrightness(level, pos, blockState);
    }
    public static int getEmissionBrightness(@NotNull LevelAccessor level, BlockPos pos, @NotNull BlockStateAccessor blockState) {
        ResourceKey<Block> blockResourceKey = blockState.getBlockKey();

        if(blockResourceKey != null) {
            ColorEmitter config = colorEmitters.get(blockResourceKey.location());
            if(config != null && config.overriddenBrightness4 >= 0)
                return config.overriddenBrightness4;
        }
        return blockState.getLightEmission(level, pos);
    }
    public static int getEmissionBrightness(BlockStateAccessor blockState) {
        ResourceKey<Block> blockResourceKey = blockState.getBlockKey();
        if(blockResourceKey != null) {
            ColorEmitter config = colorEmitters.get(blockResourceKey.location());
            if(config != null && config.overriddenBrightness4 >= 0)
                return config.overriddenBrightness4;
        }
        return blockState.getLightEmission();
    }

    /**
     * @param color light color
     * @param overriddenBrightness4 4 bit value in range 0..15, by which light color is multiplied, if -1, vanilla emission for given block is used
     */
    public record ColorEmitter(ColorRGB4 color, int overriddenBrightness4) {
        public static ColorEmitter fromJsonElement(JsonElement value) throws IllegalArgumentException {
            ColorRGB4 color = getColorFromJsonElement(value);
            Integer brightness = getBrightnessFromJsonElement(value);
            if(color == null) throw new IllegalArgumentException("Invalid color.");
            if(brightness == null) throw new IllegalArgumentException("Invalid brightness.");
            return new ColorEmitter(color, brightness);
        }

        private static ColorRGB4 getColorFromJsonElement(JsonElement value) {
            if(value.isJsonArray()) {
                var array = value.getAsJsonArray();
                if(array.size() < 3) return null;
                return JsonHelper.getColor4FromJsonElements(array.get(0), array.get(1), array.get(2));
            }
            return JsonHelper.getColor4FromString(value.getAsString().split(";")[0]);
        }

        private static Integer getBrightnessFromJsonElement(JsonElement value) {
            if(value.isJsonArray()) {
                var array = value.getAsJsonArray();
                if(array.size() < 4) return -1;
                return JsonHelper.getInt4FromJsonElement(array.get(3));
            }
            String[] args = value.getAsString().split(";");
            if(args.length < 2) return -1;
            try {
                int brightness = Integer.parseInt(args[1], 16);
                if(brightness >= 0 && brightness <= 15) return brightness;
                return null;
            }
            catch (NumberFormatException ignore) {
                return null;
            }
        }
    }
    public record ColorFilter(ColorRGB4 transmittance) {
        public static ColorFilter fromJsonElement(JsonElement value) throws IllegalArgumentException {
            ColorRGB4 color = getColorFromJsonElement(value);
            if(color == null) throw new IllegalArgumentException("Invalid color.");
            return new ColorFilter(color);
        }

        private static ColorRGB4 getColorFromJsonElement(JsonElement value) {
            if(value.isJsonArray()) {
                var array = value.getAsJsonArray();
                if(array.size() < 3) return null;
                return JsonHelper.getColor4FromJsonElements(array.get(0), array.get(1), array.get(2));
            }
            return JsonHelper.getColor4FromString(value.getAsString());
        }
    }
}
