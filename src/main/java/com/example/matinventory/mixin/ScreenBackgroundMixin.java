package com.example.matinventory.mixin;

import com.example.matinventory.screen.NativeMenuPreview;
import com.example.matinventory.screen.NativePreviewAccess;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(Screen.class)
abstract class ScreenBackgroundMixin {
   @WrapOperation(
      method = "extractRenderStateWithTooltipAndSubtitles",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screens/Screen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"
      )
   )
   private void matinventory$nativeBackground(Screen screen, GuiGraphicsExtractor g, int x, int y, float partial, Operation<Void> original) {
      NativeMenuPreview preview = screen instanceof NativePreviewAccess access ? access.matinventory$nativePreview() : null;
      if (preview == null) {
         original.call(screen, g, x, y, partial);
      } else {
         preview.background(g, x, y, () -> original.call(screen, g, x, y, partial));
      }
   }
}
