package com.example.examplemod;

import com.example.examplemod.block.ModBlocks;
import com.example.examplemod.client.ModVertexFormats;
import com.example.examplemod.client.debug.ModKeyBinds;
import com.example.examplemod.client.resourcemanager.ModResourceManagers;
import com.mojang.logging.LogUtils;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.slf4j.Logger;

// TODO
// - check ponder
//
// * some chunks are not set dirty
// * sculk sesnor
@Mod(ExampleMod.MOD_ID)
public class ExampleMod
{
    public static final String MOD_ID = "examplemod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ExampleMod(IEventBus modEventBus, ModContainer modContainer)
    {
        ModBlocks.register(modEventBus);
        ModKeyBinds.register(modEventBus);
        ModResourceManagers.register(modEventBus);
        ModVertexFormats.register();
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        private static void onChunkLoad(ChunkEvent.Load event) {
            ChunkAccess chunkAccess = event.getChunk();
            if(!chunkAccess.getLevel().isClientSide()) return;
            ColoredLightManager.getInstance().onChunkLoad(chunkAccess.getLevel().getChunkSource(), chunkAccess);
        }

        @SubscribeEvent
        private static void onChunkUnload(ChunkEvent.Unload event) {
            ChunkAccess chunkAccess = event.getChunk();
            if(!chunkAccess.getLevel().isClientSide()) return;
            ColoredLightManager.getInstance().onChunkUnload(chunkAccess);
        }

        @SubscribeEvent
        private static void onLevelUnload(LevelEvent.Unload event) {
            if(!event.getLevel().isClientSide()) return;
            ColoredLightManager.getInstance().onLevelUnload();
        }
    }
}
