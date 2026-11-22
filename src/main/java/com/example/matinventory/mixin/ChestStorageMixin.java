package com.example.matinventory.mixin;

import com.example.matinventory.capacity.ExtraInventory;
import com.example.matinventory.capacity.MatArmorSlot;
import com.example.matinventory.capacity.MatContainerSlots;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestMenu.class)
abstract class ChestStorageMixin extends AbstractContainerMenu {
   protected ChestStorageMixin(MenuType<?> type, int id) {
      super(type, id);
   }

   @ModifyArg(
      method = "quickMoveStack",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ChestMenu;moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z", ordinal = 0),
      index = 2
   )
   private int matinventory$storageEnd(int end) {
      return end - 5;
   }

   @Inject(
      method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;I)V",
      at = @At("RETURN")
   )
   private void matinventory$storage(MenuType<?> type, int id, Inventory inventory, Container container, int rows, CallbackInfo ci) {
      ExtraInventory extra = ExtraInventory.of(inventory.player);

      for (int i = 0; i < extra.getContainerSize(); i++) {
         this.addSlot(MatContainerSlots.storage(extra, i));
      }

      EquipmentSlot[] kinds = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

      for (int i = 0; i < 4; i++) {
         this.addSlot(new MatArmorSlot(inventory, kinds[i], 39 - i));
      }

      this.addSlot(new Slot(inventory, 40, -10000, -10000));
   }
}
