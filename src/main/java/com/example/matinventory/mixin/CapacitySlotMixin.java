package com.example.matinventory.mixin;

import com.example.matinventory.capacity.ExtraInventory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
abstract class CapacitySlotMixin {
   @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
   private void matinventory$limit(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
      Slot slot = (Slot)(Object)this;
      if (slot.container instanceof Inventory inventory && ExtraInventory.enabled(inventory.player)) {
         ExtraInventory extra = ExtraInventory.of(inventory.player);
         int index = slot.getContainerSlot();
         if (index >= 9 && index < 36 ? !extra.acceptsStorage(stack, slot.getItem()) : !extra.accepts(stack, slot.getItem())) {
            cir.setReturnValue(false);
         }
      }
   }
}
