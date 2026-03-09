package me.erykczy.colorfullighting.mixin.compat.sodium;

import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShaderChunkRenderer.class)
public interface ShaderChunkRendererAccessor {
    @Accessor(value = "activeProgram", remap = false)
    GlProgram<ChunkShaderInterface> getActiveProgram();
}
