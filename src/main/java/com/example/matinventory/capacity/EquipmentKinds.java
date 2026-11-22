package com.example.matinventory.capacity;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;

public final class EquipmentKinds {
   public static final List<String> DEFAULT = List.of("helmet", "chestplate", "leggings", "boots", "animal_armor");

   public static int playerSlot(ItemStack stack) {
      String key = kind(stack);
      byte var10000;
      if (key == null) {
         var10000 = -1;
      } else {
         switch (key) {
            case "helmet":
               var10000 = 39;
               break;
            case "chestplate":
            case "elytra":
               var10000 = 38;
               break;
            case "leggings":
               var10000 = 37;
               break;
            case "boots":
               var10000 = 36;
               break;
            default:
               var10000 = -1;
         }
      }

      return var10000;
   }

   public static boolean equipped(Slot slot) {
      return slot.container instanceof Inventory && !slot.getItem().isEmpty() && slot.getContainerSlot() == playerSlot(slot.getItem());
   }

   public static String kind(ItemStack stack) {
      if (stack.isEmpty()) {
         return null;
      }

      if (stack.is(Items.SHIELD)) {
         return "shield";
      }

      if (stack.is(Items.ELYTRA)) {
         return "elytra";
      }

      Equippable equip = stack.get(DataComponents.EQUIPPABLE);
      if (equip == null) {
         return null;
      }

      return switch (equip.slot()) {
         case HEAD -> "helmet";
         case LEGS -> "leggings";
         case FEET -> "boots";
         case CHEST -> "chestplate";
         case BODY, SADDLE -> "animal_armor";
         case OFFHAND -> "shield";
         default -> null;
      };
   }

   private EquipmentKinds() {
   }
}
