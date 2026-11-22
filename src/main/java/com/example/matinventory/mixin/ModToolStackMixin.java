package com.example.matinventory.mixin;

import com.example.matinventory.capacity.ModToolRules;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemStack.class)
abstract class ModToolStackMixin implements ItemInstance {
   @Override
   public int getMaxStackSize() {
      return ModToolRules.stackLimit((ItemStack)(Object)this, ItemInstance.super.getMaxStackSize());
   }
}
