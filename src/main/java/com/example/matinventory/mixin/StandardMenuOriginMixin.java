package com.example.matinventory.mixin;

import com.example.matinventory.screen.NativePreviewAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractMountInventoryScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.CrafterScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin(
   {
         BrewingStandScreen.class,
         CrafterScreen.class,
         DispenserScreen.class,
         GrindstoneScreen.class,
         HopperScreen.class,
         AbstractMountInventoryScreen.class,
         ShulkerBoxScreen.class
   }
)
abstract class StandardMenuOriginMixin {
   @ModifyVariable(method = "extractBackground", at = @At("STORE"), index = 6)
   private int matinventory$origin(int top) {
      AbstractContainerScreen<?> s = (AbstractContainerScreen<?>)(Object)this;
      return ((NativePreviewAccess)(Object)this).matinventory$nativePreview() != null ? ((NativeScreenInvoker)s).matinventory$top() : top;
   }
}
