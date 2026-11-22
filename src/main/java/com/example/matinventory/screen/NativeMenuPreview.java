package com.example.matinventory.screen;

import com.example.matinventory.MatInventoryClient;
import com.example.matinventory.capacity.ExtraInventory;
import com.example.matinventory.capacity.NativeExtraMenuAccess;
import com.example.matinventory.compat.MatInput;
import com.example.matinventory.layout.MatLayout;
import com.example.matinventory.mixin.CreativeSlotAccess;
import com.example.matinventory.mixin.NativeScreenInvoker;
import java.util.IdentityHashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;

@Environment(EnvType.CLIENT)
public final class NativeMenuPreview {
   private final AbstractContainerScreen<?> screen;
   private final Map<Slot, SlotCoordinates.Point> original = new IdentityHashMap<>();
   private final int cutY;
   private final int cleanFrom;
   private final int playerX;
   private final int playerWidth;
   public MatEmbeddedPreview preview;
   private final MatPreviewButton grid;
   private boolean recipeReady;
   private boolean cycleRelease;
   private double actionX = -1000.0;
   private double actionY = -1000.0;
   private MatEmbeddedPreview.ClickState transaction;
   private final EmbeddedItemInteraction interaction;

   public static NativeMenuPreview create(AbstractContainerScreen<?> screen) {
      Minecraft c = Minecraft.getInstance();
      AbstractContainerMenu menu = screen.getMenu();
      boolean creative = screen instanceof CreativeModeInventoryScreen && menu.slots.stream().anyMatch(s -> s instanceof CreativeSlotAccess);
      if (!creative && screen instanceof CreativeModeInventoryScreen) {
         return null;
      } else {
         return MatInventoryClient.config != null
               && MatInventoryClient.config.enabled
               && !MatInventoryClient.renderFailed
               && c.player != null
               && !(screen instanceof MatInventoryScreen)
               && !(screen instanceof MatBlockInventoryScreen)
               && !(menu instanceof InventoryMenu)
               && !(menu instanceof CraftingMenu)
               && !(menu instanceof ChestMenu)
               && (creative || menu instanceof NativeExtraMenuAccess access && access.matinventory$extraEnd() > 0)
            ? new NativeMenuPreview(screen)
            : null;
      }
   }

   private NativeMenuPreview(AbstractContainerScreen<?> screen) {
      this.screen = screen;
      Inventory inventory = Minecraft.getInstance().player.getInventory();
      int y = Integer.MAX_VALUE;
      int left = Integer.MAX_VALUE;
      int right = 0;
      int content = 24;

      for (Slot slot : screen.getMenu().slots) {
         Slot real = MatEmbeddedPreview.unwrap(slot);
         if (real.container == inventory || real.container instanceof ExtraInventory) {
            this.original.put(slot, new SlotCoordinates.Point(slot.x, slot.y));
         } else if (slot.x >= 0 && slot.y >= 0) {
            content = Math.max(content, slot.y + 21);
         }

         if (real.container == inventory && real.getContainerSlot() >= 9 && real.getContainerSlot() < 36) {
            y = Math.min(y, slot.y);
            left = Math.min(left, slot.x);
            right = Math.max(right, slot.x);
         }
      }

      if (screen instanceof AnvilScreen) {
         content = Math.max(content, 82);
      }

      if (screen instanceof CartographyTableScreen || screen instanceof SmithingScreen) {
         content = Math.max(content, 84);
      }

      this.cutY = screen instanceof CreativeModeInventoryScreen
         ? 44
         : Math.max(content, Math.max(24, y == Integer.MAX_VALUE ? ((NativeScreenInvoker)screen).matinventory$height() - 94 : y - 12));
      this.cleanFrom = screen instanceof CreativeModeInventoryScreen ? 44 : Math.max(24, y == Integer.MAX_VALUE ? this.cutY : y - 1);
      this.playerX = Math.max(0, left == Integer.MAX_VALUE ? 0 : left - 8);
      this.playerWidth = left == Integer.MAX_VALUE ? 176 : right - left + 32;
      int targetTop = screen instanceof CreativeModeInventoryScreen ? 54 : 6;
      if (screen instanceof CreativeModeInventoryScreen) {
         int delta = targetTop - ((NativeScreenInvoker)screen).matinventory$top();

         for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget widget) {
               widget.setY(widget.getY() + delta);
            }
         }

         Slot trash = screen.getMenu().slots.getLast();
         this.original.put(trash, new SlotCoordinates.Point(trash.x, trash.y));
         SlotCoordinates.move(trash, 173, 20);
      }

      ((NativeScreenInvoker)screen).matinventory$top(targetTop);
      this.grid = new MatPreviewButton(b -> {
         if (this.preview != null) {
            this.preview.toggleGrid();
         }
      });
      this.interaction = new EmbeddedItemInteraction(screen, () -> this.preview, this::inPreview);
   }

   public void restore() {
      this.original.forEach((slot, p) -> SlotCoordinates.move(slot, p.x(), p.y()));
   }

   public boolean owns(Slot slot) {
      return this.preview != null && this.preview.owns(slot);
   }

   public boolean inPreview(double x, double y) {
      return this.preview != null && this.preview.contains(x, y) && !this.nativeAt(x, y);
   }

   private int merchantHeight() {
      int offers = ((MerchantMenu)this.screen.getMenu()).getOffers().size();
      return Math.max(this.cutY, 18 + Math.min(7, Math.max(1, offers)) * 20 + 5);
   }

   public boolean nativeAt(double x, double y) {
      if (this.screen instanceof MerchantScreen) {
         int localY = (int)y - ((NativeScreenInvoker)this.screen).matinventory$top();
         return x >= ((NativeScreenInvoker)this.screen).matinventory$left()
            && x < ((NativeScreenInvoker)this.screen).matinventory$left() + ((NativeScreenInvoker)this.screen).matinventory$width()
            && localY >= 0
            && localY < (x < ((NativeScreenInvoker)this.screen).matinventory$left() + 106 ? this.merchantHeight() + 7 : this.cutY + 7);
      } else {
         return x >= ((NativeScreenInvoker)this.screen).matinventory$left()
            && x < ((NativeScreenInvoker)this.screen).matinventory$left() + ((NativeScreenInvoker)this.screen).matinventory$width()
            && y >= ((NativeScreenInvoker)this.screen).matinventory$top()
            && y < ((NativeScreenInvoker)this.screen).matinventory$top() + this.reservedHeight();
      }
   }

   private boolean sidebars() {
      int l = ((NativeScreenInvoker)this.screen).matinventory$left() + this.playerX;
      int r = l + this.playerWidth;
      int b = ((NativeScreenInvoker)this.screen).matinventory$top() + this.cutY;
      return this.screen
         .children()
         .stream()
         .anyMatch(w -> w instanceof AbstractWidget v && v.getY() + v.getHeight() > b && (v.getX() < l || v.getX() + v.getWidth() > r));
   }

   private RecipeBookComponent<?> recipeBook() {
      return this.screen.children().stream().filter(w -> w instanceof RecipeBookComponent).map(w -> (RecipeBookComponent<?>)w).findFirst().orElse(null);
   }

   private int reservedHeight() {
      boolean book = this.screen instanceof RecipeUpdateListener listener && this.recipeBook() != null && this.recipeBook().isVisible();
      return !book && !this.sidebars() ? this.cutY + 7 : Math.max(((NativeScreenInvoker)this.screen).matinventory$height(), 178);
   }

   private void geometry() {
      Minecraft c = Minecraft.getInstance();
      if (!this.recipeReady && this.screen instanceof RecipeUpdateListener listener && this.screen.getMenu() instanceof RecipeBookMenu menu) {
         RecipeBookComponent<?> book = this.recipeBook();
         if (book != null) {
            boolean open = book.isVisible();
            book.init(this.screen.width, 178, c, this.screen.width < 379);
            if (open != book.isVisible()) {
               book.toggleVisibility();
            }
         }

         this.recipeReady = true;
      }

      if (this.screen instanceof RecipeUpdateListener) {
         for (GuiEventListener child : this.screen.children()) {
            if (child instanceof ImageButton button && button.getWidth() == 20 && button.getHeight() == 18) {
               button.setX(((NativeScreenInvoker)this.screen).matinventory$left() + 20);
               button.setY(((NativeScreenInvoker)this.screen).matinventory$top() + 34);
            }
         }
      }

      int y = ((NativeScreenInvoker)this.screen).matinventory$top() + this.reservedHeight() + 4;
      int w = this.screen.width - 20;
      int h = Math.max(24, this.screen.height - y - 50);
      int px = 10;
      if (this.screen instanceof MerchantScreen) {
         int below = ((NativeScreenInvoker)this.screen).matinventory$top() + this.merchantHeight() + 13;
         if (this.screen.height - below - 34 >= 80) {
            y = below;
            w = Math.min(this.screen.width - 20, ((NativeScreenInvoker)this.screen).matinventory$width() + 100);
            px = (this.screen.width - w) / 2;
            h = this.screen.height - y - 34;
         } else {
            px = ((NativeScreenInvoker)this.screen).matinventory$left() + 106;
            y = ((NativeScreenInvoker)this.screen).matinventory$top() + this.cutY + 11;
            w = ((NativeScreenInvoker)this.screen).matinventory$width() - 106;
            h = Math.max(24, this.screen.height - y - 34);
         }

         h = Math.min(h, Math.max(24, Math.round(w * 0.5F)));
      }

      if (this.preview == null || this.preview.x != px || this.preview.y != y || this.preview.width != w || this.preview.height != h) {
         this.preview = new MatEmbeddedPreview(this.screen.getMenu(), px, y, w, h);
      }

      this.preview.slotOrigin(((NativeScreenInvoker)this.screen).matinventory$left(), ((NativeScreenInvoker)this.screen).matinventory$top());
   }

   public void background(GuiGraphicsExtractor g, int mx, int my, Runnable nativeBackground) {
      this.geometry();
      boolean sidebar = this.sidebars();
      if (this.screen instanceof MerchantScreen) {
         NativePanelFooter.merchant(
            ((NativeScreenInvoker)this.screen).matinventory$left(),
            ((NativeScreenInvoker)this.screen).matinventory$top(),
            ((NativeScreenInvoker)this.screen).matinventory$width(),
            ((NativeScreenInvoker)this.screen).matinventory$height(),
            this.cutY,
            this.merchantHeight(),
            nativeBackground
         );
      } else {
         NativePanelFooter.during(
            ((NativeScreenInvoker)this.screen).matinventory$left(),
            ((NativeScreenInvoker)this.screen).matinventory$top(),
            ((NativeScreenInvoker)this.screen).matinventory$width(),
            ((NativeScreenInvoker)this.screen).matinventory$height(),
            sidebar ? this.playerX : 0,
            sidebar ? this.playerWidth : ((NativeScreenInvoker)this.screen).matinventory$width(),
            this.cleanFrom,
            this.cutY,
            nativeBackground
         );
      }

      this.preview.render(g, this.nativeAt(mx, my) ? -1000 : mx, this.nativeAt(mx, my) ? -1000 : my);
      if (this.screen instanceof CreativeModeInventoryScreen) {
         int tx = ((NativeScreenInvoker)this.screen).matinventory$left() + 173;
         int ty = ((NativeScreenInvoker)this.screen).matinventory$top() + 20;
         g.fill(tx - 1, ty - 1, tx + 17, ty + 17, -11184811);
         g.text(Minecraft.getInstance().font, "×", tx + 5, ty + 4, -1144696, false);
      }
   }

   public void labels(GuiGraphicsExtractor g, int mx, int my) {
      NativeScreenInvoker bridge = (NativeScreenInvoker)this.screen;
      Component title = bridge.matinventory$inventoryTitle();
      bridge.matinventory$inventoryTitle(Component.empty());

      try {
         bridge.matinventory$labels(g, mx, my);
      } finally {
         bridge.matinventory$inventoryTitle(title);
      }
   }

   public void controls(GuiGraphicsExtractor g, int mx, int my, float partial) {
      if (this.preview != null) {
         this.grid.setX(6);
         this.grid.setY(this.screen.height - 21);
         this.grid.active = this.screen.getMenu().getCarried().isEmpty();
         this.grid.extractRenderState(g, mx, my, partial);
      }
   }

   public boolean click(MouseButtonEvent event) {
      double x = event.x();
      double y = event.y();
      int button = MatInput.button(event);
      this.actionX = x;
      this.actionY = y;
      if (this.grid.active && button == 0 && this.grid.isMouseOver(x, y)) {
         this.grid.onPress(new KeyEvent(0, 0, 0));
         this.cycleRelease = true;
         return true;
      } else if (this.inPreview(x, y) && (button == 2 || button == 1 && event.hasControlDown()) && this.preview.confirmCycle(x, y)) {
         this.cycleRelease = true;
         return true;
      } else {
         return this.interaction.click(x, y, button, event.hasShiftDown());
      }
   }

   public boolean release(double x, double y, int button) {
      this.actionX = x;
      this.actionY = y;
      if (this.cycleRelease) {
         this.cycleRelease = false;
         return true;
      } else {
         return this.interaction.release(x, y, button);
      }
   }

   public void pointer(double x, double y) {
      this.actionX = x;
      this.actionY = y;
   }

   public void beforeClick(Slot slot) {
      this.transaction = this.preview == null ? null : this.preview.beforeClick(slot, this.actionX, this.actionY);
   }

   public int pickupButton(Slot slot, int button, ContainerInput type) {
      return this.preview != null
            && button == 0
            && type == ContainerInput.PICKUP
            && this.screen.getMenu().getCarried().isEmpty()
            && slot != null
            && this.owns(slot)
            && MatLayout.toolKind(slot.getItem()) != null
            && slot.getItem().getCount() == 2
         ? 1
         : button;
   }

   public void afterClick(Slot slot, ContainerInput type) {
      if (this.preview != null) {
         this.preview.afterClick(slot, type, this.transaction);
      }
   }
}
