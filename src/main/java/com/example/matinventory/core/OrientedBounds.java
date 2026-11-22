package com.example.matinventory.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record OrientedBounds(float x, float z, float halfWidth, float halfDepth, float yaw) {
   public boolean overlaps(OrientedBounds b) {
      double a = Math.toRadians(this.yaw);
      double q = Math.toRadians(b.yaw);
      double ac = Math.cos(a);
      double as = Math.sin(a);
      double bc = Math.cos(q);
      double bs = Math.sin(q);
      return this.axis(b, ac, -as, ac, as, bc, bs)
         && this.axis(b, as, ac, ac, as, bc, bs)
         && this.axis(b, bc, -bs, ac, as, bc, bs)
         && this.axis(b, bs, bc, ac, as, bc, bs);
   }

   private boolean axis(OrientedBounds b, double nx, double nz, double ac, double as, double bc, double bs) {
      double ra = this.halfWidth * Math.abs(nx * ac - nz * as) + this.halfDepth * Math.abs(nx * as + nz * ac);
      double rb = b.halfWidth * Math.abs(nx * bc - nz * bs) + b.halfDepth * Math.abs(nx * bs + nz * bc);
      return Math.abs((this.x - b.x) * nx + (this.z - b.z) * nz) < ra + rb - 1.0E-4;
   }
}
