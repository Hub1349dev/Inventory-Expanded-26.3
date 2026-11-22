package com.example.matinventory.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public final class RenderedItemBounds {
   private final Matrix4f inverse = new Matrix4f();
   private final Vector3f min = new Vector3f();
   private final Vector3f max = new Vector3f();
   private final Vector3f origin = new Vector3f();
   private final Vector3f direction = new Vector3f();
   public final Vector3f center = new Vector3f();

   public void expandXZ(float[] bounds, Matrix4f transform, Vector3f corner) {
      this.inverse.invert(transform);

      for (int i = 0; i < 8; i++) {
         corner.set((i & 1) == 0 ? this.min.x : this.max.x, (i & 2) == 0 ? this.min.y : this.max.y, (i & 4) == 0 ? this.min.z : this.max.z)
            .mulPosition(transform);
         bounds[0] = Math.min(bounds[0], corner.x);
         bounds[1] = Math.min(bounds[1], corner.z);
         bounds[2] = Math.max(bounds[2], corner.x);
         bounds[3] = Math.max(bounds[3], corner.z);
      }
   }

   public void set(Matrix4fc transform, float x0, float y0, float z0, float x1, float y1, float z1) {
      this.inverse.set(transform).invert();
      this.min.set(x0, y0, z0);
      this.max.set(x1, y1, z1);
      this.center.set((x0 + x1) / 2.0F, (y0 + y1) / 2.0F, (z0 + z1) / 2.0F).mulPosition(transform);
   }

   public float intersect(Vector3fc worldOrigin, Vector3fc worldDirection) {
      this.inverse.transformPosition(worldOrigin, this.origin);
      this.inverse.transformDirection(worldDirection, this.direction);
      float near = 0.0F;
      float far = Float.POSITIVE_INFINITY;

      for (int axis = 0; axis < 3; axis++) {
         float o = this.origin.get(axis);
         float d = this.direction.get(axis);
         float lo = this.min.get(axis);
         float hi = this.max.get(axis);
         if (Math.abs(d) < 1.0E-7F) {
            if (o < lo || o > hi) {
               return Float.POSITIVE_INFINITY;
            }
         } else {
            float a = (lo - o) / d;
            float b = (hi - o) / d;
            near = Math.max(near, Math.min(a, b));
            far = Math.min(far, Math.max(a, b));
            if (near > far) {
               return Float.POSITIVE_INFINITY;
            }
         }
      }

      return near;
   }
}
