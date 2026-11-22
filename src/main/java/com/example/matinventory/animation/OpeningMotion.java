package com.example.matinventory.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class OpeningMotion {
   public static final float FALL_SECONDS = 0.09F;
   public static final float UNFOLD_SECONDS = 0.59090906F;
   public static final float CAMERA_SECONDS = 1.02F;

   public static float smooth(float t) {
      t = Math.clamp(t, 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }

   public static boolean active(float t) {
      return t >= 0.0F && t < 1.02F;
   }

   public static float fold(float t) {
      return Math.clamp((t - 0.09F) / 0.59090906F, 0.0F, 1.0F);
   }

   public static float fall(float t) {
      float a = 1.0F - smooth(t / 0.09F);
      return 1.35F * a * a;
   }

   public static float follow(float t) {
      return smooth(t / 0.68090904F);
   }

   public static float returnHome(float t) {
      return smooth((t - 0.56F) / 0.46F);
   }

   public static float hand(float t) {
      return Math.clamp((t - 0.045F) / 0.46F, 0.0F, 1.0F);
   }

   public static boolean handVisible(float t) {
      return t > 0.045F && t < 0.505F;
   }

   public static float shake(float t) {
      float impact = t - 0.09F;
      return !(impact < 0.0F) && !(impact > 0.4F) ? 0.18F * (float)Math.exp(-impact * 18.0F) * (float)Math.sin(impact * 85.0F) : 0.0F;
   }

   private OpeningMotion() {
   }
}
