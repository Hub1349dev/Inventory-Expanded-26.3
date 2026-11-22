package com.example.matinventory.render;

import com.example.matinventory.MatInventoryClient;
import com.example.matinventory.capacity.EquipmentKinds;
import com.example.matinventory.compat.LegacyGui;
import com.example.matinventory.core.GridController;
import com.example.matinventory.core.LedController;
import com.example.matinventory.core.RecipeBookProp;
import com.example.matinventory.layout.MatLayout;
import com.example.matinventory.scene.MatCamera;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public final class MatOverlay {
   private int clipOriginX;
   private int clipOriginY;
   private int minimumTop;
   private static final float TEXT_SCALE = 1.0F;
   private static final Identifier MOUSE_LEFT = Identifier.fromNamespaceAndPath("matinventory", "textures/gui/mouse_left.png");
   private static final Identifier MOUSE_RIGHT = Identifier.fromNamespaceAndPath("matinventory", "textures/gui/mouse_right.png");
   private static final Identifier MOUSE_MIDDLE = Identifier.fromNamespaceAndPath("matinventory", "textures/gui/mouse_middle.png");
   private static final Identifier KEY_CTRL = Identifier.fromNamespaceAndPath("matinventory", "textures/gui/key_ctrl.png");
   private static final Identifier KEY_E = Identifier.fromNamespaceAndPath("matinventory", "textures/gui/key_e.png");
   private static final int MOUSE_WIDTH = 12;
   private static final int MOUSE_HEIGHT = 16;
   private static final int ACTION_GAP = 4;
   private static final int ACTION_ROW = 18;
   public static final int ACTION_CYCLE = 1;
   public static final int ACTION_EQUIP = 2;
   public static final int ACTION_UNEQUIP = 4;
   public static final int ACTION_EAT = 8;
   private MatOverlay.Label active;
   private MatOverlay.Label leaving;
   private float lastElapsed;
   private float hungerAlpha;
   private float craftLabelX = -1.0F;
   private float craftLabelY = -1.0F;
   private String cycleKey = "";
   private int pendingCycle = -1;
   private int wheelPosition;
   private float clickAge = 1.0F;
   private float lastClickScale = 1.0F;
   private float unhovered;
   private boolean cycleScrolled;

   public void clipOrigin(int x, int y) {
      this.clipOriginX = x;
      this.clipOriginY = y;
   }

   public void minimumTop(int y) {
      this.minimumTop = Math.max(0, y);
   }

   private static MutableComponent ui(Component text) {
      return text.copy().withStyle(s -> s.withColor(16777215).withBold(false));
   }

   private List<MatLayout.Place> syncCycle(MatLayout layout, int slot) {
      List<MatLayout.Place> preview = layout.cyclePreview(slot);
      if (preview.size() < 2) {
         return preview;
      }

      MatLayout.Place p = layout.place(slot);
      String key = p.equipmentCell >= 0 ? "e:" + p.equipmentCell : "t:" + p.toolCell;
      if (!key.equals(this.cycleKey) || this.pendingCycle == preview.getFirst().slot() || preview.stream().noneMatch(q -> q.slot() == this.pendingCycle)) {
         this.cycleKey = key;
         this.pendingCycle = preview.get(1).slot();
         this.wheelPosition = 0;
         this.cycleScrolled = false;
      }

      return preview;
   }

   public void actionConfirmed() {
      this.clickAge = 0.0F;
   }

   public int cycleGlow(MatLayout layout, int slot) {
      List<MatLayout.Place> preview = this.syncCycle(layout, slot);
      return this.cycleScrolled && preview.stream().anyMatch(p -> p.slot() == this.pendingCycle) ? this.pendingCycle : slot;
   }

   public int cycleSelection(MatLayout layout, int slot) {
      this.syncCycle(layout, slot);
      return this.pendingCycle;
   }

   public boolean scrollCycle(MatLayout layout, int slot, double amount) {
      List<MatLayout.Place> preview = this.syncCycle(layout, slot);
      if (preview.size() >= 2 && amount != 0.0) {
         this.cycleScrolled = true;
         this.wheelPosition += amount > 0.0 ? -1 : 1;
         this.pendingCycle = preview.get(1 + Math.floorMod(this.wheelPosition, preview.size() - 1)).slot();
         return true;
      } else {
         return false;
      }
   }

   public void cycleConfirmed(AbstractContainerMenu menu) {
      this.clickAge = 0.0F;
      if (this.active != null && this.pendingCycle >= 0) {
         this.active.slot = this.pendingCycle;
         this.active.stack = menu.getSlot(this.pendingCycle).getItem().copy();
         this.active.name = ui(this.active.stack.getHoverName());
         this.active.count = this.active.stack.getCount();
         this.active.scrollOffset = this.active.scrollVelocity = 0.0F;
      }

      this.pendingCycle = -1;
      this.cycleScrolled = false;
   }

   public int cycleOwnerAt(double x, double y) {
      return this.active != null
            && this.active.alpha > 0.05F
            && (this.active.actions & 1) != 0
            && x >= this.active.x
            && x <= this.active.x + this.active.width
            && y >= this.active.y
            && y <= this.active.y + this.active.height
         ? this.active.slot
         : -1;
   }

   public void effects(GuiGraphicsExtractor g, int width, int height) {
      LegacyGui.normalized(g, MatMaterials.VIGNETTE, 0, 0, width, height, 0.0F, 1.0F, 0.0F, 1.0F);
   }

   public void craftVignette(GuiGraphicsExtractor g, int width, int height, float strength) {
      if (!(strength < 0.001F)) {
         int alpha = Math.round(108.0F * strength);
         int edge = alpha << 24;
         int band = Math.max(1, height / 4);
         g.fillGradient(0, 0, width, band, edge, 0);
         g.fillGradient(0, height - band, width, height, 0, edge);
         int side = Math.max(1, width / 7);

         for (int i = 0; i < side; i += 4) {
            int a = Math.round(alpha * (1.0F - (float)i / side));
            g.fill(i, 0, Math.min(side, i + 4), height, a << 24);
            g.fill(width - Math.min(side, i + 4), 0, width - i, height, a << 24);
         }
      }
   }

   public void craftCinemaBars(GuiGraphicsExtractor g, int width, int height, float strength) {
      float extent = height * 0.085F * Math.clamp(strength, 0.0F, 1.0F);
      int whole = (int)extent;
      if (whole > 0) {
         g.fill(0, 0, width, whole, -16777216);
         g.fill(0, height - whole, width, height, -16777216);
      }

      int alpha = Math.round((extent - whole) * 255.0F);
      if (alpha > 0) {
         g.fill(0, whole, width, whole + 1, alpha << 24);
         g.fill(0, height - whole - 1, width, height - whole, alpha << 24);
      }
   }

   public void counts(GuiGraphicsExtractor g, AbstractContainerMenu menu, MatLayout layout, MatCamera camera, int width, int height) {
      for (MatLayout.Place p : layout.places()) {
         p.countX = p.countY = -1.0F;
         if (p.visible
            && p.occupied
            && (p.group() == MatLayout.Group.CRAFT || p.group() == MatLayout.Group.RESULT)
            && Float.isFinite(p.labelRight)
            && Float.isFinite(p.labelBottom)
            && !(p.labelRight < 0.0F)
            && !(p.labelLeft >= width)
            && !(p.labelBottom < 0.0F)
            && !(p.labelTop >= height)) {
            MutableComponent count = ui(Component.literal("×" + menu.getSlot(p.slot()).getItem().getCount()));
            p.countX = Math.clamp(
               p.labelRight - Minecraft.getInstance().font.width(count) + 1.0F, 2.0F, Math.max(2, width - Minecraft.getInstance().font.width(count) - 2)
            );
            p.countY = Math.clamp(p.labelBottom + 2.0F, 2.0F, Math.max(2, height - 11));
            this.text(g, count, p.countX, p.countY, 1.0F);
         }
      }

      this.equipmentIndicators(g, menu, layout, camera);
   }

   public void equipmentIndicators(GuiGraphicsExtractor g, AbstractContainerMenu menu, MatLayout layout, MatCamera camera) {
      for (MatLayout.Place p : layout.places()) {
         if (p.visible && p.occupied && EquipmentKinds.equipped(menu.getSlot(p.slot()))) {
            double yaw = Math.toRadians(p.yaw);
            float depth = p.halfWidth * (float)Math.abs(Math.sin(yaw)) + p.halfDepth * (float)Math.abs(Math.cos(yaw));
            Vector4f point = camera.project(p.x, p.surfaceHeight, p.z - depth);
            int x = Math.round(point.x);
            int y = Math.round(point.y) - 3;
            g.fill(x, y - 1, x + 1, y + 2, -1);
            g.fill(x - 1, y, x + 2, y + 1, -1);
         }
      }
   }

   public void render(
      GuiGraphicsExtractor g, AbstractContainerMenu menu, MatLayout layout, MatCamera camera, int hovered, int x, int y, int width, int height, float elapsed
   ) {
      this.render(g, menu, layout, camera, hovered, x, y, width, height, elapsed, 0);
   }

   public void render(
      GuiGraphicsExtractor g,
      AbstractContainerMenu menu,
      MatLayout layout,
      MatCamera camera,
      int hovered,
      int x,
      int y,
      int width,
      int height,
      float elapsed,
      int actions
   ) {
      Minecraft client = Minecraft.getInstance();
      Font font = client.font;
      float dt = Math.clamp(elapsed - this.lastElapsed, 0.0F, 0.05F);
      this.lastElapsed = elapsed;
      this.clickAge = Math.min(1.0F, this.clickAge + dt);
      float t = Math.clamp(this.clickAge / 0.28F, 0.0F, 1.0F);
      float press = t < 0.34F ? t / 0.34F : (1.0F - t) / 0.66F;
      press = press * press * press * (press * (press * 6.0F - 15.0F) + 10.0F);
      this.lastClickScale = 1.0F - 0.065F * press;
      if (this.cycleOwnerAt(x, y) >= 0
         && menu.getCarried().isEmpty()
         && (hovered < 0 || hovered == this.active.slot || layout.cyclePreview(hovered).stream().anyMatch(px -> px.slot() == this.active.slot))
         && ItemStack.isSameItemSameComponents(this.active.stack, menu.getSlot(this.active.slot).getItem())) {
         hovered = this.active.slot;
         actions = this.active.actions;
      }

      ItemStack stack = hovered >= 0 ? menu.getSlot(hovered).getItem() : ItemStack.EMPTY;
      if (stack.isEmpty()) {
         hovered = -1;
      }

      boolean foodHover = hovered >= 0 && (actions & 8) != 0;
      this.hungerAlpha = this.hungerAlpha + ((foodHover ? 1 : 0) - this.hungerAlpha) * (1.0F - (float)Math.exp(-dt * 12.0F));
      this.hunger(g, width, height, this.hungerAlpha);
      boolean bundleHover = hovered >= 0 && MatBundleTooltip.hasContents(stack);
      ItemStack bundle = bundleHover ? stack : (hovered >= 0 && MatBundleTooltip.hasContents(menu.getCarried()) ? menu.getCarried() : ItemStack.EMPTY);
      boolean inspect = hovered >= 0
         && (
            InputConstants.isKeyDown(InputConstants.KEY_LALT)
               || InputConstants.isKeyDown(InputConstants.KEY_RALT)
               || InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)
               || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT)
         );
      if (inspect || !bundle.isEmpty()) {
         hovered = -1;
      }

      if (hovered < 0
         && !inspect
         && bundle.isEmpty()
         && menu.getCarried().isEmpty()
         && this.active != null
         && ItemStack.isSameItemSameComponents(this.active.stack, menu.getSlot(this.active.slot).getItem())) {
         this.unhovered += dt;
         if (this.unhovered < 0.09F) {
            hovered = this.active.slot;
            stack = menu.getSlot(hovered).getItem();
            actions = this.active.actions;
         }
      } else {
         this.unhovered = 0.0F;
      }

      if (this.active != null && this.active.slot == hovered && ItemStack.isSameItemSameComponents(this.active.stack, stack)) {
         this.active.stack = stack.copy();
         this.active.count = stack.getCount();
      }

      if (this.active != null && (this.active.slot != hovered || !ItemStack.matches(this.active.stack, stack))) {
         this.leaving = this.active;
         this.active = null;
      }

      if (this.active == null && hovered >= 0) {
         this.active = new MatOverlay.Label(stack, hovered);
      }

      if (this.leaving != null) {
         this.leaving.alpha = (float)(this.leaving.alpha * Math.exp(-dt * 14.0F));
         if (this.leaving.alpha < 0.015F) {
            this.leaving = null;
         }
      }

      if (this.active != null) {
         this.active.actions = actions;
         List<MatLayout.Place> preview = (actions & 1) != 0 ? this.syncCycle(layout, this.active.slot) : List.of();
         if (!this.active.positioned) {
            this.active.scrollOffset = this.wheelPosition;
            this.active.scrollVelocity = 0.0F;
         } else {
            float omega = 20.0F;
            float e = (float)Math.exp(-omega * dt);
            float delta = this.active.scrollOffset - this.wheelPosition;
            float j = this.active.scrollVelocity + omega * delta;
            this.active.scrollOffset = this.wheelPosition + (delta + j * dt) * e;
            this.active.scrollVelocity = (this.active.scrollVelocity - omega * j * dt) * e;
         }

         this.active.nextItems = preview.stream().skip(1L).<Component>map(next -> ui(menu.getSlot(next.slot()).getItem().getHoverName())).toList();
         this.active.alpha = this.active.alpha + (1.0F - this.active.alpha) * (1.0F - (float)Math.exp(-dt * 12.0F));
         MatLayout.Place p = layout.place(this.active.slot);
         this.active.countText = ui(Component.literal("×" + this.active.count));
         float textWidth = font.width(this.active.name) * 1.0F;
         float countWidth = font.width(this.active.countText) * 1.0F;
         float left = p.labelLeft;
         float top = p.labelTop;
         float right = p.labelRight;
         float bottom = p.labelBottom;

         for (MatLayout.Place q : preview) {
            if (q.visible && Float.isFinite(q.labelLeft)) {
               left = Math.min(left, q.labelLeft);
               top = Math.min(top, q.labelTop);
               right = Math.max(right, q.labelRight);
               bottom = Math.max(bottom, q.labelBottom);
            }
         }

         int hintRows = Integer.bitCount(actions) + ((actions & 1) != 0 ? Math.max(0, Math.min(3, this.active.nextItems.size()) - 1) : 0);
         float blockWidth = textWidth + countWidth + 5.0F;

         for (Component name : this.active.nextItems) {
            blockWidth = Math.max(blockWidth, 16 + font.width(name));
         }

         for (MatLayout.Place q : preview) {
            MutableComponent name = ui(menu.getSlot(q.slot()).getItem().getHoverName());
            blockWidth = Math.max(blockWidth, 16 + font.width(name));
         }

         if ((actions & 6) != 0) {
            blockWidth = Math.max(
               blockWidth, 51 + font.width(Component.translatable((actions & 4) != 0 ? "matinventory.action.unequip" : "matinventory.action.equip"))
            );
         }

         if ((actions & 8) != 0) {
            blockWidth = Math.max(blockWidth, 17 + font.width(Component.translatable("matinventory.action.eat")));
         }

         this.active.width = Math.max(1.0F, Math.min(blockWidth, width - 10));
         this.active.height = 12 + hintRows * 18;
         this.position(this.active, layout, p, preview, left, top, right, bottom, width, height);
         this.active.y = Math.max(this.minimumTop, this.active.y);
         this.active.shownName = this.fit(this.active.name, this.active.width - countWidth - 5.0F);
         this.active.countX = this.active.x + font.width(this.active.shownName) * 1.0F + 5.0F;
         this.active.countY = this.active.y;
         if (p.group() != MatLayout.Group.CRAFT && p.group() != MatLayout.Group.RESULT) {
            p.countX = this.active.countX;
            p.countY = this.active.countY;
         }
      }

      this.draw(g, this.leaving);
      this.draw(g, this.active);
      if (!bundle.isEmpty()) {
         MatBundleTooltip.render(g, bundle, menu.getCarried(), bundleHover, x, y);
      } else if (inspect) {
         g.setTooltipForNextFrame(font, stack, x, y);
      }
   }

   private void draw(GuiGraphicsExtractor g, MatOverlay.Label label) {
      if (label != null && !(label.alpha < 0.02F)) {
         g.pose().pushMatrix();
         float cx = label.x + label.width * 0.5F;
         float cy = label.y + label.height * 0.5F;
         g.pose().translate(cx, cy);
         g.pose().scale(this.lastClickScale, this.lastClickScale);
         g.pose().translate(-cx, -cy);
         this.text(g, label.shownName, label.x, label.y, label.alpha);
         this.text(g, label.countText, label.countX, label.countY, label.alpha);
         int x = Math.round(label.x);
         int y = Math.round(label.y) + 12;
         if (!label.nextItems.isEmpty()) {
            int firstY = y;
            int rows = Math.min(3, label.nextItems.size());
            int rail = Math.round(label.alpha * 140.0F) << 24 | 16777215;
            if (rows > 1) {
               g.fill(x + 5, y + 8, x + 7, y + (rows - 1) * 18 + 8, rail);
            }

            for (int i = 0; i < rows; i++) {
               int dot = Math.round(label.alpha * 210.0F) << 24 | 16777215;
               g.fill(x + 4, y + 5, x + 8, y + 11, dot);
               g.fill(x + 3, y + 6, x + 9, y + 10, dot);
               y += 18;
            }

            float cx2 = label.x + label.width * 0.5F;
            float cy2 = label.y + label.height * 0.5F;
            int clipX = (int)Math.floor(cx2 + (x + 12 + 4 - 2 - cx2) * this.lastClickScale);
            int clipY = (int)Math.floor(cy2 + (firstY + 1 - cy2) * this.lastClickScale);
            int clipRight = (int)Math.ceil(cx2 + (x + label.width + 2.0F - cx2) * this.lastClickScale);
            int clipBottom = (int)Math.ceil(cy2 + (firstY + rows * 18 - cy2) * this.lastClickScale);
            if (clipRight > clipX && clipBottom > clipY) {
               g.enableScissor(clipX + this.clipOriginX, clipY + this.clipOriginY, clipRight + this.clipOriginX, clipBottom + this.clipOriginY);

               try {
                  int start = (int)Math.floor(label.scrollOffset);

                  for (int i = start - 1; i <= start + rows; i++) {
                     float rowY = firstY + (i - label.scrollOffset) * 18.0F + 4.0F;
                     this.text(
                        g, this.fit(label.nextItems.get(Math.floorMod(i, label.nextItems.size())), label.width - 12.0F - 4.0F), x + 12 + 4, rowY, label.alpha
                     );
                  }
               } finally {
                  g.disableScissor();
               }
            }

            int color = Math.round(label.alpha * 255.0F) << 24 | 16777215;
            LegacyGui.blit(g, UiButtonTextures.themed(MOUSE_MIDDLE), x, firstY, 0.0F, 0.0F, 12, 16, 12, 16, color);
         }

         if ((label.actions & 6) != 0) {
            int color = Math.round(label.alpha * 255.0F) << 24 | 16777215;
            LegacyGui.blit(g, UiButtonTextures.themed(KEY_CTRL), x, y + 1, 0.0F, 0.0F, 25, 13, 25, 13, color);
            this.text(g, Component.literal("+"), x + 28, y + 5, label.alpha);
            this.actionRow(
               g,
               MOUSE_RIGHT,
               Component.translatable((label.actions & 4) != 0 ? "matinventory.action.unequip" : "matinventory.action.equip"),
               x + 35,
               y,
               label.alpha
            );
            y += 18;
         }

         if ((label.actions & 8) != 0) {
            int color = Math.round(label.alpha * 255.0F) << 24 | 16777215;
            LegacyGui.blit(g, UiButtonTextures.themed(KEY_E), x, y + 1, 0.0F, 0.0F, 13, 13, 13, 13, color);
            this.text(g, Component.translatable("matinventory.action.eat"), x + 17, y + 5, label.alpha);
         }

         g.pose().popMatrix();
      }
   }

   private Component fit(Component name, float width) {
      Font font = Minecraft.getInstance().font;
      int limit = Math.max(0, (int)width);
      return font.width(name) <= limit
         ? name
         : Component.literal(font.substrByWidth(name, Math.max(0, limit - font.width("…"))).getString() + "…").withStyle(name.getStyle());
   }

   private void position(
      MatOverlay.Label label,
      MatLayout layout,
      MatLayout.Place owner,
      List<MatLayout.Place> group,
      float left,
      float top,
      float right,
      float bottom,
      int width,
      int height
   ) {
      left = Math.clamp(left, 5.0F, Math.max(5, width - 5));
      right = Math.clamp(right, left, Math.max(left, width - 5));
      top = Math.clamp(top, 5.0F, Math.max(5, height - 5));
      bottom = Math.clamp(bottom, top, Math.max(top, height - 5));
      float w = label.width;
      float h = label.height;
      float middleX = (left + right - w) * 0.5F;
      float middleY = (top + bottom - h) * 0.5F;
      float[][] candidates = new float[][]{
         {right + 2.0F, middleY},
         {left - w - 2.0F, middleY},
         {middleX, bottom + 2.0F},
         {middleX, top - h - 2.0F},
         {right + 2.0F, top},
         {left - w - 2.0F, top},
         {right + 2.0F, bottom - h},
         {left - w - 2.0F, bottom - h},
         {left, bottom + 2.0F},
         {right - w, bottom + 2.0F},
         {left, top - h - 2.0F},
         {right - w, top - h - 2.0F}
      };
      float best = Float.POSITIVE_INFINITY;
      int previous = label.placement;

      for (int i = 0; i < candidates.length; i++) {
         float x = Math.clamp(candidates[i][0], 5.0F, Math.max(5.0F, width - w - 5.0F));
         float y = Math.clamp(candidates[i][1], 5.0F, Math.max(5.0F, height - h - 5.0F));
         float onItem = Math.max(0.0F, Math.min(x + w, right) - Math.max(x, left)) * Math.max(0.0F, Math.min(y + h, bottom) - Math.max(y, top));
         float moved = Math.abs(x - candidates[i][0]) + Math.abs(y - candidates[i][1]);
         float score = this.overlap(layout, owner, group, x, y, w, h) + onItem * 8.0F + moved * 0.25F + i * 0.5F - (i == previous ? 12 : 0);
         if (score < best) {
            best = score;
            label.x = x;
            label.y = y;
            label.placement = i;
         }
      }

      label.positioned = true;
   }

   private float overlap(MatLayout layout, MatLayout.Place owner, List<MatLayout.Place> group, float x, float y, float width, float height) {
      float area = 0.0F;

      for (MatLayout.Place p : layout.places()) {
         if (p != owner && !group.contains(p) && p.visible && p.occupied && Float.isFinite(p.labelLeft)) {
            area += Math.max(0.0F, Math.min(x + width, p.labelRight + 3.0F) - Math.max(x, p.labelLeft - 3.0F))
               * Math.max(0.0F, Math.min(y + height, p.labelBottom + 3.0F) - Math.max(y, p.labelTop - 3.0F));
         }
      }

      return area;
   }

   private void hunger(GuiGraphicsExtractor g, int width, int height, float alpha) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null && !(alpha < 0.02F)) {
         int food = player.getFoodData().getFoodLevel();
         int right = width - 12;
         int top = height - 21;
         int color = Math.round(alpha * 255.0F) << 24 | 16777215;
         boolean effect = player.hasEffect(MobEffects.HUNGER);
         Identifier empty = Identifier.withDefaultNamespace(effect ? "hud/food_empty_hunger" : "hud/food_empty");
         Identifier full = Identifier.withDefaultNamespace(effect ? "hud/food_full_hunger" : "hud/food_full");
         Identifier half = Identifier.withDefaultNamespace(effect ? "hud/food_half_hunger" : "hud/food_half");

         for (int i = 0; i < 10; i++) {
            int y = top;
            if (player.getFoodData().getSaturationLevel() <= 0.0F && player.tickCount % (food * 3 + 1) == 0) {
               y += Math.floorMod(player.tickCount * 31 + i * 17, 3) - 1;
            }

            int x = right - i * 8 - 9;
            LegacyGui.sprite(g, empty, x, y, 9, 9, color);
            if (i * 2 + 1 < food) {
               LegacyGui.sprite(g, full, x, y, 9, 9, color);
            } else if (i * 2 + 1 == food) {
               LegacyGui.sprite(g, half, x, y, 9, 9, color);
            }
         }
      }
   }

   public void craftLabel(GuiGraphicsExtractor g, MatLayout layout, float alpha, int width, int height) {
      if (alpha < 0.02F) {
         this.craftLabelX = this.craftLabelY = -1.0F;
      } else {
         MutableComponent label = Component.translatable("matinventory.action.craft");
         int blockWidth = 16 + Minecraft.getInstance().font.width(label);
         MatLayout.Place result = layout.places().stream().filter(p -> p.visible && p.occupied && p.group() == MatLayout.Group.RESULT).findFirst().orElse(null);
         if (result != null && Float.isFinite(result.labelRight) && Float.isFinite(result.labelBottom)) {
            this.craftLabelX = Math.round(Math.clamp((result.labelLeft + result.labelRight - blockWidth) * 0.5F, 5.0F, Math.max(5, width - blockWidth - 5)));
            float y = result.labelBottom + 15.0F;
            if (y + 16.0F > height - 5) {
               y = result.labelTop - 16.0F - 8.0F;
            }

            this.craftLabelY = Math.round(Math.clamp(y, 5.0F, Math.max(5, height - 16 - 5)));
            this.actionRow(g, MOUSE_RIGHT, label, Math.round(this.craftLabelX), Math.round(this.craftLabelY), alpha);
         } else {
            this.craftLabelX = this.craftLabelY = -1.0F;
         }
      }
   }

   public void carried(GuiGraphicsExtractor g, AbstractContainerMenu menu, MatSceneState frame, MatCamera camera, int width, int height) {
      if (!menu.getCarried().isEmpty()) {
         for (int i = 0; i < frame.itemCount; i++) {
            MatSceneState.Item v = frame.items()[i];
            if (v.slot == 511) {
               Vector4f p = camera.project(v.x, v.y, v.z);
               MutableComponent count = ui(Component.literal("×" + menu.getCarried().getCount()));
               float w = Minecraft.getInstance().font.width(count) * 1.0F;
               this.text(g, count, Math.clamp(p.x + 9.0F, 4.0F, Math.max(4.0F, width - w - 4.0F)), Math.clamp(p.y + 8.0F, 4.0F, height - 13), 1.0F);
               return;
            }
         }
      }
   }

   public void matLabel(GuiGraphicsExtractor g, MatCamera camera, float alpha, int width, int height) {
      if (!(alpha < 0.02F)) {
         Vector4f p = camera.project(LedController.matButtonX(), 0.23F, LedController.z());
         this.action(g, p.x, p.y, alpha, width, height, "matinventory.action.mat_color", null);
      }
   }

   public void gridLabel(GuiGraphicsExtractor g, MatCamera camera, float alpha, int width, int height) {
      if (!(alpha < 0.02F)) {
         Vector4f p = camera.project(GridController.x(), 0.23F, GridController.z());
         this.action(
            g, p.x, p.y, alpha, width, height, MatInventoryClient.config.gridEnabled ? "matinventory.action.grid_off" : "matinventory.action.grid_on", null
         );
      }
   }

   public void controllerLabel(GuiGraphicsExtractor g, MatCamera camera, float alpha, int width, int height, boolean powered) {
      if (!(alpha < 0.02F)) {
         Vector4f point = camera.project(LedController.buttonX() + 0.14F, 0.23F, LedController.z());
         this.action(
            g,
            point.x,
            point.y,
            alpha,
            width,
            height,
            powered ? "matinventory.action.light_off" : "matinventory.action.light_on",
            "matinventory.action.light_color"
         );
      }
   }

   public void bookLabel(GuiGraphicsExtractor g, MatCamera camera, float alpha, int width, int height, boolean open) {
      if (!(alpha < 0.02F)) {
         Vector4f p = camera.project(RecipeBookProp.x() + 0.276F, 0.258F, RecipeBookProp.z());
         this.action(g, p.x, p.y, alpha, width, height, open ? "matinventory.action.recipes_close" : "matinventory.action.recipes_open", null);
      }
   }

   private void action(GuiGraphicsExtractor g, float anchorX, float anchorY, float alpha, int width, int height, String leftKey, String rightKey) {
      Font font = Minecraft.getInstance().font;
      MutableComponent left = Component.translatable(leftKey);
      MutableComponent right = rightKey == null ? null : Component.translatable(rightKey);
      int labelWidth = Math.max(font.width(left), right == null ? 0 : font.width(right));
      int blockWidth = 16 + labelWidth;
      int blockHeight = 16 + (right == null ? 0 : 18);
      int x = Math.round(Math.clamp(anchorX + 5.0F, 5.0F, Math.max(5, width - blockWidth - 5)));
      int y = Math.round(Math.clamp(anchorY - blockHeight - 5.0F, 5.0F, Math.max(5, height - blockHeight - 5)));
      g.pose().pushMatrix();
      float cx = x + blockWidth * 0.5F;
      float cy = y + blockHeight * 0.5F;
      g.pose().translate(cx, cy);
      g.pose().scale(this.lastClickScale, this.lastClickScale);
      g.pose().translate(-cx, -cy);
      this.actionRow(g, MOUSE_LEFT, left, x, y, alpha);
      if (right != null) {
         this.actionRow(g, MOUSE_RIGHT, right, x, y + 18, alpha);
      }

      g.pose().popMatrix();
   }

   private void actionRow(GuiGraphicsExtractor g, Identifier mouse, Component label, int x, int y, float alpha) {
      int color = Math.round(Math.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24 | 16777215;
      LegacyGui.blit(g, UiButtonTextures.themed(mouse), x, y, 0.0F, 0.0F, 12, 16, 12, 16, color);
      this.text(g, label, x + 12 + 4, y + 5, alpha);
   }

   private void text(GuiGraphicsExtractor g, Component text, float x, float y, float alpha) {
      Font font = Minecraft.getInstance().font;
      text = ui(text);
      g.pose().pushMatrix();
      g.pose().translate(x, y);
      g.pose().scale(1.0F, 1.0F);
      MutableComponent shadowText = ui(text).withStyle(s -> s.withColor(3159096));

      for (int sy = -1; sy <= 1; sy++) {
         for (int sx = -1; sx <= 1; sx++) {
            float weight = (float)Math.exp(-(sx * sx + sy * sy) / 1.5F);
            int opacity = Math.round(alpha * 58.0F * weight);
            if (opacity >= 4) {
               g.pose().pushMatrix();
               g.pose().translate(0.65F + sx * 0.45F, 1.05F + sy * 0.45F);
               g.text(font, shadowText, 0, 0, opacity << 24, false);
               g.pose().popMatrix();
            }
         }
      }

      g.text(font, text, 0, 0, (int)(alpha * 255.0F) << 24 | 16777215, false);
      g.pose().popMatrix();
   }

   @Environment(EnvType.CLIENT)
   private static final class Label {
      ItemStack stack;
      Component name;
      Component shownName;
      Component countText;
      int slot;
      int count;
      int actions;
      int placement = -1;
      float alpha;
      float x;
      float y;
      float countX;
      float countY;
      float width;
      float height;
      float scrollOffset;
      float scrollVelocity;
      boolean positioned;
      List<Component> nextItems = List.of();

      Label(ItemStack stack, int slot) {
         this.stack = stack.copy();
         this.slot = slot;
         this.count = stack.getCount();
         this.name = MatOverlay.ui(stack.getHoverName());
         this.countText = MatOverlay.ui(Component.literal("×" + this.count));
      }
   }
}
