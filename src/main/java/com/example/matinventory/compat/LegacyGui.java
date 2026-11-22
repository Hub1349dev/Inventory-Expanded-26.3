package com.example.matinventory.compat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public final class LegacyGui {
   public static void blit(GuiGraphicsExtractor g, Identifier texture, int x, int y, float u, float v, int w, int h, int tw, int th, int color) {
      g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, w, h, tw, th, color);
   }

   public static void sprite(GuiGraphicsExtractor g, Identifier texture, int x, int y, int w, int h, int color) {
      g.blitSprite(RenderPipelines.GUI_TEXTURED, texture, x, y, w, h, color);
   }

   public static void normalized(GuiGraphicsExtractor g, Identifier texture, int x, int y, int w, int h, float u0, float u1, float v0, float v1) {
      g.blit(texture, x, y, x + w, y + h, u0, u1, v0, v1);
   }

   private LegacyGui() {
   }
}
