package com.example.matinventory.config;

import com.example.matinventory.MatInventoryClient;
import com.example.matinventory.arrangement.ItemPresentationData;
import com.example.matinventory.capacity.ExtraInventory;
import com.example.matinventory.layout.MatLayout;
import java.util.HashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.storage.LevelResource;

@Environment(EnvType.CLIENT)
public final class PlacementProfiles {
   private static boolean ready;

   public static void activate(Minecraft client) {
      if (client.player != null) {
         String world = client.getSingleplayerServer() != null
            ? "world:" + client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize()
            : "server:" + (client.getCurrentServer() == null ? "local" : client.getCurrentServer().ip);
         select(world + "|" + client.player.getUUID());
         ready = false;
      }
   }

   public static void select(String key) {
      MatConfig c = MatInventoryClient.config;
      if (!c.placementProfile.equals(key)) {
         if (c.placementProfile.isEmpty()) {
            c.placementProfiles.put(key, c.detachedItems);
         } else {
            c.placementProfiles.put(c.placementProfile, c.detachedItems);
         }

         c.detachedItems = c.placementProfiles.computeIfAbsent(key, k -> new HashMap<>());
         c.placementProfile = key;
         c.save();
      }
   }

   public static void awaitSnapshot() {
      ready = false;
   }

   public static void synced() {
      ready = true;
      prune();
   }

   public static void prune() {
      Minecraft client = Minecraft.getInstance();
      if (ready && client.player != null && !client.player.isDeadOrDying() && MatInventoryClient.config != null) {
         InventoryMenu menu = client.player.inventoryMenu;
         if (menu.getCarried().isEmpty() && client.player.containerMenu.getCarried().isEmpty()) {
            boolean changed = false;

            for (Slot slot : menu.slots) {
               if (slot.container instanceof Inventory || slot.container instanceof ExtraInventory) {
                  String key = MatLayout.storageKey(slot);
                  MatConfig.DetachedItem saved = MatInventoryClient.config.detachedItems.get(key);
                  if (saved != null && (!slot.hasItem() || !saved.item.equals(ItemPresentationData.id(slot.getItem())))) {
                     MatInventoryClient.config.detachedItems.remove(key);
                     changed = true;
                  }
               }
            }

            if (changed) {
               MatInventoryClient.config.save();
            }
         }
      }
   }

   private PlacementProfiles() {
   }
}
