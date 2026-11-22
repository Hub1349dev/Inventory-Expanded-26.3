package com.example.matinventory.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.world.item.ItemDisplayContext;

@Environment(EnvType.CLIENT)
final class LitItemCollector extends SubmitNodeStorage {
   void clear() {
      this.drainPhases(phase -> phase.sortInto((node, ordered) -> {}));
   }

   private final AlphaBuffers buffers;
   private final AlphaBuffers customBuffers;
   boolean rendered;
   SubmitNodeStorage specials;

   LitItemCollector(SceneBuffers source) {
      this.buffers = new AlphaBuffers(source);
      this.customBuffers = new AlphaBuffers(source);
   }

   void tint(int rgb) {
      this.buffers.red = (rgb >> 16 & 0xFF) / 255.0F;
      this.buffers.green = (rgb >> 8 & 0xFF) / 255.0F;
      this.buffers.blue = (rgb & 0xFF) / 255.0F;
   }

   void shade(float amount) {
      this.buffers.red *= amount;
      this.buffers.green *= amount;
      this.buffers.blue *= amount;
   }

   @Override
   public SubmitNodeCollection order(int order) {
      this.rendered = true;
      return this.specials.order(order);
   }

   @Override
   public void submitCustomGeometry(PoseStack pose, RenderType type, CustomGeometryRenderer renderer) {
      this.rendered = true;
      renderer.render(pose.last(), this.customBuffers.getBuffer(type));
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
      this.rendered = true;
      this.specials.submitModel(model, state, pose, type, light, overlay, color, sprite, outline);
   }

   @Override
   public void submitItem(
      PoseStack pose, ItemDisplayContext context, int light, int overlay, int outline, int[] tints, ItemQuads quads, FoilType foil
   ) {
      if (!quads.isEmpty()) {
         this.rendered = true;
         ItemQuadRenderer.render(context, pose, this.buffers, light, overlay, tints, quads.all(), null, foil);
      }
   }
}
