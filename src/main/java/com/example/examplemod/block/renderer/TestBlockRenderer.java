package com.example.examplemod.block.renderer;

import com.example.examplemod.block.entity.TestBlockEntity;
import com.example.examplemod.client.ModRenderTypes;
import com.example.examplemod.util.BufferUtils;
import com.example.examplemod.util.Color3;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.*;

public class TestBlockRenderer implements BlockEntityRenderer<TestBlockEntity> {

    public TestBlockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TestBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderTest(poseStack, bufferSource);
    }

    private static void renderTest(PoseStack poseStack, MultiBufferSource bufferSource) {

        BufferBuilder consumer = (BufferBuilder) bufferSource.getBuffer(ModRenderTypes.COLORED_LIGHT_SOLID);
        poseStack.pushPose();
        poseStack.translate(0.0f, 2.0f, 0.0f);
        addVertex(consumer, poseStack, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, Color.WHITE.getRGB(), new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f), 0.0f, 1.0f);
        addVertex(consumer, poseStack, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, Color.WHITE.getRGB(), new Vector3f(0.0f, 0.0f, 1.0f), new Vector3f(0.0f, 1.0f, 0.0f), 0.0f, 0.0f);
        addVertex(consumer, poseStack, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, Color.WHITE.getRGB(), new Vector3f(1.0f, 0.0f, 1.0f), new Vector3f(0.0f, 1.0f, 0.0f), 1.0f, 0.0f);
        addVertex(consumer, poseStack, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, Color.WHITE.getRGB(), new Vector3f(1.0f, 0.0f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f), 1.0f, 1.0f);
        poseStack.popPose();
    }

    private static void addVertex(BufferBuilder buffer, PoseStack stack, int packedLight, int packedOverlay, int color, Vector3f pos, Vector3f normal, float u, float v) {
        Matrix4f mat = stack.last().pose();
        Vector3f tranPos = mat.transformPosition(pos, new Vector3f(0.0f, 0.0f, 0.0f));
        Vector3f tranNormal = mat.transformDirection(normal, new Vector3f(0.0f, 0.0f, 0.0f));
        buffer.addVertex(tranPos.x, tranPos.y, tranPos.z, Color.RED.getRGB(), u, v, packedOverlay, packedLight, tranNormal.x, tranNormal.y, tranNormal.z);
        BufferUtils.setLightColor(buffer, new Color3((byte)0, (byte)255, (byte)0));
    }
}
