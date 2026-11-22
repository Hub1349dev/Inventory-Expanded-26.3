package com.example.matinventory.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class GroundSampler {
   public static int support(GroundSampler.Columns c, int x, int y, int z) {
      for (int d = 0; d <= 16; d++) {
         if (c.solid(x, y - d, z)) {
            return y - d;
         }
      }

      return Integer.MIN_VALUE;
   }

   public static int surface(GroundSampler.Columns c, int x, int y, int z) {
      for (int a = y + 2; a >= y - 3; a--) {
         if (c.snow(x, a, z) || c.solid(x, a, z)) {
            return a;
         }
      }

      return Integer.MIN_VALUE;
   }

   public static int[] rotate(int x, int z, int turns) {
      return switch (Math.floorMod(turns, 4)) {
         case 1 -> new int[]{-z, x};
         case 2 -> new int[]{-x, -z};
         case 3 -> new int[]{z, -x};
         default -> new int[]{x, z};
      };
   }

   private GroundSampler() {
   }

   @Environment(EnvType.CLIENT)
   public interface Columns {
      boolean solid(int var1, int var2, int var3);

      boolean snow(int var1, int var2, int var3);
   }
}
