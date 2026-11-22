package com.example.matinventory.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.MouseButtonEvent;

@Environment(EnvType.CLIENT)
public final class MatInput {
   public static int button(MouseButtonEvent event) {
      return switch (event.button()) {
         case InputConstants.MOUSE_BUTTON_LEFT -> 0;
         case InputConstants.MOUSE_BUTTON_RIGHT -> 1;
         case InputConstants.MOUSE_BUTTON_MIDDLE -> 2;
         default -> event.button() - 1;
      };
   }

   public static int physical(int button) {
      return switch (button) {
         case 0 -> InputConstants.MOUSE_BUTTON_LEFT;
         case 1 -> InputConstants.MOUSE_BUTTON_RIGHT;
         case 2 -> InputConstants.MOUSE_BUTTON_MIDDLE;
         default -> button + 1;
      };
   }

   private MatInput() {
   }
}
