package com.example.matinventory.screen;

import com.example.matinventory.compat.LegacyGui;
import com.example.matinventory.render.UiButtonTextures;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
final class MatPreviewButton extends Button {
   MatPreviewButton(OnPress action) {
      super(6, 0, 19, 19, Component.translatable("matinventory.preview.grid"), action, DEFAULT_NARRATION);
      this.setTooltip(Tooltip.create(this.getMessage()));
   }

   @Override
   protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float partial) {
      Identifier icon = Identifier.fromNamespaceAndPath("matinventory", "textures/gui/key_grid.png");
      LegacyGui.blit(g, UiButtonTextures.themed(icon), this.getX() + 3, this.getY() + 3, 0.0F, 0.0F, 13, 13, 13, 13, this.active ? -1 : -1996488705);
   }
}
