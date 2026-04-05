package me.erykczy.colorfullighting.resourcemanager;

import me.erykczy.colorfullighting.ColorfulLighting;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

public class ModResourceManagers {
    public static void register(IEventBus bus) {
        bus.addListener(ModResourceManagers::registerManagers);
    }

    @SubscribeEvent
    private static void registerManagers(AddClientReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(ColorfulLighting.MOD_ID, "config"), new ConfigResourceManager());
    }
}
