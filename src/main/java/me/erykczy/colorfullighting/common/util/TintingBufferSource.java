package me.erykczy.colorfullighting.common.util;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.HashMap;
import java.util.Map;

public class TintingBufferSource implements MultiBufferSource {
    private final MultiBufferSource parent;
    private final float mr, mg, mb;
    private final Map<RenderType, VertexConsumer> cache = new HashMap<>();

    public TintingBufferSource(MultiBufferSource parent, float mr, float mg, float mb) {
        this.parent = parent; this.mr = mr; this.mg = mg; this.mb = mb;
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        return cache.computeIfAbsent(type,
                rt -> new TintingVertexConsumer(parent.getBuffer(rt), mr, mg, mb));
    }
}

