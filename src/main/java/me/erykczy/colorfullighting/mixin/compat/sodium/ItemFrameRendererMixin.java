package me.erykczy.colorfullighting.mixin.compat.sodium;

import com.mojang.blaze3d.vertex.PoseStack;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.TintingBufferSource;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemFrameRenderer.class)
public abstract class ItemFrameRendererMixin {

    @Unique
    private static float[] cl$mulFor(ItemFrame e) {
        var eng = ColoredLightEngine.getInstance();
        if (eng == null || e == null) return null;
        AABB bb = e.getBoundingBox();
        Vec3 p = bb.getCenter();
        ColorRGB8 c = eng.sampleTrilinearLightColor(p);
        int r = c.red & 255, g = c.green & 255, b = c.blue & 255;
        int m = Math.max(r, Math.max(g, b));
        if (m == 0) return null;
        float k = m / 255f;
        return new float[]{
            1f + k * ((r / 255f) - 1f),
            1f + k * ((g / 255f) - 1f),
            1f + k * ((b / 255f) - 1f)
        };
    }

    @ModifyVariable(
        method = "render(Lnet/minecraft/world/entity/decoration/ItemFrame;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("HEAD"),
        argsOnly = true,
        index = 5
    )
    private MultiBufferSource cl$wrapItemFrameBuffers_v5(
        MultiBufferSource original,
        ItemFrame e, float yaw, float pt, PoseStack pose
    ) {
        float[] mul = cl$mulFor(e);
        return (mul != null) ? new TintingBufferSource(original, mul[0], mul[1], mul[2]) : original;
    }

    @ModifyVariable(
        method = "render(Lnet/minecraft/world/entity/decoration/ItemFrame;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("HEAD"),
        argsOnly = true,
        index = 5
    )
    private MultiBufferSource cl$wrapItemFrameBuffers_v4(
        MultiBufferSource original,
        ItemFrame e, float yaw, float pt, PoseStack pose
    ) {
        float[] mul = cl$mulFor(e);
        return (mul != null) ? new TintingBufferSource(original, mul[0], mul[1], mul[2]) : original;
    }
}
