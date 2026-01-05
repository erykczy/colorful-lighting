package me.erykczy.colorfullighting.mixin.compat.sodium;

import com.mojang.blaze3d.vertex.PoseStack;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.TintingBufferSource;
import me.erykczy.colorfullighting.common.util.MathExt;
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

            var eng = ColoredLightEngine.getInstance();
            if (eng == null) {
                renderer.render(entity, yaw, tickDelta, pose, buffers, packedLight);
                return;
            }

            Vec3 p = entity.getLightProbePosition(tickDelta);
            ColorRGB8 c = eng.sampleTrilinearLightColor(p.x, p.y, p.z);
            int r = c.red & 255, g = c.green & 255, b = c.blue & 255;
            int max = r > g ? (r > b ? r : b) : (g > b ? g : b);

            if (max > 0) {
                float k = max * (1.0f / 255.0f);

                // Adjust k based on time of day
                k *= 0.7f;
                float mr = 1.0f + k * ((r * (1.0f / 255.0f)) - 1.0f);
                float mg = 1.0f + k * ((g * (1.0f / 255.0f)) - 1.0f);
                float mb = 1.0f + k * ((b * (1.0f / 255.0f)) - 1.0f);

                MultiBufferSource out = new TintingBufferSource(buffers, mr, mg, mb);
                renderer.render(entity, yaw, tickDelta, pose, out, packedLight);
            } else {
                renderer.render(entity, yaw, tickDelta, pose, buffers, packedLight);
            }
        } else {
            renderer.render(entity, yaw, tickDelta, pose, buffers, packedLight);
        }
    }
}
