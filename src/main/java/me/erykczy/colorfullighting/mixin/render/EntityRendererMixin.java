package me.erykczy.colorfullighting.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.Config;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.common.util.PackedLightData;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
            EntityType.ITEM_FRAME,
            EntityType.SHULKER_BULLET,
            EntityType.EYE_OF_ENDER,
            EntityType.FIREBALL,
            EntityType.SMALL_FIREBALL,
            EntityType.VEX,
            EntityType.WITHER,
            EntityType.WITHER_SKULL
    ));

    @Inject(method = "getPackedLightCoords", at = @At("HEAD"), cancellable = true)
    private <T extends Entity>void colorfullighting$getPackedLightCoords(T entity, float partialTicks, CallbackInfoReturnable<Integer> cir) {
        BlockPos blockpos = BlockPos.containing(entity.getLightProbePosition(partialTicks));
        int skyLight = entity.level().getBrightness(LightLayer.SKY, blockpos);
        ColorRGB8 color = ColoredLightEngine.getInstance().sampleTrilinearLightColor(entity.getLightProbePosition(partialTicks));
        if(entity.isOnFire() || FIRE_LIT_ENTITIES.contains(entity.getType())) {
            ColorRGB8 fireColor = ColorRGB8.fromRGB4(Config.getLightColor(Blocks.FIRE.builtInRegistryHolder().getKey()));
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

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;getBlockLightLevel(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;)I"))
    private <T extends Entity>int colorfullighting$extractRenderState(EntityRenderer instance, T entity, BlockPos pos) {
        int skyLight = entity.level().getBrightness(LightLayer.SKY, pos);
        ColorRGB8 color = ColoredLightEngine.getInstance().sampleTrilinearLightColor(pos.getCenter());
        if(entity.isOnFire() || FIRE_LIT_ENTITIES.contains(entity.getType())) {
            ColorRGB8 fireColor = ColorRGB8.fromRGB4(Config.getLightColor(Blocks.FIRE.builtInRegistryHolder().getKey()));
            color = ColorRGB8.fromRGB8(
                    Math.max(fireColor.red, color.red),
                    Math.max(fireColor.green, color.green),
                    Math.max(fireColor.blue, color.blue)
            );
        }
        if(LIT_ENTITIES.contains(entity.getType())) {
            color = ColorRGB8.fromRGB8(255, 255, 255);
        }

        return PackedLightData.packData(skyLight, color);
    }
}
