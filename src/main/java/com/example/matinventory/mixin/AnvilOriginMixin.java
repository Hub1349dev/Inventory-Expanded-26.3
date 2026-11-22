package com.example.matinventory.mixin;

import com.example.matinventory.screen.NativePreviewAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin(AnvilScreen.class)
abstract class AnvilOriginMixin {
   @ModifyVariable(method = "subInit", at = @At("STORE"), index = 2)
   private int matinventory$origin(int top) {
      AbstractContainerScreen<?> s = (AbstractContainerScreen<?>)(Object)this;
      return ((NativePreviewAccess)(Object)this).matinventory$nativePreview() != null ? ((NativeScreenInvoker)s).matinventory$top() : top;
   }
}
