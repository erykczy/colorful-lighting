package me.erykczy.colorfullighting.mixin.render;

import me.erykczy.colorfullighting.common.util.PackedLightData;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrightnessCombiner.class)
public class BrightnessCombinerMixin {
    @Inject(method = "acceptDouble(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/level/block/entity/BlockEntity;)Lit/unimi/dsi/fastutil/ints/Int2IntFunction;", at = @At("HEAD"), cancellable = true)
    private <S extends BlockEntity> void colorfullighting$acceptDouble(S first, S second, CallbackInfoReturnable<Int2IntFunction> cir) {
        cir.setReturnValue(value -> {
            int firstLight = LevelRenderer.getLightColor(first.getLevel(), first.getBlockPos());
            int secondLight = LevelRenderer.getLightColor(second.getLevel(), second.getBlockPos());
            return PackedLightData.max(firstLight, secondLight);
        });
    }
}
