package com.example.matinventory.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ItemSizeResolver {
   public static float resolve(float cm, String category, String subtype, float gamma, float minimum, float multiplier) {
      float result = 0.055F * (float)Math.pow(Math.max(2.0F, cm) / 8.0F, gamma);
      if (category.equals("LONG_TOOLS")) {
         result = Math.clamp(result * 1.3F, 0.12F, cm >= 38.0F ? 0.19F : 0.15F);
      }

      if (category.equals("EQUIPMENT")) {
         result = Math.clamp(result * 1.3F, 0.12F, 0.15F);
      }

      if (category.equals("BLOCKS")) {
         result = 0.065F;
      }

      if (category.equals("FOOD")) {
         result = Math.clamp(result, subtype.equals("potion") ? 0.062F : 0.055F, subtype.equals("potion") ? 0.065F : 0.055F);
      }

      if (subtype.equals("metal") || subtype.equals("gem")) {
         result = Math.max(result, 0.05F);
      }

      if (category.equals("OTHER")) {
         result = Math.clamp(result, 0.055F, cm <= 18.0F ? 0.07F : 0.19F);
      }

      return Math.max(minimum, result * multiplier);
   }

   private ItemSizeResolver() {
   }
}
