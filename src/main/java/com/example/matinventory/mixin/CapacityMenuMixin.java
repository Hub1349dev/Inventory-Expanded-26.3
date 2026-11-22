package com.example.matinventory.mixin;

import com.example.matinventory.capacity.ExtraInventory;
import com.example.matinventory.capacity.ToolKinds;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
abstract class CapacityMenuMixin {
   @Unique
   private String matinventory$cursorBefore;
   @Unique
   private boolean matinventory$sourceOwned;
   @Unique
   private String matinventory$sourceKind;
   @Unique
   private int matinventory$sourceCountBefore;
   @Unique
   private boolean matinventory$mergedExpanded;

   @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
   private void matinventory$before(int index, int button, ContainerInput type, Player player, CallbackInfo ci) {
      if (ExtraInventory.enabled(player)) {
         AbstractContainerMenu menu = (AbstractContainerMenu)(Object)this;
         ExtraInventory extra = ExtraInventory.of(player);
         this.matinventory$cursorBefore = ToolKinds.kind(menu.getCarried());
         Slot source = index >= 0 && index < menu.slots.size() ? menu.getSlot(index) : null;
         this.matinventory$sourceOwned = source != null && source.hasItem() && (source.container == player.getInventory() || source.container == extra);
         this.matinventory$sourceKind = source == null ? null : ToolKinds.kind(source.getItem());
         this.matinventory$sourceCountBefore = extra.toolCount(this.matinventory$sourceKind);
         if (type == ContainerInput.SWAP
            && source != null
            && (button >= 0 && button < 9 || button == 40)
            && !extra.accepts(source.getItem(), player.getInventory().getItem(button))) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "clicked", at = @At("RETURN"))
   private void matinventory$after(int index, int button, ContainerInput type, Player player, CallbackInfo ci) {
      if (ExtraInventory.enabled(player)) {
         AbstractContainerMenu menu = (AbstractContainerMenu)(Object)this;
         ExtraInventory extra = ExtraInventory.of(player);
         String kind = ToolKinds.kind(menu.getCarried());
         if (kind == null) {
            extra.ownedCursorKind = null;
         } else if ((
               !this.matinventory$sourceOwned || !kind.equals(this.matinventory$sourceKind) || extra.toolCount(kind) >= this.matinventory$sourceCountBefore
            )
            && (!kind.equals(this.matinventory$cursorBefore) || !kind.equals(extra.ownedCursorKind))) {
            extra.ownedCursorKind = null;
         } else {
            extra.ownedCursorKind = kind;
         }
      }
   }

   @Unique
   private Inventory matinventory$storageInventory(int first, int end) {
      if (first >= 0 && end > first) {
         AbstractContainerMenu menu = (AbstractContainerMenu)(Object)this;
         Inventory inventory = null;
         boolean storage = false;

         for (int i = first; i < end; i++) {
            if (!(menu.getSlot(i).container instanceof Inventory candidate)) {
               return null;
            }

            if (inventory != null && inventory != candidate) {
               return null;
            }

            inventory = candidate;
            int slot = menu.getSlot(i).getContainerSlot();
            if (slot >= 9 && slot < 36) {
               storage = true;
            }
         }

         return storage && inventory != null && ExtraInventory.enabled(inventory.player) ? inventory : null;
      } else {
         return null;
      }
   }

   @Inject(method = "moveItemStackTo", at = @At("HEAD"), cancellable = true)
   private void matinventory$mergeBeforeEmpty(ItemStack stack, int first, int end, boolean reverse, CallbackInfoReturnable<Boolean> cir) {
      this.matinventory$mergedExpanded = false;
      Inventory inventory = this.matinventory$storageInventory(first, end);
      if (inventory != null && !stack.isEmpty()) {
         this.matinventory$mergedExpanded = ExtraInventory.of(inventory.player).mergeExisting(stack);
         if (stack.isEmpty()) {
            cir.setReturnValue(true);
         }
      }
   }

   @Inject(method = "moveItemStackTo", at = @At("RETURN"), cancellable = true)
   private void matinventory$overflow(ItemStack stack, int first, int end, boolean reverse, CallbackInfoReturnable<Boolean> cir) {
      if (this.matinventory$mergedExpanded) {
         cir.setReturnValue(true);
      }

      if (!stack.isEmpty()) {
         Inventory inventory = this.matinventory$storageInventory(first, end);
         if (inventory != null && ExtraInventory.of(inventory.player).insert(stack, false)) {
            cir.setReturnValue(true);
         }
      }
   }
}
