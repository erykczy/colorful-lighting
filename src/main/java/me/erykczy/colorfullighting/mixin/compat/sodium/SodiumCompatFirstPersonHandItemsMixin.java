package me.erykczy.colorfullighting.mixin.compat.sodium;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.MathExt;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemInHandRenderer.class, priority = 1100)
public abstract class SodiumCompatFirstPersonHandItemsMixin {

    @Unique
    private static final ThreadLocal<float[]> colorfullighting$prevShaderColor = new ThreadLocal<>();

    @Unique
    private static void colorfullighting$begin(AbstractClientPlayer player, float partialTick, int packedLight) {
        var eng = ColoredLightEngine.getInstance();
        if (eng == null || player == null) return;
        Vec3 eye = player.getEyePosition(partialTick);
        ColorRGB8 c = eng.sampleTrilinearLightColor(eye.x, eye.y, eye.z);
        int rc = c.red & 0xFF, gc = c.green & 0xFF, bc = c.blue & 0xFF;
        int maxc = rc > gc ? (rc > bc ? rc : bc) : (gc > bc ? gc : bc);
        if (maxc == 0) return;
        float k = maxc * (1.0f / 255.0f);

        // Adjust k based on time of day
        if (eng.clientAccessor != null && eng.clientAccessor.getLevel() != null) {
            k *= 0.7f;
        }

        float mr = 1.0f + k * ((rc * (1.0f / 255.0f)) - 1.0f);
        float mg = 1.0f + k * ((gc * (1.0f / 255.0f)) - 1.0f);
        float mb = 1.0f + k * ((bc * (1.0f / 255.0f)) - 1.0f);
        float[] prev = RenderSystem.getShaderColor().clone();
        colorfullighting$prevShaderColor.set(prev);
        RenderSystem.setShaderColor(prev[0] * mr, prev[1] * mg, prev[2] * mb, prev[3]);
    }

    @Unique
    private static void colorfullighting$end() {
        float[] prev = colorfullighting$prevShaderColor.get();
        if (prev != null) {
            RenderSystem.setShaderColor(prev[0], prev[1], prev[2], prev[3]);
            colorfullighting$prevShaderColor.remove();
        } else {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }

    @Inject(
            method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V",
            at = @At("HEAD")
    )
    private void colorfullighting$beginHandsWithItems(
            float partialTick, PoseStack poseStack, MultiBufferSource.BufferSource buffers, LocalPlayer player, int packedLight,
            CallbackInfo ci
    ) {
        colorfullighting$begin(player, partialTick, packedLight);
    }

    @Inject(
            method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V",
            at = @At("RETURN")
    )
    private void colorfullighting$endHandsWithItems(
            float partialTick, PoseStack poseStack, MultiBufferSource.BufferSource buffers, LocalPlayer player, int packedLight,
            CallbackInfo ci
    ) {
        colorfullighting$end();
    }

    @Inject(
            method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD")
    )
    private void colorfullighting$beginArmWithItem(
            AbstractClientPlayer player, float partialTick, float pitch,
            InteractionHand hand, float swingProgress, ItemStack stack, float equipProgress,
            PoseStack poseStack, MultiBufferSource buffers, int packedLight,
            CallbackInfo ci
    ) {
        colorfullighting$begin(player, partialTick, packedLight);
    }

    @Inject(
            method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN")
    )
    private void colorfullighting$endArmWithItem(
            AbstractClientPlayer player, float partialTick, float pitch,
            InteractionHand hand, float swingProgress, ItemStack stack, float equipProgress,
            PoseStack poseStack, MultiBufferSource buffers, int packedLight,
            CallbackInfo ci
    ) {
        colorfullighting$end();
    }

    @Inject(
            method = "renderPlayerArm(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IFFLnet/minecraft/world/entity/HumanoidArm;)V",
            at = @At("HEAD")
    )
    private void colorfullighting$beginPlayerArm(
            PoseStack poseStack, MultiBufferSource buffers, int packedLight,
            float partialTick, float swing, HumanoidArm arm, CallbackInfo ci
    ) {
        colorfullighting$begin(null, partialTick, packedLight);
    }

    @Inject(
            method = "renderPlayerArm(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IFFLnet/minecraft/world/entity/HumanoidArm;)V",
            at = @At("RETURN")
    )
    private void colorfullighting$endPlayerArm(
            PoseStack poseStack, MultiBufferSource buffers, int packedLight,
            float partialTick, float swing, HumanoidArm arm, CallbackInfo ci
    ) {
        colorfullighting$end();
    }
}
