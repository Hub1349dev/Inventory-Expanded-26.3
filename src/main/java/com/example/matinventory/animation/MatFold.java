package com.example.matinventory.animation;

import com.example.matinventory.arrangement.ItemPresentationData;
import com.example.matinventory.layout.MatLayout;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MatFold {
   public static final int PANELS = 3;
   public float x;
   public float y;
   public float z;
   public float nx;
   public float ny;
   public float nz;
   public float bend;
   private float progress = 1.0F;
   private float firstCut;
   private float secondCut;
   private float relief = 1.0F;
   private float fallHeight;
   private float bottom = -0.09F;
   private float top = 0.65F;
   private final float[] a = new float[]{1.0F, 1.0F, 1.0F};
   private final float[] b = new float[3];
   private final float[] tx = new float[3];
   private final float[] ty = new float[3];
   private final float[] angles = new float[3];

   private static float smooth(float t) {
      t = Math.clamp(t, 0.0F, 1.0F);
      return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
   }

   public static float crease(float left, float right, int index) {
      return left + (right - left) * (index + 1) / 3.0F;
   }

   public void set(MatLayout layout, float progress) {
      this.set(layout, progress, 0.0F);
   }

   public void set(MatLayout layout, float progress, float fallHeight) {
      this.progress = Math.clamp(progress, 0.0F, 1.0F);
      this.fallHeight = Math.max(0.0F, fallHeight);
      float left = Float.POSITIVE_INFINITY;
      float right = -left;

      for (ItemPresentationData.Compartment c : layout.solidCompartments()) {
         left = Math.min(left, c.x0());
         right = Math.max(right, c.x1());
      }

      if (!Float.isFinite(left)) {
         left = -14.0F;
         right = 18.0F;
      }

      this.firstCut = crease(left, right, 0);
      this.secondCut = crease(left, right, 1);
      this.relief = 0.25F + 0.75F * smooth((this.progress - 0.75F) / 0.25F);
      this.rebuild();
   }

   public void surfaceBounds(float bottom, float top) {
      if (this.bottom != bottom || this.top != top) {
         this.bottom = bottom;
         this.top = top;
         this.rebuild();
      }
   }

   private void rebuild() {
      if (this.active()) {
         float first = (float) Math.PI * (1.0F - smooth(this.progress / 0.6F));
         float second = (float) Math.PI * (1.0F - smooth((this.progress - 0.4F) / 0.6F));
         float low = this.bottom * this.relief;
         float high = this.top * this.relief;
         float secondHeight = high + 0.025F;
         float firstHeight = secondHeight + (high - low) * 0.5F + 0.035F;
         float ca = (float)Math.cos(first);
         float sa = (float)Math.sin(first);
         float cb = (float)Math.cos(second);
         float sb = (float)Math.sin(second);
         float ax = this.firstCut * (1.0F - ca) + firstHeight * sa;
         float ay = -this.firstCut * sa + firstHeight * (1.0F - ca);
         float bx = this.secondCut * (1.0F - cb) + secondHeight * sb;
         float by = -this.secondCut * sb + secondHeight * (1.0F - cb);
         this.a[0] = 1.0F;
         this.b[0] = this.tx[0] = this.ty[0] = this.angles[0] = 0.0F;
         this.a[1] = ca;
         this.b[1] = sa;
         this.tx[1] = ax;
         this.ty[1] = ay;
         this.angles[1] = first;
         this.a[2] = ca * cb - sa * sb;
         this.b[2] = sa * cb + ca * sb;
         this.tx[2] = ca * bx - sa * by + ax;
         this.ty[2] = sa * bx + ca * by + ay;
         this.angles[2] = first + second;
      } else {
         for (int i = 0; i < 3; i++) {
            this.a[i] = 1.0F;
            this.b[i] = this.tx[i] = this.ty[i] = this.angles[i] = 0.0F;
         }

         this.relief = 1.0F;
      }
   }

   public boolean active() {
      return this.progress < 1.0F;
   }

   public float relief() {
      return this.relief;
   }

   public int panel(float px) {
      return px <= this.firstCut ? 0 : (px <= this.secondCut ? 1 : 2);
   }

   public void map(float px, float py, float pz, float normalX, float normalY, float normalZ) {
      this.mapPanel(this.panel(px), px, py, pz, normalX, normalY, normalZ);
   }

   public void mapPanel(int panel, float px, float py, float pz, float normalX, float normalY, float normalZ) {
      float c = this.a[panel];
      float s = this.b[panel];
      this.x = c * px - s * py + this.tx[panel];
      this.y = s * px + c * py + this.ty[panel] + this.fallHeight;
      this.z = pz;
      this.nx = c * normalX - s * normalY;
      this.ny = s * normalX + c * normalY;
      this.nz = normalZ;
      this.bend = this.angles[panel];
   }
}
