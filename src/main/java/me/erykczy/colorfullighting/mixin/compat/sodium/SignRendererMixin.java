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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignRenderer.class)
public abstract class SignRendererMixin {

    @Unique private static final ThreadLocal<float[]> CL_MUL = new ThreadLocal<>();

    @Unique
    private static boolean cl$skip() {
        return ModList.get().isLoaded("embeddium");
    }

    @Unique
    private static float[] cl$mulFor(SignBlockEntity be) {
        var eng = ColoredLightEngine.getInstance();
        if (!cl$skip() || eng == null || be == null || be.getLevel() == null) return null;

        BlockPos pos = be.getBlockPos();
        ColorRGB8 c = eng.sampleTrilinearLightColor(Vec3.atCenterOf(pos));
        int r = c.red & 255, g = c.green & 255, b = c.blue & 255;
        int m = Math.max(r, Math.max(g, b));
        if (m == 0) return null;

        // multipliers around 1.0 (avoid blacking out dark colors)
        float k  = m / 255f;
        float mr = 1f + k * ((r / 255f) - 1f);
        float mg = 1f + k * ((g / 255f) - 1f);
        float mb = 1f + k * ((b / 255f) - 1f);
        return new float[]{mr, mg, mb};
    }

    // Compute the multiplier once per sign render
    @Inject(
            method = "render(Lnet/minecraft/world/level/block/entity/SignBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD")
    )
    private void cl$begin(SignBlockEntity be, float pt, PoseStack pose, MultiBufferSource buffers, int packedLight, int overlay, CallbackInfo ci) {
        CL_MUL.set(cl$mulFor(be));
    }

    // Clear at end
    @Inject(
            method = "render(Lnet/minecraft/world/level/block/entity/SignBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("RETURN")
    )
    private void cl$end(SignBlockEntity be, float pt, PoseStack pose, MultiBufferSource buffers, int packedLight, int overlay, CallbackInfo ci) {
        CL_MUL.remove();
    }

    // Wrap the MultiBufferSource param for the board/model draws
    @ModifyVariable(
            method = "render(Lnet/minecraft/world/level/block/entity/SignBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 4
    )
    private MultiBufferSource cl$wrapBuffersForBoard(MultiBufferSource original) {
        float[] mul = CL_MUL.get();
        return (mul != null) ? new TintingBufferSource(original, mul[0], mul[1], mul[2]) : original;
    }

    // Wrap the MultiBufferSource param for the text draws
    @ModifyVariable(
            method = "renderSignText(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/SignText;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIIZ)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 4
    )
    private MultiBufferSource cl$wrapBuffersForText(MultiBufferSource original) {
        float[] mul = CL_MUL.get();
        return (mul != null) ? new TintingBufferSource(original, mul[0], mul[1], mul[2]) : original;
    }
}
