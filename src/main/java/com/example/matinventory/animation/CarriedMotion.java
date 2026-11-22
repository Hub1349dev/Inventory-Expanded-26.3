package com.example.matinventory.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class CarriedMotion {
   public float x;
   public float y;
   public float z;
   public float yaw;
   public float pitch;
   public float roll;
   public float scale = 1.0F;
   private float lastX;
   private float lastZ;
   private float swayPitch;
   private float swayRoll;
   private boolean active;
   private boolean tracking;

   public boolean active() {
      return this.active;
   }

   public void reset() {
      this.active = this.tracking = false;
      this.swayPitch = this.swayRoll = 0.0F;
   }

   public void begin(float x, float y, float z, float yaw, float pitch, float roll, float scale) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.yaw = yaw;
      this.pitch = pitch;
      this.roll = roll;
      this.scale = scale;
      this.active = true;
      this.tracking = false;
      this.swayPitch = this.swayRoll = 0.0F;
   }

   public void follow(float tx, float ty, float tz, float yaw, float pitch, float roll, float scale, float dt) {
      if (!this.active) {
         this.begin(tx, ty, tz, yaw, pitch, roll, scale);
      }

      dt = Math.clamp(dt, 0.0F, 0.05F);
      float a = 1.0F - (float)Math.exp(-dt * 24.0F);
      float b = 1.0F - (float)Math.exp(-dt * 16.0F);
      float vx = this.tracking ? (tx - this.lastX) / Math.max(0.001F, dt) : 0.0F;
      float vz = this.tracking ? (tz - this.lastZ) / Math.max(0.001F, dt) : 0.0F;
      this.swayPitch = this.swayPitch + (Math.clamp(vz * 0.13F, -1.5F, 1.5F) - this.swayPitch) * b;
      this.swayRoll = this.swayRoll + (Math.clamp(-vx * 0.17F, -2.0F, 2.0F) - this.swayRoll) * b;
      this.lastX = tx;
      this.lastZ = tz;
      this.tracking = true;
      this.x = this.x + (tx - this.x) * a;
      this.y = this.y + (ty - this.y) * a;
      this.z = this.z + (tz - this.z) * a;
      this.scale = this.scale + (scale - this.scale) * a;
      this.yaw = this.yaw + wrap(yaw - this.yaw) * a;
      this.pitch = this.pitch + (pitch + this.swayPitch - this.pitch) * a;
      this.roll = this.roll + (roll + this.swayRoll - this.roll) * a;
   }

   private static float wrap(float a) {
      return (a + 540.0F) % 360.0F - 180.0F;
   }
}
