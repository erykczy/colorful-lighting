package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterNamedRenderTypesEvent;

public class ModRenderTypes {
    public static final RenderType COLORED_LIGHT_SOLID = RenderType.create(
            ExampleMod.MOD_ID+":colored_light_solid",
            ModVertexFormats.COLORED_LIGHT_BLOCK,
            VertexFormat.Mode.QUADS,
            4194304,
            true,
            false,
            RenderType.CompositeState.builder()
                    .setLightmapState(RenderType.LIGHTMAP)
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> ModShaders.COLORED_LIGHT_SOLID))
                    .setTextureState(RenderType.BLOCK_SHEET_MIPPED)
                    .createCompositeState(true)
    );
    public static final RenderType COLORED_LIGHT_CUTOUT_MIPPED = RenderType.create(
            ExampleMod.MOD_ID+":colored_light_cutout_mipped",
            ModVertexFormats.COLORED_LIGHT_BLOCK,
            VertexFormat.Mode.QUADS,
            4194304,
            true,
            false,
            RenderType.CompositeState.builder()
                    .setLightmapState(RenderType.LIGHTMAP)
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> ModShaders.COLORED_LIGHT_CUTOUT_MIPPED))
                    .setTextureState(RenderType.BLOCK_SHEET_MIPPED)
                    .createCompositeState(true)
    );
    public static final RenderType COLORED_LIGHT_CUTOUT = RenderType.create(
            ExampleMod.MOD_ID+":colored_light_cutout",
            ModVertexFormats.COLORED_LIGHT_BLOCK,
            VertexFormat.Mode.QUADS,
            786432,
            true,
            false,
            RenderType.CompositeState.builder()
                    .setLightmapState(RenderType.LIGHTMAP)
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> ModShaders.COLORED_LIGHT_CUTOUT))
                    .setTextureState(RenderType.BLOCK_SHEET)
                    .createCompositeState(true)
    );
    public static final RenderType COLORED_LIGHT_TRANSLUCENT = RenderType.create(
            ExampleMod.MOD_ID+":colored_light_translucent",
            ModVertexFormats.COLORED_LIGHT_BLOCK,
            VertexFormat.Mode.QUADS,
            786432,
            true,
            true,
            RenderType.CompositeState.builder()
                    .setLightmapState(RenderType.LIGHTMAP)
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> ModShaders.COLORED_LIGHT_TRANSLUCENT))
                    .setTextureState(RenderType.BLOCK_SHEET_MIPPED)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(RenderStateShard.TRANSLUCENT_TARGET)
                    .createCompositeState(true)
    );

    public static RenderType vanillaToModified(RenderType renderType) {
        if(renderType == RenderType.solid())
            return ModRenderTypes.COLORED_LIGHT_SOLID;
        if(renderType == RenderType.cutoutMipped())
            return ModRenderTypes.COLORED_LIGHT_CUTOUT_MIPPED;
        if(renderType == RenderType.cutout())
            return ModRenderTypes.COLORED_LIGHT_CUTOUT;
        if(renderType == RenderType.translucent())
            return ModRenderTypes.COLORED_LIGHT_TRANSLUCENT;
        return renderType;
    }

    public static void register() {
        RenderType.CHUNK_BUFFER_LAYERS = ImmutableList.<RenderType>builder()
                .addAll(RenderType.CHUNK_BUFFER_LAYERS)
                .add(COLORED_LIGHT_SOLID)
                .add(COLORED_LIGHT_CUTOUT_MIPPED)
                .add(COLORED_LIGHT_CUTOUT)
                .add(COLORED_LIGHT_TRANSLUCENT)
                .build();
        /*RenderType.SOLID = RenderType.create(
                "solid",
                ModVertexFormats.COLORED_LIGHT_BLOCK,
                VertexFormat.Mode.QUADS,
                4194304,
                true,
                false,
                RenderType.CompositeState.builder()
                        .setLightmapState(RenderType.LIGHTMAP)
                        .setShaderState(new RenderStateShard.ShaderStateShard(() -> ModShaders.COLORED_LIGHT_SOLID))
                        .setTextureState(RenderType.BLOCK_SHEET_MIPPED)
                        .createCompositeState(true)
        );*/
    }

}
