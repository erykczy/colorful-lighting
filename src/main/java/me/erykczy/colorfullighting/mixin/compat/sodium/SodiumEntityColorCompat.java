package me.erykczy.colorfullighting.mixin.compat.sodium;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class SodiumEntityColorCompat {

    @Unique
    private static final ThreadLocal<float[]> colorfullighting$prevShaderColor = new ThreadLocal<>();

    @Inject(
        method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("HEAD")
    )
    private <T extends Entity> void colorfullighting$beginTint(
            T entity, double x, double y, double z,
            float yaw, float tickDelta,
            PoseStack poseStack, MultiBufferSource buffers, int packedLight,
            CallbackInfo ci
    ) {
        var eng = ColoredLightEngine.getInstance();
        if (eng == null || entity == null) return;

        Vec3 p = entity.position();
        ColorRGB8 c = eng.sampleTrilinearLightColor(new Vec3(p.x, p.y + (entity.getBbHeight() * 0.5), p.z));

        int rc = c.red & 0xFF, gc = c.green & 0xFF, bc = c.blue & 0xFF;
        int maxc = Math.max(rc, Math.max(gc, bc));
        if (maxc == 0) return;

        float k  = maxc / 255.0f;
        float mr = 1.0f + k * ((rc / 255.0f) - 1.0f);
        float mg = 1.0f + k * ((gc / 255.0f) - 1.0f);
        float mb = 1.0f + k * ((bc / 255.0f) - 1.0f);

        float[] prev = RenderSystem.getShaderColor().clone();
        colorfullighting$prevShaderColor.set(prev);
        RenderSystem.setShaderColor(prev[0] * mr, prev[1] * mg, prev[2] * mb, prev[3]);
    }

    @Inject(
        method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("RETURN")
    )
    private <T extends Entity> void colorfullighting$endTint(
            T entity, double x, double y, double z,
            float yaw, float tickDelta,
            PoseStack poseStack, MultiBufferSource buffers, int packedLight,
            CallbackInfo ci
    ) {
        float[] prev = colorfullighting$prevShaderColor.get();
        if (prev != null) {
            RenderSystem.setShaderColor(prev[0], prev[1], prev[2], prev[3]);
            colorfullighting$prevShaderColor.remove();
        } else {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }

    @Unique
    @SuppressWarnings("unused")
    private static int colorfullighting$clamp255(int v) { return v < 0 ? 0 : Math.min(v, 255); }
}
