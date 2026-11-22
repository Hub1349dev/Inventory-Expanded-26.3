package com.example.matinventory.mixin;

import com.example.matinventory.config.PlacementProfiles;
import com.example.matinventory.render.MatItems;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ClientPacketListener.class)
abstract class PlacementSyncMixin {
   @Inject(method = "handleContainerContent", at = @At("TAIL"))
   private void matinventory$contents(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
      if (packet.containerId() == 0) {
         PlacementProfiles.synced();
      }
   }

   @Inject(method = "handleRespawn", at = @At("TAIL"))
   private void matinventory$respawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
      PlacementProfiles.awaitSnapshot();
      MatItems.clearMenus();
   }
}
