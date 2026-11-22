package com.example.matinventory.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.component.BundleContents;

@Environment(EnvType.CLIENT)
public final class BundleClient {
   public static void select(AbstractContainerMenu menu, Slot slot, int index) {
      if (slot != null && slot.getItem().has(DataComponents.BUNDLE_CONTENTS)) {
         BundleItem.toggleSelectedItem(slot.getItem(), index);
         ClientPacketListener connection = Minecraft.getInstance().getConnection();
         if (connection != null) {
            connection.send(new ServerboundSelectBundleItemPacket(slot.index, index));
         }
      }
   }

   public static boolean scroll(AbstractContainerMenu menu, Slot slot, double delta) {
      if (slot != null && delta != 0.0) {
         BundleContents contents = slot.getItem().get(DataComponents.BUNDLE_CONTENTS);
         if (contents != null && !contents.isEmpty()) {
            select(menu, slot, Math.floorMod(contents.getSelectedItemIndex() + (delta > 0.0 ? -1 : 1), contents.size()));
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private BundleClient() {
   }
}
