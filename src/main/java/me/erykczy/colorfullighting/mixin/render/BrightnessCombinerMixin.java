package me.erykczy.colorfullighting.mixin.render;

import me.erykczy.colorfullighting.common.util.PackedLightData;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrightnessCombiner.class)
public class BrightnessCombinerMixin {
    private static int unpackBlock(int packed) { return (packed >> 4) & 0xF; }
    private static int unpackSky(int packed)   { return (packed >> 20) & 0xF; }
    private static int pack(int block, int sky){ return (block << 4) | (sky << 20); }

    private static int maxPacked(int a, int b) {
        return pack(Math.max(unpackBlock(a), unpackBlock(b)),
                Math.max(unpackSky(a),   unpackSky(b)));
    }

    private static int safeLight(BlockEntity be) {
        if (be == null) return 0;
        Level lvl = be.getLevel();
        if (lvl == null) return 0;
        return LevelRenderer.getLightColor(lvl, be.getBlockPos());
    }

    @Inject(
            method = "acceptDouble(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/level/block/entity/BlockEntity;)Lit/unimi/dsi/fastutil/ints/Int2IntFunction;",
            at = @At("HEAD"),
            cancellable = true
    )
    private <S extends BlockEntity> void cl$acceptDouble(S first, S second, CallbackInfoReturnable<Int2IntFunction> cir) {
        final int a = safeLight(first);
        final int b = safeLight(second);

        if (ModList.get().isLoaded("embeddium")) {
            cir.setReturnValue(base -> maxPacked(maxPacked(base, a), b));
            cir.cancel();
        } else {
            cir.setReturnValue(base -> PackedLightData
                    .max(PackedLightData.max(base, a), b));
            cir.cancel();
        }
    }

    @Inject(
            method = "acceptSingle(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lit/unimi/dsi/fastutil/ints/Int2IntFunction;",
            at = @At("HEAD"),
            cancellable = true
    )
    private <S extends BlockEntity> void cl$acceptSingle(S be, CallbackInfoReturnable<Int2IntFunction> cir) {
        final int beLight = safeLight(be);

        if (ModList.get().isLoaded("embeddium")) {
            cir.setReturnValue(base -> maxPacked(base, beLight));
            cir.cancel();
        } else {
            cir.setReturnValue(base -> PackedLightData.max(base, beLight));
            cir.cancel();
        }
    }
}


