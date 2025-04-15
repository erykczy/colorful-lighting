package com.example.examplemod.mixin.render;

import com.example.examplemod.ColoredLightManager;
import com.example.examplemod.util.BufferUtils;
import com.example.examplemod.util.MixinBridge;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Vec3i;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    /*@Inject(method = "render", at = @At("HEAD"))
    private void coloredLights$renderHead(ItemStack itemStack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay, BakedModel p_model, CallbackInfo ci) {
        MixinBridge.itemRenderContext = displayContext;
    }
    @Inject(method = "render", at = @At("RETURN"))
    private void coloredLights$renderReturn(ItemStack itemStack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay, BakedModel p_model, CallbackInfo ci) {
        MixinBridge.itemRenderContext = ItemDisplayContext.NONE;
    }

    @Redirect(method = "renderQuadList", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIZ)V"))
    private void coloredLights$redirectPutBulkData(VertexConsumer instance, PoseStack.Pose pose, BakedQuad bakedQuad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay, boolean readExistingColor) {
        if(MixinBridge.itemRenderContext == ItemDisplayContext.GUI)
            // doesn't apply colored lighting
            instance.putBulkData(pose, bakedQuad, red, green, blue, alpha, packedLight, packedOverlay, readExistingColor);
        else
            // applies colored lighting
            putBulkData(instance, pose, bakedQuad, new float[] { 1.0F, 1.0F, 1.0F, 1.0F }, red, green, blue, alpha, new int[] { packedLight, packedLight, packedLight, packedLight }, packedOverlay, readExistingColor);
    }

    // modified code from com.mojang.blaze3d.vertex.VertexConsumer::putBulkData
    private void putBulkData(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            BakedQuad quad,
            float[] brightness,
            float red,
            float green,
            float blue,
            float alpha,
            int[] lightmap,
            int packedOverlay,
            boolean readAlpha
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
                int j1 = consumer.applyBakedLighting(lightmap[l], bytebuffer);
                float f10 = bytebuffer.getFloat(16);
                float f9 = bytebuffer.getFloat(20);
                Vector3f vector3f1 = matrix4f.transformPosition(f, f1, f2, new Vector3f());
                consumer.applyBakedNormals(vector3f, bytebuffer, pose.normal());
                consumer.addVertex(vector3f1.x(), vector3f1.y(), vector3f1.z(), i1, f10, f9, packedOverlay, j1, vector3f.x(), vector3f.y(), vector3f.z());

                Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                Vec3 vertexWorldPos = cameraPos.add(vector3f1.x, vector3f1.y, vector3f1.z);
                BufferUtils.forceSetLightColor(consumer, ColoredLightManager.getInstance().sampleTrilinearLightColor(vertexWorldPos), false);
            }
        }
    }*/
}
