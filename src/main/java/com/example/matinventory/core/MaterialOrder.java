package com.example.matinventory.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MaterialOrder {
   private final List<String> keys = new ArrayList<>();

   public MaterialOrder(Collection<String> saved) {
      if (saved != null) {
         for (String key : saved) {
            if (key != null && !key.isBlank() && !this.keys.contains(key) && this.keys.size() < 25) {
               this.keys.add(key);
            }
         }
      }

      while (this.keys.size() < 25) {
         this.keys.add("empty_material_" + this.keys.size());
      }
   }

   public List<String> keys() {
      return List.copyOf(this.keys);
   }

   public String key(int cell) {
      return this.keys.get(cell);
   }

   public int cell(String key) {
      return this.keys.indexOf(key);
   }

   public void migrate(String oldKey, String newKey) {
      int at = this.cell(oldKey);
      if (at >= 0 && !this.keys.contains(newKey)) {
         this.keys.set(at, newKey);
      }
   }

   public void ensure(Collection<String> present, Set<String> resources) {
      for (String key : present) {
         if (!this.keys.contains(key)) {
            int first = resources.contains(key) ? 0 : 15;
            int last = resources.contains(key) ? 15 : this.keys.size();
            int target = -1;

            for (int i = first; i < last; i++) {
               if (!present.contains(this.keys.get(i))) {
                  target = i;
                  break;
               }
            }

            if (target < 0) {
               for (int i = 0; i < this.keys.size(); i++) {
                  if (!present.contains(this.keys.get(i))) {
                     target = i;
                     break;
                  }
               }
            }

            if (target >= 0) {
               this.keys.set(target, key);
            }
         }
      }
   }

   public void swap(int a, int b) {
      Collections.swap(this.keys, a, b);
   }

   public void place(String key, int target) {
      int from = this.cell(key);
      if (from >= 0) {
         this.swap(from, target);
      } else {
         this.keys.set(target, key);
      }
   }
}
