package com.example.examplemod.client;

import com.mojang.blaze3d.vertex.VertexFormatElement;

public class ModVertexFormatElements {
    public static final VertexFormatElement LIGHT_COLOR = VertexFormatElement.register(VertexFormatElement.findNextId(), 0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR, 4);
}
