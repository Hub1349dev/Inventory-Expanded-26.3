package com.example.matinventory.mixin;

import com.example.matinventory.config.InventorySound;
import com.example.matinventory.screen.InventorySettingsScreen;
import com.example.matinventory.screen.MatInventoryScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine.PlayResult;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(SoundManager.class)
abstract class InventoryButtonSoundMixin {
   @Inject(method = "play", at = @At("HEAD"), cancellable = true)
   private void matinventory$noVanillaButton(SoundInstance sound, CallbackInfoReturnable<PlayResult> cir) {
      if (InventorySound.allows(sound.getIdentifier())
         && (
            !(Minecraft.getInstance().gui.screen() instanceof InventorySettingsScreen)
               || InventorySound.BUTTONS.enabled()
               || !sound.getIdentifier().equals(SoundEvents.UI_BUTTON_CLICK.value().location())
         )) {
         if (Minecraft.getInstance().gui.screen() instanceof MatInventoryScreen && sound.getIdentifier().equals(SoundEvents.UI_BUTTON_CLICK.value().location())) {
            cir.setReturnValue(PlayResult.NOT_STARTED);
         }
      } else {
         cir.setReturnValue(PlayResult.NOT_STARTED);
      }
   }
}
