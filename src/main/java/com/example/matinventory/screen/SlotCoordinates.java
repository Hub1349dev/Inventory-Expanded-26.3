package com.example.matinventory.screen;

import com.example.matinventory.mixin.GuiSlotPosition;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.inventory.Slot;

@Environment(EnvType.CLIENT)
final class SlotCoordinates {
   static void move(Slot slot, int x, int y) {
      GuiSlotPosition position = (GuiSlotPosition)slot;
      position.matinventory$x(x);
      position.matinventory$y(y);
   }

   private SlotCoordinates() {
   }

   @Environment(EnvType.CLIENT)
   record Point(int x, int y) {
   }
}
