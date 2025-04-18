package me.erykczy.colorfullighting;

import me.erykczy.colorfullighting.accessors.MinecraftWrapper;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.accessors.ClientAccessor;
import me.erykczy.colorfullighting.event.ClientEventListener;
import me.erykczy.colorfullighting.resourcemanager.ModResourceManagers;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(ColorfulLighting.MOD_ID)
public class ColorfulLighting
{
    public static final String MOD_ID = "colorfullighting";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static ClientAccessor clientAccessor;

    public ColorfulLighting(IEventBus modEventBus, ModContainer modContainer)
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
