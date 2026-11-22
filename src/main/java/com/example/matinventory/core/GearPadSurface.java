package com.example.matinventory.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class GearPadSurface {
   public static final float TOP = 0.074F;

   public static float height(float u, float v) {
      return 0.074F;
   }

   private GearPadSurface() {
   }
}
