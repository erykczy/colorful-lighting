package com.example.examplemod.block;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.block.entity.TestBlockEntity;
import com.example.examplemod.block.renderer.TestBlockRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ExampleMod.MOD_ID);
    public static DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, ExampleMod.MOD_ID);

    public static DeferredBlock<TestBlock> TEST_BLOCK = BLOCKS.register("test_block", TestBlock::new);
    public static Supplier<BlockEntityType<TestBlockEntity>> TEST_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register("test_block", () -> BlockEntityType.Builder.of(TestBlockEntity::new, TEST_BLOCK.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITY_TYPES.register(bus);
        bus.addListener(ModBlocks::onRegisterRenderers);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TEST_BLOCK_ENTITY_TYPE.get(), TestBlockRenderer::new);
    }
}
