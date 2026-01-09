package me.erykczy.colorfullighting;

import com.mojang.logging.LogUtils;
import me.erykczy.colorfullighting.accessors.MinecraftWrapper;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.accessors.BlockStateAccessor;
import me.erykczy.colorfullighting.common.accessors.ClientAccessor;
import me.erykczy.colorfullighting.common.accessors.LevelAccessor;
import me.erykczy.colorfullighting.compat.sodium.SodiumCompat;
import me.erykczy.colorfullighting.event.ClientEventListener;
import me.erykczy.colorfullighting.resourcemanager.ModResourceManagers;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.List;

@Mod(value = ColorfulLighting.MOD_ID)
@Mod.EventBusSubscriber(modid = ColorfulLighting.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ColorfulLighting
{
    public static final String MOD_ID = "colorful_lighting";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static ClientAccessor clientAccessor;

    public ColorfulLighting(FMLJavaModLoadingContext context)
    {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> new DistExecutor.SafeRunnable() {
            @Override
            public void run() {
                ModResourceManagers.register(context.getModEventBus());
                MinecraftForge.EVENT_BUS.register(new ClientEventListener());
                context.getModEventBus().addListener(ColorfulLighting::onLoadingComplete);
            }
        });


    }

    public static void onLoadingComplete(FMLLoadCompleteEvent event) {
        if (SodiumCompat.isSodiumLoaded()) {
            LOGGER.info("Embeddium/Sodium detected!");
        }
        clientAccessor = new MinecraftWrapper(Minecraft.getInstance());
        ColoredLightEngine.create(clientAccessor);
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        ColoredLightEngine.getInstance().reset();
    }
}
