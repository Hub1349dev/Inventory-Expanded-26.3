package com.example.matinventory.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public final class CraftMotion {
   public static final float DURATION = 1.1F;
   public static final float COMMIT_AT = 0.91F;
   private ItemStack[] expected = new ItemStack[0];
   public ItemStack result = ItemStack.EMPTY;
   public float time = 1.1F;
   public boolean all;
   public boolean store;
   public boolean committed;

   public boolean active() {
      return this.time < 1.1F;
   }

   public void start(AbstractContainerMenu menu, int gridSize, boolean all, boolean store) {
      this.expected = new ItemStack[gridSize * gridSize + 1];

      for (int i = 0; i < this.expected.length; i++) {
         this.expected[i] = menu.getSlot(i).getItem().copy();
      }

      this.result = this.expected[0].copy();
      this.time = 0.0F;
      this.committed = false;
      this.all = all;
      this.store = store;
   }

   public boolean matches(AbstractContainerMenu menu) {
      for (int i = 0; i < this.expected.length; i++) {
         if (!ItemStack.matches(this.expected[i], menu.getSlot(i).getItem())) {
            return false;
         }
      }

      return true;
   }

   public void advance(float dt) {
      if (this.active()) {
         this.time = Math.min(1.1F, this.time + dt);
      }
   }

   public void cancel() {
      this.time = 1.1F;
      this.result = ItemStack.EMPTY;
   }

   public float convergence() {
      return smooth(Math.clamp(this.time / 0.42F, 0.0F, 1.0F));
   }

   public float growth() {
      return smooth(Math.clamp((this.time - 0.4F) / 0.45F, 0.0F, 1.0F));
   }

   private static float smooth(float t) {
      return t * t * (3.0F - 2.0F * t);
   }
}
