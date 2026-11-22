package com.example.matinventory;

import com.example.matinventory.capacity.CapacityConfig;
import com.example.matinventory.capacity.CapacityHello;
import com.example.matinventory.capacity.ExtraInventory;
import com.example.matinventory.debug.InventoryFillCommand;
import com.example.matinventory.interaction.CraftRequest;
import com.example.matinventory.interaction.InventoryCrafting;
import com.example.matinventory.interaction.InventoryEating;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.CopyFrom;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.EndTick;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class MatInventory implements ModInitializer {
   @Override
   public void onInitialize() {
      MatSounds.register();
      PayloadTypeRegistry.serverboundPlay().register(CraftRequest.TYPE, CraftRequest.CODEC);
      ServerPlayNetworking.registerGlobalReceiver(CraftRequest.TYPE, (request, context) -> InventoryCrafting.craft(context.player(), request));
      InventoryEating.register();
      InventoryFillCommand.register();
      CapacityConfig.load();
      PayloadTypeRegistry.clientboundPlay().register(CapacityHello.TYPE, CapacityHello.CODEC);
      ServerPlayConnectionEvents.JOIN.register((Join)(handler, sender, server) -> {
         if (ServerPlayNetworking.canSend(handler.player, CapacityHello.TYPE)) {
            ServerPlayNetworking.send(handler.player, new CapacityHello(CapacityConfig.storageSlots));
         } else {
            handler.disconnect(Component.literal("Este servidor usa Inventory Expanded. Instale a mesma versão do mod no cliente."));
         }
      });
      ServerPlayerEvents.COPY_FROM.register((CopyFrom)(oldPlayer, newPlayer, alive) -> ExtraInventory.of(newPlayer).copyFrom(ExtraInventory.of(oldPlayer)));
      ServerTickEvents.END_SERVER_TICK.register((EndTick)server -> {
         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ExtraInventory extra = ExtraInventory.of(player);
            if (extra.dirty) {
               extra.dirty = false;
               if (player.containerMenu != player.inventoryMenu) {
                  player.inventoryMenu.broadcastChanges();
               }
            }
         }
      });
   }
}
