package com.example.matinventory.mixin;

import com.example.matinventory.screen.NativePanelFooter;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(GuiGraphicsExtractor.class)
abstract class NativeBackgroundBlitMixin {
   @Inject(
      method = "innerBlit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIFFFFI)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void matinventory$panel(
      RenderPipeline pipeline, Identifier texture, int x0, int x1, int y0, int y1, float u0, float u1, float v0, float v1, int color, CallbackInfo ci
   ) {
      if (NativePanelFooter.cropNative((GuiGraphicsExtractor)(Object)this, pipeline, texture, x0, x1, y0, y1, u0, u1, v0, v1, color)) {
         ci.cancel();
      }
   }
}
