package com.example.matinventory.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ModToolSize {
   public static float length(float width, float height, float depth, float multiplier) {
      float raw = Math.max(width, Math.max(height, depth));
      if (!Float.isFinite(raw) || raw <= 0.0F) {
         raw = 0.5F;
      }

      float converted = (float)Math.hypot(raw * 0.75F, 0.95F);
      return (converted <= 3.9F ? converted : 3.9F + 1.3F * (float)Math.tanh((converted - 3.9F) / 1.3F)) * multiplier;
   }

   private ModToolSize() {
   }
}
