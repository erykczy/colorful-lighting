package com.example.examplemod;

import com.example.examplemod.block.ModBlocks;
import com.example.examplemod.client.ModRenderTypes;
import com.example.examplemod.client.ModShaders;
import com.example.examplemod.client.debug.ModKeyBinds;
import com.mojang.logging.LogUtils;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(ExampleMod.MOD_ID)
public class ExampleMod
{
    public static final String MOD_ID = "examplemod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ExampleMod(IEventBus modEventBus, ModContainer modContainer)
    {
        ModBlocks.register(modEventBus);
        ModShaders.register(modEventBus);
        ModKeyBinds.register(modEventBus);
        ModRenderTypes.register();
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onChunkUnload(ChunkEvent.Unload event) {
            ChunkAccess chunkAccess = event.getChunk();
            for(int i = 0; i < chunkAccess.getSectionsCount(); i++) {
                int y = chunkAccess.getSectionYFromSectionIndex(i);
                ColoredLightManager.getInstance().storage.removeSection(SectionPos.asLong(chunkAccess.getPos().x, y, chunkAccess.getPos().z));
            }
        }
        
        @SubscribeEvent
        public static void onTick(EntityTickEvent.Post event) {
//            if(!(event.getEntity() instanceof Player player)) return;
//            long playerBlockPos = player.blockPosition().asLong();
//            Level level = event.getEntity().level();
//            BlockLightEngine blockLightEngine = (BlockLightEngine) level.getLightEngine().getLayerListener(LightLayer.BLOCK);
//            LayerLightSectionStorage storage = blockLightEngine.storage;
//            var dataLayer = storage.getDataLayer(SectionPos.blockToSection(playerBlockPos), false);
//            if(dataLayer == null) {
//                sendMessage(player, "DataLayer is null");
//                return;
//            }
//            if(dataLayer.data == null) {
//                sendMessage(player, "DATA is null");
//                return;
//            }
//
//            sendMessage(player, "DATA length: "+dataLayer.data.length);
        }

        private static void sendMessage(Player player, String text) {
            player.sendSystemMessage(Component.literal(text));
        }
    }
}
