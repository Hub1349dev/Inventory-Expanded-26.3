package com.example.matinventory.mixin;

import com.example.matinventory.screen.NativePreviewAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin(EnchantmentScreen.class)
abstract class IndependentMenuOriginMixin {
   @ModifyVariable(method = "extractBackground", at = @At("STORE"), index = 6)
   private int matinventory$origin(int top) {
      AbstractContainerScreen<?> s = (AbstractContainerScreen<?>)(Object)this;
      return ((NativePreviewAccess)(Object)this).matinventory$nativePreview() != null ? ((NativeScreenInvoker)s).matinventory$top() : top;
   }

   @ModifyVariable(method = "mouseClicked", at = @At("STORE"), index = 4)
   private int matinventory$clickOrigin(int top) {
      return this.matinventory$origin(top);
   }
}
