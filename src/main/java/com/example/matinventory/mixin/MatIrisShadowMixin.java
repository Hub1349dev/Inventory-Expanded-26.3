package com.example.matinventory.mixin;

import com.example.matinventory.render.NativeWorldScene;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Pseudo
@Mixin(targets = "net/irisshaders/iris/shadows/ShadowRenderer", remap = false)
abstract class MatIrisShadowMixin {
   @Inject(method = "renderBlockEntities", at = @At("RETURN"))
   private void matinventory$shadow(
      CallbackInfoReturnable<Integer> ci, @Local(argsOnly = true) SubmitNodeStorage storage, @Local(argsOnly = true) Camera camera
   ) {
      NativeWorldScene.draw(storage, camera.position());
   }
}
