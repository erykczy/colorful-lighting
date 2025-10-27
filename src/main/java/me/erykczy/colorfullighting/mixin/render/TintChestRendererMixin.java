package me.erykczy.colorfullighting.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.TintingVertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestRenderer.class)
public abstract class TintChestRendererMixin {

    @Unique private static final ThreadLocal<float[]> CL_MUL = new ThreadLocal<>();

    @Unique
    private static Vec3 c(BlockPos p){ return new Vec3(p.getX()+0.5, p.getY()+0.5, p.getZ()+0.5); }

    @Unique
    private static float[] m(ColorRGB8 col){
        int r=col.red&0xFF, g=col.green&0xFF, b=col.blue&0xFF;
        int mx=Math.max(r, Math.max(g,b));
        if(mx==0) return null;
        float k=mx/255f;
        return new float[]{
                1f + k*((r/255f)-1f),
                1f + k*((g/255f)-1f),
                1f + k*((b/255f)-1f)
        };
    }

    @Inject(
            method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD")
    )
    private void cl$begin(BlockEntity be, float pt, PoseStack ps, MultiBufferSource bs, int light, int overlay, CallbackInfo ci){
        if(ModList.get().isLoaded("embeddium")) {
            var eng = ColoredLightEngine.getInstance();
            if (eng == null || be == null) {
                CL_MUL.set(null);
                return;
            }
            BlockState s = be.getBlockState();
            if (!(s.getBlock() instanceof ChestBlock)) {
                CL_MUL.set(null);
                return;
            }

            ChestType t = s.getValue(ChestBlock.TYPE);
            float[] mul;
            if (t == ChestType.SINGLE) {
                mul = m(eng.sampleTrilinearLightColor(c(be.getBlockPos())));
            } else {
                Direction d = ChestBlock.getConnectedDirection(s);
                BlockPos p1 = be.getBlockPos();
                BlockPos p2 = p1.relative(d);
                ColorRGB8 c1 = eng.sampleTrilinearLightColor(c(p1));
                ColorRGB8 c2 = eng.sampleTrilinearLightColor(c(p2));
                int rr = ((c1.red & 0xFF) + (c2.red & 0xFF)) >> 1;
                int gg = ((c1.green & 0xFF) + (c2.green & 0xFF)) >> 1;
                int bb = ((c1.blue & 0xFF) + (c2.blue & 0xFF)) >> 1;
                mul = m(ColorRGB8.fromRGB8(rr, gg, bb));
            }
            CL_MUL.set(mul);
        }
    }

    @ModifyVariable(
            method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "STORE"),
            ordinal = 0
    )
    private VertexConsumer cl$wrapVertexConsumer(VertexConsumer vc, BlockEntity be, float pt, PoseStack ps, MultiBufferSource bs, int light, int overlay){
        if(ModList.get().isLoaded("embeddium")) {
            float[] m = CL_MUL.get();
            if (m == null) return vc;
            return new TintingVertexConsumer(vc, m[0], m[1], m[2]);
        }
        return vc;
    }

    @Inject(
            method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("RETURN")
    )
    private void cl$end(BlockEntity be, float pt, PoseStack ps, MultiBufferSource bs, int light, int overlay, CallbackInfo ci){
        if(ModList.get().isLoaded("embeddium")) {
            CL_MUL.remove();
        }
    }
}
