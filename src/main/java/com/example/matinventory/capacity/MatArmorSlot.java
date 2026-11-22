package com.example.matinventory.capacity;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class MatArmorSlot extends Slot {
   private final Inventory inventory;
   private final EquipmentSlot equipment;

   public MatArmorSlot(Inventory inventory, EquipmentSlot equipment, int index) {
      super(inventory, index, -10000, -10000);
      this.inventory = inventory;
      this.equipment = equipment;
   }

   @Override
   public int getMaxStackSize() {
      return 1;
   }

   @Override
   public boolean mayPlace(ItemStack stack) {
      return this.inventory.player.isEquippableInSlot(stack, this.equipment);
   }

   @Override
   public boolean isActive() {
      return this.inventory.player.canUseSlot(this.equipment);
   }

   @Override
   public boolean mayPickup(Player player) {
      return (player.isCreative() || !EnchantmentHelper.has(this.getItem(), EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) && super.mayPickup(player);
   }

   @Override
   public void setByPlayer(ItemStack next, ItemStack old) {
      this.inventory.player.onEquipItem(this.equipment, old, next);
      super.setByPlayer(next, old);
   }
}
