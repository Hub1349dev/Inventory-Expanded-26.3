package com.example.matinventory.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ConsumablePile {
   public static final float SCALE = 0.74F;

   public static int copies(int count) {
      return count <= 10 ? 1 : (count <= 32 ? 3 : 6);
   }

   public static float halfWidth(float width, float depth) {
      return width * 0.66F + depth * 0.055F;
   }

   public static float halfDepth(float width, float depth) {
      return depth * 0.56F + width * 0.05F;
   }

   public static float x(int copy, int copies, float width, int seed) {
      if (copies == 1) {
         return 0.0F;
      }

      float var10000;
      if (copies == 3) {
         var10000 = copy == 0 ? -0.1F : (copy == 1 ? 0.1F : 0.015F);
      } else {
         switch (copy) {
            case 0:
               var10000 = -0.15F;
               break;
            case 1:
               var10000 = 0.14F;
               break;
            case 2:
               var10000 = -0.1F;
               break;
            case 3:
               var10000 = 0.12F;
               break;
            case 4:
               var10000 = -0.05F;
               break;
            default:
               var10000 = 0.04F;
         }
      }

      float x = var10000;
      return ((seed & 1) == 0 ? x : -x) * width;
   }

   public static float z(int copy, int copies, float depth, int seed) {
      if (copies == 1) {
         return 0.0F;
      }

      float var10000;
      if (copies == 3) {
         var10000 = copy == 0 ? -0.02F : (copy == 1 ? 0.025F : 0.0F);
      } else {
         switch (copy) {
            case 0:
               var10000 = -0.04F;
               break;
            case 1:
               var10000 = -0.025F;
               break;
            case 2:
               var10000 = 0.04F;
               break;
            case 3:
               var10000 = 0.06F;
               break;
            case 4:
               var10000 = -0.02F;
               break;
            default:
               var10000 = 0.025F;
         }
      }

      float z = var10000;
      return ((seed & 1) == 0 ? z : -z) * depth;
   }

   public static float yaw(int copy, int copies, int seed) {
      if (copies == 1) {
         return 0.0F;
      }

      float a = switch (copy) {
         case 0 -> -4.0F;
         case 1 -> 3.0F;
         case 2 -> -2.0F;
         case 3 -> 5.0F;
         case 4 -> 1.0F;
         default -> -3.0F;
      };
      return (seed & 1) == 0 ? a : -a;
   }

   public static float y(int copy, float height) {
      return copy * Math.max(0.025F, height + 0.008F);
   }

   private ConsumablePile() {
   }
}
