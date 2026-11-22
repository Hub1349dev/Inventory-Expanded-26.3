package com.example.matinventory.capacity;

import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MinecartItem;

public final class VehicleKinds {
   public static boolean isVehicle(ItemStack stack) {
      return !stack.isEmpty() && (stack.getItem() instanceof BoatItem || stack.getItem() instanceof MinecartItem);
   }

   private VehicleKinds() {
   }
}
