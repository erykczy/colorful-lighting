// me/erykczy/colorfullighting/client/CLTintContext.java
package me.erykczy.colorfullighting.common.util;

public final class CLTintContext {
    private static final ThreadLocal<float[]> TINT = new ThreadLocal<>();
    public static void set(float[] mul) { TINT.set(mul); }
    public static float[] get() { return TINT.get(); }
    public static void clear() { TINT.remove(); }

    public static int multiplyColor(int argb, float[] mul) {
        if (mul == null) return argb;
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>>  8) & 0xFF;
        int b = (argb       ) & 0xFF;
        r = Math.min(255, Math.round(r * mul[0]));
        g = Math.min(255, Math.round(g * mul[1]));
        b = Math.min(255, Math.round(b * mul[2]));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
