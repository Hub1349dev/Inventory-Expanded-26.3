package com.example.matinventory.mixin;

import com.example.matinventory.screen.NativePreviewAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativeInventoryMixin {
   @Inject(method = "selectTab", at = @At("RETURN"))
   private void matinventory$tab(CreativeModeTab tab, CallbackInfo ci) {
      ((NativePreviewAccess)(Object)this).matinventory$refreshNativePreview();
   }

   @Inject(method = "getTabY", at = @At("RETURN"), cancellable = true)
   private void matinventory$tabHit(CreativeModeTab tab, CallbackInfoReturnable<Integer> ci) {
      CreativeModeInventoryScreen s = (CreativeModeInventoryScreen)(Object)this;
      if (((NativePreviewAccess)(Object)this).matinventory$nativePreview() != null && ci.getReturnValue() >= 0) {
         ci.setReturnValue(s.height - 42 - ((NativeScreenInvoker)s).matinventory$top());
      }
   }

   @ModifyVariable(method = "extractTabButton", at = @At(value = "STORE", ordinal = 0), index = 9)
   private int matinventory$tabDraw(int y) {
      CreativeModeInventoryScreen s = (CreativeModeInventoryScreen)(Object)this;
      return ((NativePreviewAccess)(Object)this).matinventory$nativePreview() != null && y >= ((NativeScreenInvoker)s).matinventory$top() ? s.height - 46 : y;
   }
}
