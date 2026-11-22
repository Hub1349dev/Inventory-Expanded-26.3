package com.example.matinventory.mixin;

import com.example.matinventory.capacity.ExtraInventory;
import com.example.matinventory.capacity.ExtraInventoryAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
abstract class PlayerStorageMixin implements ExtraInventoryAccess {
   @Unique
   private ExtraInventory matinventory$storage;

   @Override
   public ExtraInventory matinventory$extra() {
      if (this.matinventory$storage == null) {
         this.matinventory$storage = new ExtraInventory((Player)(Object)this);
      }

      return this.matinventory$storage;
   }

   @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
   private void matinventory$save(ValueOutput out, CallbackInfo ci) {
      this.matinventory$extra().save(out);
   }

   @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
   private void matinventory$load(ValueInput in, CallbackInfo ci) {
      this.matinventory$extra().load(in);
   }
}
