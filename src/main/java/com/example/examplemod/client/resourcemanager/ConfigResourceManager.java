package com.example.examplemod.client.resourcemanager;

import com.example.examplemod.Config;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.util.ColorRGB4;
import com.example.examplemod.util.ColorRGB8;
import com.google.gson.*;
import com.ibm.icu.impl.InvalidFormatException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
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
                    //JsonObject jsonObject = GSON.fromJson(new InputStreamReader(resource.open(), StandardCharsets.UTF_8), JsonObject.class);
                    JsonObject jsonObject = GSON.fromJson(resource.openAsReader(), JsonObject.class);
                    for(var entry : jsonObject.entrySet()) {
                        var key = ResourceLocation.parse(entry.getKey());
                        try {
                            var value = entry.getValue();
                            if(value.isJsonArray()) {
                                var array = value.getAsJsonArray();
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
                            }
                            else {
                                var hexValue = Color.decode(entry.getValue().getAsString());
                                colors.put(key, ColorRGB4.fromRGB8(hexValue.getRed(), hexValue.getGreen(), hexValue.getBlue()));
                            }
                        }
                        catch (Exception e) {
                            LOGGER.warn("Failed to load light color for {} from pack {}", key, resource.sourcePackId(), e);
                        }
                    }

                } catch (IOException e) {
                    LOGGER.warn("Failed to load light colors from pack {}", resource.sourcePackId(), e);
                }
            }
         }

        Config.setEmissionColors(colors);
    }
}
