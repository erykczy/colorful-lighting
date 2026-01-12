package me.erykczy.colorfullighting.compat.oculus;

import java.lang.reflect.Method;

public class OculusCompat {
    private static boolean checked = false;
    private static boolean loaded = false;
    private static Object irisApiInstance;
    private static Method isShaderPackInUseMethod;

    public static boolean isOculusLoaded() {
        if (!checked) {
            try {
                Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Method getInstanceMethod = apiClass.getMethod("getInstance");
                irisApiInstance = getInstanceMethod.invoke(null);
                isShaderPackInUseMethod = apiClass.getMethod("isShaderPackInUse");
                loaded = true;
            } catch (Exception e) {
                loaded = false;
            }
            checked = true;
        }
        return loaded;
    }

    public static boolean isShaderPackInUse() {
        if (!isOculusLoaded()) return false;
        try {
            return (boolean) isShaderPackInUseMethod.invoke(irisApiInstance);
        } catch (Exception e) {
            return false;
        }
    }
}
