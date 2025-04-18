package com.example.examplemod;

import com.example.examplemod.accessors.MinecraftWrapper;
import com.example.examplemod.common.ColoredLightEngine;
import com.example.examplemod.common.accessors.ClientAccessor;
import com.example.examplemod.event.ClientEventListener;
import com.example.examplemod.resourcemanager.ModResourceManagers;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(ExampleMod.MOD_ID)
public class ExampleMod
{
    public static final String MOD_ID = "examplemod";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static ClientAccessor clientAccessor;

    public ExampleMod(IEventBus modEventBus, ModContainer modContainer)
    {
        ModResourceManagers.register(modEventBus);
        NeoForge.EVENT_BUS.register(new ClientEventListener());
        modEventBus.addListener(this::onLoadingComplete);
    }

    private void onLoadingComplete(FMLLoadCompleteEvent event) {
        clientAccessor = new MinecraftWrapper(Minecraft.getInstance());
        ColoredLightEngine.create(clientAccessor);
    }
}
