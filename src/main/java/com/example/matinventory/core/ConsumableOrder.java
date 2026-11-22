package com.example.matinventory.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ConsumableOrder {
   private final List<String> order = new ArrayList<>();

   public ConsumableOrder(Collection<String> saved) {
      if (saved != null) {
         for (String key : saved) {
            if (key != null && !key.isBlank() && !this.order.contains(key) && this.order.size() < 14) {
               this.order.add(key);
            }
         }
      }

      while (this.order.size() < 14) {
         this.order.add("empty_food_" + this.order.size());
      }
   }

   public List<String> keys() {
      return List.copyOf(this.order);
   }

   public String key(int cell) {
      return this.order.get(cell);
   }

   public int cell(String key) {
      return this.order.indexOf(key);
   }

   public void migrate(String oldKey, String newKey) {
      int at = this.cell(oldKey);
      if (at >= 0 && !this.order.contains(newKey)) {
         this.order.set(at, newKey);
      }
   }

   public void ensure(Collection<String> present) {
      for (String key : present) {
         if (!this.order.contains(key)) {
            for (int i = 0; i < this.order.size(); i++) {
               if (!present.contains(this.order.get(i))) {
                  this.order.set(i, key);
                  break;
               }
            }
         }
      }
   }

   public void swap(int a, int b) {
      Collections.swap(this.order, a, b);
   }

   public void place(String key, int target) {
      int current = this.cell(key);
      if (current >= 0) {
         this.swap(current, target);
      } else {
         this.order.set(target, key);
      }
   }
}
