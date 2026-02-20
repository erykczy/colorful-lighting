package me.erykczy.colorfullighting.mixin;

import me.erykczy.colorfullighting.common.ColoredLightEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(
            method = "handleExplosion(Lnet/minecraft/network/protocol/game/ClientboundExplodePacket;)V",
            at = @At("TAIL")
    )
    private void colorfullighting$handleExplosion(ClientboundExplodePacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        List<BlockPos> toBlow = packet.getToBlow();
        if (toBlow == null || toBlow.isEmpty()) return;

        Set<Long> rebuilt = new HashSet<>();
        for (BlockPos pos : toBlow) {
            ChunkPos cp = new ChunkPos(pos);
            if (rebuilt.add(cp.toLong())) {
                ColoredLightEngine.getInstance().rebuildChunk(cp, 500);
            }
        }
    }
}
