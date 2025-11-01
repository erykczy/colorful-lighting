package me.erykczy.colorfullighting.mixin.compat.geckolib;

import com.mojang.blaze3d.vertex.PoseStack;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.TintingBufferSource;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@Mixin(value = GeoEntityRenderer.class, remap = false)
public abstract class GeoEntityRendererMixin<T extends Entity & GeoAnimatable> {

    @Unique
    private static float[] cl$mul(Entity e, float pt) {
        var eng = ColoredLightEngine.getInstance();
        if (eng == null || e == null) return null;
        Vec3 p = e.getLightProbePosition(pt);
        ColorRGB8 c = eng.sampleTrilinearLightColor(p);
        int r = c.red & 255, g = c.green & 255, b = c.blue & 255, m = Math.max(r, Math.max(g, b));
        if (m == 0) return null;
        float k = m / 255f;
        return new float[]{1f + k*((r/255f)-1f), 1f + k*((g/255f)-1f), 1f + k*((b/255f)-1f)};
    }

    @ModifyVariable(
        method = "render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("HEAD"),
        argsOnly = true,
        index = 5
    )
    private MultiBufferSource cl$wrapBuffers_render_v5(MultiBufferSource original, Entity e, float yaw, float pt, PoseStack pose) {
        float[] m = cl$mul(e, pt);
        return m != null ? new TintingBufferSource(original, m[0], m[1], m[2]) : original;
    }

    @ModifyVariable(
        method = "render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("HEAD"),
        argsOnly = true,
        index = 5
    )
    private MultiBufferSource cl$wrapBuffers_render_v4(MultiBufferSource original, Entity e, float yaw, float pt, PoseStack pose) {
        float[] m = cl$mul(e, pt);
        return m != null ? new TintingBufferSource(original, m[0], m[1], m[2]) : original;
    }

    @ModifyVariable(
        method = "actuallyRender(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/Entity;Lsoftware/bernie/geckolib/cache/object/BakedGeoModel;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIIFFFF)V",
        at = @At("HEAD"),
        argsOnly = true,
        index = 5
    )
    private MultiBufferSource cl$wrapBuffers_actually_v5(MultiBufferSource original, PoseStack pose, T anim, BakedGeoModel model, RenderType renderType) {
        float[] m = cl$mul(anim, 0f);
        return m != null ? new TintingBufferSource(original, m[0], m[1], m[2]) : original;
    }

    @ModifyVariable(
        method = "actuallyRender(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/Entity;Lsoftware/bernie/geckolib/cache/object/BakedGeoModel;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIIFFFF)V",
        at = @At("HEAD"),
        argsOnly = true,
        index = 5
    )
    private MultiBufferSource cl$wrapBuffers_actually_v4(MultiBufferSource original, PoseStack pose, T anim, BakedGeoModel model, RenderType renderType) {
        float[] m = cl$mul(anim, 0f);
        return m != null ? new TintingBufferSource(original, m[0], m[1], m[2]) : original;
    }
}
