package com.example.matinventory.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
final class ItemShadowCollector extends SubmitNodeStorage {
   void clear() {
      this.drainPhases(phase -> phase.sortInto((node, ordered) -> {}));
   }

   private final AlphaBuffers buffers;
   final Vector3f up = new Vector3f();
   private final Vector3f normal = new Vector3f();
   private final List<BakedQuad> facing = new ArrayList<>();
   float opacity;
   boolean rendered;
   boolean equipment;
   Identifier texture;

   void project(Matrix4f view, float support) {
      if (this.buffers.shadowProjection == null) {
         this.buffers.shadowProjection = new Matrix4f();
      }

      this.buffers.shadowProjection.set(view).translate(0.0F, support, 0.0F).scale(1.0F, 0.0F, 1.0F).mul(new Matrix4f(view).invert());
   }

   ItemShadowCollector(SceneBuffers source) {
      this.buffers = new AlphaBuffers(source);
      this.buffers.red = this.buffers.green = this.buffers.blue = 0.0F;
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
      if (this.equipment && (sprite instanceof TextureAtlasSprite || this.texture != null)) {
         this.rendered = true;
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
      this.facing.clear();

      for (BakedQuad q : quads.all()) {
         Direction d = q.direction();
         this.normal.set(d.getStepX(), d.getStepY(), d.getStepZ());
         pose.last().normal().transform(this.normal);
         if (this.normal.dot(this.up) > 0.01F) {
            this.facing.add(q);
         }
      }

      if (!this.facing.isEmpty()) {
         this.rendered = true;
         this.buffers.alpha = this.opacity;
         ItemQuadRenderer.render(
            context,
            pose,
            this.buffers,
            light,
            overlay,
            new int[0],
            this.facing,
            MatRenderTypes.silhouette(this.facing.getFirst().materialInfo().sprite().atlasLocation()),
            FoilType.NONE
         );
      }
   }
}
