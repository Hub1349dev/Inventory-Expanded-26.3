package com.example.matinventory.platform;

import com.example.matinventory.capacity.ExtraInventory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

@Environment(EnvType.CLIENT)
public final class ClientNetwork {
   public static boolean supported() {
      return Minecraft.getInstance().getConnection() != null && ExtraInventory.clientSupported;
   }

   public static void send(CustomPacketPayload request) {
      ClientPlayNetworking.send(request);
   }

   private ClientNetwork() {
   }
}
