package com.example.matinventory.mixin;

import com.example.matinventory.screen.NativePreviewAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin(MerchantScreen.class)
abstract class MerchantOriginMixin {
   @ModifyVariable(method = "extractBackground", at = @At("STORE"), index = 6)
   private int matinventory$origin(int top) {
      AbstractContainerScreen<?> s = (AbstractContainerScreen<?>)(Object)this;
      return ((NativePreviewAccess)(Object)this).matinventory$nativePreview() != null ? ((NativeScreenInvoker)s).matinventory$top() : top;
   }

   @ModifyVariable(method = "extractContents", at = @At("STORE"), index = 7)
   private int matinventory$renderOrigin(int top) {
      return this.matinventory$origin(top);
   }

   @ModifyVariable(method = "init", at = @At("STORE"), index = 2)
   private int matinventory$initOrigin(int top) {
      return this.matinventory$origin(top);
   }

   @ModifyVariable(method = "mouseClicked", at = @At("STORE"), index = 4)
   private int matinventory$clickOrigin(int top) {
      return this.matinventory$origin(top);
   }
}
