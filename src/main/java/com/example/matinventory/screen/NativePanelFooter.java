package com.example.matinventory.screen;

import com.example.matinventory.mixin.GuiBlitAccess;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public final class NativePanelFooter {
   private static NativePanelFooter.Crop active;
   private static boolean drawing;
   private static RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
   private static int tint = -1;
   private static int merchantBottom;
   public static Identifier lastTexture;

   public static boolean cropNative(
      GuiGraphicsExtractor g, RenderPipeline p, Identifier texture, int x0, int x1, int y0, int y1, float u0, float u1, float v0, float v1, int color
   ) {
      RenderPipeline old = pipeline;
      int oldTint = tint;
      pipeline = p;
      tint = color;

      try {
         return crop(g, texture, x0, x1, y0, y1, 0, u0, u1, v0, v1);
      } finally {
         pipeline = old;
         tint = oldTint;
      }
   }

   public static void during(int left, int top, int width, int height, int playerX, int playerWidth, int cleanFrom, int cut, Runnable render) {
      NativePanelFooter.Crop previous = active;
      active = new NativePanelFooter.Crop(left, top, width, height, playerX, playerWidth, Math.min(cleanFrom, cut), cut);

      try {
         render.run();
      } finally {
         active = previous;
      }
   }

   public static void merchant(int left, int top, int width, int height, int cut, int bottom, Runnable render) {
      int previous = merchantBottom;
      merchantBottom = bottom;

      try {
         during(left, top, width, height, 106, width - 106, cut, cut, render);
      } finally {
         merchantBottom = previous;
      }
   }

   public static boolean crop(GuiGraphicsExtractor g, Identifier texture, int x0, int x1, int y0, int y1, int z, float u0, float u1, float v0, float v1) {
      NativePanelFooter.Crop c = active;
      if (!drawing && c != null && x0 == c.left && y0 == c.top && x1 - x0 == c.width && y1 - y0 == c.height) {
         drawing = true;
         lastTexture = texture;

         try {
            if (merchantBottom > 0) {
               float du = (u1 - u0) / (x1 - x0);
               float dv = (v1 - v0) / (y1 - y0);
               int side = x0 + 106;
               int split = y0 + c.cut;
               int bottom = y0 + merchantBottom;
               quad(g, texture, x0, x1, y0, split, z, u0, u1, v0, v0 + c.cut * dv);
               if (bottom > split) {
                  quad(g, texture, x0, side, split, bottom, z, u0, u0 + 106.0F * du, v0 + c.cut * dv, v0 + merchantBottom * dv);
               }

               if (bottom > split) {
                  quad(g, texture, side - 6, side, split, bottom, z, u1 - 6.0F * du, u1, v0 + 7.0F * dv, v0 + 8.0F * dv);
               }

               footer(g, texture, x0, bottom, 106, z, u0, u0 + 106.0F * du, v1, dv);
               footer(g, texture, side, split, x1 - side, z, u0 + 106.0F * du, u1, v1, dv);
               return true;
            }

            int clean = y0 + c.cleanFrom;
            int split = y0 + c.cut;
            int left = x0 + c.playerX;
            int right = left + c.playerWidth;
            float du = (u1 - u0) / (x1 - x0);
            float dv = (v1 - v0) / (y1 - y0);
            float vs = v0 + c.cleanFrom * dv;
            quad(g, texture, x0, x1, y0, clean, z, u0, u1, v0, vs);
            if (left > x0) {
               quad(g, texture, x0, left, clean, y1, z, u0, u0 + c.playerX * du, vs, v1);
            }

            if (right < x1) {
               quad(g, texture, right, x1, clean, y1, z, u0 + (right - x0) * du, u1, vs, v1);
            }

            if (split > clean) {
               quad(g, texture, left, right, clean, split, z, u0 + c.playerX * du, u0 + (c.playerX + c.playerWidth) * du, v1 - 6.0F * dv, v1 - 5.0F * dv);
            }

            footer(g, texture, left, split, c.playerWidth, z, u0 + c.playerX * du, u0 + (c.playerX + c.playerWidth) * du, v1, dv);
         } finally {
            drawing = false;
         }

         return true;
      } else {
         return false;
      }
   }

   public static void draw(GuiGraphicsExtractor g, Identifier texture, int x, int y, int width, int sourceBottom) {
      lastTexture = texture;
      footer(g, texture, x, y, width, 0, 0.0F, width / 256.0F, sourceBottom / 256.0F, 0.00390625F);
   }

   private static void footer(GuiGraphicsExtractor g, Identifier texture, int x, int y, int width, int z, float u0, float u1, float bottom, float row) {
      float padding = bottom - 6.0F * row;
      quad(g, texture, x, x + width, y, y + 1, z, u0, u1, padding, padding + row);
      quad(g, texture, x, x + width, y + 1, y + 7, z, u0, u1, padding, bottom);
   }

   private static void quad(GuiGraphicsExtractor g, Identifier texture, int x0, int x1, int y0, int y1, int z, float u0, float u1, float v0, float v1) {
      ((GuiBlitAccess)g).matinventory$blit(pipeline, texture, x0, x1, y0, y1, u0, u1, v0, v1, tint);
   }

   private NativePanelFooter() {
   }

   @Environment(EnvType.CLIENT)
   private record Crop(int left, int top, int width, int height, int playerX, int playerWidth, int cleanFrom, int cut) {
   }
}
