package com.example.matinventory.core;

import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class FreePositionSearch {
   private float[] left = new float[32];
   private float[] right = new float[32];
   private float[] top = new float[32];
   private float[] bottom = new float[32];
   private int count;
   private float minX;
   private float minZ;
   private float maxX;
   private float maxZ;
   private float halfWidth;
   private float halfDepth;
   private float best;
   private float wantX;
   private float wantZ;
   public float x;
   public float z;

   public void reset(float minX, float minZ, float maxX, float maxZ, float halfWidth, float halfDepth) {
      this.minX = minX;
      this.minZ = minZ;
      this.maxX = maxX;
      this.maxZ = maxZ;
      this.halfWidth = halfWidth;
      this.halfDepth = halfDepth;
      this.count = 0;
   }

   public void add(float x, float z, float hw, float hd) {
      float padX = this.halfWidth + hw + 0.1001F;
      float padZ = this.halfDepth + hd + 0.1001F;
      float l = x - padX;
      float r = x + padX;
      float t = z - padZ;
      float b = z + padZ;
      if (!(r <= this.minX) && !(l >= this.maxX) && !(b <= this.minZ) && !(t >= this.maxZ)) {
         if (this.count == this.left.length) {
            int size = this.count * 2;
            this.left = Arrays.copyOf(this.left, size);
            this.right = Arrays.copyOf(this.right, size);
            this.top = Arrays.copyOf(this.top, size);
            this.bottom = Arrays.copyOf(this.bottom, size);
         }

         int i;
         for (i = this.count++; i > 0 && this.top[i - 1] > t; i--) {
            this.left[i] = this.left[i - 1];
            this.right[i] = this.right[i - 1];
            this.top[i] = this.top[i - 1];
            this.bottom[i] = this.bottom[i - 1];
         }

         this.left[i] = l;
         this.right[i] = r;
         this.top[i] = t;
         this.bottom[i] = b;
      }
   }

   public boolean freeAt(float x, float z) {
      if (!(x < this.minX) && !(x > this.maxX) && !(z < this.minZ) && !(z > this.maxZ) && Float.isFinite(x + z)) {
         for (int i = 0; i < this.count; i++) {
            if (x > this.left[i] && x < this.right[i] && z > this.top[i] && z < this.bottom[i]) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean find(float requestedX, float requestedZ) {
      if (!(this.minX > this.maxX) && !(this.minZ > this.maxZ) && Float.isFinite(requestedX + requestedZ)) {
         this.wantX = requestedX;
         this.wantZ = requestedZ;
         this.best = Float.POSITIVE_INFINITY;
         this.column(Math.clamp(this.wantX, this.minX, this.maxX));
         if (this.best == 0.0F) {
            return true;
         }

         this.column(this.minX);
         this.column(this.maxX);

         for (int i = 0; i < this.count; i++) {
            this.column(this.left[i]);
            this.column(this.right[i]);
         }

         return Float.isFinite(this.best);
      } else {
         return false;
      }
   }

   private void column(float px) {
      if (!(px < this.minX) && !(px > this.maxX) && !((px - this.wantX) * (px - this.wantX) > this.best)) {
         float pz = Math.clamp(this.wantZ, this.minZ, this.maxZ);
         if (this.freeAt(px, pz)) {
            this.consider(px, pz);
         } else {
            float start = Float.NaN;
            float end = Float.NaN;

            for (int i = 0; i < this.count; i++) {
               if (!(px <= this.left[i]) && !(px >= this.right[i])) {
                  if (Float.isNaN(start)) {
                     start = this.top[i];
                     end = this.bottom[i];
                  } else if (this.top[i] < end) {
                     end = Math.max(end, this.bottom[i]);
                  } else {
                     if (pz > start && pz < end) {
                        this.ends(px, start, end);
                        return;
                     }

                     start = this.top[i];
                     end = this.bottom[i];
                  }
               }
            }

            if (pz > start && pz < end) {
               this.ends(px, start, end);
            }
         }
      }
   }

   private void ends(float px, float start, float end) {
      if (start >= this.minZ && start <= this.maxZ) {
         this.consider(px, start);
      }

      if (end >= this.minZ && end <= this.maxZ) {
         this.consider(px, end);
      }
   }

   private void consider(float px, float pz) {
      float d = (px - this.wantX) * (px - this.wantX) + (pz - this.wantZ) * (pz - this.wantZ);
      if (d < this.best) {
         this.best = d;
         this.x = px;
         this.z = pz;
      }
   }
}
