package me.erykczy.colorfullighting.resourcemanager;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

public class ModResourceManagers {
    public static void register(IEventBus bus) {
        bus.addListener(ModResourceManagers::registerManagers);
    }

    @SubscribeEvent
    private static void registerManagers(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ConfigResourceManager());
    }
}
