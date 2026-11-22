package com.example.matinventory.render;

import com.example.matinventory.core.RecipeBookProp;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel.State;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
final class NativeRecipeBook {
   private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/enchantment/enchanting_table_book.png");
   private BookModel model;

   void draw(PoseStack pose, SceneBuffers buffers, MatSceneState state) {
      if (this.model == null) {
         this.model = new BookModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.BOOK));
      }

      this.model.setupAnim(State.forAnimation(0.0F, 0.0F, 0.0F, 0.0F));
      float x = RecipeBookProp.x();
      float z = RecipeBookProp.z();
      float y = 0.166F;
      pose.pushPose();
      state.fold.map(x, y * state.fold.relief(), z, 0.0F, 1.0F, 0.0F);
      pose.translate(state.fold.x, state.fold.y, state.fold.z);
      if (state.fold.active()) {
         pose.rotate(Axis.ZP.rotation(state.fold.bend));
      }

      pose.scale(1.0F, state.fold.relief(), 1.0F);
      pose.rotate(Axis.YP.rotationDegrees(45.0F));
      pose.rotate(Axis.XP.rotationDegrees(90.0F));
      pose.scale(1.4719999F, 1.4719999F, 1.4719999F);
      pose.translate(-0.1875F, 0.0F, 0.0F);
      this.model
         .renderToBuffer(
            pose,
            buffers.getBuffer(RenderTypes.entitySolid(TEXTURE)),
            15728880,
            OverlayTexture.NO_OVERLAY,
            state.world.color(-1, x, z, 0.0F, 1.0F, 0.0F, false)
         );
      pose.popPose();
      if (!state.fold.active() && !(state.bookHover < 0.01F)) {
         VertexConsumer out = buffers.getBuffer(MatRenderTypes.decal(MatMaterials.GLOW));
         pose.pushPose();
         pose.translate(x, 0.0F, z);
         pose.rotate(Axis.YP.rotationDegrees(45.0F));
         x = 0.0F;
         z = 0.0F;
         int color = (int)(state.bookHover * 220.0F) << 24 | 16777215;
         float h = 0.009F;
         float[][] sides = new float[][]{
            {x - 0.276F, z - 0.46F, x + 0.276F, z - 0.46F + h},
            {x - 0.276F, z + 0.46F - h, x + 0.276F, z + 0.46F},
            {x - 0.276F, z - 0.46F + h, x - 0.276F + h, z + 0.46F - h},
            {x + 0.276F - h, z - 0.46F + h, x + 0.276F, z + 0.46F - h}
         };

         for (float[] s : sides) {
            for (float[] p : new float[][]{{s[0], s[1]}, {s[0], s[3]}, {s[2], s[3]}, {s[2], s[1]}}) {
               out.addVertex(pose.last(), p[0], 0.25899997F, p[1])
                  .setColor(color)
                  .setUv(0.5F, 0.5F)
                  .setOverlay(OverlayTexture.NO_OVERLAY)
                  .setLight(15728880)
                  .setNormal(pose.last(), 0.0F, 1.0F, 0.0F);
            }
         }

         pose.popPose();
      }
   }
}
