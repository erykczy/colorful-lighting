package com.example.examplemod.client.resourcemanager;

import com.example.examplemod.Config;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.util.ColorRGB4;
import com.google.gson.*;
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
        HashMap<ResourceLocation, ColorRGB4> colors = new HashMap<>();

        for(String namespace : resourceManager.getNamespaces()) {
            var resources = resourceManager.getResourceStack(ResourceLocation.fromNamespaceAndPath(namespace, "lights/colors.json"));
            for(Resource resource : resources) {
                try {
                    handleJsonObject(GSON.fromJson(resource.openAsReader(), JsonObject.class), colors);
                }
                catch (Exception e) {
                    LOGGER.warn("Failed to load light colors from pack {}", resource.sourcePackId(), e);
                }
            }
         }

        Config.setEmissionColors(colors);
    }

    private static void handleJsonObject(JsonObject object, HashMap<ResourceLocation, ColorRGB4> colors) {
        for(var entry : object.entrySet()) {
            var key = ResourceLocation.parse(entry.getKey());
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
                colors.put(key, color);
                continue;
            }

            var valueAsString = entry.getValue().getAsString();

            // dye color name
            DyeColor dyeColor = DyeColor.byName(valueAsString, null);
            if(dyeColor != null) {
                Color color = new Color(dyeColor.getTextColor());
                colors.put(key, ColorRGB4.fromRGB8(color.getRed(), color.getGreen(), color.getBlue()));
                continue;
            }

            // hex color string
            var hexValue = Color.decode(valueAsString);
            colors.put(key, ColorRGB4.fromRGB8(hexValue.getRed(), hexValue.getGreen(), hexValue.getBlue()));
        }
    }
}
