package com.example.matinventory.mixin;

import com.example.matinventory.render.NativeWorldScene;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.GameRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
abstract class MatGameSceneMixin {
   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   @Final
   private Camera mainCamera;
   @Shadow
   @Final
   private GameRenderState gameRenderState;

   @Shadow
   private void extractCamera(DeltaTracker deltaTracker, float partialTicks) {
      throw new AssertionError();
   }

   @Inject(method = "extract", at = @At("HEAD"))
   private void matinventory$begin(CallbackInfo ci) {
      NativeWorldScene.beginFrame();
   }

   @WrapOperation(
      method = "extract",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;extractCamera(Lnet/minecraft/client/DeltaTracker;F)V")
   )
   private void matinventory$deferCamera(GameRenderer renderer, DeltaTracker delta, float partial, Operation<Void> original) {
      if (!NativeWorldScene.deferred()) {
         original.call(renderer, delta, partial);
      }
   }

   @WrapOperation(
      method = "extract",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;extract(Lnet/minecraft/client/DeltaTracker;Lnet/minecraft/client/Camera;F)V"
      )
   )
   private void matinventory$deferLevel(LevelExtractor extractor, DeltaTracker delta, Camera camera, float partial, Operation<Void> original) {
      if (!NativeWorldScene.deferred()) {
         original.call(extractor, delta, camera, partial);
      }
   }

   @WrapOperation(
      method = "extract",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V")
   )
   private void matinventory$extractLevelAfterScene(Gui gui, DeltaTracker delta, boolean level, boolean loaded, Operation<Void> original) {
      original.call(gui, delta, level, loaded);
      if (NativeWorldScene.deferred() && this.gameRenderState.shouldRenderLevel) {
         float partial = delta.getGameTimeDeltaPartialTick(false);
         NativeWorldScene.camera(this.mainCamera);
         this.extractCamera(delta, partial);
         this.minecraft.levelExtractor.extract(delta, this.mainCamera, partial);
      }

      NativeWorldScene.finishExtraction();
   }

   @Inject(method = "render", at = @At("TAIL"))
   private void matinventory$restore(CallbackInfo ci) {
      NativeWorldScene.restoreCamera(this.mainCamera);
   }

   @Inject(method = {"bobHurt", "bobView", "renderItemInHand"}, at = @At("HEAD"), cancellable = true)
   private void matinventory$steady(CallbackInfo ci) {
      if (NativeWorldScene.active()) {
         ci.cancel();
      }
   }

   @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
   private void matinventory$outline(CallbackInfoReturnable<Boolean> ci) {
      if (NativeWorldScene.active()) {
         ci.setReturnValue(false);
      }
   }
}
