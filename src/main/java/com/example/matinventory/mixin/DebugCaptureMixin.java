package com.example.matinventory.mixin;

import com.example.matinventory.debug.MatVisualDebug;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
abstract class DebugCaptureMixin {
   @Inject(method = "render", at = @At("TAIL"))
   private void matinventory$captureCompletedGui(CallbackInfo ci) {
      MatVisualDebug.flushCapture();
   }
}
