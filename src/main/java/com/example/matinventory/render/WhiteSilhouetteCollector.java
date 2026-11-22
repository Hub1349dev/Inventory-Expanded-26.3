package com.example.matinventory.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;

@Environment(EnvType.CLIENT)
final class WhiteSilhouetteCollector extends SubmitNodeStorage {
   void clear() {
      this.drainPhases(phase -> phase.sortInto((node, ordered) -> {}));
   }

   private static final int[] WHITE = new int[0];
   private final AlphaBuffers buffers;
   float opacity = 1.0F;
   Identifier texture;

   WhiteSilhouetteCollector(SceneBuffers buffers) {
      this.buffers = new AlphaBuffers(buffers);
      this.buffers.white = true;
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
      if (sprite instanceof TextureAtlasSprite || this.texture != null) {
         this.buffers.alpha = this.opacity;
         model.setupAnim(state);
         VertexConsumer consumer = this.buffers.getBuffer(MatRenderTypes.silhouette(sprite instanceof TextureAtlasSprite atlas ? atlas.atlasLocation() : this.texture));
         model.renderToBuffer(pose, sprite == null ? consumer : sprite.wrap(consumer), light, overlay, -1);
      }
   }

   @Override
   public void submitItem(
      PoseStack pose, ItemDisplayContext context, int light, int overlay, int outline, int[] tints, ItemQuads quads, FoilType foil
   ) {
      if (!quads.isEmpty()) {
         this.buffers.alpha = this.opacity;
         ItemQuadRenderer.render(
            context, pose, this.buffers, light, overlay, WHITE, quads.all(), MatRenderTypes.silhouette(quads.all().getFirst().materialInfo().sprite().atlasLocation()), FoilType.NONE
         );
      }
   }
}
