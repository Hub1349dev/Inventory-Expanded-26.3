package com.example.matinventory.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ToolOrder {
   public static final List<String> DEFAULT = List.of("axe", "pickaxe", "shovel", "sword", "hoe", "crossbow", "fishing_rod", "mace", "spear", "bow", "trident");
   private final List<String> order = new ArrayList<>();

   public ToolOrder(Collection<String> saved) {
      if (saved != null) {
         for (String key : saved) {
            if (key != null && !key.isBlank() && !key.startsWith("empty_cell_")) {
               if (key.startsWith("mod:") && key.contains("/")) {
                  key = key.substring(0, key.indexOf(47));
               }
               key = switch (key) {
                  case "brush", "stick", "carrot_on_a_stick", "warped_fungus_on_a_stick" -> "fishing_rod";
                  case "shears" -> "spear";
                  default -> key;
               };
               if (!this.order.contains(key)) {
                  this.order.add(key);
               }
            }
         }
      }

      for (String key : DEFAULT) {
         if (!this.order.contains(key)) {
            this.order.add(key);
         }
      }
   }

   public int cell(String key) {
      int i = this.order.indexOf(key);
      if (i < 0) {
         i = this.order.size();
         this.order.add(key);
      }

      return i;
   }

   public void ensureCell(int index) {
      while (this.order.size() <= index) {
         this.order.add("empty_cell_" + this.order.size());
      }
   }

   public String key(int cell) {
      return this.order.get(cell);
   }

   public List<String> keys() {
      return List.copyOf(this.order);
   }

   public void swap(int a, int b) {
      Collections.swap(this.order, a, b);
   }
}
