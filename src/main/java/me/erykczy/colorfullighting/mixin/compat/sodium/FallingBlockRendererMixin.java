package me.erykczy.colorfullighting.mixin.compat.sodium;

import com.mojang.blaze3d.vertex.PoseStack;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.TintingBufferSource;
import me.erykczy.colorfullighting.common.util.MathExt;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlockRenderer.class)
public abstract class FallingBlockRendererMixin {

    @Unique private static final ThreadLocal<float[]> CL_mul = new ThreadLocal<>();

    @Unique
    private static boolean cl$skip() {
        return ModList.get().isLoaded("embeddium");
    }

    @Inject(
        method = "render(Lnet/minecraft/world/entity/item/FallingBlockEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("HEAD")
    )
    private void cl$begin(FallingBlockEntity e, float yaw, float pt,
                          PoseStack pose, MultiBufferSource buffers, int packedLight,
                          CallbackInfo ci) {
        if (!cl$skip() || e == null) {
            CL_mul.remove();
            return;
        }
        var eng = ColoredLightEngine.getInstance();
        if (eng == null) { CL_mul.remove(); return; }

        ColorRGB8 c = eng.sampleTrilinearLightColor(e.getX(), e.getY() + 0.5, e.getZ());
        int r = c.red & 255, g = c.green & 255, b = c.blue & 255;
        int m = r > g ? (r > b ? r : b) : (g > b ? g : b);
        if (m == 0) { CL_mul.remove(); return; }

        float k = m * (1.0f / 255.0f);

        // Adjust k based on time of day
        k *= MathExt.getTimeOfDayFalloff(e.level().getDayTime()) * 255.0f;

        float mr = 1.0f + k * ((r * (1.0f / 255.0f)) - 1.0f);
        float mg = 1.0f + k * ((g * (1.0f / 255.0f)) - 1.0f);
        float mb = 1.0f + k * ((b * (1.0f / 255.0f)) - 1.0f);
        CL_mul.set(new float[]{mr, mg, mb});
    }

    @Redirect(
        method = "render(Lnet/minecraft/world/entity/item/FallingBlockEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
        )
    )
    private com.mojang.blaze3d.vertex.VertexConsumer cl$tintedBuffer(
        MultiBufferSource buffers, net.minecraft.client.renderer.RenderType rt) {

        float[] mul = CL_mul.get();
        if (mul != null) {
            buffers = new TintingBufferSource(buffers, mul[0], mul[1], mul[2]);
        }
        return buffers.getBuffer(rt);
    }

    @Inject(
        method = "render(Lnet/minecraft/world/entity/item/FallingBlockEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("RETURN")
    )
    private void cl$end(FallingBlockEntity e, float yaw, float pt,
                        PoseStack pose, MultiBufferSource buffers, int packedLight,
                        CallbackInfo ci) {
        CL_mul.remove();
    }
}
