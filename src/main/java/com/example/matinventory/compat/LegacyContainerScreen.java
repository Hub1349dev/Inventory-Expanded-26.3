package com.example.matinventory.compat;

import com.example.matinventory.compat.MatInput;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

@Environment(EnvType.CLIENT)
public abstract class LegacyContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
   private int eventModifiers;
   private boolean eventTwice;

   protected LegacyContainerScreen(T menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
   }

   private MouseButtonEvent event(double x, double y, int b) {
      return new MouseButtonEvent(x, y, new MouseButtonInfo(MatInput.physical(b), this.eventModifiers));
   }

   protected final int inputModifiers() {
      return this.eventModifiers;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent e, boolean twice) {
      int old = this.eventModifiers;
      boolean oldTwice = this.eventTwice;
      this.eventModifiers = e.modifiers();
      this.eventTwice = twice;

      try {
         return this.mouseClicked(e.x(), e.y(), MatInput.button(e));
      } finally {
         this.eventModifiers = old;
         this.eventTwice = oldTwice;
      }
   }

   public boolean mouseClicked(double x, double y, int b) {
      return super.mouseClicked(this.event(x, y, b), this.eventTwice);
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent e) {
      int old = this.eventModifiers;
      this.eventModifiers = e.modifiers();

      try {
         return this.mouseReleased(e.x(), e.y(), MatInput.button(e));
      } finally {
         this.eventModifiers = old;
      }
   }

   public boolean mouseReleased(double x, double y, int b) {
      return super.mouseReleased(this.event(x, y, b));
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
      int old = this.eventModifiers;
      this.eventModifiers = e.modifiers();

      try {
         return this.mouseDragged(e.x(), e.y(), MatInput.button(e), dx, dy);
      } finally {
         this.eventModifiers = old;
      }
   }

   public boolean mouseDragged(double x, double y, int b, double dx, double dy) {
      return super.mouseDragged(this.event(x, y, b), dx, dy);
   }
}
