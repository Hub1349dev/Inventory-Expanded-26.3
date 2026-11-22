package com.example.matinventory.interaction;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class InventoryCrafting {
   public static Slot output(AbstractContainerMenu menu) {
      return menu instanceof CraftOutputAccess access ? access.matinventory$craftOutput() : null;
   }

   public static boolean fits(AbstractContainerMenu menu) {
      Slot slot = output(menu);
      ItemStack recipe = menu.getSlot(0).getItem();
      if (slot != null && !recipe.isEmpty()) {
         ItemStack pending = slot.getItem();
         return (pending.isEmpty() || ItemStack.isSameItemSameComponents(pending, recipe))
            && pending.getCount() + recipe.getCount() <= slot.getMaxStackSize(recipe);
      } else {
         return false;
      }
   }

   public static boolean craft(ServerPlayer player, CraftRequest request) {
      AbstractContainerMenu menu = player.containerMenu;
      if (player.isAlive()
         && !player.isSpectator()
         && !player.isCreative()
         && menu.containerId == request.menuId()
         && (menu == player.inventoryMenu || menu instanceof CraftingMenu)
         && menu.stillValid(player)
         && menu.getCarried().isEmpty()
         && ItemStack.matches(menu.getSlot(0).getItem(), request.expected())) {
         Slot output = output(menu);
         boolean crafted = false;

         for (int i = 0; i < (request.all() ? 64 : 1) && fits(menu); i++) {
            Slot result = menu.getSlot(0);
            if (!result.mayPickup(player)) {
               break;
            }

            menu.clicked(0, 0, ContainerInput.PICKUP, player);
            ItemStack stack = menu.getCarried();
            if (stack.isEmpty()) {
               break;
            }

            ItemStack pending = output.getItem();
            if (pending.isEmpty()) {
               output.set(stack);
            } else {
               pending.grow(stack.getCount());
               output.setChanged();
            }

            menu.setCarried(ItemStack.EMPTY);
            crafted = true;
         }

         menu.broadcastChanges();
         return crafted;
      } else {
         return false;
      }
   }

   private InventoryCrafting() {
   }
}
