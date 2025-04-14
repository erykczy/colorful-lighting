package com.example.examplemod.util;

import com.example.examplemod.client.ModVertexFormatElements;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.lwjgl.system.MemoryUtil;

public class BufferUtils {
    public static void forceSetLightColor(VertexConsumer buffer, ColorRGB8 lightColor, boolean useVanillaLighting) { if(buffer instanceof BufferBuilder bufferBuilder) forceSetLightColor(bufferBuilder, lightColor, useVanillaLighting); }
    public static void forceSetLightColor(BufferBuilder buffer, ColorRGB8 lightColor, boolean useVanillaLighting) {
        if(!buffer.format.contains(ModVertexFormatElements.LIGHT_COLOR)) return;
        lightColor = lightColor.clamp();
        int red = lightColor.red;
        int green = lightColor.green;
        int blue = lightColor.blue;

        long pointer = buffer.vertexPointer + buffer.offsetsByElement[ModVertexFormatElements.LIGHT_COLOR.id()];
        MemoryUtil.memPutByte(pointer, (byte)red);
        MemoryUtil.memPutByte(pointer + 1L, (byte)green);
        MemoryUtil.memPutByte(pointer + 2L, (byte)blue);
        MemoryUtil.memPutByte(pointer + 3L, (byte)(useVanillaLighting ? 255 : 0));
        buffer.elementsToFill = buffer.elementsToFill & ~ModVertexFormatElements.LIGHT_COLOR.mask();
    }

    public static boolean isLightColorFilled(BufferBuilder buffer) {
        if(!buffer.format.contains(ModVertexFormatElements.LIGHT_COLOR)) return true;
        // logic from com.mojang.blaze3d.vertex.BufferBuilder::beginElement
        int elementsToFill = buffer.elementsToFill;
        int afterFilling = elementsToFill & ~ModVertexFormatElements.LIGHT_COLOR.mask();
        return afterFilling == elementsToFill;
    }
}
