package com.example.matinventory.core;

import com.example.matinventory.capacity.EquipmentKinds;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class EquipmentOrder {
   private final List<String> keys = new ArrayList<>();

   public EquipmentOrder(Collection<String> saved) {
      if (saved != null) {
         for (String old : saved) {
            String key = "elytra".equals(old) ? "animal_armor" : old;
            if (EquipmentKinds.DEFAULT.contains(key) && !this.keys.contains(key)) {
               this.keys.add(key);
            }
         }
      }

      for (String key : EquipmentKinds.DEFAULT) {
         if (!this.keys.contains(key)) {
            this.keys.add(key);
         }
      }
   }

   public int cell(String key) {
      return "shield".equals(key) ? 5 : ("elytra".equals(key) ? 6 : this.keys.indexOf(key));
   }

   public String key(int index) {
      return this.keys.get(index);
   }

   public List<String> keys() {
      return List.copyOf(this.keys);
   }

   public void swap(int a, int b) {
      if (a >= 0 && b >= 0 && a < 5 && b < 5) {
         Collections.swap(this.keys, a, b);
      }
   }
}
