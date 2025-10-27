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
    private void colorfullighting$ensureWhiteAndTint(BlockRenderContext ctx, ColorProvider<BlockState> colorProvider, BakedQuadView quad, CallbackInfoReturnable<int[]> cir) {
        int[] colors = cir.getReturnValue();
        if (colors == null) {
            colors = new int[] { 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF };
        } else {
            for (int i = 0; i < colors.length; i++) {
                if ((colors[i] & 0x00FFFFFF) == 0) colors[i] = (colors[i] & 0xFF000000) | 0x00FFFFFF;
            }
        }

        var eng = ColoredLightEngine.getInstance();
        if (eng != null && ctx != null && quad != null) {
            for (int i = 0; i < 4; i++) {
                double wx = ctx.pos().getX() + quad.getX(i);
                double wy = ctx.pos().getY() + quad.getY(i);
                double wz = ctx.pos().getZ() + quad.getZ(i);
                ColorRGB8 c = eng.sampleTrilinearLightColor(new Vec3(wx, wy, wz));

                int rc = c.red & 0xFF, gc = c.green & 0xFF, bc = c.blue & 0xFF;
                int maxc = Math.max(rc, Math.max(gc, bc));
                if (maxc == 0) continue;

                float k  = maxc / 255.0f;
                float mr = 1.0f + k * ((rc / 255.0f) - 1.0f);
                float mg = 1.0f + k * ((gc / 255.0f) - 1.0f);
                float mb = 1.0f + k * ((bc / 255.0f) - 1.0f);

                int abgr = colors[i];
                int a = (abgr >>> 24) & 0xFF;
                int b = (abgr >>> 16) & 0xFF;
                int g = (abgr >>> 8)  & 0xFF;
                int r = (abgr)        & 0xFF;

                int nr = colorfullighting$clamp255(Math.round(r * mr));
                int ng = colorfullighting$clamp255(Math.round(g * mg));
                int nb = colorfullighting$clamp255(Math.round(b * mb));
                colors[i] = (a << 24) | (nb << 16) | (ng << 8) | nr; // ABGR
            }
        }
        cir.setReturnValue(colors);
    }

    @Unique
    private static int colorfullighting$clamp255(int v) { return v < 0 ? 0 : Math.min(v, 255); }
}
