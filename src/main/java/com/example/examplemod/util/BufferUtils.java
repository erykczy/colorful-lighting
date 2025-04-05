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
    public static void setLightColor(BufferBuilder buffer, Color3 lightColor) {
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
        int[] vertices = quad.getVertices();
        Matrix4f poseMatrix = pose.pose();
        Vec3i untransformedNormal = quad.getDirection().getNormal();
        Vector3f normal = pose.transformNormal((float)untransformedNormal.getX(), (float)untransformedNormal.getY(), (float)untransformedNormal.getZ(), new Vector3f());
        int alphaInt = (int)(alpha * 255.0F);

        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = memorystack.malloc(DefaultVertexFormat.BLOCK.getVertexSize());
            IntBuffer intBuffer = byteBuffer.asIntBuffer();

            for (int i = 0; i < vertices.length / 8; i++) {
                intBuffer.clear();
                intBuffer.put(vertices, i * 8, 8);
                float x = byteBuffer.getFloat(0);
                float y = byteBuffer.getFloat(4);
                float z = byteBuffer.getFloat(8);
                float f3;
                float f4;
                float f5;
                if (readAlpha) {
                    float f6 = (float)(byteBuffer.get(12) & 255);
                    float f7 = (float)(byteBuffer.get(13) & 255);
                    float f8 = (float)(byteBuffer.get(14) & 255);
                    f3 = f6 * brightness[i] * red;
                    f4 = f7 * brightness[i] * green;
                    f5 = f8 * brightness[i] * blue;
                } else {
                    f3 = brightness[i] * red * 255.0F;
                    f4 = brightness[i] * green * 255.0F;
                    f5 = brightness[i] * blue * 255.0F;
                }

                // Neo: also apply alpha that's coming from the baked quad
                int vertexAlpha = readAlpha ? (int)((alpha * (float) (byteBuffer.get(15) & 255) / 255.0F) * 255) : alphaInt;
                int i1 = FastColor.ARGB32.color(vertexAlpha, (int)f3, (int)f4, (int)f5);
                int j1 = buffer.applyBakedLighting(lightmap[i], byteBuffer);
                float f10 = byteBuffer.getFloat(16);
                float f9 = byteBuffer.getFloat(20);
                Vector3f transformedPos = poseMatrix.transformPosition(x, y, z, new Vector3f());
                buffer.applyBakedNormals(normal, byteBuffer, pose.normal());
                buffer.addVertex(transformedPos.x(), transformedPos.y(), transformedPos.z(), i1, f10, f9, packedOverlay, j1, normal.x(), normal.y(), normal.z());
                BlockPos sectionOrigin = SectionPos.of(blockPos).origin();

                Color3 lightColor;
                if(Minecraft.useAmbientOcclusion())
                    lightColor = ColoredLightManager.getInstance().sampleMixedLightColor(transformedPos.add(sectionOrigin.getX(), sectionOrigin.getY(), sectionOrigin.getZ()));
                else
                     lightColor = ColoredLightManager.getInstance().sampleLightColor(blockPos.offset(quad.getDirection().getNormal()));
                setLightColor(buffer, lightColor);
            }
        }
    }
}
