package me.erykczy.colorfullighting.resourcemanager;

import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ModResourceManagers {
    public static void register(IEventBus bus) {
        bus.addListener(ModResourceManagers::registerManagers);
    }

    @SubscribeEvent
    public static void registerManagers(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ConfigResourceManager());
    }
}
