package com.example.matinventory.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class BlockPile {
   public static final float WIDTH = 2.16F;
   public static final float DEPTH = 2.16F;
   public static final float PRIMARY_SCALE = 0.57199997F;
   public static final float DUPLICATE_SCALE = 0.88F;

   public static int copies(int count) {
      return count <= 10 ? 1 : (count <= 32 ? 3 : 6);
   }

   public static float scale(int copy) {
      return copy == 0 ? 1.0F : 0.88F;
   }

   public static int variant(int seed) {
      return (seed ^ seed >>> 16) & 1;
   }

   public static float x(int copy, int copies, float width, int seed) {
      if (copies == 1) {
         return 0.0F;
      }

      float base;
      if (variant(seed) == 0) {
         float var10000;
         if (copies == 3) {
            var10000 = copy == 0 ? -0.52F : (copy == 1 ? 0.5F : -0.04F);
         } else {
            switch (copy) {
               case 0:
                  var10000 = -0.52F;
                  break;
               case 1:
                  var10000 = 0.51F;
                  break;
               case 2:
                  var10000 = -0.51F;
                  break;
               case 3:
                  var10000 = 0.52F;
                  break;
               case 4:
                  var10000 = -0.5F;
                  break;
               default:
                  var10000 = 0.52F;
            }
         }

         base = var10000;
      } else {
         float var5;
         if (copies == 3) {
            var5 = copy == 0 ? 0.52F : (copy == 1 ? -0.5F : 0.06F);
         } else {
            switch (copy) {
               case 0:
                  var5 = 0.51F;
                  break;
               case 1:
                  var5 = -0.52F;
                  break;
               case 2:
                  var5 = 0.5F;
                  break;
               case 3:
                  var5 = -0.53F;
                  break;
               case 4:
                  var5 = 0.5F;
                  break;
               default:
                  var5 = -0.52F;
            }
         }

         base = var5;
      }

      return (base + jitter(seed, copy, 1) * 0.01F) * width;
   }

   public static float z(int copy, int copies, float depth, int seed) {
      if (copies == 1) {
         return 0.0F;
      }

      float base;
      if (variant(seed) == 0) {
         float var10000;
         if (copies == 3) {
            var10000 = copy == 0 ? 0.0F : (copy == 1 ? 0.02F : 0.16F);
         } else {
            switch (copy) {
               case 0:
               case 1:
                  var10000 = 0.52F;
                  break;
               case 2:
               case 3:
                  var10000 = -0.52F;
                  break;
               case 4:
                  var10000 = 0.41F;
                  break;
               default:
                  var10000 = 0.4F;
            }
         }

         base = var10000;
      } else {
         float var5;
         if (copies == 3) {
            var5 = copy == 0 ? -0.04F : (copy == 1 ? 0.04F : -0.12F);
         } else {
            switch (copy) {
               case 0:
               case 1:
                  var5 = 0.51F;
                  break;
               case 2:
                  var5 = -0.53F;
                  break;
               case 3:
                  var5 = -0.51F;
                  break;
               case 4:
                  var5 = 0.1F;
                  break;
               default:
                  var5 = 0.44F;
            }
         }

         base = var5;
      }

      return (base + jitter(seed, copy, 2) * 0.01F) * depth;
   }

   public static float yaw(int copy, int seed) {
      return 90 * Math.floorMod(seed + copy * 31, 4) + (copy == 0 ? 2 : 3) * jitter(seed, copy, 3);
   }

   private static float jitter(int seed, int copy, int axis) {
      int n = seed ^ (copy + 1) * 73244475 ^ axis * 295559667;
      n ^= n >>> 16;
      n *= 73244475;
      n ^= n >>> 16;
      return Math.floorMod(n, 2001) / 1000.0F - 1.0F;
   }

   public static float y(int copy, int copies, float height) {
      if (copy == 0) {
         return 0.0F;
      } else {
         float actualHeight = Math.max(0.0F, height - 0.025F);
         if (copies == 3 ? copy == 2 : copy >= 4) {
            float support = copies != 3 && copy != 4 ? 0.88F : 1.0F;
            return actualHeight * (support - 0.5F + 0.44F) + 0.025F;
         } else {
            return actualHeight * -0.120000005F * 0.5F;
         }
      }
   }

   private BlockPile() {
   }
}
