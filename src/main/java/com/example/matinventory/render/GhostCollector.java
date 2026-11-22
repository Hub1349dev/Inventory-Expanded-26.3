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
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.world.item.ItemDisplayContext;

@Environment(EnvType.CLIENT)
final class GhostCollector extends SubmitNodeStorage {
   void clear() {
      this.drainPhases(phase -> phase.sortInto((node, ordered) -> {}));
   }

   private final AlphaBuffers buffers;
   boolean rendered;
   SubmitNodeStorage specials;

   @Override
   public SubmitNodeCollection order(int order) {
      this.rendered = true;
      return this.specials.order(order);
   }

   @Override
   public void submitCustomGeometry(PoseStack pose, RenderType type, CustomGeometryRenderer renderer) {
      this.rendered = true;
      this.specials.submitCustomGeometry(pose, type, renderer);
   }

   GhostCollector(SceneBuffers source) {
      this.buffers = new AlphaBuffers(source);
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
         this.buffers.alpha = 0.43F;
         ItemQuadRenderer.render(
            context, pose, this.buffers, light, overlay, tints, quads.all(), RenderTypes.entityTranslucent(quads.all().getFirst().materialInfo().sprite().atlasLocation()), FoilType.NONE
         );
      }
   }
}
