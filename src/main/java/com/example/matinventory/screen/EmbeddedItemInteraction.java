package com.example.matinventory.screen;

import com.example.matinventory.mixin.NativeScreenInvoker;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

@Environment(EnvType.CLIENT)
final class EmbeddedItemInteraction {
   private final AbstractContainerScreen<?> screen;
   private final Supplier<MatEmbeddedPreview> preview;
   private final BiPredicate<Double, Double> inside;
   private boolean pressed;
   private boolean ownPress;
   private boolean emptyPress;
   private int button;
   private double downX;
   private double downY;

   EmbeddedItemInteraction(AbstractContainerScreen<?> screen, Supplier<MatEmbeddedPreview> preview, BiPredicate<Double, Double> inside) {
      this.screen = screen;
      this.preview = preview;
      this.inside = inside;
   }

   boolean click(double x, double y, int button) {
      return this.click(
         x, y, button, InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT)
      );
   }

   boolean click(double x, double y, int button, boolean shift) {
      this.pressed = button == 0 || button == 1;
      if (!this.pressed) {
         return false;
      }

      this.button = button;
      this.downX = x;
      this.downY = y;
      this.ownPress = this.inside.test(x, y);
      this.emptyPress = this.screen.getMenu().getCarried().isEmpty();
      if (!this.ownPress) {
         return false;
      }

      this.reset();
      this.commit(x, y, button, shift && this.emptyPress ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP);
      return true;
   }

   boolean release(double x, double y, int button) {
      if (this.pressed && button == this.button) {
         this.pressed = false;
         boolean dragged = this.emptyPress && !this.screen.getMenu().getCarried().isEmpty() && Math.hypot(x - this.downX, y - this.downY) > 3.0;
         if (this.ownPress) {
            if (dragged) {
               this.commit(x, y, button, ContainerInput.PICKUP);
            }

            this.reset();
            return true;
         } else if (!this.screen.getMenu().getCarried().isEmpty() && Math.hypot(x - this.downX, y - this.downY) > 3.0 && this.inside.test(x, y)) {
            this.commit(x, y, button, ContainerInput.PICKUP);
            this.reset();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void commit(double x, double y, int button, ContainerInput type) {
      NativeScreenInvoker bridge = (NativeScreenInvoker)this.screen;
      Slot slot = this.inside.test(x, y) ? this.preview.get().pick(x, y) : bridge.matinventory$find(x, y);
      if (slot != null) {
         bridge.matinventory$click(slot, slot.index, button, type);
      }
   }

   private void reset() {
      NativeScreenInvoker bridge = (NativeScreenInvoker)this.screen;
      bridge.matinventory$skipRelease(false);
      bridge.matinventory$quickCrafting(false);
      bridge.matinventory$doubleClick(false);
      bridge.matinventory$lastClickSlot(null);
      bridge.matinventory$dragSlots().clear();
      this.screen.setDragging(false);
   }
}
