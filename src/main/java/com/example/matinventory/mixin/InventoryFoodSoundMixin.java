package com.example.matinventory.mixin;

import com.example.matinventory.interaction.InventoryEating;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FoodProperties.class)
abstract class InventoryFoodSoundMixin {
   @WrapWithCondition(
      method = "onConsume",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"
      )
   )
   private boolean matinventory$customEatingAudio(
      Level level, Entity excluded, double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch
   ) {
      return !InventoryEating.silentUse();
   }
}
