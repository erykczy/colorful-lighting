package me.erykczy.colorfullighting.mixin.compat.sodium;

import com.mojang.blaze3d.vertex.PoseStack;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.TintingBufferSource;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderDispatcher.class)
public abstract class SodiumEntityColorCompatMixin {

    private static float[] cl$mul(Entity e, float pt) {
        var eng = ColoredLightEngine.getInstance();
        if (eng == null || e == null) return null;

        Vec3 p = e.getLightProbePosition(pt);
        ColorRGB8 c = eng.sampleTrilinearLightColor(p);
        int r = c.red & 255, g = c.green & 255, b = c.blue & 255;
        int m = Math.max(r, Math.max(g, b));
        if (m == 0) return null;

        float k = m / 255f;
        return new float[] {
                1f + k * ((r / 255f) - 1f),
                1f + k * ((g / 255f) - 1f),
                1f + k * ((b / 255f) - 1f)
        };
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private <E extends Entity> void cl$renderTinted(
            EntityRenderer<? super E> renderer,
            E entity, float yaw, float tickDelta,
            PoseStack pose, MultiBufferSource buffers, int packedLight
    ) {
        if (ModList.get().isLoaded("embeddium")) {
            if (entity instanceof FallingBlockEntity) {
                renderer.render(entity, yaw, tickDelta, pose, buffers, packedLight);
                return;
            }

            float[] m = cl$mul(entity, tickDelta);
            MultiBufferSource out = (m != null)
                    ? new TintingBufferSource(buffers, m[0], m[1], m[2])
                    : buffers;

            renderer.render(entity, yaw, tickDelta, pose, out, packedLight);
        } else {
            renderer.render(entity, yaw, tickDelta, pose, buffers, packedLight);
        }
    }
}
