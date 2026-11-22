package com.example.matinventory.capacity;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ToolKinds {
   public static String kind(ItemStack stack) {
      if (stack.isEmpty() || BlockKinds.isTorch(stack)) {
         return null;
      } else if (VehicleKinds.isVehicle(stack)) {
         return "vehicle";
      } else if (stack.is(ItemTags.AXES)) {
         return "axe";
      } else if (stack.is(ItemTags.PICKAXES)) {
         return "pickaxe";
      } else if (stack.is(ItemTags.SHOVELS)) {
         return "shovel";
      } else if (stack.is(ItemTags.SWORDS)) {
         return "sword";
      } else if (stack.is(ItemTags.HOES)) {
         return "hoe";
      } else {
         Item item = stack.getItem();
         if (item == Items.SHIELD) {
            return "shield";
         } else if (item == Items.CROSSBOW) {
            return "crossbow";
         } else if (item == Items.BRUSH) {
            return "brush";
         } else if (item == Items.MACE) {
            return "mace";
         } else if (item == Items.SHEARS) {
            return "shears";
         } else if (item == Items.BOW) {
            return "bow";
         } else if (item == Items.FISHING_ROD || item == Items.CARROT_ON_A_STICK || item == Items.WARPED_FUNGUS_ON_A_STICK) {
            return "fishing_rod";
         } else if (item == Items.TRIDENT) {
            return "trident";
         } else {
            return stack.is(ItemTags.SPEARS) ? "spear" : ModItemSemantics.inferredTool(stack);
         }
      }
   }

   private ToolKinds() {
   }

   public static int limit(String kind) {
      return kind != null && kind.startsWith("mod:") ? Integer.MAX_VALUE : ("shield".equals(kind) ? 1 : ("vehicle".equals(kind) ? 3 : 3));
   }
}
