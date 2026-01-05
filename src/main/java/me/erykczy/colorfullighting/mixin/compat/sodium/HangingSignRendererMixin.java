package me.erykczy.colorfullighting.mixin.compat.sodium;

import com.mojang.blaze3d.vertex.PoseStack;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.TintingBufferSource;
import me.erykczy.colorfullighting.common.util.MathExt;
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
    private static float[] cl$mulFromPos(BlockPos pos, int packedLight) {
        var eng = ColoredLightEngine.getInstance();
        if (eng == null || pos == null) return null;
        ColorRGB8 c = eng.sampleTrilinearLightColor(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        int r = c.red & 255, g = c.green & 255, b = c.blue & 255;
        int m = r > g ? (r > b ? r : b) : (g > b ? g : b);
        if (m == 0) return null;
        float k = m * (1.0f / 255.0f);

        // Adjust k based on time of day
        if (eng.clientAccessor != null && eng.clientAccessor.getLevel() != null) {
            k *= MathExt.getTimeOfDayFalloff(eng.clientAccessor.getLevel().getDayTime()) * 255.0f;
        }

        float mr = 1.0f + k * ((r * (1.0f / 255.0f)) - 1.0f);
        float mg = 1.0f + k * ((g * (1.0f / 255.0f)) - 1.0f);
        float mb = 1.0f + k * ((b * (1.0f / 255.0f)) - 1.0f);
        return new float[]{mr, mg, mb};
    }

    @ModifyVariable(
            method = "render(Lnet/minecraft/world/level/block/entity/SignBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 4
    )
    private MultiBufferSource cl$wrapBuffers_render_i3(MultiBufferSource original, SignBlockEntity be, float pt, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        float[] m = (be != null) ? cl$mulFromPos(be.getBlockPos(), packedLight) : null;
        return m != null ? new TintingBufferSource(original, m[0], m[1], m[2]) : original;
    }

    @ModifyVariable(
            method = "render(Lnet/minecraft/world/level/block/entity/SignBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 4
    )
    private MultiBufferSource cl$wrapBuffers_render_i4(MultiBufferSource original, SignBlockEntity be, float pt, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        float[] m = (be != null) ? cl$mulFromPos(be.getBlockPos(), packedLight) : null;
        return m != null ? new TintingBufferSource(original, m[0], m[1], m[2]) : original;
    }
}
