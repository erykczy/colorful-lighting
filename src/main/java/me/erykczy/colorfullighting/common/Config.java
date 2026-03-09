package me.erykczy.colorfullighting.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.erykczy.colorfullighting.common.accessors.BlockStateAccessor;
import me.erykczy.colorfullighting.common.accessors.LevelAccessor;
import me.erykczy.colorfullighting.common.util.ColorRGB4;
import me.erykczy.colorfullighting.common.util.JsonHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    public static final ColorRGB4 defaultColor = ColorRGB4.fromRGB4(15, 15, 15);
    private static HashMap<ResourceLocation, BlockEmitterConfig> colorEmitters = new HashMap<>();
    private static HashMap<ResourceLocation, BlockFilterConfig> colorFilters = new HashMap<>();
    private static HashMap<ResourceLocation, ColorEmitter> entityEmitters = new HashMap<>();
    private static HashMap<ResourceLocation, ColorEmitter> itemEmitters = new HashMap<>();
    private static Map<Integer, ColorMoonPhase> moonPhases = new HashMap<>();

    public static void setColorEmitters(HashMap<ResourceLocation, BlockEmitterConfig> colors) {
        colorEmitters = colors;
    }

    public static void setColorFilters(HashMap<ResourceLocation, BlockFilterConfig> filters) {
        colorFilters = filters;
    }

    public static void setEntityEmitters(HashMap<ResourceLocation, ColorEmitter> emitters) {
        entityEmitters = emitters;
    }

    public static void setItemEmitters(HashMap<ResourceLocation, ColorEmitter> emitters) {
        itemEmitters = emitters;
    }

    public static void setMoonPhases(Map<Integer, ColorMoonPhase> phases) {
        moonPhases = new HashMap<>(phases);
    }

    public static float getMoonVibrancy(int phase) {
        ColorMoonPhase moonPhase = moonPhases.get(phase);
        if (moonPhase != null) {
            return moonPhase.vibrancy();
        }
        // Fallback to original calculation if not defined
        int dist = Math.abs(phase - 4);
        float normalizedDist = dist / 4.0f;
        return 1.0f - normalizedDist;
    }

    public static ColorRGB4 getColorEmission(@NotNull LevelAccessor level, BlockPos pos) { return getColorEmission(level, pos, level.getBlockState(pos)); }
    public static ColorRGB4 getColorEmission(@NotNull LevelAccessor level, BlockPos pos, @NotNull BlockStateAccessor blockState) {
        float lightEmission = blockState.getLightEmission(level, pos)/15.0f;

        ResourceKey<Block> blockResourceKey = blockState.getBlockKey();

        if(blockResourceKey != null) {
            // Dynamic Lighting Integration
            if (blockResourceKey.location().equals(ForgeRegistries.BLOCKS.getKey(Blocks.LIGHT))) {
                ColorRGB4 dynamicColor = getDynamicColorFromNearbyEntities(pos);
                if (dynamicColor != null) {
                    return dynamicColor.mul(lightEmission);
                }
            }

            BlockEmitterConfig config = colorEmitters.get(blockResourceKey.location());
            if(config != null) {
                ColorEmitter emitter = config.getEmitter(blockState);
                if (emitter != null) {
                    return emitter.color().mul(emitter.overriddenBrightness4 < 0 ? lightEmission : emitter.overriddenBrightness4 / 15.0f);
                }
            }
        }
        return defaultColor.mul(lightEmission);
    }
    
    private static ColorRGB4 getDynamicColorFromNearbyEntities(BlockPos lightBlockPos) {
        if (Minecraft.getInstance().level == null) return null;
        
        // Search for entities within a small radius
        AABB searchBox = new AABB(lightBlockPos).inflate(2.0);
        List<Entity> nearbyEntities = Minecraft.getInstance().level.getEntitiesOfClass(Entity.class, searchBox);

        for (Entity entity : nearbyEntities) {
            if (entity instanceof Player player) {
                // Check Main Hand
                ItemStack mainHand = player.getMainHandItem();
                ResourceLocation mainHandKey = ForgeRegistries.ITEMS.getKey(mainHand.getItem());
                if (mainHandKey != null) {
                    BlockEmitterConfig config = colorEmitters.get(mainHandKey);
                    if (config != null) return config.defaultEmitter.color();
                }
                
                // Check Off Hand
                ItemStack offHand = player.getOffhandItem();
                ResourceLocation offHandKey = ForgeRegistries.ITEMS.getKey(offHand.getItem());
                if (offHandKey != null) {
                    BlockEmitterConfig config = colorEmitters.get(offHandKey);
                    if (config != null) return config.defaultEmitter.color();
                }
            } else if (entity instanceof ItemEntity itemEntity) {
                ItemStack itemStack = itemEntity.getItem();
                ResourceLocation itemKey = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
                if (itemKey != null) {
                    BlockEmitterConfig config = colorEmitters.get(itemKey);
                    if (config != null) return config.defaultEmitter.color();
                }
            }

            ResourceLocation entityKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (entityKey != null) {
                ColorEmitter config = entityEmitters.get(entityKey);
                if (config != null) return config.color();
            }
        }
        return null;
    }

    public static ColorRGB4 getLightColor(@NotNull BlockStateAccessor blockState) {
        return getLightColor(blockState.getBlockKey());
    }
    public static ColorRGB4 getLightColor(@Nullable ResourceKey<Block> blockLocation) {
        if(blockLocation != null) {
            BlockEmitterConfig config = colorEmitters.get(blockLocation.location());
            if(config != null && config.defaultEmitter != null)
                return config.defaultEmitter.color();
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
        BlockFilterConfig config = colorFilters.get(blockResourceKey.location());
        if(config == null) return ColorRGB4.fromRGB4(15, 15, 15);
        ColorFilter filter = config.getFilter(blockState);
        return filter != null ? filter.transmittance : ColorRGB4.fromRGB4(15, 15, 15);
    }

    public static ColorRGB4 getColoredLightTransmittance(@NotNull LevelAccessor level, BlockPos pos, @NotNull BlockStateAccessor blockState, Direction direction) {
        ColorRGB4 baseColor = getColoredLightTransmittance(level, pos, blockState);
        if (baseColor.equals(ColorRGB4.WHITE)) return baseColor; // No filter

        // Check for Glass Pane logic
        String north = blockState.getPropertyString("north");
        String south = blockState.getPropertyString("south");
        String east = blockState.getPropertyString("east");
        String west = blockState.getPropertyString("west");

        if (north != null && south != null && east != null && west != null) {
            // It's a pane-like block
            if (direction.getAxis().isVertical()) return ColorRGB4.WHITE;

            boolean hasNorth = north.equals("true");
            boolean hasSouth = south.equals("true");
            boolean hasEast = east.equals("true");
            boolean hasWest = west.equals("true");

            if (direction.getAxis() == Direction.Axis.X) { // East/West movement
                if (hasNorth || hasSouth) return baseColor; // Blocked by N-S glass
                return ColorRGB4.WHITE;
            }
            if (direction.getAxis() == Direction.Axis.Z) { // North/South movement
                if (hasEast || hasWest) return baseColor; // Blocked by E-W glass
                return ColorRGB4.WHITE;
            }
        }

        return baseColor;
    }
    
    public static int getLightAbsorption(@NotNull LevelAccessor level, BlockPos pos, @NotNull BlockStateAccessor blockState) {
        ResourceKey<Block> blockResourceKey = blockState.getBlockKey();
        if(blockResourceKey == null) return -1;
        BlockFilterConfig config = colorFilters.get(blockResourceKey.location());
        if(config == null) return -1;
        ColorFilter filter = config.getFilter(blockState);
        return filter != null ? filter.absorption : -1;
    }

    public static int getEmissionBrightness(@NotNull LevelAccessor level, BlockPos pos, int defaultValue) {
        var blockState = level.getBlockState(pos);
        return blockState == null ? defaultValue : getEmissionBrightness(level, pos, blockState);
    }
    public static int getEmissionBrightness(@NotNull LevelAccessor level, BlockPos pos, @NotNull BlockStateAccessor blockState) {
        ResourceKey<Block> blockResourceKey = blockState.getBlockKey();

        if(blockResourceKey != null) {
            BlockEmitterConfig config = colorEmitters.get(blockResourceKey.location());
            if(config != null) {
                ColorEmitter emitter = config.getEmitter(blockState);
                if (emitter != null && emitter.overriddenBrightness4 >= 0) {
                    return emitter.overriddenBrightness4;
                }
            }
        }
        return blockState.getLightEmission(level, pos);
    }
    public static int getEmissionBrightness(BlockStateAccessor blockState) {
        ResourceKey<Block> blockResourceKey = blockState.getBlockKey();
        if(blockResourceKey != null) {
            BlockEmitterConfig config = colorEmitters.get(blockResourceKey.location());
            if(config != null) {
                ColorEmitter emitter = config.getEmitter(blockState);
                if (emitter != null && emitter.overriddenBrightness4 >= 0) {
                    return emitter.overriddenBrightness4;
                }
            }
        }
        return blockState.getLightEmission();
    }

    public static class BlockEmitterConfig {
        public final ColorEmitter defaultEmitter;
        public final Map<String, ColorEmitter> stateEmitters;

        public BlockEmitterConfig(ColorEmitter defaultEmitter, Map<String, ColorEmitter> stateEmitters) {
            this.defaultEmitter = defaultEmitter;
            this.stateEmitters = stateEmitters;
        }

        public static BlockEmitterConfig fromJsonElement(JsonElement value) {
            if (value.isJsonObject()) {
                JsonObject obj = value.getAsJsonObject();
                ColorEmitter defaultEmitter = null;
                if (obj.has("default")) {
                    defaultEmitter = ColorEmitter.fromJsonElement(obj.get("default"));
                }

                Map<String, ColorEmitter> stateEmitters = new HashMap<>();
                if (obj.has("states")) {
                    JsonObject statesObj = obj.getAsJsonObject("states");
                    for (var entry : statesObj.entrySet()) {
                        stateEmitters.put(entry.getKey(), ColorEmitter.fromJsonElement(entry.getValue()));
                    }
                }
                return new BlockEmitterConfig(defaultEmitter, stateEmitters);
            } else {
                return new BlockEmitterConfig(ColorEmitter.fromJsonElement(value), new HashMap<>());
            }
        }

        public ColorEmitter getEmitter(BlockStateAccessor blockState) {
            if (stateEmitters != null && !stateEmitters.isEmpty()) {
                for (var entry : stateEmitters.entrySet()) {
                    String[] statePairs = entry.getKey().split(",");
                    boolean allMatch = true;
                    for (String pair : statePairs) {
                        String[] kv = pair.split("=");
                        if (kv.length == 2) {
                            String propName = kv[0].trim();
                            String targetValue = kv[1].trim();
                            String propValue = blockState.getPropertyString(propName);

                            if (propValue == null || !propValue.equals(targetValue)) {
                                allMatch = false;
                                break;
                            }
                        }
                    }
                    if (allMatch) {
                        return entry.getValue();
                    }
                }
            }
            return defaultEmitter;
        }
    }
    
    public static class BlockFilterConfig {
        public final ColorFilter defaultFilter;
        public final Map<String, ColorFilter> stateFilters;

        public BlockFilterConfig(ColorFilter defaultFilter, Map<String, ColorFilter> stateFilters) {
            this.defaultFilter = defaultFilter;
            this.stateFilters = stateFilters;
        }

        public static BlockFilterConfig fromJsonElement(JsonElement value) {
            if (value.isJsonObject()) {
                JsonObject obj = value.getAsJsonObject();
                ColorFilter defaultFilter = null;
                if (obj.has("default")) {
                    defaultFilter = ColorFilter.fromJsonElement(obj.get("default"));
                }

                Map<String, ColorFilter> stateFilters = new HashMap<>();
                if (obj.has("states")) {
                    JsonObject statesObj = obj.getAsJsonObject("states");
                    for (var entry : statesObj.entrySet()) {
                        stateFilters.put(entry.getKey(), ColorFilter.fromJsonElement(entry.getValue()));
                    }
                }
                return new BlockFilterConfig(defaultFilter, stateFilters);
            } else {
                return new BlockFilterConfig(ColorFilter.fromJsonElement(value), new HashMap<>());
            }
        }

        public ColorFilter getFilter(BlockStateAccessor blockState) {
            if (stateFilters != null && !stateFilters.isEmpty()) {
                for (var entry : stateFilters.entrySet()) {
                    String[] statePairs = entry.getKey().split(",");
                    boolean allMatch = true;
                    for (String pair : statePairs) {
                        String[] kv = pair.split("=");
                        if (kv.length == 2) {
                            String propName = kv[0].trim();
                            String targetValue = kv[1].trim();
                            String propValue = blockState.getPropertyString(propName);

                            if (propValue == null || !propValue.equals(targetValue)) {
                                allMatch = false;
                                break;
                            }
                        }
                    }
                    if (allMatch) {
                        return entry.getValue();
                    }
                }
            }
            return defaultFilter;
        }
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
                int brightness = Integer.parseInt(args[1]);
                if(brightness >= 0 && brightness <= 15) return brightness;
            }
            catch (NumberFormatException ignore) {}
            
            try {
                int brightness = Integer.parseInt(args[1], 16);
                if(brightness >= 0 && brightness <= 15) return brightness;
            }
            catch (NumberFormatException ignore) {}
            
            return null;
        }
    }
    public record ColorFilter(ColorRGB4 transmittance, int absorption) {
        public static ColorFilter fromJsonElement(JsonElement value) throws IllegalArgumentException {
            ColorRGB4 color = getColorFromJsonElement(value);
            Integer absorption = getAbsorptionFromJsonElement(value);
            if(color == null) throw new IllegalArgumentException("Invalid color.");
            return new ColorFilter(color, absorption != null ? absorption : -1);
        }

        private static ColorRGB4 getColorFromJsonElement(JsonElement value) {
            if(value.isJsonArray()) {
                var array = value.getAsJsonArray();
                if(array.size() < 3) return null;
                return JsonHelper.getColor4FromJsonElements(array.get(0), array.get(1), array.get(2));
            }
            return JsonHelper.getColor4FromString(value.getAsString().split(";")[0]);
        }
        
        private static Integer getAbsorptionFromJsonElement(JsonElement value) {
            if(value.isJsonArray()) {
                var array = value.getAsJsonArray();
                if(array.size() < 4) return -1;
                return JsonHelper.getInt4FromJsonElement(array.get(3));
            }
            String[] args = value.getAsString().split(";");
            if(args.length < 2) return -1;
            try {
                int absorption = Integer.parseInt(args[1]);
                if(absorption >= 0 && absorption <= 15) return absorption;
            }
            catch (NumberFormatException ignore) {}
            
            try {
                int absorption = Integer.parseInt(args[1], 16);
                if(absorption >= 0 && absorption <= 15) return absorption;
            }
            catch (NumberFormatException ignore) {}
            
            return null;
        }
    }

    public record ColorMoonPhase(float vibrancy) {
        public static ColorMoonPhase fromJsonElement(JsonElement value) throws IllegalArgumentException {
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                float vibrancy = value.getAsFloat();
                if (vibrancy < 0.0f || vibrancy > 1.0f) {
                    throw new IllegalArgumentException("Vibrancy must be between 0.0 and 1.0.");
                }
                return new ColorMoonPhase(vibrancy);
            }
            throw new IllegalArgumentException("Invalid moon phase value.");
        }
    }
}
