package me.erykczy.colorfullighting.mixin.compat.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "org.embeddedt.embeddium.impl.render.chunk.compile.pipeline.FluidRenderer", remap = false)
public class SodiumFluidRendererMixin {

}
