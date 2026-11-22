package com.example.matinventory.mixin;

import com.example.matinventory.interaction.InventoryEating;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Consumable.class)
abstract class InventoryConsumeMixin {
   @Inject(method = "emitParticlesAndSounds", at = @At("HEAD"), cancellable = true)
   private void matinventory$quietInventoryUse(RandomSource random, LivingEntity player, ItemStack stack, int count, CallbackInfo ci) {
      if (InventoryEating.silentUse(player)) {
         ci.cancel();
      }
   }
}
