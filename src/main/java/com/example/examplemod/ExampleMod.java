package com.example.examplemod;

import com.example.examplemod.block.ModBlocks;
import com.example.examplemod.client.ModRenderTypes;
import com.example.examplemod.client.ModShaders;
import com.example.examplemod.client.debug.ModKeyBinds;
import com.mojang.logging.LogUtils;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.slf4j.Logger;

// TODO
// - check why bug0
// - check ponder
// - entities, blockentities
// - disable block light loading
// - remove vertex attribute, add light color info to overlay, override lightmap
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
                long sectionPos = SectionPos.asLong(chunkAccess.getPos().x, y, chunkAccess.getPos().z);
                ColoredLightManager.getInstance().storage.removeSection(sectionPos);
                ColoredLightManager.getInstance().dequeuePropagateLight(sectionPos);
            }
        }
    }
}
