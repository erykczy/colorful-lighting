package me.erykczy.colorfullighting.mixin.compat.sodium;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class SodiumCompatBlockEntitiesMixin {

    @Unique private static final ThreadLocal<float[]> CL_prev = new ThreadLocal<>();

    @Unique
    private static Vec3 center(BlockPos p) {
        return new Vec3(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
    }

    @Unique
    private static float[] mulFrom(ColorRGB8 c) {
        int r8 = c.red & 0xFF, g8 = c.green & 0xFF, b8 = c.blue & 0xFF;
        int m = Math.max(r8, Math.max(g8, b8));
        if (m == 0) return null;
        float k  = m / 255f;
        float mr = 1f + k * ((r8 / 255f) - 1f);
        float mg = 1f + k * ((g8 / 255f) - 1f);
        float mb = 1f + k * ((b8 / 255f) - 1f);
        return new float[]{mr, mg, mb};
    }

    @Unique
    private static void begin(BlockEntity be) {
        var eng = ColoredLightEngine.getInstance();
        if (eng == null || be == null) return;

        float[] mul = null;

        if (be instanceof ChestBlockEntity cbe) {
            BlockPos pos = cbe.getBlockPos();
            BlockState s = cbe.getBlockState();
            if (s.getBlock() instanceof ChestBlock) {
                ChestType t = s.getValue(ChestBlock.TYPE);
                if (t == ChestType.SINGLE) {
                    mul = mulFrom(eng.sampleTrilinearLightColor(center(pos)));
                } else {
                    Direction d = ChestBlock.getConnectedDirection(s);
                    BlockPos pos2 = pos.relative(d);
                    ColorRGB8 c1 = eng.sampleTrilinearLightColor(center(pos));
                    ColorRGB8 c2 = eng.sampleTrilinearLightColor(center(pos2));
                    int r = ((c1.red & 0xFF) + (c2.red & 0xFF)) >> 1;
                    int g = ((c1.green & 0xFF) + (c2.green & 0xFF)) >> 1;
                    int b = ((c1.blue & 0xFF) + (c2.blue & 0xFF)) >> 1;
                    mul = mulFrom(ColorRGB8.fromRGB8(r, g, b));
                }
            }
        } else {
            mul = mulFrom(eng.sampleTrilinearLightColor(center(be.getBlockPos())));
        }

        if (mul == null) return;

        float[] prev = RenderSystem.getShaderColor().clone();
        CL_prev.set(prev);
        RenderSystem.setShaderColor(prev[0] * mul[0], prev[1] * mul[1], prev[2] * mul[2], prev[3]);
    }

    @Unique
    private static void end() {
        float[] prev = CL_prev.get();
        if (prev != null) {
            RenderSystem.setShaderColor(prev[0], prev[1], prev[2], prev[3]);
            CL_prev.remove();
        } else {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
            at = @At("HEAD")
    )
    private void cl$begin(BlockEntity be, float pt, PoseStack ps, MultiBufferSource bs, CallbackInfo ci) {
        begin(be);
    }

    @Inject(
            method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
            at = @At("RETURN")
    )
    private void cl$end(BlockEntity be, float pt, PoseStack ps, MultiBufferSource bs, CallbackInfo ci) {
        end();
    }
}
