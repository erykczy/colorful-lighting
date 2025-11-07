package me.erykczy.colorfullighting.mixin.compat.sodium;

import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.util.ColorRGB8;

import me.jellysquid.mods.sodium.client.model.color.ColorProvider;
import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockRenderer.class, remap = false)
public abstract class SodiumLevelRenderCompatMixin {

    @Inject(
            method = "getVertexColors",
            at = @At("RETURN"),
            cancellable = true,
            require = 0
    )
    private void colorfullighting$ensureWhiteAndTint(
            BlockRenderContext ctx,
            ColorProvider<BlockState> colorProvider,
            BakedQuadView quad,
            CallbackInfoReturnable<int[]> cir
    ) {
        int[] colors = cir.getReturnValue();

        if (colors == null) {
            colors = new int[] { 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF };
        } else {
            for (int i = 0; i < colors.length; i++) {
                if ((colors[i] & 0x00FF_FFFF) == 0) {
                    colors[i] = (colors[i] & 0xFF00_0000) | 0x00FF_FFFF;
                }
            }
        }

        var eng = ColoredLightEngine.getInstance();

        if (eng != null && ctx != null && quad != null) {
            for (int i = 0; i < 4; i++) {
                double wx = ctx.pos().getX() + quad.getX(i);
                double wy = ctx.pos().getY() + quad.getY(i);
                double wz = ctx.pos().getZ() + quad.getZ(i);

                ColorRGB8 c = eng.sampleTrilinearLightColor(new Vec3(wx, wy, wz));

                int rc = c.red   & 0xFF;
                int gc = c.green & 0xFF;
                int bc = c.blue  & 0xFF;

                int maxc = Math.max(rc, Math.max(gc, bc));
                if (maxc == 0) {
                    continue;
                }

                int abgr = colors[i];
                int a = (abgr >>> 24) & 0xFF;
                int b = (abgr >>> 16) & 0xFF;
                int g = (abgr >>> 8)  & 0xFF;
                int r = (abgr)        & 0xFF;

                float intensity = (float) Math.pow(maxc / 255.0f, 1.25f);
                float rN = rc / (float) maxc;
                float gN = gc / (float) maxc;
                float bN = bc / (float) maxc;

                rN = (float) Math.pow(rN, 0.75f);
                gN = (float) Math.pow(gN, 0.75f);
                bN = (float) Math.pow(bN, 0.75f);

                float falloff = a / 255.0f;
                falloff *= falloff; // smoother taper
                intensity *= falloff;

                float mr = 1.0f + intensity * (rN - 1.0f);
                float mg = 1.0f + intensity * (gN - 1.0f);
                float mb = 1.0f + intensity * (bN - 1.0f);

                int nr = colorfullighting$clamp255(Math.round(r * mr));
                int ng = colorfullighting$clamp255(Math.round(g * mg));
                int nb = colorfullighting$clamp255(Math.round(b * mb));

                colors[i] = (a << 24) | (nb << 16) | (ng << 8) | nr;
            }
        }

        cir.setReturnValue(colors);
    }

    @Unique
    private static int colorfullighting$clamp255(int v) {
        return v < 0 ? 0 : Math.min(v, 255);
    }
}
