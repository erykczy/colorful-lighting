package me.erykczy.colorfullighting;

import com.mojang.logging.LogUtils;
import me.erykczy.colorfullighting.accessors.MinecraftWrapper;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.accessors.ClientAccessor;
import me.erykczy.colorfullighting.event.ClientEventListener;
import me.erykczy.colorfullighting.resourcemanager.ModResourceManagers;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(value = ColorfulLighting.MOD_ID)
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
        clientAccessor = new MinecraftWrapper(Minecraft.getInstance());
        ColoredLightEngine.create(clientAccessor);
    }
}
