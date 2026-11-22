package com.example.matinventory.render;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

@Environment(EnvType.CLIENT)
final class MatBundleTooltip {
   private static final Identifier LEFT = mouse("left");
   private static final Identifier RIGHT = mouse("right");
   private static final Identifier WHEEL = mouse("middle");

   private static Identifier mouse(String key) {
      return Identifier.fromNamespaceAndPath("matinventory", "textures/gui/mouse_" + key + ".png");
   }

   static boolean hasContents(ItemStack stack) {
      return stack.has(DataComponents.BUNDLE_CONTENTS);
   }

   static void render(GuiGraphicsExtractor graphics, ItemStack bundle, ItemStack carried, boolean onBundle, int x, int y) {
      Font font = Minecraft.getInstance().font;
      BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
      if (contents != null) {
         ArrayList<ClientTooltipComponent> parts = new ArrayList<>();
         parts.add(ClientTooltipComponent.create(bundle.getHoverName().getVisualOrderText()));
         parts.add(ClientTooltipComponent.create(new BundleTooltip(contents)));
         ArrayList<MatBundleTooltip.Hint> hints = new ArrayList<>();
         hints.add(new MatBundleTooltip.Hint(LEFT, Component.translatable(carried.isEmpty() ? "matinventory.bundle.pickup" : "matinventory.bundle.insert")));
         if (onBundle && carried.isEmpty() && !contents.isEmpty()) {
            hints.add(new MatBundleTooltip.Hint(RIGHT, Component.translatable("matinventory.bundle.extract")));
            if (contents.size() > 1) {
               hints.add(new MatBundleTooltip.Hint(WHEEL, Component.translatable("matinventory.bundle.scroll")));
            }
         }

         parts.add(new MatBundleTooltip.Hints(hints));
         graphics.nextStratum();
         graphics.tooltip(font, parts, x, y, DefaultTooltipPositioner.INSTANCE, bundle.get(DataComponents.TOOLTIP_STYLE), false);
      }
   }

   @Environment(EnvType.CLIENT)
   private record Hint(Identifier icon, Component text) {
   }

   @Environment(EnvType.CLIENT)
   private record Hints(List<MatBundleTooltip.Hint> rows) implements ClientTooltipComponent {
      @Override
      public int getHeight(Font font) {
         return this.rows.size() * 18 + 4;
      }

      @Override
      public int getWidth(Font font) {
         int width = 0;

         for (MatBundleTooltip.Hint row : this.rows) {
            width = Math.max(width, 16 + font.width(row.text));
         }

         return width;
      }

      @Override
      public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
         int top = y + 4;

         for (MatBundleTooltip.Hint row : this.rows) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, row.icon, x, top, 0.0F, 0.0F, 12, 16, 12, 16, -1);
            graphics.text(font, row.text, x + 16, top + 4, -1644313, false);
            top += 18;
         }
      }
   }
}
