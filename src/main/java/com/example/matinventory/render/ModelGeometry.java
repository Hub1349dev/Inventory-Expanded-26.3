package com.example.matinventory.render;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class ModelGeometry extends SubmitNodeCollection implements SubmitNodeCollector, VertexConsumer {
   private double x0;
   private double y0;
   private double z0;
   private double x1;
   private double y1;
   private double z1;
   public boolean special;
   private final Vector3f point = new Vector3f();

   public ModelGeometry() {
      super(false, new TranslucentFeatureRenderPhase());
   }

   public AABB measure(ItemStackRenderState model) {
      this.x0 = this.y0 = this.z0 = Double.POSITIVE_INFINITY;
      this.x1 = this.y1 = this.z1 = Double.NEGATIVE_INFINITY;
      this.special = false;

      try {
         model.submit(new PoseStack(), this, 15728880, 0, 0);
      } finally {
         this.allPhases().forEach(phase -> phase.sortInto((node, ordered) -> {}));
      }

      return this.x0 <= this.x1 ? sanitize(new AABB(this.x0, this.y0, this.z0, this.x1, this.y1, this.z1)) : null;
   }

   public static boolean valid(AABB b) {
      return Double.isFinite(b.minX)
         && Double.isFinite(b.minY)
         && Double.isFinite(b.minZ)
         && Double.isFinite(b.maxX)
         && Double.isFinite(b.maxY)
         && Double.isFinite(b.maxZ)
         && Math.max(b.getXsize(), Math.max(b.getYsize(), b.getZsize())) > 1.0E-5;
   }

   public static AABB sanitize(AABB b) {
      if (b != null && valid(b)) {
         double px = Math.max(0.0, 0.001 - b.getXsize()) * 0.5;
         double py = Math.max(0.0, 0.001 - b.getYsize()) * 0.5;
         double pz = Math.max(0.0, 0.001 - b.getZsize()) * 0.5;
         return b.inflate(px, py, pz);
      } else {
         return new AABB(-0.25, -0.25, -0.25, 0.25, 0.25, 0.25);
      }
   }

   @Override
   public OrderedSubmitNodeCollector order(int order) {
      return this;
   }

   @Override
   public void submitItem(
      PoseStack pose, ItemDisplayContext context, int light, int overlay, int outline, int[] tint, ItemQuads quads, FoilType foil
   ) {
      for (BakedQuad q : quads.all()) {
         for (int i = 0; i < 4; i++) {
            this.point.set(q.position(i)).mulPosition(pose.last().pose());
            this.addVertex(this.point.x, this.point.y, this.point.z);
         }
      }
   }

   @Override
   public <S> void submitModel(
      Model<? super S> model,
      S state,
      PoseStack pose,
      RenderType type,
      int light,
      int overlay,
      int color,
      UvMapping sprite,
      int outline
   ) {
      this.special = true;
      model.setupAnim(state);
      model.renderToBuffer(pose, this, light, overlay, color);
   }

   @Override
   public void submitCustomGeometry(PoseStack pose, RenderType type, CustomGeometryRenderer renderer) {
      this.special = true;
      renderer.render(pose.last(), this);
   }

   @Override
   public VertexConsumer addVertex(float x, float y, float z) {
      if (Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z)) {
         this.x0 = Math.min(this.x0, x);
         this.y0 = Math.min(this.y0, y);
         this.z0 = Math.min(this.z0, z);
         this.x1 = Math.max(this.x1, x);
         this.y1 = Math.max(this.y1, y);
         this.z1 = Math.max(this.z1, z);
      }

      return this;
   }

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
      return this;
   }

   @Override
   public VertexConsumer setColor(int r, int g, int b, int a) {
      return this;
   }

   @Override
   public VertexConsumer setColor(int color) {
      return this;
   }

   @Override
   public VertexConsumer setUv(float u, float v) {
      return this;
   }

   @Override
   public VertexConsumer setUv1(int u, int v) {
      return this;
   }

   @Override
   public VertexConsumer setUv2(int u, int v) {
      return this;
   }

   @Override
   public VertexConsumer setNormal(float x, float y, float z) {
      return this;
   }

   @Override
   public VertexConsumer setUv3(float u, float v) {
      return this;
   }

   @Override
   public VertexConsumer setLineWidth(float width) {
      return this;
   }
}
