package com.example.examplemod.client.debug;

import com.example.examplemod.ColoredLightManager;
import com.example.examplemod.util.Color3;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

public class ModKeyBinds {
    public static boolean debug_test2 = true;

    public static final Lazy<KeyMapping> CLEAR_SECTIONS = Lazy.of(() -> new KeyMapping(
            "key.examplemod.clear_sections",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "Colored Lights Debug Category"
    ));
    public static final Lazy<KeyMapping> SET_SECTIONS_DIRTY = Lazy.of(() -> new KeyMapping(
            "key.examplemod.set_sections_dirty",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "Colored Lights Debug Category"
    ));
    public static final Lazy<KeyMapping> TOGGLE_LIGHT_PROPAGATION = Lazy.of(() -> new KeyMapping(
            "key.examplemod.toggle_light_propagation",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            "Colored Lights Debug Category"
    ));
    public static final Lazy<KeyMapping> CHECK_STORAGE = Lazy.of(() -> new KeyMapping(
            "key.examplemod.check_storage",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "Colored Lights Debug Category"
    ));
    public static final Lazy<KeyMapping> PROPAGATE_TEST = Lazy.of(() -> new KeyMapping(
            "key.examplemod.propagate_test",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            "Colored Lights Debug Category"
    ));
    public static final Lazy<KeyMapping> DEPROPAGATE_TEST = Lazy.of(() -> new KeyMapping(
            "key.examplemod.depropagate_test",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            "Colored Lights Debug Category"
    ));
    public static final Lazy<KeyMapping> BLOCK_TEST = Lazy.of(() -> new KeyMapping(
            "key.examplemod.block_test",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "Colored Lights Debug Category"
    ));

    public static void register(IEventBus bus) {
        bus.addListener(ModKeyBinds::registerBindings);
        NeoForge.EVENT_BUS.addListener(ModKeyBinds::onClientTick);
    }

    private static void registerBindings(RegisterKeyMappingsEvent event) {
        event.register(CLEAR_SECTIONS.get());
        event.register(SET_SECTIONS_DIRTY.get());
        event.register(TOGGLE_LIGHT_PROPAGATION.get());
        event.register(CHECK_STORAGE.get());
        event.register(PROPAGATE_TEST.get());
        event.register(DEPROPAGATE_TEST.get());
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null) return;
        Level level = player.level();

        while (CLEAR_SECTIONS.get().consumeClick()) {
            SectionPos sectionPos = SectionPos.of(player.blockPosition());
            for (int z = -1; z <= 1; z++) {
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        if(ColoredLightManager.getInstance().storage.containsSection(sectionPos.offset(x, y, z).asLong()))
                            ColoredLightManager.getInstance().storage.getSection(sectionPos.offset(x, y, z).asLong()).clear();
                    }
                }
            }
            //ColoredLightManager.getInstance().storage.getLayer(sectionPos.offset(1, 0, 0).asLong()).clear();
            player.sendSystemMessage(Component.literal("DEBUG: Sections cleared.").withColor(CommonColors.LIGHT_GRAY));
        }
        while (SET_SECTIONS_DIRTY.get().consumeClick()) {
            SectionPos sectionPos = SectionPos.of(player.blockPosition());
            ClientLevel clientLevel = player.clientLevel;
            clientLevel.setSectionDirtyWithNeighbors(sectionPos.x(), sectionPos.y(), sectionPos.z());
            player.sendSystemMessage(Component.literal("DEBUG: Sections set dirty.").withColor(CommonColors.GRAY));
        }

        while (TOGGLE_LIGHT_PROPAGATION.get().consumeClick()) {
            debug_test2 = !debug_test2;
            player.sendSystemMessage(Component.literal("TEST2: "+debug_test2).withColor(CommonColors.WHITE));
        }

        while(CHECK_STORAGE.get().consumeClick()) {
            SectionPos sectionPos = SectionPos.of(player.blockPosition());
            boolean contains = ColoredLightManager.getInstance().storage.containsSection(sectionPos.asLong());
            player.sendSystemMessage(Component.literal("CONTAINS DATA: "+contains).withColor(CommonColors.WHITE));
            if(contains) {
                //FastColor3 color = ColoredLightManager.getInstance().storage.getEntry(player.blockPosition().getX(), player.blockPosition().getY(), player.blockPosition().getZ());
                //player.sendSystemMessage(Component.literal(""+Byte.toUnsignedInt(color.red())).withColor(CommonColors.RED));
                Color3 color = ColoredLightManager.getInstance().sampleLightColor(player.blockPosition().below());
                player.sendSystemMessage(
                        Component.literal(color.red+" ").withColor(CommonColors.RED).append(
                                Component.literal(color.green+" ").withColor(CommonColors.GREEN).append(
                                        Component.literal(color.blue+" ").withColor(CommonColors.BLUE)
                                )
                        )
                );
            }
            LayerLightSectionStorage.SectionType type = level.getLightEngine().blockEngine.getDebugSectionType(sectionPos.asLong());
            player.sendSystemMessage(Component.literal(type.toString()).withColor(CommonColors.WHITE));
        }

        while (PROPAGATE_TEST.get().consumeClick()) {
            ColoredLightManager.getInstance().propagateLight(level, player.blockPosition().below(), true, null);
        }
        while (DEPROPAGATE_TEST.get().consumeClick()) {
            ColoredLightManager.getInstance().propagateLight(level, player.blockPosition().below(), false, null);
        }
        while (BLOCK_TEST.get().consumeClick()) {
            ColoredLightManager.getInstance().blockLights(level, player.blockPosition().below());
        }

        HitResult result = Minecraft.getInstance().hitResult;
        if(result != null && result.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult) result;
            BlockPos pos = blockHitResult.getBlockPos().offset(blockHitResult.getDirection().getNormal());
            if(!level.isOutsideBuildHeight(pos)) {
                if(!ColoredLightManager.getInstance().storage.containsSection(SectionPos.blockToSection(pos.asLong()))) {
                    System.out.println("Doesn't contain.");
                    return;
                }
                Color3 color = ColoredLightManager.getInstance().sampleLightColor(pos);
                Minecraft.getInstance().gui.setTimes(0, 1, 0);
                Minecraft.getInstance().gui.setTitle(Component.literal(""));
                Minecraft.getInstance().gui.setSubtitle(
                        Component.literal(color.red+" ").withColor(CommonColors.RED).append(
                            Component.literal(color.green+" ").withColor(CommonColors.GREEN).append(
                                    Component.literal(color.blue+" ").withColor(CommonColors.BLUE)
                            )
                        )
                );
            }
            //int red = ColoredLightManager.getInstance().sampleLightColor(pos).red;
        }
    }
}
