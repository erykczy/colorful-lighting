package me.erykczy.colorfullighting.mixin.compat.sodium;

import com.mojang.blaze3d.vertex.PoseStack;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.util.CLRendererExtension;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.PackedLightData; // your packer that supports RGB
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntityRenderer.class)
public interface BlockEntityRendererMixin extends CLRendererExtension {

    @Override
    default void colorfullighting$render(BlockEntity be, float pt, PoseStack ps, MultiBufferSource bs,
                                         int packedLight, int overlay) {
        if (ModList.get().isLoaded("embeddium") || be == null || be.getLevel() == null) {
            @SuppressWarnings("unchecked")
            BlockEntityRenderer<BlockEntity> self = (BlockEntityRenderer<BlockEntity>) (Object) this;
            self.render(be, pt, ps, bs, packedLight, overlay);
            return;
        }

        var level = be.getLevel();
        BlockPos pos = be.getBlockPos();
        int sky = level.getBrightness(LightLayer.SKY, pos);

        var sampleAt = pos.getCenter();
        ColorRGB8 rgb = ColoredLightEngine.getInstance().sampleTrilinearLightColor(sampleAt);

        int coloredPacked = PackedLightData.packData(sky, rgb);

        @SuppressWarnings("unchecked")
        BlockEntityRenderer<BlockEntity> self = (BlockEntityRenderer<BlockEntity>) (Object) this;
        self.render(be, pt, ps, bs, coloredPacked, overlay);
    }
}
