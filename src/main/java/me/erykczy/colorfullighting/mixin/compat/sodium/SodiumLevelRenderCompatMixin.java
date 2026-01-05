package me.erykczy.colorfullighting.mixin.compat.sodium;

import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.util.ColorRGB8;

import me.erykczy.colorfullighting.common.util.MathExt;
import me.jellysquid.mods.sodium.client.model.color.ColorProvider;
import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockRenderer.class, remap = false)
public abstract class SodiumLevelRenderCompatMixin {

    @Unique
    private static final float[] COLOR_INTENSITY_LUT = new float[256];
    @Unique
    private static final float[] COLOR_POW_LUT = new float[256];

    static {
        for (int i = 0; i < 256; i++) {
            COLOR_INTENSITY_LUT[i] = (float) Math.pow(i / 255.0f, 1.25f);
            COLOR_POW_LUT[i] = (float) Math.pow(i / 255.0f, 0.75f);
        }
    }

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
            float ox = ctx.pos().getX();
            float oy = ctx.pos().getY();
            float oz = ctx.pos().getZ();

            var face = quad.getLightFace();
            double dx = face.getStepX() * 0.45;
            double dy = face.getStepY() * 0.45;
            double dz = face.getStepZ() * 0.45;

            for (int i = 0; i < 4; i++) {
                double vqx = quad.getX(i);
                double vqy = quad.getY(i);
                double vqz = quad.getZ(i);

                // Pull sampling point slightly away from the quad edges towards the center of the block face
                // to avoid "bleeding" from dark adjacent blocks due to trilinear interpolation.
                double bx = (0.5 - vqx) * 0.15;
                double by = (0.5 - vqy) * 0.15;
                double bz = (0.5 - vqz) * 0.15;

                double wx = ox + vqx + dx + (face.getStepX() == 0 ? bx : 0);
                double wy = oy + vqy + dy + (face.getStepY() == 0 ? by : 0);
                double wz = oz + vqz + dz + (face.getStepZ() == 0 ? bz : 0);

                ColorRGB8 c = eng.sampleTrilinearLightColor(wx, wy, wz);

                int rc = c.red   & 0xFF;
                int gc = c.green & 0xFF;
                int bc = c.blue  & 0xFF;

                int maxc = rc > gc ? (rc > bc ? rc : bc) : (gc > bc ? gc : bc);
                if (maxc == 0) {
                    continue;
                }

                int abgr = colors[i];
                int a = (abgr >>> 24) & 0xFF;
                int b = (abgr >>> 16) & 0xFF;
                int g = (abgr >>> 8)  & 0xFF;
                int r = (abgr)        & 0xFF;

                float intensity = COLOR_INTENSITY_LUT[maxc];
                float rN = COLOR_POW_LUT[rc * 255 / maxc];
                float gN = COLOR_POW_LUT[gc * 255 / maxc];
                float bN = COLOR_POW_LUT[bc * 255 / maxc];
                
                if (Minecraft.getInstance().level != null) {
                    intensity *= a * (0.7f / 255f);
                }

                float mr = 1.0f + intensity * (rN - 1.0f);
                float mg = 1.0f + intensity * (gN - 1.0f);
                float mb = 1.0f + intensity * (bN - 1.0f);

                int nr = colorfullighting$clamp255((int)(r * mr + 0.5f));
                int ng = colorfullighting$clamp255((int)(g * mg + 0.5f));
                int nb = colorfullighting$clamp255((int)(b * mb + 0.5f));

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
