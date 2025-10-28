package me.erykczy.colorfullighting.common.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface CLRendererExtension {
    void colorfullighting$render(BlockEntity be, float pt, PoseStack ps, MultiBufferSource bs, int packedLight, int overlay);
}
