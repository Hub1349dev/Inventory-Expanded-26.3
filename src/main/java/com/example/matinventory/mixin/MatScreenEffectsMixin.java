package com.example.matinventory.mixin;

import com.example.matinventory.render.NativeWorldScene;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ScreenEffectRenderer.class)
abstract class MatScreenEffectsMixin {
   @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
   private void matinventory$scene(CallbackInfo ci) {
      if (NativeWorldScene.active()) {
         ci.cancel();
      }
   }
}
