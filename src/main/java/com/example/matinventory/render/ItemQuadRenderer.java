package com.example.matinventory.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.math.MatrixUtil;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;

@Environment(EnvType.CLIENT)
final class ItemQuadRenderer {
   private static final QuadInstance INSTANCE = new QuadInstance();

   static void render(
      ItemDisplayContext context, PoseStack pose, SceneBuffers buffers, int light, int overlay, int[] tints, List<BakedQuad> quads, RenderType forced, FoilType foil
   ) {
      INSTANCE.setLightCoords(light);
      INSTANCE.setOverlayCoords(overlay);
      boolean special = forced == null && foil == FoilType.SPECIAL;
      PoseStack.Pose decal = null;
      if (special) {
         decal = pose.last().copy();
         if (context == ItemDisplayContext.GUI) {
            MatrixUtil.mulComponentWise(decal.pose(), 0.5F);
         }
      }

      for (BakedQuad quad : quads) {
         BakedQuad.MaterialInfo material = quad.materialInfo();
         RenderType type = forced;
         if (type == null) {
            type = switch (foil) {
               case NONE -> material.itemRenderType();
               case STANDARD -> material.itemGlintRenderType();
               case SPECIAL -> material.itemGlintSpecialRenderType();
            };
         }

         int tint = material.tintIndex();
         INSTANCE.setColor(tint >= 0 && tint < tints.length ? tints[tint] : -1);
         if (special) {
            buffers.getBuffer(type).putBakedQuadWithGlint(pose.last(), quad, INSTANCE, decal);
         } else {
            buffers.getBuffer(type).putBakedQuad(pose.last(), quad, INSTANCE);
         }
      }
   }

   private ItemQuadRenderer() {
   }
}
