package com.example.examplemod.util;

import com.example.examplemod.ColoredLightManager;
import com.example.examplemod.client.ModVertexFormatElements;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.FastColor;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

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

    // from VertexConsumer::putBulkData
    public static void putQuadWithColoredLighting(
            BufferBuilder buffer,
            PoseStack.Pose pose,
            BakedQuad quad,
            float[] brightness,
            float red,
            float green,
            float blue,
            float alpha,
            int[] lightmap,
            int packedOverlay,
            boolean readAlpha,
            BlockPos blockPos
            //Color3 lightColor
    ) {
        
    }
}
