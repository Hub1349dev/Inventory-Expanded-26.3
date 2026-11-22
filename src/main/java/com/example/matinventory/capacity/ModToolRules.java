package com.example.matinventory.capacity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public final class ModToolRules {
   public static boolean applies(ItemStack stack) {
      return !stack.isEmpty()
            && ModItemSemantics.external(stack)
            && !(stack.getItem() instanceof BlockItem)
            && !stack.has(DataComponents.FOOD)
            && !stack.has(DataComponents.POTION_CONTENTS)
            && EquipmentKinds.kind(stack) == null
            && !VehicleKinds.isVehicle(stack)
         ? ToolKinds.kind(stack) != null
         : false;
   }

   public static int stackLimit(ItemStack stack, int original) {
      return applies(stack) ? Math.min(2, original) : original;
   }

   private ModToolRules() {
   }
}
