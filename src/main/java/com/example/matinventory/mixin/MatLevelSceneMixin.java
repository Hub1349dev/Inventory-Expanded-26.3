package com.example.matinventory.mixin;

import com.example.matinventory.render.NativeWorldScene;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(LevelRenderer.class)
abstract class MatLevelSceneMixin {
   @Inject(method = "submitBlockEntities", at = @At("TAIL"))
   private void matinventory$scene(PoseStack pose, LevelRenderState level, SubmitNodeCollector collector, CallbackInfo ci) {
      if (collector instanceof SubmitNodeStorage storage) {
         NativeWorldScene.draw(storage, level.cameraRenderState.pos);
      }
   }
}
