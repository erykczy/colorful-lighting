package com.example.examplemod.mixin.render;

import com.example.examplemod.ColoredLightManager;
import com.example.examplemod.util.BufferUtils;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {
    @Unique
    private BlockPos coloredLights$blockPos;

    @Redirect(method = "tesselate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;vertex(Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFFFFFFFI)V"))
    private void coloredLights$vertex(
            LiquidBlockRenderer instance,
            VertexConsumer buffer,
            float x,
            float y,
            float z,
            float red,
            float green,
            float blue,
            float alpha,
            float u,
            float v,
            int packedLight
    ) {
        if(!(buffer instanceof BufferBuilder bufferBuilder)) return;
        //ci.cancel();
        buffer.addVertex(x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setLight(packedLight)
                .setNormal(0.0F, 1.0F, 0.0F);
        SectionPos sectionPos = SectionPos.of(this.coloredLights$blockPos);
        BufferUtils.setLightColor(bufferBuilder, ColoredLightManager.getInstance().sampleMixedLightColor(new Vector3f(sectionPos.x() * 16 + x, sectionPos.y() * 16 + y, sectionPos.z() * 16 + z)));
    }


    @Inject(method = "tesselate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;vertex(Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFFFFFFFI)V"))
    private void coloredLights$beforeVertex(BlockAndTintGetter level, BlockPos pos, VertexConsumer buffer, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        this.coloredLights$blockPos = pos;
    }


    /*@Inject(at = @At("HEAD"), method = "tesselate", cancellable = true)
    public void tesselate(BlockAndTintGetter level, BlockPos pos, VertexConsumer buffer, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        ci.cancel();
        LiquidBlockRenderer renderer = (LiquidBlockRenderer)(Object)this;

        boolean flag = fluidState.is(FluidTags.LAVA);
        TextureAtlasSprite[] atextureatlassprite = net.neoforged.neoforge.client.textures.FluidSpriteCache.getFluidSprites(level, pos, fluidState);
        int i = net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluidState).getTintColor(fluidState, level, pos);
        float alpha = (float)(i >> 24 & 255) / 255.0F;
        float f = (float)(i >> 16 & 0xFF) / 255.0F;
        float f1 = (float)(i >> 8 & 0xFF) / 255.0F;
        float f2 = (float)(i & 0xFF) / 255.0F;
        BlockState blockstate = level.getBlockState(pos.relative(Direction.DOWN));
        FluidState fluidstate = blockstate.getFluidState();
        BlockState blockstate1 = level.getBlockState(pos.relative(Direction.UP));
        FluidState fluidstate1 = blockstate1.getFluidState();
        BlockState blockstate2 = level.getBlockState(pos.relative(Direction.NORTH));
        FluidState fluidstate2 = blockstate2.getFluidState();
        BlockState blockstate3 = level.getBlockState(pos.relative(Direction.SOUTH));
        FluidState fluidstate3 = blockstate3.getFluidState();
        BlockState blockstate4 = level.getBlockState(pos.relative(Direction.WEST));
        FluidState fluidstate4 = blockstate4.getFluidState();
        BlockState blockstate5 = level.getBlockState(pos.relative(Direction.EAST));
        FluidState fluidstate5 = blockstate5.getFluidState();
        boolean flag1 = !LiquidBlockRenderer.isNeighborStateHidingOverlay(fluidState, blockstate1, Direction.DOWN);
        boolean flag2 = renderer.shouldRenderFace(level, pos, fluidState, blockState, Direction.DOWN, blockstate)
                && !LiquidBlockRenderer.isFaceOccludedByNeighbor(level, pos, Direction.DOWN, 0.8888889F, blockstate);
        boolean flag3 = renderer.shouldRenderFace(level, pos, fluidState, blockState, Direction.NORTH, blockstate2);
        boolean flag4 = renderer.shouldRenderFace(level, pos, fluidState, blockState, Direction.SOUTH, blockstate3);
        boolean flag5 = renderer.shouldRenderFace(level, pos, fluidState, blockState, Direction.WEST, blockstate4);
        boolean flag6 = renderer.shouldRenderFace(level, pos, fluidState, blockState, Direction.EAST, blockstate5);
        if (flag1 || flag2 || flag6 || flag5 || flag3 || flag4) {
            float f3 = level.getShade(Direction.DOWN, true);
            float f4 = level.getShade(Direction.UP, true);
            float f5 = level.getShade(Direction.NORTH, true);
            float f6 = level.getShade(Direction.WEST, true);
            Fluid fluid = fluidState.getType();
            float f11 = renderer.getHeight(level, fluid, pos, blockState, fluidState);
            float f7;
            float f8;
            float f9;
            float f10;
            if (f11 >= 1.0F) {
                f7 = 1.0F;
                f8 = 1.0F;
                f9 = 1.0F;
                f10 = 1.0F;
            } else {
                float f12 = renderer.getHeight(level, fluid, pos.north(), blockstate2, fluidstate2);
                float f13 = renderer.getHeight(level, fluid, pos.south(), blockstate3, fluidstate3);
                float f14 = renderer.getHeight(level, fluid, pos.east(), blockstate5, fluidstate5);
                float f15 = renderer.getHeight(level, fluid, pos.west(), blockstate4, fluidstate4);
                f7 = renderer.calculateAverageHeight(level, fluid, f11, f12, f14, pos.relative(Direction.NORTH).relative(Direction.EAST));
                f8 = renderer.calculateAverageHeight(level, fluid, f11, f12, f15, pos.relative(Direction.NORTH).relative(Direction.WEST));
                f9 = renderer.calculateAverageHeight(level, fluid, f11, f13, f14, pos.relative(Direction.SOUTH).relative(Direction.EAST));
                f10 = renderer.calculateAverageHeight(level, fluid, f11, f13, f15, pos.relative(Direction.SOUTH).relative(Direction.WEST));
            }

            float f36 = (float)(pos.getX() & 15);
            float f37 = (float)(pos.getY() & 15);
            float f38 = (float)(pos.getZ() & 15);
            float f39 = 0.001F;
            float f16 = flag2 ? 0.001F : 0.0F;
            if (flag1 && !LiquidBlockRenderer.isFaceOccludedByNeighbor(level, pos, Direction.UP, Math.min(Math.min(f8, f10), Math.min(f9, f7)), blockstate1)) {
                f8 -= 0.001F;
                f10 -= 0.001F;
                f9 -= 0.001F;
                f7 -= 0.001F;
                Vec3 vec3 = fluidState.getFlow(level, pos);
                float f17;
                float f18;
                float f19;
                float f20;
                float f21;
                float f22;
                float f23;
                float f24;
                if (vec3.x == 0.0 && vec3.z == 0.0) {
                    TextureAtlasSprite textureatlassprite1 = atextureatlassprite[0];
                    f17 = textureatlassprite1.getU(0.0F);
                    f21 = textureatlassprite1.getV(0.0F);
                    f18 = f17;
                    f22 = textureatlassprite1.getV(1.0F);
                    f19 = textureatlassprite1.getU(1.0F);
                    f23 = f22;
                    f20 = f19;
                    f24 = f21;
                } else {
                    TextureAtlasSprite textureatlassprite = atextureatlassprite[1];
                    float f25 = (float) Mth.atan2(vec3.z, vec3.x) - (float) (Math.PI / 2);
                    float f26 = Mth.sin(f25) * 0.25F;
                    float f27 = Mth.cos(f25) * 0.25F;
                    float f28 = 0.5F;
                    f17 = textureatlassprite.getU(0.5F + (-f27 - f26));
                    f21 = textureatlassprite.getV(0.5F + -f27 + f26);
                    f18 = textureatlassprite.getU(0.5F + -f27 + f26);
                    f22 = textureatlassprite.getV(0.5F + f27 + f26);
                    f19 = textureatlassprite.getU(0.5F + f27 + f26);
                    f23 = textureatlassprite.getV(0.5F + (f27 - f26));
                    f20 = textureatlassprite.getU(0.5F + (f27 - f26));
                    f24 = textureatlassprite.getV(0.5F + (-f27 - f26));
                }

                float f53 = (f17 + f18 + f19 + f20) / 4.0F;
                float f54 = (f21 + f22 + f23 + f24) / 4.0F;
                float f55 = atextureatlassprite[0].uvShrinkRatio();
                f17 = Mth.lerp(f55, f17, f53);
                f18 = Mth.lerp(f55, f18, f53);
                f19 = Mth.lerp(f55, f19, f53);
                f20 = Mth.lerp(f55, f20, f53);
                f21 = Mth.lerp(f55, f21, f54);
                f22 = Mth.lerp(f55, f22, f54);
                f23 = Mth.lerp(f55, f23, f54);
                f24 = Mth.lerp(f55, f24, f54);
                int l = renderer.getLightColor(level, pos);
                float f57 = f4 * f;
                float f29 = f4 * f1;
                float f30 = f4 * f2;
                vertex(buffer, f36 + 0.0F, f37 + f8, f38 + 0.0F, f57, f29, f30, alpha, f17, f21, l, pos);
                vertex(buffer, f36 + 0.0F, f37 + f10, f38 + 1.0F, f57, f29, f30, alpha, f18, f22, l, pos);
                vertex(buffer, f36 + 1.0F, f37 + f9, f38 + 1.0F, f57, f29, f30, alpha, f19, f23, l, pos);
                vertex(buffer, f36 + 1.0F, f37 + f7, f38 + 0.0F, f57, f29, f30, alpha, f20, f24, l, pos);
                if (fluidState.shouldRenderBackwardUpFace(level, pos.above())) {
                    vertex(buffer, f36 + 0.0F, f37 + f8, f38 + 0.0F, f57, f29, f30, alpha, f17, f21, l, pos);
                    vertex(buffer, f36 + 1.0F, f37 + f7, f38 + 0.0F, f57, f29, f30, alpha, f20, f24, l, pos);
                    vertex(buffer, f36 + 1.0F, f37 + f9, f38 + 1.0F, f57, f29, f30, alpha, f19, f23, l, pos);
                    vertex(buffer, f36 + 0.0F, f37 + f10, f38 + 1.0F, f57, f29, f30, alpha, f18, f22, l, pos);
                }
            }

            if (flag2) {
                float f40 = atextureatlassprite[0].getU0();
                float f41 = atextureatlassprite[0].getU1();
                float f42 = atextureatlassprite[0].getV0();
                float f43 = atextureatlassprite[0].getV1();
                int k = renderer.getLightColor(level, pos.below());
                float f46 = f3 * f;
                float f48 = f3 * f1;
                float f50 = f3 * f2;
                vertex(buffer, f36, f37 + f16, f38 + 1.0F, f46, f48, f50, alpha, f40, f43, k, pos);
                vertex(buffer, f36, f37 + f16, f38, f46, f48, f50, alpha, f40, f42, k, pos);
                vertex(buffer, f36 + 1.0F, f37 + f16, f38, f46, f48, f50, alpha, f41, f42, k, pos);
                vertex(buffer, f36 + 1.0F, f37 + f16, f38 + 1.0F, f46, f48, f50, alpha, f41, f43, k, pos);
            }

            int j = renderer.getLightColor(level, pos);

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                float f44;
                float f45;
                float f47;
                float f49;
                float f51;
                float f52;
                boolean flag7;
                switch (direction) {
                    case NORTH:
                        f44 = f8;
                        f45 = f7;
                        f47 = f36;
                        f51 = f36 + 1.0F;
                        f49 = f38 + 0.001F;
                        f52 = f38 + 0.001F;
                        flag7 = flag3;
                        break;
                    case SOUTH:
                        f44 = f9;
                        f45 = f10;
                        f47 = f36 + 1.0F;
                        f51 = f36;
                        f49 = f38 + 1.0F - 0.001F;
                        f52 = f38 + 1.0F - 0.001F;
                        flag7 = flag4;
                        break;
                    case WEST:
                        f44 = f10;
                        f45 = f8;
                        f47 = f36 + 0.001F;
                        f51 = f36 + 0.001F;
                        f49 = f38 + 1.0F;
                        f52 = f38;
                        flag7 = flag5;
                        break;
                    default:
                        f44 = f7;
                        f45 = f9;
                        f47 = f36 + 1.0F - 0.001F;
                        f51 = f36 + 1.0F - 0.001F;
                        f49 = f38;
                        f52 = f38 + 1.0F;
                        flag7 = flag6;
                }

                if (flag7
                        && !LiquidBlockRenderer.isFaceOccludedByNeighbor(level, pos, direction, Math.max(f44, f45), level.getBlockState(pos.relative(direction)))) {
                    BlockPos blockpos = pos.relative(direction);
                    TextureAtlasSprite textureatlassprite2 = atextureatlassprite[1];
                    if (atextureatlassprite[2] != null) {
                        if (level.getBlockState(blockpos).shouldDisplayFluidOverlay(level, blockpos, fluidState)) {
                            textureatlassprite2 = atextureatlassprite[2];
                        }
                    }

                    float f56 = textureatlassprite2.getU(0.0F);
                    float f58 = textureatlassprite2.getU(0.5F);
                    float f59 = textureatlassprite2.getV((1.0F - f44) * 0.5F);
                    float f60 = textureatlassprite2.getV((1.0F - f45) * 0.5F);
                    float f31 = textureatlassprite2.getV(0.5F);
                    float f32 = direction.getAxis() == Direction.Axis.Z ? f5 : f6;
                    float f33 = f4 * f32 * f;
                    float f34 = f4 * f32 * f1;
                    float f35 = f4 * f32 * f2;
                    vertex(buffer, f47, f37 + f44, f49, f33, f34, f35, alpha, f56, f59, j, pos);
                    vertex(buffer, f51, f37 + f45, f52, f33, f34, f35, alpha, f58, f60, j, pos);
                    vertex(buffer, f51, f37 + f16, f52, f33, f34, f35, alpha, f58, f31, j, pos);
                    vertex(buffer, f47, f37 + f16, f49, f33, f34, f35, alpha, f56, f31, j, pos);
                    if (textureatlassprite2 != atextureatlassprite[2]) { // Neo: use custom fluid's overlay texture
                        vertex(buffer, f47, f37 + f16, f49, f33, f34, f35, alpha, f56, f31, j, pos);
                        vertex(buffer, f51, f37 + f16, f52, f33, f34, f35, alpha, f58, f31, j, pos);
                        vertex(buffer, f51, f37 + f45, f52, f33, f34, f35, alpha, f58, f60, j, pos);
                        vertex(buffer, f47, f37 + f44, f49, f33, f34, f35, alpha, f56, f59, j, pos);
                    }
                }
            }
        }
    }*/
}
