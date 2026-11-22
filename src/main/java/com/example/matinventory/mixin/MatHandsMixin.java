package com.example.matinventory.mixin;

import com.example.matinventory.render.NativeWorldScene;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(FirstPersonHandsAndItemsRenderer.class)
abstract class MatHandsMixin {
   @Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
   private void matinventory$steady(CallbackInfo ci) {
      if (NativeWorldScene.active()) {
         ci.cancel();
      }
   }
}
