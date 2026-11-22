package com.example.matinventory.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MaterialPile {
   public static final int FLAT = 0;
   public static final int MINERAL = 1;
   public static final int RODS = 2;
   public static final float ITEM_SIZE = 1.16F;
   public static final float ROD_LENGTH = 1.32F;
   public static final float TINY_SIZE = 0.64F;

   public static int copies(int count) {
      return count <= 10 ? 1 : (count <= 32 ? 3 : 6);
   }

   public static float scale(int copy, int style) {
      return copy != 0 && style != 2 ? 0.9F : 1.0F;
   }

   public static float halfWidth(float w, float d, int style) {
      return style == 2 ? w * 0.5F + step(w) + d * 0.02F : (style == 1 ? w * 0.58F + d * 0.035F : w * 0.53F + d * 0.06F);
   }

   public static float halfDepth(float w, float d, int style) {
      return style == 2 ? d * 0.55F + w * 0.03F : (style == 1 ? d * 0.58F + w * 0.035F : d * 0.53F + w * 0.06F);
   }

   private static float step(float width) {
      return width + 0.065F;
   }

   public static float x(int copy, int count, float w, int seed, int style) {
      if (count == 1) {
         return 0.0F;
      }

      if (style == 2) {
         return (count == 3 ? copy - 1 : (copy < 3 ? copy - 1 : (copy == 3 ? -0.5F : (copy == 4 ? 0.5F : 0.0F)))) * step(w);
      }

      float var10000;
      if (style == 1) {
         if (count == 3) {
            var10000 = copy == 0 ? -0.125F : (copy == 1 ? 0.125F : 0.0F);
         } else {
            switch (copy) {
               case 0:
                  var10000 = -0.125F;
                  break;
               case 1:
                  var10000 = 0.125F;
                  break;
               case 2:
                  var10000 = 0.0F;
                  break;
               case 3:
                  var10000 = -0.06F;
                  break;
               case 4:
                  var10000 = 0.06F;
                  break;
               default:
                  var10000 = 0.015F;
            }
         }
      } else if (count == 3) {
         var10000 = copy == 0 ? -0.075F : (copy == 1 ? 0.075F : 0.0F);
      } else {
         switch (copy) {
            case 0:
               var10000 = -0.08F;
               break;
            case 1:
               var10000 = 0.08F;
               break;
            case 2:
               var10000 = -0.06F;
               break;
            case 3:
               var10000 = 0.05F;
               break;
            case 4:
               var10000 = -0.025F;
               break;
            default:
               var10000 = 0.015F;
         }
      }

      float x = var10000;
      return ((seed & 1) == 0 ? x : -x) * w;
   }

   public static float z(int copy, int count, float d, int seed, int style) {
      if (count == 1) {
         return 0.0F;
      }

      float var10000;
      if (style == 2) {
         switch (copy) {
            case 0:
               var10000 = -0.025F;
               break;
            case 1:
               var10000 = 0.03F;
               break;
            case 2:
               var10000 = -0.015F;
               break;
            case 3:
               var10000 = 0.025F;
               break;
            case 4:
               var10000 = -0.03F;
               break;
            default:
               var10000 = 0.01F;
         }
      } else if (style == 1) {
         switch (copy) {
            case 0:
               var10000 = 0.06F;
               break;
            case 1:
               var10000 = 0.05F;
               break;
            case 2:
               var10000 = -0.09F;
               break;
            case 3:
               var10000 = 0.035F;
               break;
            case 4:
               var10000 = 0.015F;
               break;
            default:
               var10000 = -0.01F;
         }
      } else {
         var10000 = copy % 2 == 0 ? -0.025F : 0.025F;
      }

      float z = var10000;
      return ((seed & 1) == 0 ? z : -z) * d;
   }

   public static float yaw(int copy, int count, int seed, int style) {
      if (count == 1) {
         return 0.0F;
      }

      float var10000;
      if (style == 2) {
         switch (copy) {
            case 0:
               var10000 = -1.5F;
               break;
            case 1:
               var10000 = 0.8F;
               break;
            case 2:
               var10000 = 1.5F;
               break;
            case 3:
               var10000 = -1.0F;
               break;
            case 4:
               var10000 = 0.5F;
               break;
            default:
               var10000 = -0.6F;
         }
      } else if (style == 1) {
         switch (copy) {
            case 0:
               var10000 = -3.0F;
               break;
            case 1:
               var10000 = 4.0F;
               break;
            case 2:
               var10000 = -4.0F;
               break;
            case 3:
               var10000 = 2.0F;
               break;
            case 4:
               var10000 = -2.0F;
               break;
            default:
               var10000 = 3.0F;
         }
      } else {
         switch (copy) {
            case 0:
               var10000 = -3.0F;
               break;
            case 1:
               var10000 = 7.0F;
               break;
            case 2:
               var10000 = -6.0F;
               break;
            case 3:
               var10000 = 4.0F;
               break;
            case 4:
               var10000 = -2.0F;
               break;
            default:
               var10000 = 3.0F;
         }
      }

      float angle = var10000;
      return (seed & 1) == 0 ? angle : -angle;
   }

   public static float y(int copy, int count, float height, int style) {
      float h = Math.max(0.0F, height - 0.025F);
      if (style != 2) {
         return copy == 0 ? 0.0F : h * (0.5F + 0.9F * (copy - 0.5F)) + 0.015F * copy;
      } else {
         return count != 3 && copy >= 3 ? (copy < 5 ? 1 : 2) * (h + 0.022F) : 0.0F;
      }
   }

   private MaterialPile() {
   }
}
