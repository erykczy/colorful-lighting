package com.example.examplemod.mixin.render;

import com.example.examplemod.util.PackedLightData;
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
    private <S extends BlockEntity> void coloredLights$acceptDouble(S first, S second, CallbackInfoReturnable<Int2IntFunction> cir) {
        cir.setReturnValue(value -> {
            int firstLight = LevelRenderer.getLightColor(first.getLevel(), first.getBlockPos());
            int secondLight = LevelRenderer.getLightColor(second.getLevel(), second.getBlockPos());
            var firstData = PackedLightData.unpackData(firstLight);
            var secondData = PackedLightData.unpackData(secondLight);
            //int blockLight = Math.max(firstData.blockLight, secondData.blockLight);
            int skyLight = Math.max(firstData.skyLight4, secondData.skyLight4);
            int red4 = Math.max(firstData.red8, secondData.red8);
            int green4 = Math.max(firstData.green8, secondData.green8);
            int blue4 = Math.max(firstData.blue8, secondData.blue8);

            return PackedLightData.packData(skyLight, red4, green4, blue4);
        });
    }
}
