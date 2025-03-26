package com.example.examplemod.util;

import com.example.examplemod.client.ModVertexFormatElements;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Vec3i;
import net.minecraft.util.FastColor;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class BufferUtils {
    public static void setLightColor(BufferBuilder buffer, int red, int green, int blue) {
        long i = buffer.beginElement(ModVertexFormatElements.LIGHT_COLOR);
        if (i != -1L) {
            MemoryUtil.memPutByte(i, (byte)red);
            MemoryUtil.memPutByte(i + 1L, (byte)green);
            MemoryUtil.memPutByte(i + 2L, (byte)blue);
        }
    }

    public static void putBulkData(
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
            int lightRed,
            int lightGreen,
            int lightBlue
    ) {
        int[] aint = quad.getVertices();
        Vec3i vec3i = quad.getDirection().getNormal();
        Matrix4f matrix4f = pose.pose();
        Vector3f vector3f = pose.transformNormal((float)vec3i.getX(), (float)vec3i.getY(), (float)vec3i.getZ(), new Vector3f());
        int i = 8;
        int j = aint.length / 8;
        int k = (int)(alpha * 255.0F);

        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            ByteBuffer bytebuffer = memorystack.malloc(DefaultVertexFormat.BLOCK.getVertexSize());
            IntBuffer intbuffer = bytebuffer.asIntBuffer();

            for (int l = 0; l < j; l++) {
                intbuffer.clear();
                intbuffer.put(aint, l * 8, 8);
                float f = bytebuffer.getFloat(0);
                float f1 = bytebuffer.getFloat(4);
                float f2 = bytebuffer.getFloat(8);
                float f3;
                float f4;
                float f5;
                if (readAlpha) {
                    float f6 = (float)(bytebuffer.get(12) & 255);
                    float f7 = (float)(bytebuffer.get(13) & 255);
                    float f8 = (float)(bytebuffer.get(14) & 255);
                    f3 = f6 * brightness[l] * red;
                    f4 = f7 * brightness[l] * green;
                    f5 = f8 * brightness[l] * blue;
                } else {
                    f3 = brightness[l] * red * 255.0F;
                    f4 = brightness[l] * green * 255.0F;
                    f5 = brightness[l] * blue * 255.0F;
                }

                // Neo: also apply alpha that's coming from the baked quad
                int vertexAlpha = readAlpha ? (int)((alpha * (float) (bytebuffer.get(15) & 255) / 255.0F) * 255) : k;
                int i1 = FastColor.ARGB32.color(vertexAlpha, (int)f3, (int)f4, (int)f5);
                int j1 = buffer.applyBakedLighting(lightmap[l], bytebuffer);
                float f10 = bytebuffer.getFloat(16);
                float f9 = bytebuffer.getFloat(20);
                Vector3f vector3f1 = matrix4f.transformPosition(f, f1, f2, new Vector3f());
                buffer.applyBakedNormals(vector3f, bytebuffer, pose.normal());
                buffer.addVertex(vector3f1.x(), vector3f1.y(), vector3f1.z(), i1, f10, f9, packedOverlay, j1, vector3f.x(), vector3f.y(), vector3f.z());
                setLightColor(buffer, lightRed, lightGreen, lightBlue);
            }
        }
    }
}
