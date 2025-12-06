package me.erykczy.colorfullighting.mixin.create;

import me.erykczy.colorfullighting.common.util.PackedLightData;
import net.createmod.catnip.render.ShadeSeparatingSuperByteBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ShadeSeparatingSuperByteBuffer.class)
public class ShadeSeparatingSuperByteBufferMixin {
    @Redirect(method = "renderInto", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/render/SuperByteBuffer;maxLight(II)I", ordinal = 0))
    private int colorfullighting$maxLight0(int packedLight1, int packedLight2) {
        return packedLight2;
    }
    @Redirect(method = "renderInto", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/render/SuperByteBuffer;maxLight(II)I", ordinal = 1))
    private int colorfullighting$maxLight1(int packedLight1, int packedLight2) {
        return PackedLightData.max(packedLight1, packedLight2);
    }
}
