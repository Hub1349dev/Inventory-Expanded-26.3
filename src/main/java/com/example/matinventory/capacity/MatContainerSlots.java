package com.example.matinventory.capacity;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class MatContainerSlots {
   public static Slot storage(ExtraInventory extra, int index) {
      return new Slot(extra, index, -10000, -10000) {
         @Override
         public boolean mayPlace(ItemStack stack) {
            return extra.canPlaceItem(this.getContainerSlot(), stack);
         }

         @Override
         public boolean isActive() {
            return ExtraInventory.enabled(extra.owner)
               && (this.getContainerSlot() < extra.activeSlots() || extra.equipmentReserve(this.getContainerSlot()) || this.hasItem());
         }
      };
   }

   private MatContainerSlots() {
   }
}
