package com.example.examplemod.util;

import com.example.examplemod.ColoredLightManager;
import com.example.examplemod.client.ModVertexFormatElements;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

public class BufferUtils {
    public static void forceSetLightColor(VertexConsumer buffer, ColorRGB8 lightColor) { if(buffer instanceof BufferBuilder bufferBuilder) forceSetLightColor(bufferBuilder, lightColor); }
    public static void forceSetLightColor(BufferBuilder buffer, ColorRGB8 lightColor) {
        lightColor = lightColor.clamp();
        int red = lightColor.red;
        int green = lightColor.green;
        int blue = lightColor.blue;

        long pointer = buffer.vertexPointer + buffer.offsetsByElement[ModVertexFormatElements.LIGHT_COLOR.id()];
        MemoryUtil.memPutByte(pointer, (byte)red);
        MemoryUtil.memPutByte(pointer + 1L, (byte)green);
        MemoryUtil.memPutByte(pointer + 2L, (byte)blue);
        buffer.elementsToFill = buffer.elementsToFill & ~ModVertexFormatElements.LIGHT_COLOR.mask();
    }

    public static boolean isLightColorFilled(BufferBuilder buffer) {
        // logic from com.mojang.blaze3d.vertex.BufferBuilder::beginElement
        int elementsToFill = buffer.elementsToFill;
        int afterFilling = elementsToFill & ~ModVertexFormatElements.LIGHT_COLOR.mask();
        return afterFilling == elementsToFill;
    }

    public static void DEBUG_DO_TEST(PoseStack.Pose pose, VertexConsumer buffer, int packedLight, int packedOverlay, int color, Vector3f vertexPos) {
        if(!(buffer instanceof BufferBuilder bufferBuilder)) return;
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 vertexWorldPos = cameraPos.add(vertexPos.x, vertexPos.y, vertexPos.z);

        ColorRGB8 lightColor = ColoredLightManager.getInstance().sampleMixedLightColor(vertexWorldPos);
        BufferUtils.forceSetLightColor(buffer, lightColor);
    }

}
