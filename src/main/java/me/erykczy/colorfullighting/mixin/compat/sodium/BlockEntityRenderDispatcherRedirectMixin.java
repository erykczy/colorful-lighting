package me.erykczy.colorfullighting.mixin.compat.sodium;

import com.mojang.blaze3d.vertex.PoseStack;
import me.erykczy.colorfullighting.accessors.BlockStateWrapper;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.Config;
import me.erykczy.colorfullighting.common.accessors.BlockStateAccessor;
import me.erykczy.colorfullighting.common.accessors.LevelAccessor;
import me.erykczy.colorfullighting.common.util.ColorRGB4;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.PackedLightData;
import me.erykczy.colorfullighting.common.util.TintingBufferSource;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BlockEntityRenderDispatcher.class, priority = 99999)
public abstract class BlockEntityRenderDispatcherRedirectMixin {

    @Unique
    private static boolean cl$skip() {
        return ModList.get().isLoaded("embeddium");
    }

    @Unique
    private static int colorful_lighting$attenuateSky(int sky, ColorRGB4 color) {
        int max = Math.max(color.red4, Math.max(color.green4, color.blue4));
        return (15 - max) * sky / 15;
    }

    @Redirect(
            method = "setupAndRender(Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"
            )
    )
    private static <T extends BlockEntity> void cl$renderWithColoredLight(
            BlockEntityRenderer<T> renderer,
            T be, float pt, PoseStack pose, MultiBufferSource buffers,
            int packedLight, int overlay
    ) {
        var engine = ColoredLightEngine.getInstance();
        if (!cl$skip() || be == null || be.getLevel() == null || engine == null) {
            renderer.render(be, pt, pose, buffers, packedLight, overlay);
            return;
        }

        BlockPos pos = be.getBlockPos();
        BlockState state = be.getBlockState();
        int sky = be.getLevel().getBrightness(LightLayer.SKY, pos);

        ColorRGB8 rgb;
        int skyAtt = sky;

        if (state.emissiveRendering(be.getLevel(), pos)) {
            LevelAccessor levelAccessor = engine.clientAccessor.getLevel();
            BlockStateAccessor stateAccessor = new BlockStateWrapper(state);
            if (levelAccessor != null) {
                ColorRGB4 emission = Config.getLightColor(stateAccessor);
                skyAtt = colorful_lighting$attenuateSky(sky, emission);
                rgb = ColorRGB8.fromRGB4(emission);
            } else {
                skyAtt = 15;
                rgb = ColorRGB8.fromRGB8(255, 255, 255);
            }
        } else {
            rgb = engine.sampleTrilinearLightColor(Vec3.atCenterOf(pos));
            ColorRGB4 color4 = ColorRGB4.fromRGB4((rgb.red >> 4), (rgb.green >> 4), (rgb.blue >> 4));
            skyAtt = colorful_lighting$attenuateSky(sky, color4);
        }

        int r8 = rgb.red & 0xFF, g8 = rgb.green & 0xFF, b8 = rgb.blue & 0xFF;
        int max = Math.max(r8, Math.max(g8, b8));
        float[] mul = null;
        if (max > 0) {
            float k  = max / 255f;
            float mr = 1f + k * ((r8 / 255f) - 1f);
            float mg = 1f + k * ((g8 / 255f) - 1f);
            float mb = 1f + k * ((b8 / 255f) - 1f);
            mul = new float[]{mr, mg, mb};
        }

        if (mul == null) {
            renderer.render(be, pt, pose, buffers, packedLight, overlay);
            return;
        }

        MultiBufferSource tinted = new TintingBufferSource(buffers, mul[0], mul[1], mul[2]);
        int coloredPacked = PackedLightData.packData(skyAtt, rgb);
        int lightOut = coloredPacked;

        renderer.render(be, pt, pose, tinted, lightOut, overlay);
    }
}