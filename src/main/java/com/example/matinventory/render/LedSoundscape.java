package com.example.matinventory.render;

import com.example.matinventory.MatSounds;
import com.example.matinventory.config.InventorySound;
import com.example.matinventory.scene.MatCamera;
import java.util.ArrayList;
import java.util.function.BooleanSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public final class LedSoundscape {
   private final ArrayList<LedSoundscape.Pulse> pulses = new ArrayList<>();
   private LedSoundscape.Loop loop;
   private Screen owner;
   private BooleanSupplier enabled = () -> false;

   public void sync(Screen owner, BooleanSupplier enabled) {
      Minecraft client = Minecraft.getInstance();
      if (enabled.getAsBoolean() && client.gui.screen() == owner) {
         this.owner = owner;
         this.enabled = enabled;
         if (InventorySound.LED_AMBIENT.enabled() && (this.loop == null || this.loop.isStopped())) {
            this.loop = new LedSoundscape.Loop(() -> client.gui.screen() == owner && enabled.getAsBoolean());
            client.getSoundManager().play(this.loop);
         }

         long now = System.nanoTime();
         this.pulses.removeIf(p -> now - p.at > 2000000000L && !client.getSoundManager().isActive(p.sound));
      } else {
         this.stop();
      }
   }

   public void flash(MatCamera camera, float x, float z) {
      if (InventorySound.LED_FLICKER.enabled()) {
         Minecraft client = Minecraft.getInstance();
         Vector4f point = camera.project(x, 0.17F, z);
         float pan = Math.clamp((point.x / Math.max(1, camera.width) * 2.0F - 1.0F) * 0.85F, -0.85F, 0.85F);
         Screen activeOwner = this.owner;
         BooleanSupplier power = this.enabled;
         LedSoundscape.Flash sound = new LedSoundscape.Flash(() -> client.gui.screen() == activeOwner && power.getAsBoolean(), pan);
         this.pulses.add(new LedSoundscape.Pulse(sound, System.nanoTime()));
         client.getSoundManager().play(sound);
      }
   }

   public void stop() {
      SoundManager manager = Minecraft.getInstance().getSoundManager();
      if (this.loop != null) {
         this.loop.halt();
         manager.stop(this.loop);
         this.loop = null;
      }

      for (LedSoundscape.Pulse pulse : this.pulses) {
         if (pulse.sound instanceof LedSoundscape.Flash flash) {
            flash.halt();
         }

         manager.stop(pulse.sound);
      }

      this.pulses.clear();
   }

   @Environment(EnvType.CLIENT)
   private static final class Flash extends AbstractTickableSoundInstance {
      private final BooleanSupplier active;
      private final long end = System.nanoTime() + 1000000000L;

      Flash(BooleanSupplier active, float pan) {
         super(MatSounds.LED_FLICKER, SoundSource.AMBIENT, RandomSource.create());
         this.active = active;
         this.volume = 0.045F;
         this.pitch = 0.97F + this.random.nextFloat() * 0.06F;
         this.x = pan;
         this.z = -1.0;
         this.relative = true;
         this.attenuation = Attenuation.NONE;
      }

      @Override
      public boolean canPlaySound() {
         return !this.isStopped() && this.active.getAsBoolean() && InventorySound.LED_FLICKER.enabled();
      }

      @Override
      public void tick() {
         if (!this.active.getAsBoolean() || System.nanoTime() > this.end || !InventorySound.LED_FLICKER.enabled()) {
            this.stop();
         }
      }

      void halt() {
         this.stop();
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Loop extends AbstractTickableSoundInstance {
      private final BooleanSupplier active;

      Loop(BooleanSupplier active) {
         super(MatSounds.LED_AMBIENT, SoundSource.AMBIENT, RandomSource.create());
         this.active = active;
         this.volume = 0.075F;
         this.pitch = 1.0F;
         this.looping = true;
         this.delay = 0;
         this.relative = true;
         this.attenuation = Attenuation.NONE;
      }

      @Override
      public void tick() {
         if (!this.active.getAsBoolean() || !InventorySound.LED_AMBIENT.enabled()) {
            this.stop();
         }
      }

      @Override
      public boolean canPlaySound() {
         return !this.isStopped() && this.active.getAsBoolean() && InventorySound.LED_AMBIENT.enabled();
      }

      void halt() {
         this.stop();
      }
   }

   @Environment(EnvType.CLIENT)
   private record Pulse(SoundInstance sound, long at) {
   }
}
