package com.example.matinventory.render;

import com.example.matinventory.MatSounds;
import com.example.matinventory.animation.MatAnimation;
import com.example.matinventory.arrangement.ItemCategoryClassifier;
import com.example.matinventory.config.InventorySound;
import com.example.matinventory.layout.MatLayout;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public final class ItemSoundscape {
   private final long[] last = new long[4];
   private final BitSet landed = new BitSet(512);
   private final List<SimpleSoundInstance> playing = new ArrayList<>();
   private int hovered = -1;
   private long hoverAt;
   private long hoverSince;
   private boolean hoverPlayed;

   public static SoundEvent sound(ItemStack stack) {
      return switch (sector(stack)) {
         case 0 -> MatSounds.SEARCH_BLOCKS;
         case 1 -> MatSounds.SEARCH_TOOLS;
         case 2 -> MatSounds.SEARCH_CLOTHING;
         default -> MatSounds.SEARCH_CRAFT_FOOD;
      };
   }

   private static int sector(ItemStack stack) {
      return switch (ItemCategoryClassifier.classify(stack).category()) {
         case BLOCKS -> 0;
         case LONG_TOOLS, VEHICLES -> 1;
         case EQUIPMENT -> 2;
         default -> 3;
      };
   }

   public void hover(int slot, ItemStack stack) {
      long now = System.nanoTime();
      if (slot != this.hovered) {
         this.hovered = slot;
         this.hoverSince = now;
         this.hoverPlayed = false;
      }

      if (!this.hoverPlayed && slot >= 0 && !stack.isEmpty() && now - this.hoverSince >= 75000000L && now - this.hoverAt >= 180000000L) {
         this.hoverAt = now;
         this.hoverPlayed = true;
         if (InventorySound.HOVER.enabled()) {
            this.play(stack, 0.12650001F, 0.99F);
         }
      }
   }

   public void pickup(ItemStack stack) {
      if (InventorySound.PICKUP.enabled()) {
         this.play(stack, 0.264F, 1.04F);
      }
   }

   public void place(ItemStack stack) {
      if (InventorySound.PLACE.enabled()) {
         this.play(stack, 0.23650001F, 0.98F);
      }
   }

   public void land(AbstractContainerMenu menu, MatLayout layout, MatAnimation animation) {
      for (MatLayout.Place p : layout.places()) {
         if (p.visible && p.occupied && !this.landed.get(p.slot()) && animation.itemVisible(p.x()) && animation.dropAt(p.x()) < 5.0E-4F) {
            this.landed.set(p.slot());
            ItemStack stack = menu.getSlot(p.slot()).getItem();
            int s = sector(stack);
            long now = System.nanoTime();
            if (!stack.isEmpty() && now - this.last[s] > 90000000L) {
               this.last[s] = now;
               if (InventorySound.LAND.enabled()) {
                  this.play(stack, 0.27F, 1.0F);
               }
            }
         }
      }
   }

   private void play(ItemStack stack, float volume, float pitch) {
      if (!stack.isEmpty()) {
         SoundManager manager = Minecraft.getInstance().getSoundManager();
         this.playing.removeIf(s -> !manager.isActive(s));
         if (this.playing.size() >= 4) {
            manager.stop(this.playing.removeFirst());
         }

         SimpleSoundInstance sound = SimpleSoundInstance.forUI(sound(stack), pitch, volume * 0.4F);
         this.playing.add(sound);
         manager.play(sound);
      }
   }

   public void stop() {
      SoundManager manager = Minecraft.getInstance().getSoundManager();

      for (SimpleSoundInstance sound : this.playing) {
         manager.stop(sound);
      }

      this.playing.clear();
      this.hovered = -1;
      this.landed.clear();
   }
}
