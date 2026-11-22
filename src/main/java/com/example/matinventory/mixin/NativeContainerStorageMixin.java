package com.example.matinventory.mixin;

import com.example.matinventory.capacity.ExtraInventory;
import com.example.matinventory.capacity.MatArmorSlot;
import com.example.matinventory.capacity.MatContainerSlots;
import com.example.matinventory.capacity.NativeExtraMenuAccess;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
abstract class NativeContainerStorageMixin implements NativeExtraMenuAccess {
   @Unique
   private int matinventory$extraEnd;
   @Unique
   private int matinventory$extraStart;
   @Unique
   private Inventory matinventory$owner;

   @Shadow
   protected abstract Slot addSlot(Slot var1);

   @Shadow
   protected abstract boolean moveItemStackTo(ItemStack var1, int var2, int var3, boolean var4);

   @Override
   public int matinventory$extraEnd() {
      return this.matinventory$extraEnd;
   }

   @Inject(method = "addSlot", at = @At("RETURN"))
   private void matinventory$append(Slot slot, CallbackInfoReturnable<Slot> ci) {
      AbstractContainerMenu menu = (AbstractContainerMenu)(Object)this;
      if (this.matinventory$extraEnd > 0) {
         if (slot.container != this.matinventory$owner && !(slot.container instanceof ExtraInventory) && slot.index >= this.matinventory$extraStart) {
            menu.slots.remove(slot.index);
            menu.slots.add(this.matinventory$extraStart++, slot);
            this.matinventory$extraEnd++;
            int i = 0;

            while (i < menu.slots.size()) {
               menu.slots.get(i).index = i++;
            }
         }
      } else if (this.matinventory$extraEnd == 0
         && slot.container instanceof Inventory inv
         && slot.getContainerSlot() == 8
         && !(menu instanceof InventoryMenu)
         && !(menu instanceof CraftingMenu)
         && !(menu instanceof ChestMenu)
         && !menu.getClass().getName().contains("CreativeModeInventoryScreen")) {
         this.matinventory$extraStart = menu.slots.size();
         this.matinventory$owner = inv;
         this.matinventory$extraEnd = -1;
         ExtraInventory extra = ExtraInventory.of(inv.player);

         for (int i = 0; i < extra.getContainerSize(); i++) {
            this.addSlot(MatContainerSlots.storage(extra, i));
         }

         this.matinventory$extraEnd = menu.slots.size();
         EquipmentSlot[] kinds = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

         for (int i = 0; i < 4; i++) {
            this.addSlot(new MatArmorSlot(inv, kinds[i], 39 - i));
         }

         this.addSlot(new Slot(inv, 40, -10000, -10000));
      }
   }

   @ModifyVariable(method = "moveItemStackTo", at = @At("HEAD"), argsOnly = true, ordinal = 1)
   private int matinventory$avoidEquipment(int end) {
      AbstractContainerMenu menu = (AbstractContainerMenu)(Object)this;
      return this.matinventory$extraEnd > 0 && end == menu.slots.size() ? this.matinventory$extraEnd : end;
   }

   @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
   private void matinventory$shiftExtra(int index, int button, ContainerInput type, Player player, CallbackInfo ci) {
      AbstractContainerMenu menu = (AbstractContainerMenu)(Object)this;
      if (this.matinventory$extraEnd > 0 && type == ContainerInput.QUICK_MOVE && index >= 0 && index < menu.slots.size()) {
         Slot slot = menu.getSlot(index);
         if (slot.container instanceof ExtraInventory && slot.hasItem() && slot.mayPickup(player)) {
            ItemStack stack = slot.getItem();
            ItemStack old = stack.copy();
            boolean moved = false;

            for (Slot destination : menu.slots) {
               if (!(destination.container instanceof Inventory) && !(destination.container instanceof ExtraInventory) && destination.mayPlace(stack)) {
                  moved |= this.moveItemStackTo(stack, destination.index, destination.index + 1, false);
               }
            }

            if (moved) {
               if (stack.isEmpty()) {
                  slot.setByPlayer(ItemStack.EMPTY);
               } else {
                  slot.setChanged();
               }

               slot.onTake(player, old.copyWithCount(old.getCount() - stack.getCount()));
            }

            ci.cancel();
         }
      }
   }
}
