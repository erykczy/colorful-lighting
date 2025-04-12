package com.example.examplemod.mixin.render;

import com.example.examplemod.util.BufferUtils;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.Cube.class)
public class ModelPartCubeMixin {
    @Inject(method = "compile", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(FFFIFFIIFFF)V", shift = At.Shift.AFTER))
    private void coloredLights$compile(PoseStack.Pose pose, VertexConsumer buffer, int packedLight, int packedOverlay, int color, CallbackInfo ci, @Local(ordinal = 2) Vector3f vector3f2) {
        BufferUtils.DEBUG_DO_TEST(pose, buffer, packedLight, packedOverlay, color, vector3f2);
    }
}
