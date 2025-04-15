package com.example.examplemod.mixin.render;

import com.example.examplemod.ColoredLightManager;
import com.example.examplemod.util.BufferUtils;
import com.example.examplemod.util.ColorRGB8;
import com.example.examplemod.util.MixinBridge;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.Cube.class)
public class ModelPartCubeMixin {
    /*@Inject(method = "compile", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(FFFIFFIIFFF)V", shift = At.Shift.AFTER))
    private void coloredLights$compile(PoseStack.Pose pose, VertexConsumer buffer, int packedLight, int packedOverlay, int color, CallbackInfo ci, @Local(ordinal = 2) Vector3f vertexPos) {
        if(buffer instanceof SpriteCoordinateExpander expander)
            buffer = expander.delegate;
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 vertexWorldPos = cameraPos.add(vertexPos.x, vertexPos.y, vertexPos.z);

        if(MixinBridge.itemRenderContext == ItemDisplayContext.NONE) {
            ColorRGB8 lightColor = ColoredLightManager.getInstance().sampleTrilinearLightColor(vertexWorldPos);
            BufferUtils.forceSetLightColor(buffer, lightColor, false);
        }
        else if(MixinBridge.itemRenderContext != ItemDisplayContext.GUI) {
            ColorRGB8 lightColor = ColoredLightManager.getInstance().sampleTrilinearLightColorAtLocalPlayer();
            BufferUtils.forceSetLightColor(buffer, lightColor, false);
        }
    }*/
}
