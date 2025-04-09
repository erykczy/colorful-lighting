package com.example.examplemod.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public class ModVertexFormats {
    public static final VertexFormat COLORED_LIGHT_BLOCK = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .add("BlockLightColor", ModVertexFormatElements.LIGHT_COLOR)
            .padding(2) // TODO
            .build();

    public static final VertexFormat COLORED_LIGHT_PARTICLE = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.UV0)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV2", VertexFormatElement.UV2)
            .add("BlockLightColor", ModVertexFormatElements.LIGHT_COLOR) // TODO padding
            .build();

    public static void register() {
        DefaultVertexFormat.PARTICLE = COLORED_LIGHT_PARTICLE;
    }
}
