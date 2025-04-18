package com.example.examplemod;

import com.example.examplemod.event.ClientEventListener;
import com.example.examplemod.resourcemanager.ModResourceManagers;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(ExampleMod.MOD_ID)
public class ExampleMod
{
    public static final String MOD_ID = "examplemod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ExampleMod(IEventBus modEventBus, ModContainer modContainer)
    {
        ModResourceManagers.register(modEventBus);
        NeoForge.EVENT_BUS.register(new ClientEventListener());
    }
}
