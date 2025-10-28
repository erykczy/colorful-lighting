package me.erykczy.colorfullighting.common.util;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class TintingVertexConsumer implements VertexConsumer {
    private final VertexConsumer d;
    private final float mr, mg, mb;

    public TintingVertexConsumer(VertexConsumer d, float mr, float mg, float mb) {
        this.d = d; this.mr = mr; this.mg = mg; this.mb = mb;
    }

    private static int clamp(int v){ return v < 0 ? 0 : (v > 255 ? 255 : v); }
    private int tint(int c, float m){ return clamp(Math.round(c * m)); }

    @Override public VertexConsumer vertex(double x, double y, double z){ return d.vertex(x,y,z); }

    @Override public VertexConsumer color(int r, int g, int b, int a){
        return d.color(tint(r, mr), tint(g, mg), tint(b, mb), a);
    }

    @Override public VertexConsumer uv(float u, float v){ return d.uv(u,v); }
    @Override public VertexConsumer overlayCoords(int u, int v){ return d.overlayCoords(u,v); }
    @Override public VertexConsumer uv2(int u, int v){ return d.uv2(u,v); }
    @Override public VertexConsumer normal(float x, float y, float z){ return d.normal(x,y,z); }
    @Override public void endVertex(){ d.endVertex(); }

    @Override public void defaultColor(int r, int g, int b, int a){
        d.defaultColor(tint(r, mr), tint(g, mg), tint(b, mb), a);
    }

    @Override public void unsetDefaultColor(){ d.unsetDefaultColor(); }
}

