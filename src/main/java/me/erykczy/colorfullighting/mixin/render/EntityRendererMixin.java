package me.erykczy.colorfullighting.mixin.render;

import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.Config;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.PackedLightData;
import net.minecraft.client.renderer.entity.DragonFireballRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    private static final Set<EntityType<?>> FIRE_LIT_ENTITIES = new HashSet<>(Arrays.asList(
            EntityType.BLAZE,
            EntityType.MAGMA_CUBE
    ));
    private static final Set<EntityType<?>> LIT_ENTITIES = new HashSet<>(Arrays.asList(
            EntityType.ALLAY,
            EntityType.DRAGON_FIREBALL,
            EntityType.EXPERIENCE_ORB,
            EntityType.GLOW_SQUID,
            // EntityType.ITEM_FRAME, // Removed to allow colored lighting on Item Frames
            EntityType.SHULKER_BULLET,
            EntityType.EYE_OF_ENDER,
            EntityType.FIREBALL,
            EntityType.SMALL_FIREBALL,
            EntityType.VEX,
            EntityType.WITHER,
            EntityType.WITHER_SKULL
    ));

    @Inject(method = "getPackedLightCoords", at = @At("HEAD"), cancellable = true, require = 0)
    private <T extends Entity>void colorfullighting$getPackedLightCoords(T entity, float partialTicks, CallbackInfoReturnable<Integer> cir) {
        BlockPos blockpos = BlockPos.containing(entity.getLightProbePosition(partialTicks));
        int skyLight = entity.level().getBrightness(LightLayer.SKY, blockpos);
        ColorRGB8 color = ColoredLightEngine.getInstance().sampleTrilinearLightColor(entity.getLightProbePosition(partialTicks));
        if(entity.isOnFire() || FIRE_LIT_ENTITIES.contains(entity.getType())) {
            ColorRGB8 fireColor = ColorRGB8.fromRGB4(Config.getLightColor(Blocks.FIRE.builtInRegistryHolder().key()));
            color = ColorRGB8.fromRGB8(
                Math.max(fireColor.red, color.red),
                Math.max(fireColor.green, color.green),
                Math.max(fireColor.blue, color.blue)
            );
        }
        if(LIT_ENTITIES.contains(entity.getType())) {
            color = ColorRGB8.fromRGB8(255, 255, 255);
        }

        cir.setReturnValue(PackedLightData.packData(skyLight, color));
    }
}
