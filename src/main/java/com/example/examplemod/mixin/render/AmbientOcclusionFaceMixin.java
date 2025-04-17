package com.example.examplemod.mixin.render;

import com.example.examplemod.util.PackedLightData;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelBlockRenderer.AmbientOcclusionFace.class)
public class AmbientOcclusionFaceMixin {

    @Inject(method = "blend(IIII)I", at = @At("HEAD"), cancellable = true)
    private void coloredLights$blend(int lightColor0, int lightColor1, int lightColor2, int lightColor3, CallbackInfoReturnable<Integer> cir) {
        if (PackedLightData.isBlack(lightColor0)) lightColor0 = lightColor3;
        if (PackedLightData.isBlack(lightColor1)) lightColor1 = lightColor3;
        if (PackedLightData.isBlack(lightColor2)) lightColor2 = lightColor3;

        var data0 = PackedLightData.unpackData(lightColor0);
        var data1 = PackedLightData.unpackData(lightColor1);
        var data2 = PackedLightData.unpackData(lightColor2);
        var data3 = PackedLightData.unpackData(lightColor3);
        //int blockLight = (data0.blockLight + data1.blockLight + data2.blockLight + data3.blockLight) >> 2;
        int skyLight = (data0.skyLight4 + data1.skyLight4 + data2.skyLight4 + data3.skyLight4) >> 2;
        int red8 = (data0.red8 + data1.red8 + data2.red8 + data3.red8) >> 2;
        int green8 = (data0.green8 + data1.green8 + data2.green8 + data3.green8) >> 2;
        int blue8 = (data0.blue8 + data1.blue8 + data2.blue8 + data3.blue8) >> 2;

        cir.setReturnValue(PackedLightData.packData(skyLight, red8, green8, blue8));
    }

    @Inject(method = "blend(IIIIFFFF)I", at = @At("HEAD"), cancellable = true)
    private void coloredLights$blend(int lightColor0, int lightColor1, int lightColor2, int lightColor3, float weight0, float weight1, float weight2, float weight3, CallbackInfoReturnable<Integer> cir) {
        var data0 = PackedLightData.unpackData(lightColor0);
        var data1 = PackedLightData.unpackData(lightColor1);
        var data2 = PackedLightData.unpackData(lightColor2);
        var data3 = PackedLightData.unpackData(lightColor3);
        //float blockLight = data0.blockLight * weight0 + data1.blockLight * weight1 + data2.blockLight * weight2 + data3.blockLight * weight3;
        float skyLight = data0.skyLight4 * weight0 + data1.skyLight4 * weight1 + data2.skyLight4 * weight2 + data3.skyLight4 * weight3;
        float red8 = data0.red8 * weight0 + data1.red8 * weight1 + data2.red8 * weight2 + data3.red8 * weight3;
        float green8 = data0.green8 * weight0 + data1.green8 * weight1 + data2.green8 * weight2 + data3.green8 * weight3;
        float blue8 = data0.blue8 * weight0 + data1.blue8 * weight1 + data2.blue8 * weight2 + data3.blue8 * weight3;

        cir.setReturnValue(PackedLightData.packData((int)skyLight, (int)red8, (int)green8, (int)blue8));
    }
}
