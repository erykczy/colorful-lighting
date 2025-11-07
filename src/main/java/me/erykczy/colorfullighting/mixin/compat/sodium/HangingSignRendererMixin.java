package me.erykczy.colorfullighting.mixin.compat.sodium;

import com.mojang.blaze3d.vertex.PoseStack;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.TintingBufferSource;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(HangingSignRenderer.class)
public abstract class HangingSignRendererMixin {

    @Unique
    private static float[] cl$mulFromPos(BlockPos pos) {
        var eng = ColoredLightEngine.getInstance();
        if (eng == null || pos == null) return null;
        ColorRGB8 c = eng.sampleTrilinearLightColor(Vec3.atCenterOf(pos));
        int r = c.red & 255, g = c.green & 255, b = c.blue & 255;
        int m = Math.max(r, Math.max(g, b));
        if (m == 0) return null;
        float k  = m / 255f;
        float mr = 1f + k * ((r / 255f) - 1f);
        float mg = 1f + k * ((g / 255f) - 1f);
        float mb = 1f + k * ((b / 255f) - 1f);
        return new float[]{mr, mg, mb};
    }

    @ModifyVariable(
            method = "render(Lnet/minecraft/world/level/block/entity/SignBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 4
    )
    private MultiBufferSource cl$wrapBuffers_render_i3(MultiBufferSource original, SignBlockEntity be, float pt, PoseStack pose) {
        float[] m = (be != null) ? cl$mulFromPos(be.getBlockPos()) : null;
        return m != null ? new TintingBufferSource(original, m[0], m[1], m[2]) : original;
    }

    @ModifyVariable(
            method = "render(Lnet/minecraft/world/level/block/entity/SignBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 4
    )
    private MultiBufferSource cl$wrapBuffers_render_i4(MultiBufferSource original, SignBlockEntity be, float pt, PoseStack pose) {
        float[] m = (be != null) ? cl$mulFromPos(be.getBlockPos()) : null;
        return m != null ? new TintingBufferSource(original, m[0], m[1], m[2]) : original;
    }
}
