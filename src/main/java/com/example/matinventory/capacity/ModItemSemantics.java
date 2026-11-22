package com.example.matinventory.capacity;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;

public final class ModItemSemantics {
   private static final ClassValue<ModItemSemantics.Role> ROLES = new ClassValue<ModItemSemantics.Role>() {
      protected ModItemSemantics.Role computeValue(Class<?> type) {
         for (Class<?> current = type; current != null && current != Item.class; current = current.getSuperclass()) {
            String name = current.getSimpleName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
            Set<String> words = Set.copyOf(Arrays.asList(name.split("_")));
            if (words.stream().anyMatch(Set.of("ammo", "ammunition", "bullet", "cartridge")::contains)) {
               return ModItemSemantics.Role.AMMUNITION;
            }

            if (words.stream().anyMatch(Set.of("attachment", "scope", "sight", "optic", "magazine", "suppressor")::contains)) {
               return ModItemSemantics.Role.ATTACHMENT;
            }

            if (words.stream().anyMatch(Set.of("gun", "rifle", "pistol", "firearm", "musket")::contains)) {
               return ModItemSemantics.Role.FIREARM;
            }
         }

         return ModItemSemantics.Role.UNKNOWN;
      }
   };

   public static ModItemSemantics.Role role(ItemStack stack) {
      return stack.isEmpty() ? ModItemSemantics.Role.UNKNOWN : ROLES.get(stack.getItem().getClass());
   }

   public static boolean external(ItemStack stack) {
      return !BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("minecraft");
   }

   public static String taggedTool(ItemStack stack) {
      String best = null;
      Iterator<TagKey<Item>> tags = stack.tags().iterator();

      while (tags.hasNext()) {
         String p = tags.next().location().getPath();
         if ((
               p.equals("tools")
                  || p.startsWith("tools/")
                  || p.equals("weapons")
                  || p.startsWith("weapons/")
                  || p.equals("guns")
                  || p.startsWith("guns/")
                  || p.equals("ranged_weapons")
                  || p.startsWith("ranged_weapons/")
                  || p.equals("bows")
                  || p.equals("crossbows")
            )
            && (best == null || p.length() > best.length() || p.length() == best.length() && p.compareTo(best) < 0)) {
            best = p;
         }
      }

      return best;
   }

   public static String inferredTool(ItemStack stack) {
      String tag = taggedTool(stack);
      if (tag != null) {
         return tag;
      }

      if (!external(stack)) {
         return null;
      }

      if (!(stack.getItem() instanceof BlockItem)
         && !stack.has(DataComponents.FOOD)
         && !stack.has(DataComponents.POTION_CONTENTS)
         && EquipmentKinds.kind(stack) == null) {
         if (role(stack) == ModItemSemantics.Role.FIREARM) {
            return "mod:firearm";
         } else if (stack.getItem() instanceof ProjectileWeaponItem) {
            return "mod:ranged_weapon";
         } else if (stack.getItem() instanceof FishingRodItem) {
            return "fishing_rod";
         } else if (stack.has(DataComponents.KINETIC_WEAPON) || stack.has(DataComponents.PIERCING_WEAPON) || stack.has(DataComponents.WEAPON)) {
            return "mod:weapon";
         } else {
            return stack.has(DataComponents.TOOL) ? "mod:tool" : null;
         }
      } else {
         return null;
      }
   }

   public static boolean resourceTag(String path) {
      return path.contains("ingot")
         || path.contains("nugget")
         || path.contains("gem")
         || path.contains("dust")
         || path.contains("seed")
         || path.equals("rods")
         || path.startsWith("rods/");
   }

   private ModItemSemantics() {
   }

   public enum Role {
      FIREARM,
      ATTACHMENT,
      AMMUNITION,
      UNKNOWN;
   }
}
