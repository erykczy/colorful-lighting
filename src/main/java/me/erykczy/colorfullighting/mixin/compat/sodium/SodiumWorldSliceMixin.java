package me.erykczy.colorfullighting.mixin.compat.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "org.embeddedt.embeddium.impl.world.WorldSlice", remap = false)
public class SodiumWorldSliceMixin {
    // We are now using SodiumLightDataAccessMixin and SodiumFlatLightPipelineMixin.
    // This mixin might not be needed if we don't touch WorldSlice directly.
}
