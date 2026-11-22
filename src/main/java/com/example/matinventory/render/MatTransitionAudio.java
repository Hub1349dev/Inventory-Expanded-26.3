package com.example.matinventory.render;

import com.example.matinventory.MatSounds;
import com.example.matinventory.animation.MatAnimation;
import com.example.matinventory.config.InventorySound;
import java.util.ArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

@Environment(EnvType.CLIENT)
public final class MatTransitionAudio {
   private boolean dropped;
   private boolean opened;
   private boolean closed;
   private final ArrayList<MatTransitionAudio.Clip> playing = new ArrayList<>();

   public void update(Screen owner, MatAnimation animation) {
      float time = animation.openingClock();
      SoundManager manager = Minecraft.getInstance().getSoundManager();

      for (MatTransitionAudio.Clip clip : this.playing) {
         if (!animation.closing()
            && (clip.event == MatSounds.INVENTORY_OPEN && animation.matOpen() == 1.0F || clip.event == MatSounds.MAT_DROP && time >= 0.09F)) {
            clip.halt();
         }

         clip.update();
         if (clip.isStopped()) {
            manager.stop(clip);
         }
      }

      this.playing.removeIf(AbstractTickableSoundInstance::isStopped);
      if (animation.closing()) {
         if (animation.closingMat() && !this.closed) {
            this.closed = true;
            this.play(owner, MatSounds.INVENTORY_CLOSE, animation.closeMotionRemaining(), 0.7F);
         }
      } else if (time >= 0.0F) {
         if (!this.dropped) {
            this.dropped = true;
            if (time < 0.09F) {
               this.play(owner, MatSounds.MAT_DROP, 0.09F - time, 0.7F);
            }
         }

         if (time >= 0.09F && !this.opened) {
            this.opened = true;
            float end = 0.68090904F;
            this.play(owner, MatSounds.INVENTORY_OPEN, end - time, 0.7F);
         }
      }
   }

   private void play(Screen owner, SoundEvent event, float seconds, float volume) {
      if (!(seconds <= 0.001F) && InventorySound.allows(event.location())) {
         MatTransitionAudio.Clip clip = new MatTransitionAudio.Clip(owner, event, seconds, volume);
         this.playing.add(clip);
         Minecraft.getInstance().getSoundManager().play(clip);
      }
   }

   public void beginClosing() {
      this.stop();
      this.closed = false;
   }

   public void stop() {
      SoundManager manager = Minecraft.getInstance().getSoundManager();

      for (MatTransitionAudio.Clip clip : this.playing) {
         clip.halt();
         manager.stop(clip);
      }

      this.playing.clear();
   }

   @Environment(EnvType.CLIENT)
   private static final class Clip extends AbstractTickableSoundInstance {
      private final Screen owner;
      private final SoundEvent event;
      private final long end;
      private final float gain;

      Clip(Screen owner, SoundEvent event, float seconds, float gain) {
         super(event, SoundSource.MASTER, RandomSource.create());
         this.owner = owner;
         this.event = event;
         this.gain = gain;
         this.end = System.nanoTime() + (long)(seconds * 1.0E9);
         this.volume = gain;
         this.pitch = 1.0F;
         this.relative = true;
         this.attenuation = Attenuation.NONE;
      }

      @Override
      public boolean canPlaySound() {
         return !this.isStopped() && Minecraft.getInstance().gui.screen() == this.owner;
      }

      @Override
      public void tick() {
         this.update();
      }

      void update() {
         long left = this.end - System.nanoTime();
         if (left > 0L && Minecraft.getInstance().gui.screen() == this.owner) {
            this.volume = this.gain * Math.clamp((float)left / 3.5E7F, 0.0F, 1.0F);
         } else {
            this.stop();
         }
      }

      void halt() {
         this.stop();
      }
   }
}
