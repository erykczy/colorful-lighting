package me.erykczy.colorfullighting.flywheel;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import me.erykczy.colorfullighting.common.util.PackedLightData;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class FlywheelCompat {
    private static FlywheelCompat instance;
    public ColoredLightFlywheelStorage flywheelColoredLightStorage;

    public static void create() {
        RenderSystem.recordRenderCall(() -> instance = new FlywheelCompat());
    }

    public static FlywheelCompat getInstance() {
        return instance;
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    public FlywheelCompat() {
        flywheelColoredLightStorage = new ColoredLightFlywheelStorage();
    }

    public boolean colorfullighting$getLightColor(BlockAndTintGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if(level instanceof PonderLevel) {
            cir.setReturnValue(PackedLightData.packData(15, 0, 0, 0));
            return true;
        }
        if(level instanceof VirtualRenderWorld) {
            cir.setReturnValue(0);
            return true;
        }
        return false;
    }
}
