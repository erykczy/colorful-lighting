package com.example.examplemod.mixin;

import com.example.examplemod.common.ColoredLightEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockLightEngine.class)
public abstract class BlockLightEngineMixin {
    @Inject(method = "checkNode", at = @At("TAIL"))
    private void coloredLights$checkNode(long packedPos, CallbackInfo ci) {
        if(!Minecraft.getInstance().isSameThread()) return; // only client side
        BlockLightEngine lightEngine = (BlockLightEngine)(Object)this;
        SectionPos sectionPos = SectionPos.of(BlockPos.of(packedPos));
        for(int x = -1; x <= 1; ++x) {
            for(int z = -1; z <= 1; ++z) {
                if(!ColoredLightEngine.getInstance().storage.containsSection(sectionPos.offset(x, 0, z).asLong())) return;
            }
        }
        ColoredLightEngine.getInstance().onBlockLightPropertiesChanged(BlockPos.of(packedPos));
    }
}
