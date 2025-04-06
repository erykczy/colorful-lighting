package com.example.examplemod.mixin;

import com.example.examplemod.ColoredLightManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockLightEngine.class)
public abstract class BlockLightEngineMixin {
    @Inject(method = "checkNode", at = @At("TAIL"))
    private void coloredLights$checkNode(long packedPos, CallbackInfo ci) {
        if(!Minecraft.getInstance().isSameThread()) return; // only client side
        BlockLightEngine lightEngine = (BlockLightEngine)(Object)this;
        SectionPos sectionPos = SectionPos.of(BlockPos.of(packedPos));
        for(int x = -1; x <= 1; ++x) {
            for(int z = -1; z <= 1; ++z) {
                if(!ColoredLightManager.getInstance().storage.containsSection(sectionPos.offset(x, 0, z).asLong())) return;
            }
        }
        ColoredLightManager.getInstance().onBlockLightPropertiesChanged(lightEngine.chunkSource.getLevel(), BlockPos.of(packedPos));
    }

    /*@Inject(at = @At("HEAD"), method = "checkNode", cancellable = true)
    protected void checkNode(long blockPos, CallbackInfo ci) {
        if(!Minecraft.getInstance().isSameThread()) return; // only client side
        ci.cancel();
        BlockLightEngine engine = (BlockLightEngine)(Object)this;

        long sectionPos = SectionPos.blockToSection(blockPos);
        // if no light data for the section, return
        if (!engine.storage.storingLightForSection(sectionPos)) return;
        if (!ColoredLightManager.getInstance().storage.containsLayer(sectionPos)) return; // added

        BlockState blockState = engine.getState(BlockPos.of(blockPos));
        int blockEmission = engine.getEmission(blockPos, blockState);
        FastColor3 blockEmissionColor = ColoredLightManager.getInstance().getEmissionColor(engine.getChunk(SectionPos.x(sectionPos), SectionPos.z(sectionPos)), BlockPos.of(blockPos)); // added
        int lightLevel = engine.storage.getStoredLevel(blockPos);

        // THEORY: checkNode function executes only when block pos changes its light properties
        // if the new block lets light through, then lightLevel (that hasn't been updated yet) is 0 and the condition is false
        // if block emits less light than it has
        if (blockEmission < lightLevel) {
            engine.storage.setStoredLevel(blockPos, 0);
            ColoredLightManager.getInstance().storage.setLightColor(BlockPos.getX(blockPos), BlockPos.getY(blockPos), BlockPos.getZ(blockPos), new FastColor3()); // added
            engine.enqueueDecrease(blockPos, LightEngine.QueueEntry.decreaseAllDirections(lightLevel));
        } else {
            // if(lightLevel <= blockEmission)
            // executed if block emits more or equal light than it has
            // pull light from nearby blocks
            engine.enqueueDecrease(blockPos, BlockLightEngine.PULL_LIGHT_IN_ENTRY);
        }

        // enqueue increase for emmisive block
        if (blockEmission > 0) {
            engine.enqueueIncrease(blockPos, LightEngine.QueueEntry.increaseLightFromEmission(blockEmission, engine.isEmptyShape(blockState)));
            ColoredLightManager.getInstance().enqueueIncrease(blockEmissionColor); // added
        }
    }*/
}
