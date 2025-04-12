package com.example.examplemod.client.resourcemanager;

import com.example.examplemod.Config;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.util.ColorRGB4;
import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.DyeColor;
import org.slf4j.Logger;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ConfigResourceManager implements ResourceManagerReloadListener {
    private static final Gson GSON = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
    private static final Logger LOGGER = ExampleMod.LOGGER;

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        HashMap<ResourceLocation, Config.LightColor> colors = new HashMap<>();

        for(String namespace : resourceManager.getNamespaces()) {
            var resources = resourceManager.getResourceStack(ResourceLocation.fromNamespaceAndPath(namespace, "lights/colors.json"));
            for(Resource resource : resources) {
                try {
                    JsonObject object = GSON.fromJson(resource.openAsReader(), JsonObject.class);
                    for(var entry : object.entrySet()) {
                        try {
                            handleEntry(entry, colors);
                        }
                        catch (Exception e) {
                            LOGGER.warn("Failed to load light colors entry {} from pack {}", entry.toString(), resource.sourcePackId(), e);
                        }
                    }
                }
                catch (Exception e) {
                    LOGGER.warn("Failed to load light colors from pack {}", resource.sourcePackId(), e);
                }
            }
        }

        Config.setEmissionColors(colors);
    }

    private static void handleEntry(Map.Entry<String, JsonElement> entry, HashMap<ResourceLocation, Config.LightColor> colors) {
        var key = ResourceLocation.parse(entry.getKey());
        if(!BuiltInRegistries.BLOCK.containsKey(key)) {
            //LOGGER.warn("Failed to load light color for block {} from pack {}", key, resource.sourcePackId());
            throw new IllegalArgumentException("Couldn't find block "+key);
        }
        var rawValue = entry.getValue();

        // rgb values from array
        if(rawValue.isJsonArray()) {
            var array = rawValue.getAsJsonArray();
            if(array.size() != 3 && array.size() != 4) throw new IllegalArgumentException("Array has "+array.size()+" elements instead of 3 or 4");

            ColorRGB4 color;
            int emissionBrightness4 = -1;
            try {
                int r = array.get(0).getAsBigInteger().intValue();
                int g = array.get(1).getAsBigInteger().intValue();
                int b = array.get(2).getAsBigInteger().intValue();
                if(array.size() == 4) emissionBrightness4 = array.get(3).getAsBigInteger().intValue();
                color = ColorRGB4.fromRGB8(r, g, b);
            }
            catch (NumberFormatException e) {
                float r = array.get(0).getAsFloat();
                float g = array.get(1).getAsFloat();
                float b = array.get(2).getAsFloat();
                if(array.size() == 4) emissionBrightness4 = (int)(array.get(3).getAsFloat()*15.0f);
                color = ColorRGB4.fromRGBFloat(r, g, b);
            }
            if(!color.isInValidState()) throw new IllegalArgumentException("RGB values are out of range: "+color);
            if(emissionBrightness4 >= 16) throw new IllegalArgumentException("Brightness value is out of range: "+emissionBrightness4);
            colors.put(key, new Config.LightColor(color, emissionBrightness4));
            return;
        }

        var valueAsString = entry.getValue().getAsString();
        int emissionBrightness4 = -1;
        try {
            if(valueAsString.length() >= 2) {
                char preLast = valueAsString.charAt(valueAsString.length()-2);
                if(preLast == ';') {
                    char last = valueAsString.charAt(valueAsString.length()-1);
                    emissionBrightness4 = Integer.parseInt(String.valueOf(last), 16);
                    valueAsString = valueAsString.substring(0, valueAsString.length()-2);
                }
            }
        }
        catch (NumberFormatException ignored) {}

        // dye color name
        DyeColor dyeColor = DyeColor.byName(valueAsString, null);
        if(dyeColor != null) {
            Color color = new Color(dyeColor.getTextColor());
            ColorRGB4 color4 = ColorRGB4.fromRGB8(color.getRed(), color.getGreen(), color.getBlue());
            colors.put(key, new Config.LightColor(color4, emissionBrightness4));
            return;
        }

        // hex color string
        if(valueAsString.length() != 6+1 || !valueAsString.startsWith("#")) throw new IllegalArgumentException("Invalid color: " + valueAsString);
        long colorFromHex = Long.parseLong(valueAsString.substring(1), 16);
        long blue = colorFromHex & 0xFF;
        long green = (colorFromHex >> 8) & 0xFF;
        long red = (colorFromHex >> 16) & 0xFF;
        ColorRGB4 color4 = ColorRGB4.fromRGB8((int)red, (int)green, (int)blue);
        colors.put(key, new Config.LightColor(color4, emissionBrightness4));
    }
}
