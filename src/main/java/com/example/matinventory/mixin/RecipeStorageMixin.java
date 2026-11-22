package com.example.matinventory.mixin;

import com.example.matinventory.capacity.ExtraInventory;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlaceRecipe.class)
abstract class RecipeStorageMixin {
   @Unique
   private static final int MAT_EXTRA = 1000;
   @Shadow
   @Final
   private Inventory inventory;

   @Redirect(
      method = "moveItemToGrid",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Inventory;findSlotMatchingCraftingIngredient(Lnet/minecraft/core/Holder;Lnet/minecraft/world/item/ItemStack;)I"
      )
   )
   private int matinventory$ingredient(Inventory inv, Holder<Item> ingredient, ItemStack target) {
      int index = inv.findSlotMatchingCraftingIngredient(ingredient, target);
      if (index < 0 && ExtraInventory.enabled(inv.player)) {
         ExtraInventory extra = ExtraInventory.of(inv.player);

         for (int i = 0; i < extra.activeSlots(); i++) {
            ItemStack stack = extra.getItem(i);
            if (!stack.isEmpty()
               && stack.is(ingredient)
               && !stack.isDamaged()
               && !stack.isEnchanted()
               && !stack.has(DataComponents.CUSTOM_NAME)
               && (target.isEmpty() || ItemStack.isSameItemSameComponents(target, stack))) {
               return 1000 + i;
            }
         }

         return -1;
      } else {
         return index;
      }
   }

   @Redirect(
      method = "moveItemToGrid",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getItem(I)Lnet/minecraft/world/item/ItemStack;")
   )
   private ItemStack matinventory$get(Inventory inv, int slot) {
      return slot >= 1000 ? ExtraInventory.of(inv.player).getItem(slot - 1000) : inv.getItem(slot);
   }

   @Redirect(
      method = "moveItemToGrid",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;removeItem(II)Lnet/minecraft/world/item/ItemStack;")
   )
   private ItemStack matinventory$remove(Inventory inv, int slot, int count) {
      return slot >= 1000 ? ExtraInventory.of(inv.player).removeItem(slot - 1000, count) : inv.removeItem(slot, count);
   }

   @Redirect(
      method = "moveItemToGrid",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;removeItemNoUpdate(I)Lnet/minecraft/world/item/ItemStack;")
   )
   private ItemStack matinventory$removeWhole(Inventory inv, int slot) {
      return slot >= 1000 ? ExtraInventory.of(inv.player).removeItemNoUpdate(slot - 1000) : inv.removeItemNoUpdate(slot);
   }

   @Inject(method = "getAmountOfFreeSlotsInInventory", at = @At("RETURN"), cancellable = true)
   private void matinventory$freeSlots(CallbackInfoReturnable<Integer> cir) {
      if (ExtraInventory.enabled(this.inventory.player)) {
         int free = cir.getReturnValue();
         ExtraInventory extra = ExtraInventory.of(this.inventory.player);

         for (int i = 0; i < extra.activeSlots(); i++) {
            if (extra.getItem(i).isEmpty()) {
               free++;
            }
         }

         cir.setReturnValue(free);
      }
   }

   @Redirect(
      method = "testClearGrid",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getSlotWithRemainingSpace(Lnet/minecraft/world/item/ItemStack;)I")
   )
   private int matinventory$mergeSpace(Inventory inv, ItemStack incoming) {
      int index = inv.getSlotWithRemainingSpace(incoming);
      if (index < 0 && ExtraInventory.enabled(inv.player)) {
         ExtraInventory extra = ExtraInventory.of(inv.player);

         for (int i = 0; i < extra.activeSlots(); i++) {
            ItemStack stack = extra.getItem(i);
            if (ItemStack.isSameItemSameComponents(incoming, stack) && stack.getCount() < stack.getMaxStackSize()) {
               return 1000 + i;
            }
         }

         return -1;
      } else {
         return index;
      }
   }
}
