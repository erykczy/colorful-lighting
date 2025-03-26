package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

public class ModShaders {
    public static ShaderInstance COLORED_LIGHT_SOLID;
    public static ShaderInstance COLORED_LIGHT_CUTOUT_MIPPED;
    public static ShaderInstance COLORED_LIGHT_CUTOUT;
    public static ShaderInstance COLORED_LIGHT_TRANSLUCENT;


    public static void register(IEventBus bus) {
        bus.addListener(ModShaders::onRegisterShaders);
    }

    private static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "colored_light_solid"), ModVertexFormats.COLORED_LIGHT_BLOCK), (instance) -> COLORED_LIGHT_SOLID = instance);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "colored_light_cutout_mipped"), ModVertexFormats.COLORED_LIGHT_BLOCK), (instance) -> COLORED_LIGHT_CUTOUT_MIPPED = instance);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "colored_light_cutout"), ModVertexFormats.COLORED_LIGHT_BLOCK), (instance) -> COLORED_LIGHT_CUTOUT = instance);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "colored_light_translucent"), ModVertexFormats.COLORED_LIGHT_BLOCK), (instance) -> COLORED_LIGHT_TRANSLUCENT = instance);
        }
        catch (IOException exception) {
            System.err.println(exception.getMessage());
        }
    }
}
