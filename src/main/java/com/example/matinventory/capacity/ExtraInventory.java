package com.example.matinventory.capacity;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ExtraInventory extends SimpleContainer {
   public static boolean clientSupported;
   public static int clientStorageSlots = 144;
   public final Player owner;
   public String ownedCursorKind;
   public boolean dirty;

   public ExtraInventory(Player owner) {
      super(234);
      this.owner = owner;
   }

   @Override
   public void setChanged() {
      super.setChanged();
      this.dirty = true;
      if (this.owner != null) {
         this.owner.getInventory().setChanged();
      }
   }

   public static ExtraInventory of(Player player) {
      return ((ExtraInventoryAccess)player).matinventory$extra();
   }

   public static boolean enabled(Player player) {
      return !player.level().isClientSide() || clientSupported;
   }

   public int activeSlots() {
      return (this.owner.level().isClientSide() ? clientStorageSlots : CapacityConfig.storageSlots) - 27;
   }

   public int toolCount(String kind) {
      if (kind == null) {
         return 0;
      }

      int count = 0;

      for (int i = 0; i < this.owner.getInventory().getContainerSize(); i++) {
         ItemStack stack = this.owner.getInventory().getItem(i);
         if (kind.equals(ToolKinds.kind(stack))) {
            count += stack.getCount();
         }
      }

      for (ItemStack stack : this.getItems()) {
         if (kind.equals(ToolKinds.kind(stack))) {
            count += stack.getCount();
         }
      }

      if (this.owner.containerMenu != null) {
         ItemStack cursor = this.owner.containerMenu.getCarried();
         if (this.ownedCursor(cursor) && kind.equals(ToolKinds.kind(cursor))) {
            count += cursor.getCount();
         }
      }

      return count;
   }

   public boolean ownsReference(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }

      for (int i = 0; i < this.owner.getInventory().getContainerSize(); i++) {
         if (this.owner.getInventory().getItem(i) == stack) {
            return true;
         }
      }

      return this.getItems().stream().anyMatch(s -> s == stack);
   }

   public boolean ownedCursor(ItemStack stack) {
      return this.owner.containerMenu != null && this.owner.containerMenu.getCarried() == stack && Objects.equals(this.ownedCursorKind, ToolKinds.kind(stack));
   }

   public boolean accepts(ItemStack incoming, ItemStack replaced) {
      String kind = ToolKinds.kind(incoming);
      if (kind == null || this.owner.hasInfiniteMaterials() || this.ownsReference(incoming) || this.ownedCursor(incoming)) {
         return true;
      } else {
         return "vehicle".equals(kind)
            ? this.toolCount(kind) < 3
            : this.toolCount(kind) - (kind.equals(ToolKinds.kind(replaced)) ? replaced.getCount() : 0) < ToolKinds.limit(kind);
      }
   }

   public Set<Item> blockTypes(ItemStack excluded) {
      HashSet<Item> types = new HashSet<>();

      for (int i = 9; i < 36; i++) {
         ItemStack s = this.owner.getInventory().getItem(i);
         if (s != excluded && BlockKinds.isBlock(s)) {
            types.add(s.getItem());
         }
      }

      for (ItemStack s : this.getItems()) {
         if (s != excluded && BlockKinds.isBlock(s)) {
            types.add(s.getItem());
         }
      }

      return types;
   }

   public boolean acceptsStorage(ItemStack incoming, ItemStack replaced) {
      if (!this.accepts(incoming, replaced)) {
         return false;
      } else if (!this.owner.hasInfiniteMaterials() && MaterialKinds.isMaterial(incoming)) {
         Set<String> types = this.materialTypes(replaced);
         return types.contains(MaterialKinds.key(incoming)) || types.size() < 25;
      } else if (!this.owner.hasInfiniteMaterials() && ConsumableKinds.isConsumable(incoming)) {
         Set<String> types = this.consumableTypes(replaced);
         return types.contains(ConsumableKinds.key(incoming)) || types.size() < 14;
      } else if (!this.owner.hasInfiniteMaterials() && BlockKinds.isBlock(incoming)) {
         Set<Item> types = this.blockTypes(replaced);
         return types.contains(incoming.getItem()) || types.size() < 16;
      } else {
         return true;
      }
   }

   public Set<String> consumableTypes(ItemStack excluded) {
      HashSet<String> types = new HashSet<>();

      for (int i = 9; i < 36; i++) {
         ItemStack s = this.owner.getInventory().getItem(i);
         if (s != excluded && ConsumableKinds.isConsumable(s)) {
            types.add(ConsumableKinds.key(s));
         }
      }

      for (ItemStack s : this.getItems()) {
         if (s != excluded && ConsumableKinds.isConsumable(s)) {
            types.add(ConsumableKinds.key(s));
         }
      }

      return types;
   }

   public Set<String> materialTypes(ItemStack excluded) {
      HashSet<String> types = new HashSet<>();

      for (int i = 9; i < 36; i++) {
         ItemStack stack = this.owner.getInventory().getItem(i);
         if (stack != excluded && MaterialKinds.isMaterial(stack)) {
            types.add(MaterialKinds.key(stack));
         }
      }

      for (ItemStack stack : this.getItems()) {
         if (stack != excluded && MaterialKinds.isMaterial(stack)) {
            types.add(MaterialKinds.key(stack));
         }
      }

      return types;
   }

   public boolean equipmentReserve(int slot) {
      return slot >= 229 && slot < this.getContainerSize();
   }

   @Override
   public boolean canPlaceItem(int slot, ItemStack stack) {
      if (!enabled(this.owner)) {
         return false;
      } else {
         return this.equipmentReserve(slot)
            ? stack.getCount() == 1 && EquipmentKinds.playerSlot(stack) == (slot == 233 ? 38 : 39 - (slot - 229))
            : slot < this.activeSlots() && this.acceptsStorage(stack, this.getItem(slot));
      }
   }

   public int allowance(ItemStack stack) {
      String kind = ToolKinds.kind(stack);
      return kind != null && !this.ownsReference(stack) && !this.ownedCursor(stack)
         ? Math.min(stack.getCount(), Math.max(0, ToolKinds.limit(kind) - this.toolCount(kind)))
         : stack.getCount();
   }

   public boolean insert(ItemStack stack, boolean includeVanilla) {
      return this.insert(stack, includeVanilla, false);
   }

   public boolean mergeExisting(ItemStack stack) {
      return this.insert(stack, false, true);
   }

   private boolean insert(ItemStack stack, boolean includeVanilla, boolean mergeOnly) {
      int allowed = this.allowance(stack);
      if (allowed <= 0) {
         return false;
      }

      ItemStack pending = stack.copyWithCount(allowed);

      for (int pass = 0; pass < (mergeOnly ? 1 : 2) && !pending.isEmpty(); pass++) {
         if (includeVanilla) {
            for (int i = 0; i < 36 && !pending.isEmpty(); i++) {
               ItemStack target = this.owner.getInventory().getItem(i);
               if (target != stack
                  && (
                     i < 9
                        || !BlockKinds.isBlock(pending) && !ConsumableKinds.isConsumable(pending) && !MaterialKinds.isMaterial(pending)
                        || this.acceptsStorage(pending, target)
                  )) {
                  int moved = this.move(pending, target, pass);
                  if (moved > 0 && target.isEmpty()) {
                     this.owner.getInventory().setItem(i, pending.copyWithCount(moved));
                  }

                  if (moved > 0) {
                     pending.shrink(moved);
                  }
               }
            }
         }

         for (int i = 0; i < this.activeSlots() && !pending.isEmpty(); i++) {
            ItemStack target = this.getItem(i);
            if (target != stack
               && (
                  !BlockKinds.isBlock(pending) && !ConsumableKinds.isConsumable(pending) && !MaterialKinds.isMaterial(pending)
                     || this.acceptsStorage(pending, target)
               )) {
               int moved = this.move(pending, target, pass);
               if (moved > 0 && target.isEmpty()) {
                  this.setItem(i, pending.copyWithCount(moved));
               }

               if (moved > 0) {
                  pending.shrink(moved);
               }
            }
         }
      }

      int moved = allowed - pending.getCount();
      stack.shrink(moved);
      if (moved > 0) {
         this.setChanged();
         this.owner.getInventory().setChanged();
      }

      return moved > 0;
   }

   private int move(ItemStack from, ItemStack target, int pass) {
      if (pass == 0) {
         if (!target.isEmpty() && ItemStack.isSameItemSameComponents(from, target)) {
            int moved = Math.min(from.getCount(), Math.max(0, target.getMaxStackSize() - target.getCount()));
            target.grow(moved);
            return moved;
         } else {
            return 0;
         }
      } else {
         return target.isEmpty() ? Math.min(from.getCount(), from.getMaxStackSize()) : 0;
      }
   }

   public void save(ValueOutput out) {
      out.store("matinventory:extra", ItemStack.OPTIONAL_CODEC.listOf(), List.copyOf(this.getItems()));
   }

   public void load(ValueInput in) {
      this.clearContent();
      in.read("matinventory:extra", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(items -> {
         for (int i = 0; i < Math.min(items.size(), this.getContainerSize()); i++) {
            this.setItem(i, items.get(i));
         }
      });
   }

   public void copyFrom(ExtraInventory other) {
      for (int i = 0; i < this.getContainerSize(); i++) {
         this.setItem(i, other.getItem(i).copy());
      }
   }

   public void dropAll() {
      for (int i = 0; i < this.getContainerSize(); i++) {
         ItemStack stack = this.getItem(i);
         if (!stack.isEmpty()) {
            if (!this.owner.level().isClientSide()) {
               ItemEntity drop = this.owner.createItemStackToDrop(stack, true, false);
               if (drop != null) {
                  this.owner.level().addFreshEntity(drop);
               }
            }

            this.setItem(i, ItemStack.EMPTY);
         }
      }
   }
}
