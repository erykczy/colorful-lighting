package com.example.examplemod.util;

import com.google.gson.JsonElement;
import net.minecraft.world.item.DyeColor;

import java.awt.*;

public abstract class JsonHelper {
    public static ColorRGB4 getColor4FromString(String string) {
        ColorRGB4 color = getColor4FromDyeName(string);
        if(color != null) return color;
        color = getColor4FromHexString(string);
        if(color != null) return color;
        return color;
    }

    public static ColorRGB4 getColor4FromDyeName(String dyeName) {
        DyeColor dyeColor = DyeColor.byName(dyeName, null);
        if(dyeColor != null) {
            Color color = new Color(dyeColor.getTextColor());
            return ColorRGB4.fromRGB8(color.getRed(), color.getGreen(), color.getBlue());
        }
        return null;
    }

    public static ColorRGB4 getColor4FromHexString(String string) {
        if(string.length() != 6+1 || !string.startsWith("#")) return null;
        long colorFromHex = Long.parseLong(string.substring(1), 16);
        long blue = colorFromHex & 0xFF;
        long green = (colorFromHex >> 8) & 0xFF;
        long red = (colorFromHex >> 16) & 0xFF;
        return ColorRGB4.fromRGB8((int)red, (int)green, (int)blue);
    }

    public static ColorRGB4 getColor4FromJsonElements(JsonElement red, JsonElement green, JsonElement blue) {
        Integer redI = getInt4FromJsonElement(red);
        Integer greenI = getInt4FromJsonElement(green);
        Integer blueI = getInt4FromJsonElement(blue);
        if(redI == null || greenI == null || blueI == null) return null;
        ColorRGB4 color = ColorRGB4.fromRGB4(redI, greenI, blueI);
        if(!color.isInValidState()) return null;
        return color;
    }
    public static Integer getInt4FromJsonElement(JsonElement element) {
        int value;
        try {
            value = element.getAsBigInteger().intValue();
        }
        catch (NumberFormatException e) {
            value = (int)(element.getAsFloat()*15.0f);
        }
        if(value >= 0 && value < 16) return value;
        return null;
    }
}
