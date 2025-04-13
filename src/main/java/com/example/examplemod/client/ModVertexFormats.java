package com.example.examplemod.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.simibubi.create.foundation.model.BakedQuadHelper;

public class ModVertexFormats {
    public static final VertexFormat COLORED_LIGHT_BLOCK = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .add("BlockLightColor", ModVertexFormatElements.LIGHT_COLOR)
            .padding(1)
            .build();
    public static VertexFormat COLORED_LIGHT_NEW_ENTITY = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV1", VertexFormatElement.UV1)
            .add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .add("BlockLightColor", ModVertexFormatElements.LIGHT_COLOR)
            .padding(1)
            .build();
    public static final VertexFormat COLORED_LIGHT_PARTICLE = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.UV0)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV2", VertexFormatElement.UV2)
            .add("BlockLightColor", ModVertexFormatElements.LIGHT_COLOR)
            .build();

    public static void register() {
        System.out.println(BakedQuadHelper.FORMAT);
        DefaultVertexFormat.BLOCK = COLORED_LIGHT_BLOCK;
        DefaultVertexFormat.NEW_ENTITY = COLORED_LIGHT_NEW_ENTITY;
        DefaultVertexFormat.PARTICLE = COLORED_LIGHT_PARTICLE;
    }
}
