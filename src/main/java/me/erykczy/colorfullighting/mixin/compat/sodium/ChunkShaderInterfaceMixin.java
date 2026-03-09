package me.erykczy.colorfullighting.mixin.compat.sodium;

import me.erykczy.colorfullighting.compat.sodium.ChunkShaderInterfaceExtension;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformFloat;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ShaderBindingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkShaderInterface.class)
public class ChunkShaderInterfaceMixin implements ChunkShaderInterfaceExtension {
    @Unique
    private GlUniformFloat uniformNightVibrancy;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(ShaderBindingContext context, ChunkShaderOptions options, CallbackInfo ci) {
        try {
            this.uniformNightVibrancy = context.bindUniform("u_NightVibrancy", GlUniformFloat::new);
        } catch (Exception ignored) {
            // Uniform might be missing if shaders are overridden (e.g. by Oculus/Iris)
        }
    }

    @Override
    public void setNightVibrancy(float vibrancy) {
        if (this.uniformNightVibrancy != null) {
            this.uniformNightVibrancy.setFloat(vibrancy);
        }
    }
}
