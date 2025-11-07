package me.erykczy.colorfullighting.mixin.compat.sodium;

import com.mojang.blaze3d.vertex.PoseStack;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.TintingBufferSource;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SkullBlockRenderer.class)
public abstract class SkullRendererMixin {

    @Unique
    private static float[] cl$mulFromPos(BlockPos pos) {
        var eng = ColoredLightEngine.getInstance();
        if (eng == null || pos == null) return null;
        ColorRGB8 c = eng.sampleTrilinearLightColor(Vec3.atCenterOf(pos));
        int r = c.red & 255, g = c.green & 255, b = c.blue & 255;
        int m = Math.max(r, Math.max(g, b));
        if (m == 0) return null;
        float k = m / 255f;
        float mr = 1f + k * ((r / 255f) - 1f);
        float mg = 1f + k * ((g / 255f) - 1f);
        float mb = 1f + k * ((b / 255f) - 1f);
        return new float[]{mr, mg, mb};
    }

    @ModifyVariable(
        method = "render(Lnet/minecraft/world/level/block/entity/SkullBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
        at = @At("HEAD"),
        argsOnly = true,
        index = 4
    )
    private MultiBufferSource cl$wrapBuffers_v4(
        MultiBufferSource original,
        SkullBlockEntity be, float pt, PoseStack pose
    ) {
        float[] mul = (be != null) ? cl$mulFromPos(be.getBlockPos()) : null;
        return (mul != null) ? new TintingBufferSource(original, mul[0], mul[1], mul[2]) : original;
    }

    @ModifyVariable(
        method = "render(Lnet/minecraft/world/level/block/entity/SkullBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
        at = @At("HEAD"),
        argsOnly = true,
        index = 4
    )
    private MultiBufferSource cl$wrapBuffers_v3(
        MultiBufferSource original,
        SkullBlockEntity be, float pt, PoseStack pose
    ) {
        float[] mul = (be != null) ? cl$mulFromPos(be.getBlockPos()) : null;
        return (mul != null) ? new TintingBufferSource(original, mul[0], mul[1], mul[2]) : original;
    }
}
