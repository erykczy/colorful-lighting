package com.example.examplemod.util;

import com.example.examplemod.client.ModVertexFormatElements;
import com.mojang.blaze3d.vertex.BufferBuilder;
import org.lwjgl.system.MemoryUtil;

public class BufferUtils {
    public static void setLightColor(BufferBuilder buffer, ColorRGB8 lightColor) {
        long i = buffer.beginElement(ModVertexFormatElements.LIGHT_COLOR);
        if (i != -1L) {
            int red = lightColor.red;
            int green = lightColor.green;
            int blue = lightColor.blue;
            if(red > 255) red = 255;
            if(green > 255) green = 255;
            if(blue > 255) blue = 255;

            MemoryUtil.memPutByte(i, (byte)red);
            MemoryUtil.memPutByte(i + 1L, (byte)green);
            MemoryUtil.memPutByte(i + 2L, (byte)blue);
        }
    }

    public static boolean isLightColorFilled(BufferBuilder buffer) {
        // logic from com.mojang.blaze3d.vertex.BufferBuilder::beginElement
        int elementsToFill = buffer.elementsToFill;
        int afterFilling = elementsToFill & ~ModVertexFormatElements.LIGHT_COLOR.mask();
        return afterFilling == elementsToFill;
    }

    public static void setLightColorFastFormat(BufferBuilder buffer, ColorRGB8 lightColor) {
        int red = lightColor.red;
        int green = lightColor.green;
        int blue = lightColor.blue;

        long prevAttributesSize = (buffer.fullFormat ? 28L : 24L) + 7L; // see com.mojang.blaze3d.vertex.BufferBuilder::addVertex
        long pointer = buffer.vertexPointer + prevAttributesSize;
        MemoryUtil.memPutByte(pointer, (byte)red);
        MemoryUtil.memPutByte(pointer + 1L, (byte)green);
        MemoryUtil.memPutByte(pointer + 2L, (byte)blue);
    }
}
