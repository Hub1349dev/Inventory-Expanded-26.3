package com.example.matinventory.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

@Environment(EnvType.CLIENT)
final class WorldBuffers implements SceneBuffers {
   private final Map<RenderType, Vertices> vertices = new LinkedHashMap<>();

   @Override
   public VertexConsumer getBuffer(RenderType type) {
      return this.vertices.computeIfAbsent(type, key -> new Vertices());
   }

   void submit(SubmitNodeCollector collector) {
      PoseStack pose = new PoseStack();
      this.vertices.forEach((type, recorded) -> collector.submitCustomGeometry(pose, type, (last, out) -> recorded.replay(out)));
      this.vertices.clear();
   }

   @Environment(EnvType.CLIENT)
   private static final class Vertices implements VertexConsumer {
      private static final int FLOATS = 10;
      private static final int INTS = 3;
      private float[] floats = new float[FLOATS * 256];
      private int[] ints = new int[INTS * 256];
      private int count;
      private int f;
      private int i;

      @Override
      public VertexConsumer addVertex(float x, float y, float z) {
         this.f = this.count * FLOATS;
         this.i = this.count * INTS;
         this.count++;
         if (this.f == this.floats.length) {
            this.floats = Arrays.copyOf(this.floats, this.f * 2);
            this.ints = Arrays.copyOf(this.ints, this.i * 2);
         }

         this.floats[this.f] = x;
         this.floats[this.f + 1] = y;
         this.floats[this.f + 2] = z;
         this.floats[this.f + 8] = 1.0F;
         this.ints[this.i] = -1;
         this.ints[this.i + 1] = OverlayTexture.NO_OVERLAY;
         return this;
      }

      @Override
      public VertexConsumer setColor(int r, int g, int b, int a) {
         return this.setColor(a << 24 | r << 16 | g << 8 | b);
      }

      @Override
      public VertexConsumer setColor(int color) {
         this.ints[this.i] = color;
         return this;
      }

      @Override
      public VertexConsumer setUv(float u, float v) {
         this.floats[this.f + 3] = u;
         this.floats[this.f + 4] = v;
         return this;
      }

      @Override
      public VertexConsumer setUv1(int u, int v) {
         this.ints[this.i + 1] = u & 65535 | v << 16;
         return this;
      }

      @Override
      public VertexConsumer setUv2(int u, int v) {
         this.ints[this.i + 2] = u & 65535 | v << 16;
         return this;
      }

      @Override
      public VertexConsumer setUv3(float u, float v) {
         this.floats[this.f + 5] = u;
         this.floats[this.f + 6] = v;
         return this;
      }

      @Override
      public VertexConsumer setNormal(float x, float y, float z) {
         this.floats[this.f + 7] = x;
         this.floats[this.f + 8] = y;
         this.floats[this.f + 9] = z;
         return this;
      }

      @Override
      public VertexConsumer setLineWidth(float width) {
         return this;
      }

      void replay(VertexConsumer out) {
         float[] v = this.floats;

         for (int n = 0; n < this.count; n++) {
            int a = n * FLOATS;
            int b = n * INTS;
            out.addVertex(v[a], v[a + 1], v[a + 2], this.ints[b], v[a + 3], v[a + 4], this.ints[b + 1], this.ints[b + 2], v[a + 7], v[a + 8], v[a + 9]);
            out.setUv3(v[a + 5], v[a + 6]);
         }
      }
   }
}
