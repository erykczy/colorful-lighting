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
import java.io.IOException;
import java.util.HashMap;

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
                    handleJsonObject(resource, GSON.fromJson(resource.openAsReader(), JsonObject.class), colors);
                }
                catch (Exception e) {
                    LOGGER.warn("Failed to load light colors from pack {}", resource.sourcePackId(), e);
                }
            }
         }

        Config.setEmissionColors(colors);
    }

    private static void handleJsonObject(Resource resource, JsonObject object, HashMap<ResourceLocation, Config.LightColor> colors) {
        for(var entry : object.entrySet()) {
            var key = ResourceLocation.parse(entry.getKey());
            if(!BuiltInRegistries.BLOCK.containsKey(key)) {
                LOGGER.warn("Failed to load light color for block {} from pack {}", key, resource.sourcePackId());
                continue;
            }
            var rawValue = entry.getValue();

            // rgb values from array
            if(rawValue.isJsonArray()) {
                var array = rawValue.getAsJsonArray();
                if(array.size() != 3) throw new IllegalArgumentException("Array has "+array.size()+" elements instead of 3");

                ColorRGB4 color;
                try {
                    int r = array.get(0).getAsBigInteger().intValue();
                    int g = array.get(1).getAsBigInteger().intValue();
                    int b = array.get(2).getAsBigInteger().intValue();
                    color = ColorRGB4.fromRGB8(r, g, b);
                }
                catch (NumberFormatException e) {
                    float r = array.get(0).getAsFloat();
                    float g = array.get(1).getAsFloat();
                    float b = array.get(2).getAsFloat();
                    color = ColorRGB4.fromRGBFloat(r, g, b);
                }
                if(!color.isInValidState()) throw new IllegalArgumentException("Invalid color: "+color);
                colors.put(key, new Config.LightColor(color, -1));
                continue;
            }

            var valueAsString = entry.getValue().getAsString();

            // dye color name
            DyeColor dyeColor = DyeColor.byName(valueAsString, null);
            if(dyeColor != null) {
                Color color = new Color(dyeColor.getTextColor());
                ColorRGB4 color4 = ColorRGB4.fromRGB8(color.getRed(), color.getGreen(), color.getBlue());
                colors.put(key, new Config.LightColor(color4, -1));
                continue;
            }

            // hex color string
            if(valueAsString.length() == 9) {
                long colorFromHex = Long.parseLong(valueAsString.substring(1), 16);
                long alpha = colorFromHex & 0xFF;
                long blue = (colorFromHex >> 8) & 0xFF;
                long green = (colorFromHex >> 16) & 0xFF;
                long red = (colorFromHex >> 24) & 0xFF;
                ColorRGB4 color4 = ColorRGB4.fromRGB8((int)red, (int)green, (int)blue);
                colors.put(key, new Config.LightColor(color4, (int)alpha/17));
            }
            else {
                long colorFromHex = Long.parseLong(valueAsString.substring(1), 16);
                long blue = colorFromHex & 0xFF;
                long green = (colorFromHex >> 4) & 0xFF;
                long red = (colorFromHex >> 8) & 0xFF;
                ColorRGB4 color4 = ColorRGB4.fromRGB8((int)red, (int)green, (int)blue);
                colors.put(key, new Config.LightColor(color4, -1));
            }

        }
    }
}
