package com.example.matinventory.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class CraftCameraMotion {
   private float first;
   private float second;
   private float value;

   public float advance(float target, float dt) {
      dt = Math.clamp(dt, 0.0F, 0.05F);
      float t = 22.0F * dt;
      float e = (float)Math.exp(-t);
      float a = this.first - target;
      float b = this.second - target;
      float c = this.value - target;
      this.value = target + (c + b * t + a * t * t * 0.5F) * e;
      this.second = target + (b + a * t) * e;
      this.first = target + a * e;
      if (Math.abs(this.first - target) + Math.abs(this.second - target) + Math.abs(this.value - target) < 1.0E-5F) {
         this.first = this.second = this.value = target;
      }

      return this.value;
   }
}
