package com.example.matinventory.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class DecorativeBlockPile {
   public static int copies(int count) {
      return count <= 10 ? 1 : (count <= 32 ? 3 : 4);
   }

   public static float x(int copy, int count, float width) {
      return count == 1 ? 0.0F : ((copy & 1) == 0 ? -0.54F : 0.54F) * width;
   }

   public static float z(int copy, int count, float depth) {
      return count == 1 ? 0.0F : (count == 3 ? (copy == 2 ? -0.54F : 0.54F) * depth : (copy < 2 ? 0.54F : -0.54F) * depth);
   }

   private DecorativeBlockPile() {
   }
}
