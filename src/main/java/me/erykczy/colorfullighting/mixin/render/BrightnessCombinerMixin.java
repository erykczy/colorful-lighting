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

    @Inject(method = "acceptDouble(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/level/block/entity/BlockEntity;)Lit/unimi/dsi/fastutil/ints/Int2IntFunction;", at = @At("HEAD"), cancellable = true)
    private <S extends BlockEntity> void colorfullighting$acceptDouble(S first, S second, CallbackInfoReturnable<Int2IntFunction> cir) {
        if(ModList.get().isLoaded("embeddium")) {
            if (first == null || second == null) return;
            Level lf = first.getLevel();
            Level ls = second.getLevel();
            if (lf == null || ls == null) return;

            final int a = LevelRenderer.getLightColor(lf, first.getBlockPos());
            final int b = LevelRenderer.getLightColor(ls, second.getBlockPos());
            final int both = maxPacked(a, b);

            cir.setReturnValue(base -> maxPacked(base, both));
        } else {
            cir.setReturnValue(value -> {
                int firstLight = LevelRenderer.getLightColor(first.getLevel(), first.getBlockPos());
                int secondLight = LevelRenderer.getLightColor(second.getLevel(), second.getBlockPos());
                return PackedLightData.max(firstLight, secondLight);
            });
        }
    }

    @Inject(
            method = "acceptSingle(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lit/unimi/dsi/fastutil/ints/Int2IntFunction;",
            at = @At("HEAD"),
            cancellable = true
    )
    private <S extends BlockEntity> void cl$acceptSingle(S be, CallbackInfoReturnable<Int2IntFunction> cir) {
        if (ModList.get().isLoaded("embeddium")) {
            if (be == null) return;
            Level level = be.getLevel();
            if (level == null) return;

            final int beLight = LevelRenderer.getLightColor(level, be.getBlockPos());
            cir.setReturnValue(base -> maxPacked(base, beLight));
        }
    }
}

