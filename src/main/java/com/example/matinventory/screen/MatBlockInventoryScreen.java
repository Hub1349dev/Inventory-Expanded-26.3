package com.example.matinventory.screen;

import com.example.matinventory.capacity.ExtraInventory;
import com.example.matinventory.compat.LegacyContainerScreen;
import com.example.matinventory.layout.MatLayout;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;

@Environment(EnvType.CLIENT)
public final class MatBlockInventoryScreen extends LegacyContainerScreen<AbstractContainerMenu> {
   private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
   private static final Identifier CRAFT = Identifier.withDefaultNamespace("textures/gui/container/crafting_table.png");
   private final Inventory inventory;
   private final Map<Slot, SlotCoordinates.Point> originals = new IdentityHashMap<>();
   public MatEmbeddedPreview preview;
   private int blockX;
   private int blockY;
   private int blockHeight;
   private final boolean chest;
   private final List<MatPreviewButton> controls = new ArrayList<>();
   private double actionX = -1000.0;
   private double actionY = -1000.0;
   private EmbeddedItemInteraction interaction;
   private boolean cycleRelease;

   public List<Button> controls() {
      return List.copyOf(this.controls);
   }

   public MatBlockInventoryScreen(AbstractContainerMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.inventory = inventory;
      this.chest = menu instanceof ChestMenu;
   }

   @Override
   protected void init() {
      this.originals.forEach((slot, p) -> SlotCoordinates.move(slot, p.x(), p.y()));
      this.originals.clear();
      super.init();
      this.leftPos = this.topPos = 0;
      this.blockX = (this.width - 176) / 2;
      this.blockY = 6;
      this.blockHeight = this.chest ? ((ChestMenu)this.menu).getRowCount() * 18 + 24 : 91;

      for (Slot slot : this.menu.slots) {
         this.originals.put(slot, new SlotCoordinates.Point(slot.x, slot.y));
         if (slot.container == this.inventory || slot.container instanceof ExtraInventory) {
            SlotCoordinates.move(slot, -10000, -10000);
         } else if (!this.chest && slot.index >= 10) {
            SlotCoordinates.move(slot, -10000, -10000);
         } else {
            SlotCoordinates.move(slot, slot.x + this.blockX, slot.y + this.blockY);
         }
      }

      int w = this.width - 20;
      int h = Math.max(24, this.height - this.blockY - this.blockHeight - 4 - 50);
      this.preview = new MatEmbeddedPreview(this.menu, (this.width - w) / 2, this.blockY + this.blockHeight + 4, w, h);
      this.interaction = new EmbeddedItemInteraction(this, () -> this.preview, (x, y) -> this.preview.contains(x, y) && !this.blockAt(x, y));
      this.controls.clear();
      this.controls.add(this.addWidget(new MatPreviewButton(b -> this.preview.toggleGrid())));
   }

   @Override
   public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
      super.extractBackground(g, mx, my, partial);
      if (this.chest && this.preview.camera.width > 0) {
         float[] bounds = this.preview.centeredBounds();
         int modelTop = this.preview.y + Math.round(this.preview.camera.project(bounds[0], 0.0F, bounds[1]).y);
         int next = Math.max(6, modelTop - this.blockHeight - 3);
         int delta = next - this.blockY;
         if (delta != 0) {
            for (Slot slot : this.menu.slots) {
               if (slot.container == ((ChestMenu)this.menu).getContainer()) {
                  SlotCoordinates.move(slot, slot.x, slot.y + delta);
               }
            }

            this.blockY = next;
         }
      }

      Identifier texture = this.chest ? CHEST : CRAFT;
      int sourceHeight = this.chest ? 222 : 166;
      NativePanelFooter.during(
         this.blockX,
         this.blockY,
         176,
         sourceHeight,
         0,
         176,
         this.chest ? this.blockHeight - 7 : 72,
         this.blockHeight - 7,
         () -> g.blit(RenderPipelines.GUI_TEXTURED, texture, this.blockX, this.blockY, 0.0F, 0.0F, 176, sourceHeight, 256, 256)
      );
      g.text(this.font, this.title, this.blockX + 8, this.blockY + 6, -12566464, false);
      this.preview.occludedTop(this.blockY + this.blockHeight - this.preview.y + 2);
      this.preview.render(g, this.blockAt(mx, my) ? -1000 : mx, this.blockAt(mx, my) ? -1000 : my);
   }

   @Override
   protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
      super.extractRenderState(g, mx, my, partial);
      if (this.preview.contains(mx, my) && !this.blockAt(mx, my)) {
         this.hoveredSlot = this.preview.pick(mx, my);
      }

      for (MatPreviewButton button : this.controls) {
         button.setX(6);
         button.setY(this.height - 21);
         button.active = this.menu.getCarried().isEmpty();
         button.extractRenderState(g, mx, my, partial);
      }
   }

   @Override
   protected void extractTooltip(GuiGraphicsExtractor g, int mx, int my) {
      if (!this.preview.contains(mx, my) || this.blockAt(mx, my)) {
         super.extractTooltip(g, mx, my);
      }
   }

   public boolean blockAt(double x, double y) {
      return x >= this.blockX && x < this.blockX + 176 && y >= this.blockY && y < this.blockY + this.blockHeight;
   }

   @Override
   public boolean mouseScrolled(double x, double y, double dx, double dy) {
      return !this.blockAt(x, y) && this.preview.scroll(x, y, dy) || super.mouseScrolled(x, y, dx, dy);
   }

   @Override
   public boolean mouseClicked(double x, double y, int button) {
      this.actionX = x;
      this.actionY = y;
      if (!this.blockAt(x, y) && (button == 2 || button == 1 && (this.inputModifiers() & InputConstants.MOD_CONTROL) != 0) && this.preview.confirmCycle(x, y)) {
         this.cycleRelease = true;
         return true;
      } else {
         return this.interaction.click(x, y, button, (this.inputModifiers() & InputConstants.MOD_SHIFT) != 0) ? true : super.mouseClicked(x, y, button);
      }
   }

   @Override
   public boolean mouseReleased(double x, double y, int button) {
      this.actionX = x;
      this.actionY = y;
      if (this.cycleRelease) {
         this.cycleRelease = false;
         return true;
      } else {
         return this.interaction.release(x, y, button) ? true : super.mouseReleased(x, y, button);
      }
   }

   @Override
   public void mouseMoved(double x, double y) {
      this.actionX = x;
      this.actionY = y;
      super.mouseMoved(x, y);
   }

   @Override
   protected void slotClicked(Slot slot, int index, int button, ContainerInput type) {
      MatEmbeddedPreview.ClickState state = this.preview.beforeClick(slot, this.actionX, this.actionY);
      boolean pickOne = type == ContainerInput.PICKUP
         && button == 0
         && this.menu.getCarried().isEmpty()
         && slot != null
         && this.preview.owns(slot)
         && MatLayout.toolKind(slot.getItem()) != null
         && slot.getItem().getCount() == 2;
      super.slotClicked(slot, index, pickOne ? 1 : button, type);
      this.preview.afterClick(slot, type, state);
   }

   @Override
   protected boolean hasClickedOutside(double x, double y, int left, int top) {
      return !this.preview.contains(x, y) && (!(x >= this.blockX) || !(x < this.blockX + 176) || !(y >= this.blockY) || !(y < this.blockY + this.blockHeight));
   }

   @Override
   public void removed() {
      this.originals.forEach((slot, p) -> SlotCoordinates.move(slot, p.x(), p.y()));
      super.removed();
   }

   public Screen nativeScreen() {
      return this.chest
         ? new ContainerScreen((ChestMenu)this.menu, this.inventory, this.title)
         : new CraftingScreen((CraftingMenu)this.menu, this.inventory, this.title);
   }
}
