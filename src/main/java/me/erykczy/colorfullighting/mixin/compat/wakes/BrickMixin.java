package me.erykczy.colorfullighting.mixin.compat.wakes;

import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.util.PackedLightData;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.leclowndu93150.wakes.simulation.Brick", remap = false)
public class BrickMixin {

    @Unique
    private int colorful_lighting$lastLightCoordinate;

    @Redirect(method = "populatePixels", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"), remap = true)
    private int redirectGetLightColor(BlockAndTintGetter level, BlockPos pos) {
        int light = LevelRenderer.getLightColor(level, pos);
        this.colorful_lighting$lastLightCoordinate = light;
        return light;
    }

    @Redirect(method = "populatePixels", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/NativeImage;getPixelRGBA(II)I"), remap = true)
    private int redirectGetPixelRGBA(NativeImage lightPixels, int x, int y) {
        if (ColoredLightEngine.getInstance().isEnabled()) {
            int light = this.colorful_lighting$lastLightCoordinate;
            
            // Check if the light value is actually a packed colored light value
            if ((light & 0xF0000000) != 0) {
                PackedLightData data = PackedLightData.unpackData(light);
                int r = data.red8;
                int g = data.green8;
                int b = data.blue8;
                
                // Get skylight color from the light map
                // We use block light 0 (x=0) and the skylight level from our data (y=skyLight4)
                int skyLightColor = lightPixels.getPixelRGBA(0, data.skyLight4);
                
                // Unpack skylight color (ABGR)
                int skyR = skyLightColor & 0xFF;
                int skyG = (skyLightColor >> 8) & 0xFF;
                int skyB = (skyLightColor >> 16) & 0xFF;
                
                // Add skylight to block light
                r = Math.min(255, r + skyR);
                g = Math.min(255, g + skyG);
                b = Math.min(255, b + skyB);

                int a = 255; // Full alpha for the pixel
                
                // NativeImage expects ABGR
                return (a << 24) | (b << 16) | (g << 8) | r;
            }
        }

        return lightPixels.getPixelRGBA(x, y);
    }
}
