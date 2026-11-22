package com.example.matinventory.mixin;

import com.example.matinventory.capacity.BlockKinds;
import com.example.matinventory.capacity.ConsumableKinds;
import com.example.matinventory.capacity.EquipmentKinds;
import com.example.matinventory.capacity.ExtraInventory;
import com.example.matinventory.capacity.MatArmorSlot;
import com.example.matinventory.capacity.MaterialKinds;
import com.example.matinventory.capacity.NativeExtraMenuAccess;
import com.example.matinventory.interaction.CraftOutputAccess;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({InventoryMenu.class, CraftingMenu.class})
abstract class ExpandedMenuMixin extends AbstractContainerMenu implements CraftOutputAccess {
   @Unique
   private Slot matinventory$pendingOutput;

   @Override
   public Slot matinventory$craftOutput() {
      return this.matinventory$pendingOutput;
   }

   protected ExpandedMenuMixin(MenuType<?> type, int id) {
      super(type, id);
   }

   @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
   private void matinventory$blockToHotbar(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
      if (index >= 0 && index < this.slots.size()) {
         Slot slot = this.getSlot(index);
         if (slot == this.matinventory$pendingOutput) {
            ItemStack stack = slot.getItem();
            ItemStack before = stack.copy();
            int start = (Object)this instanceof InventoryMenu ? 9 : 10;
            if (!this.moveItemStackTo(stack, start, start + 36, false)) {
               cir.setReturnValue(ItemStack.EMPTY);
            } else {
               if (stack.isEmpty()) {
                  slot.setByPlayer(ItemStack.EMPTY);
               } else {
                  slot.setChanged();
               }

               cir.setReturnValue(before);
            }
         } else {
            boolean armor = EquipmentKinds.kind(slot.getItem()) != null && (slot.container instanceof Inventory || slot.container instanceof ExtraInventory);
            boolean offhand = (Object)this instanceof CraftingMenu && slot.container instanceof Inventory && slot.getContainerSlot() == 40;
            if (ExtraInventory.enabled(player)
               && (
                  offhand
                     || armor
                     || slot.container instanceof ExtraInventory
                        && (BlockKinds.isBlock(slot.getItem()) || ConsumableKinds.isConsumable(slot.getItem()) || MaterialKinds.isMaterial(slot.getItem()))
               )) {
               if (!slot.mayPickup(player)) {
                  cir.setReturnValue(ItemStack.EMPTY);
               } else {
                  ItemStack stack = slot.getItem();
                  ItemStack before = stack.copy();
                  int start = (Object)this instanceof InventoryMenu ? 36 : 37;
                  int end = start + 9;
                  boolean inHotbar = slot.container instanceof Inventory && slot.getContainerSlot() < 9;
                  if (inHotbar) {
                     start = (Object)this instanceof InventoryMenu ? 9 : 10;
                     end = start + 27;
                  }

                  if (!this.moveItemStackTo(stack, start, end, false)) {
                     cir.setReturnValue(ItemStack.EMPTY);
                  } else {
                     if (stack.isEmpty()) {
                        slot.setByPlayer(ItemStack.EMPTY);
                     } else {
                        slot.setChanged();
                     }

                     slot.onTake(player, stack);
                     cir.setReturnValue(before);
                  }
               }
            }
         }
      }
   }

   @Inject(
      method = {
            "<init>(Lnet/minecraft/world/entity/player/Inventory;ZLnet/minecraft/world/entity/player/Player;)V",
            "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V"
      },
      at = @At("RETURN")
   )
   private void matinventory$slots(CallbackInfo ci) {
      if (!this.slots.stream().anyMatch(s -> s.container instanceof ExtraInventory)) {
         Inventory inventory = this.slots.stream().map(s -> s.container).filter(c -> c instanceof Inventory).map(c -> (Inventory)c).findFirst().orElse(null);
         if (inventory != null) {
            final ExtraInventory extra = ExtraInventory.of(inventory.player);

            for (int i = 0; i < extra.getContainerSize(); i++) {
               this.addSlot(
                  new Slot(extra, i, -10000, -10000) {
                     @Override
                     public boolean mayPlace(ItemStack stack) {
                        return extra.canPlaceItem(this.getContainerSlot(), stack);
                     }

                     @Override
                     public boolean isActive() {
                        return ExtraInventory.enabled(extra.owner)
                           && (
                              !extra.owner.hasInfiniteMaterials()
                                 || extra.owner.containerMenu instanceof CraftingMenu
                                 || extra.owner.containerMenu instanceof ChestMenu
                                 || extra.owner.containerMenu instanceof NativeExtraMenuAccess access && access.matinventory$extraEnd() > 0
                           )
                           && (this.getContainerSlot() < extra.activeSlots() || extra.equipmentReserve(this.getContainerSlot()) || this.hasItem());
                     }
                  }
               );
            }

            if ((Object)this instanceof CraftingMenu && ExtraInventory.enabled(inventory.player)) {
               this.addSlot(new Slot(inventory, 40, -10000, -10000));
               EquipmentSlot[] kinds = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

               for (int i = 0; i < 4; i++) {
                  this.addSlot(new MatArmorSlot(inventory, kinds[i], 39 - i));
               }
            }

            this.matinventory$pendingOutput = this.addSlot(new Slot(new SimpleContainer(1), 0, -10000, -10000) {
               @Override
               public boolean mayPlace(ItemStack stack) {
                  return false;
               }
            });
         }
      }
   }

   @Inject(method = "removed", at = @At("HEAD"))
   private void matinventory$returnOutput(Player player, CallbackInfo ci) {
      if (!player.level().isClientSide() && this.matinventory$pendingOutput != null) {
         this.clearContainer(player, this.matinventory$pendingOutput.container);
      }
   }
}
