package com.example.examplemod.client.resourcemanager;

import com.example.examplemod.ColoredLightManager;
import com.example.examplemod.Config;
import com.example.examplemod.ExampleMod;
import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

import java.util.HashMap;

public class ConfigResourceManager implements ResourceManagerReloadListener {
    private static final Gson GSON = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
    private static final Logger LOGGER = ExampleMod.LOGGER;

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        HashMap<ResourceLocation, Config.ColorEmitter> emitters = new HashMap<>();
        HashMap<ResourceLocation, Config.ColorFilter> filters = new HashMap<>();

        resourceManager.listPacks().forEach((pack) -> {
            for(String namespace : pack.getNamespaces(PackType.CLIENT_RESOURCES)) {
                for(Resource resource : resourceManager.getResourceStack(ResourceLocation.fromNamespaceAndPath(namespace, "lights/emitters.json"))) {
                    try {
                        JsonObject object = GSON.fromJson(resource.openAsReader(), JsonObject.class);
                        for(var entry : object.entrySet()) {
                            try {
                                var key = ResourceLocation.parse(entry.getKey());
                                if(!BuiltInRegistries.BLOCK.containsKey(key)) throw new IllegalArgumentException("Couldn't find block "+key);
                                emitters.put(key, Config.ColorEmitter.fromJsonElement(entry.getValue()));
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

                for(Resource resource : resourceManager.getResourceStack(ResourceLocation.fromNamespaceAndPath(namespace, "lights/filters.json"))) {
                    try {
                        JsonObject object = GSON.fromJson(resource.openAsReader(), JsonObject.class);
                        for(var entry : object.entrySet()) {
                            try {
                                var key = ResourceLocation.parse(entry.getKey());
                                if(!BuiltInRegistries.BLOCK.containsKey(key)) throw new IllegalArgumentException("Couldn't find block "+key);
                                filters.put(key, Config.ColorFilter.fromJsonElement(entry.getValue()));
                            }
                            catch (Exception e) {
                                LOGGER.warn("Failed to load color filter entry {} from pack {}", entry.toString(), resource.sourcePackId(), e);
                            }
                        }
                    }
                    catch (Exception e) {
                        LOGGER.warn("Failed to load color filters from pack {}", resource.sourcePackId(), e);
                    }
                }
            }
        });

        Config.setColorEmitters(emitters);
        Config.setColorFilters(filters);
        ColoredLightManager.getInstance().refreshLevel();
    }
}
