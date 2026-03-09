package me.erykczy.colorfullighting.resourcemanager;

import me.erykczy.colorfullighting.ColorfulLighting;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.Config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.ToNumberPolicy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class ConfigResourceManager implements ResourceManagerReloadListener {
    private static final Gson GSON = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
    private static final Logger LOGGER = ColorfulLighting.LOGGER;

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        HashMap<ResourceLocation, Config.BlockEmitterConfig> emitters = new HashMap<>();
        HashMap<ResourceLocation, Config.BlockFilterConfig> filters = new HashMap<>();
        HashMap<ResourceLocation, Config.ColorEmitter> entityEmitters = new HashMap<>();
        HashMap<ResourceLocation, Config.ColorEmitter> itemEmitters = new HashMap<>();
        Map<Integer, Config.ColorMoonPhase> moonPhases = new HashMap<>();

        resourceManager.listPacks().forEach((pack) -> {
            for(String namespace : pack.getNamespaces(PackType.CLIENT_RESOURCES)) {
                for(Resource resource : resourceManager.getResourceStack(ResourceLocation.tryBuild(namespace, "light/emitters.json"))) {
                    try {
                        JsonObject object = GSON.fromJson(resource.openAsReader(), JsonObject.class);
                        for(var entry : object.entrySet()) {
                            try {
                                var key = ResourceLocation.tryParse(entry.getKey());
                                if(!BuiltInRegistries.BLOCK.containsKey(key)) throw new IllegalArgumentException("Couldn't find block "+key);
                                emitters.put(key, Config.BlockEmitterConfig.fromJsonElement(entry.getValue()));
                            }
                            catch (Exception e) {
                                LOGGER.warn("Failed to load light emitter entry {} from pack {}", entry.toString(), resource.sourcePackId(), e);
                            }
                        }
                    }
                    catch (Exception e) {
                        LOGGER.warn("Failed to load light emitters from pack {}", resource.sourcePackId(), e);
                    }
                }

                for(Resource resource : resourceManager.getResourceStack(ResourceLocation.tryBuild(namespace, "light/filters.json"))) {
                    try {
                        JsonObject object = GSON.fromJson(resource.openAsReader(), JsonObject.class);
                        for(var entry : object.entrySet()) {
                            try {
                                var key = ResourceLocation.tryParse(entry.getKey());
                                if(!BuiltInRegistries.BLOCK.containsKey(key)) throw new IllegalArgumentException("Couldn't find block "+key);
                                filters.put(key, Config.BlockFilterConfig.fromJsonElement(entry.getValue()));
                            }
                            catch (Exception e) {
                                LOGGER.warn("Failed to load light color filter entry {} from pack {}", entry.toString(), resource.sourcePackId(), e);
                            }
                        }
                    }
                    catch (Exception e) {
                        LOGGER.warn("Failed to load light color filters from pack {}", resource.sourcePackId(), e);
                    }
                }

                for(Resource resource : resourceManager.getResourceStack(ResourceLocation.tryBuild(namespace, "light/entities.json"))) {
                    try {
                        JsonObject object = GSON.fromJson(resource.openAsReader(), JsonObject.class);
                        for(var entry : object.entrySet()) {
                            try {
                                var key = ResourceLocation.tryParse(entry.getKey());
                                if(!BuiltInRegistries.ENTITY_TYPE.containsKey(key)) throw new IllegalArgumentException("Couldn't find entity type "+key);
                                entityEmitters.put(key, Config.ColorEmitter.fromJsonElement(entry.getValue()));
                            }
                            catch (Exception e) {
                                LOGGER.warn("Failed to load light entity entry {} from pack {}", entry.toString(), resource.sourcePackId(), e);
                            }
                        }
                    }
                    catch (Exception e) {
                        LOGGER.warn("Failed to load light entities from pack {}", resource.sourcePackId(), e);
                    }
                }

                for(Resource resource : resourceManager.getResourceStack(ResourceLocation.tryBuild(namespace, "light/items.json"))) {
                    try {
                        JsonObject object = GSON.fromJson(resource.openAsReader(), JsonObject.class);
                        for(var entry : object.entrySet()) {
                            try {
                                var key = ResourceLocation.tryParse(entry.getKey());
                                if(!BuiltInRegistries.ITEM.containsKey(key)) throw new IllegalArgumentException("Couldn't find item "+key);
                                itemEmitters.put(key, Config.ColorEmitter.fromJsonElement(entry.getValue()));
                            }
                            catch (Exception e) {
                                LOGGER.warn("Failed to load light item entry {} from pack {}", entry.toString(), resource.sourcePackId(), e);
                            }
                        }
                    }
                    catch (Exception e) {
                        LOGGER.warn("Failed to load light items from pack {}", resource.sourcePackId(), e);
                    }
                }

                for(Resource resource : resourceManager.getResourceStack(ResourceLocation.tryBuild(namespace, "light/moon_phases.json"))) {
                    try {
                        JsonObject object = GSON.fromJson(resource.openAsReader(), JsonObject.class);
                        for(var entry : object.entrySet()) {
                            try {
                                int phase = Integer.parseInt(entry.getKey());
                                if (phase < 0 || phase > 7) throw new IllegalArgumentException("Moon phase must be between 0 and 7.");
                                moonPhases.put(phase, Config.ColorMoonPhase.fromJsonElement(entry.getValue()));
                            }
                            catch (Exception e) {
                                LOGGER.warn("Failed to load moon phase entry {} from pack {}", entry.toString(), resource.sourcePackId(), e);
                            }
                        }
                    }
                    catch (Exception e) {
                        LOGGER.warn("Failed to load moon phases from pack {}", resource.sourcePackId(), e);
                    }
                }
            }
        });

        Config.setColorEmitters(emitters);
        Config.setColorFilters(filters);
        Config.setEntityEmitters(entityEmitters);
        Config.setItemEmitters(itemEmitters);
        Config.setMoonPhases(moonPhases);
        if(ColorfulLighting.clientAccessor.getLevel() != null)
            ColoredLightEngine.getInstance().reset();
    }
}
