package me.erykczy.colorfullighting.flywheel;

import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import me.erykczy.colorfullighting.common.util.PackedLightData;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class CreateCompat {
    private static CreateCompat instance;

    public static void create() {
        instance = new CreateCompat();
    }

    public static CreateCompat getInstance() {
        return instance;
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    public boolean colorfullighting$getLightColor(BlockAndTintGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if(level instanceof PonderLevel) {
            cir.setReturnValue(PackedLightData.packData(0, 15*15, 15*15, 15*15));
            return true;
        }
        if(level instanceof VirtualRenderWorld) {
            cir.setReturnValue(0);
            return true;
        }
        return false;
    }
}
