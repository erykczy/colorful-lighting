package me.erykczy.colorfullighting.mixin.compat.sodium;

import com.mojang.blaze3d.vertex.PoseStack;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.TintingBufferSource;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SignRenderer.class)
public abstract class SignRendererMixin {

    @Unique
    private static boolean cl$skip() {
        return ModList.get().isLoaded("embeddium");
    }

    @Unique
    private static float[] cl$mulFromPos(BlockPos pos) {
        var eng = ColoredLightEngine.getInstance();
        if (eng == null || !cl$skip() || pos == null) return null;

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
    private MultiBufferSource cl$wrapBuffersForBoard(
            MultiBufferSource original,
            SignBlockEntity be, float pt, PoseStack pose, MultiBufferSource _ignored, int packedLight, int overlay
    ) {
        float[] mul = (be != null) ? cl$mulFromPos(be.getBlockPos()) : null;
        return (mul != null) ? new TintingBufferSource(original, mul[0], mul[1], mul[2]) : original;
    }

    @ModifyVariable(
            method = "render(Lnet/minecraft/world/level/block/entity/SignBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 4
    )
    private MultiBufferSource cl$wrapBuffersForBoard(
            MultiBufferSource original,
            net.minecraft.world.level.block.entity.SignBlockEntity be, float pt, PoseStack pose
    ) {
        float[] mul = (be != null) ? cl$mulFromPos(be.getBlockPos()) : null;
        return (mul != null) ? new me.erykczy.colorfullighting.common.util.TintingBufferSource(original, mul[0], mul[1], mul[2]) : original;
    }
}
