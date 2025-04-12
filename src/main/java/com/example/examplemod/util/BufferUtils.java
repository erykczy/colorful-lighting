package com.example.examplemod.util;

import com.example.examplemod.ColoredLightManager;
import com.example.examplemod.client.ModVertexFormatElements;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.util.LinkedList;

public class BufferUtils {
   /* private static void setLightColorIfAbsent(BufferBuilder buffer, ColorRGB8 lightColor) {
        long i = buffer.beginElement(ModVertexFormatElements.LIGHT_COLOR);
        if (i != -1L) {
            forceSetLightColor(buffer, lightColor);
        }
    }*/

    public static void forceSetLightColor(VertexConsumer buffer, ColorRGB8 lightColor) { if(buffer instanceof BufferBuilder bufferBuilder) forceSetLightColor(bufferBuilder, lightColor); }
    public static void forceSetLightColor(BufferBuilder buffer, ColorRGB8 lightColor) {
        lightColor = lightColor.clamp();
        int red = lightColor.red;
        int green = lightColor.green;
        int blue = lightColor.blue;

        //long prevAttributesSize = (buffer.fullFormat ? 28L : 24L) + 7L; // see com.mojang.blaze3d.vertex.BufferBuilder::addVertex
        //long pointer = buffer.vertexPointer + prevAttributesSize;
        long pointer = buffer.vertexPointer + buffer.offsetsByElement[ModVertexFormatElements.LIGHT_COLOR.id()];
        MemoryUtil.memPutByte(pointer, (byte)red);
        MemoryUtil.memPutByte(pointer + 1L, (byte)green);
        MemoryUtil.memPutByte(pointer + 2L, (byte)blue);
        buffer.elementsToFill = buffer.elementsToFill & ~ModVertexFormatElements.LIGHT_COLOR.mask();
    }

    /*public static void setLightColor(VertexConsumer buffer, ColorRGB8 lightColor) {
        *//*if(!(buffer instanceof BufferBuilder bufferBuilder)) return;
        if(bufferBuilder.fastFormat)
            setLightColorFastFormat(bufferBuilder, lightColor);
        else
            setLightColorNotFastFormat(bufferBuilder, lightColor);*//*
    }*/

    public static boolean isLightColorFilled(BufferBuilder buffer) {
        // logic from com.mojang.blaze3d.vertex.BufferBuilder::beginElement
        int elementsToFill = buffer.elementsToFill;
        int afterFilling = elementsToFill & ~ModVertexFormatElements.LIGHT_COLOR.mask();
        return afterFilling == elementsToFill;
    }

    private static LinkedList<Vector3f> prevVertices = new LinkedList<>();
    //private static LinkedList<Vector3f> newVertices = null;
    public static void DEBUG_DO_TEST(PoseStack.Pose pose, VertexConsumer buffer, int packedLight, int packedOverlay, int color, Vector3f vector3f2) {
        if(!(buffer instanceof BufferBuilder bufferBuilder)) return;
        Vector3f cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().toVector3f();
        Vector3f vertexWorldPos = vector3f2.add(cameraPos, new Vector3f());
        //vertexWorldPos.sub(91972.5f, 26, 14348518);

        if(bufferBuilder.vertices == 3) {
            // it's first vertex
            //System.out.println(vertexWorldPos.z);

            /*StringBuilder debug = new StringBuilder();
            if(!prevVertices.isEmpty()) {
                for(var w : prevVertices) {
                    debug.append(w.toString()).append(" ");
                }
                prevVertices.clear();
            }
            System.out.println("prev collection: " + debug);*/
            /*if(!newVertices.isEmpty()) {
                boolean allTheSame = true;
                for(int i = 0; i < newVertices.size(); i++) {
                    if(prevVertices.get(i) != newVertices.get(i)) {
                        allTheSame = false;
                        break;
                    }
                }
                if(!allTheSame)
                    System.out.println("Not the same!");

                newVertices = null;
            }
            else if(!prevVertices.isEmpty()) {
                newVertices = new LinkedList<>();
            }*/
        }
        //prevVertices.add(vector3f2);
        /*if(newVertices == null)
            prevVertices.add(vector3f2);
        else
            newVertices.add(vector3f2)*/

        ColorRGB8 lightColor = ColoredLightManager.getInstance().sampleMixedLightColor(vertexWorldPos.add(0.0f, 0.0f, 0.0f, new Vector3f()));
        //if(vertexWorldPos.x > 0 && vertexWorldPos.y > 0 && vertexWorldPos.z > 0)
        //    lightColor = ColorRGB8.fromRGB8(255, 0, 0);
        //else
        float blue = vertexWorldPos.z;
        //lightColor = ColorRGB8.fromRGBFloat(1.0f - blue, 0, blue);
        //BufferUtils.setLightColor(buffer, lightColor);
        BufferUtils.forceSetLightColor(buffer, lightColor); //ColorRGB8.fromRGBFloat(vertexWorldPos.x, vertexWorldPos.y, vertexWorldPos.z).clamp()
    }

}
