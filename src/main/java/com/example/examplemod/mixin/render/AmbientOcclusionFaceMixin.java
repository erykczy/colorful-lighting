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
        if (PackedLightData.isEmpty(lightColor0)) lightColor0 = lightColor3;
        if (PackedLightData.isEmpty(lightColor1)) lightColor1 = lightColor3;
        if (PackedLightData.isEmpty(lightColor2)) lightColor2 = lightColor3;

        var data0 = PackedLightData.unpackData(lightColor0);
        var data1 = PackedLightData.unpackData(lightColor1);
        var data2 = PackedLightData.unpackData(lightColor2);
        var data3 = PackedLightData.unpackData(lightColor3);
        int blockLight = (data0.blockLight + data1.blockLight + data2.blockLight + data3.blockLight) >> 2;
        int skyLight = (data0.skyLight + data1.skyLight + data2.skyLight + data3.skyLight) >> 2;
        int red4 = (data0.red4 + data1.red4 + data2.red4 + data3.red4) >> 2;
        int green4 = (data0.green4 + data1.green4 + data2.green4 + data3.green4) >> 2;
        int blue4 = (data0.blue4 + data1.blue4 + data2.blue4 + data3.blue4) >> 2;

        cir.setReturnValue(PackedLightData.packData(blockLight, skyLight, red4, green4, blue4));
    }

    @Inject(method = "blend(IIIIFFFF)I", at = @At("HEAD"), cancellable = true)
    private void coloredLights$blend(int lightColor0, int lightColor1, int lightColor2, int lightColor3, float weight0, float weight1, float weight2, float weight3, CallbackInfoReturnable<Integer> cir) {
        var data0 = PackedLightData.unpackData(lightColor0);
        var data1 = PackedLightData.unpackData(lightColor1);
        var data2 = PackedLightData.unpackData(lightColor2);
        var data3 = PackedLightData.unpackData(lightColor3);
        float blockLight = data0.blockLight * weight0 + data1.blockLight * weight1 + data2.blockLight * weight2 + data3.blockLight * weight3;
        float skyLight = data0.skyLight * weight0 + data1.skyLight * weight1 + data2.skyLight * weight2 + data3.skyLight * weight3;
        float red4 = data0.red4 * weight0 + data1.red4 * weight1 + data2.red4 * weight2 + data3.red4 * weight3;
        float green4 = data0.green4 * weight0 + data1.green4 * weight1 + data2.green4 * weight2 + data3.green4 * weight3;
        float blue4 = data0.blue4 * weight0 + data1.blue4 * weight1 + data2.blue4 * weight2 + data3.blue4 * weight3;

        cir.setReturnValue(PackedLightData.packData((int)blockLight, (int)skyLight, (int)red4, (int)green4, (int)blue4));
    }
}
