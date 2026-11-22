package com.example.matinventory.render;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.IdentityHashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
final class AlphaBuffers implements SceneBuffers {
   private final SceneBuffers source;
   private final Map<RenderType, AlphaBuffers.Tinted> consumers = new IdentityHashMap<>();
   float alpha = 1.0F;
   float red = 1.0F;
   float green = 1.0F;
   float blue = 1.0F;
   boolean white;
   Matrix4f shadowProjection;

   AlphaBuffers(SceneBuffers source) {
      this.source = source;
   }

   @Override
   public VertexConsumer getBuffer(RenderType type) {
      AlphaBuffers.Tinted c = this.consumers.computeIfAbsent(type, k -> new AlphaBuffers.Tinted());
      c.target = this.source.getBuffer(type);
      return c;
   }

   @Environment(EnvType.CLIENT)
   private final class Tinted implements VertexConsumer {
      VertexConsumer target;
      private final Vector3f point = new Vector3f();
      private final Vector3f normal = new Vector3f();

      @Override
      public VertexConsumer addVertex(Pose pose, float x, float y, float z) {
         pose.pose().transformPosition(x, y, z, this.point);
         return this.addVertex(this.point.x, this.point.y, this.point.z);
      }

      @Override
      public VertexConsumer addVertex(Matrix4fc matrix, float x, float y, float z) {
         matrix.transformPosition(x, y, z, this.point);
         return this.addVertex(this.point.x, this.point.y, this.point.z);
      }

      @Override
      public VertexConsumer setNormal(Pose pose, float x, float y, float z) {
         pose.transformNormal(x, y, z, this.normal);
         this.target.setNormal(this.normal.x, this.normal.y, this.normal.z);
         return this;
      }

      @Override
      public VertexConsumer addVertex(float x, float y, float z) {
         if (AlphaBuffers.this.shadowProjection != null) {
            AlphaBuffers.this.shadowProjection.transformPosition(x, y, z, this.point);
            this.target.addVertex(this.point.x, this.point.y, this.point.z);
         } else {
            this.target.addVertex(x, y, z);
         }

         return this;
      }

      @Override
      public VertexConsumer setColor(int r, int g, int b, int a) {
         this.target
            .setColor(
               AlphaBuffers.this.white ? 255 : (int)(r * AlphaBuffers.this.red),
               AlphaBuffers.this.white ? 255 : (int)(g * AlphaBuffers.this.green),
               AlphaBuffers.this.white ? 255 : (int)(b * AlphaBuffers.this.blue),
               (int)(a * AlphaBuffers.this.alpha)
            );
         return this;
      }

      @Override
      public VertexConsumer setColor(int c) {
         return this.setColor(c >> 16 & 0xFF, c >> 8 & 0xFF, c & 0xFF, c >>> 24);
      }

      @Override
      public VertexConsumer setUv(float u, float v) {
         this.target.setUv(u, v);
         return this;
      }

      @Override
      public VertexConsumer setUv1(int u, int v) {
         this.target.setUv1(u, v);
         return this;
      }

      @Override
      public VertexConsumer setUv2(int u, int v) {
         this.target.setUv2(u, v);
         return this;
      }

      @Override
      public VertexConsumer setUv3(float u, float v) {
         this.target.setUv3(u, v);
         return this;
      }

      @Override
      public VertexConsumer setNormal(float x, float y, float z) {
         this.target.setNormal(x, y, z);
         return this;
      }

      @Override
      public VertexConsumer setLineWidth(float w) {
         this.target.setLineWidth(w);
         return this;
      }
   }
}
