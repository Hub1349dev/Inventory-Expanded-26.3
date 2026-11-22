package com.example.matinventory.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class StorageOrder {
   private final List<String> keys = new ArrayList<>();
   private Set<String> present = Set.of();

   public StorageOrder(int size, Collection<String> saved) {
      if (saved != null) {
         for (String key : saved) {
            if (key != null && !key.isBlank() && !this.keys.contains(key) && this.keys.size() < size) {
               this.keys.add(key);
            }
         }
      }

      while (this.keys.size() < size) {
         this.keys.add("empty_storage_" + this.keys.size());
      }
   }

   public List<String> keys() {
      return List.copyOf(this.keys);
   }

   public void swap(int a, int b) {
      Collections.swap(this.keys, a, b);
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

   public void ensure(Collection<String> current, List<Integer> preference) {
      this.present = Set.copyOf(current);

      for (String key : current) {
         if (!this.keys.contains(key)) {
            for (int i : preference) {
               if (!this.present.contains(this.keys.get(i))) {
                  this.keys.set(i, key);
                  break;
               }
            }
         }
      }
   }

   public void place(String key, int target) {
      int source = this.cell(key);
      if (source >= 0) {
         Collections.swap(this.keys, source, target);
      } else {
         if (this.present.contains(this.keys.get(target))) {
            for (int i = 0; i < this.keys.size(); i++) {
               if (i != target && !this.present.contains(this.keys.get(i))) {
                  this.keys.set(i, this.keys.get(target));
                  break;
               }
            }
         }

         this.keys.set(target, key);
      }
   }
}
